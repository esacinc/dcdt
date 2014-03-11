package gov.hhs.onc.dcdt.crypto.mail.utils;

import gov.hhs.onc.dcdt.crypto.certs.CertificateInfo;
import gov.hhs.onc.dcdt.crypto.certs.impl.CertificateInfoImpl;
import gov.hhs.onc.dcdt.crypto.mail.MailCryptographyException;
import gov.hhs.onc.dcdt.crypto.mail.MimeMessageWrapper;
import gov.hhs.onc.dcdt.crypto.mail.MimeType;
import gov.hhs.onc.dcdt.mail.EmailInfo;
import gov.hhs.onc.dcdt.utils.ToolClassUtils;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.security.cert.CertificateException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import javax.mail.Address;
import javax.mail.MessagingException;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;
import org.apache.log4j.Logger;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSException;
import org.bouncycastle.cms.KeyTransRecipientId;
import org.bouncycastle.cms.KeyTransRecipientInformation;
import org.bouncycastle.cms.RecipientInformation;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.mail.smime.SMIMEEnveloped;
import org.bouncycastle.mail.smime.SMIMESigned;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.util.Store;

public abstract class MailCryptographyUtils {
    private final static Properties MAIL_SESSION_PROPS = new Properties();
    private final static Session MAIL_SESSION = Session.getDefaultInstance(MAIL_SESSION_PROPS);

    private final static Logger LOGGER = Logger.getLogger(MailCryptographyUtils.class);

    public static Session getMailSession() {
        return MAIL_SESSION;
    }

    @SuppressWarnings("unchecked")
    public static Map<BigInteger, KeyTransRecipientInformation> getRecipients(SMIMEEnveloped envelopedMsg) throws MailCryptographyException {
        Collection<RecipientInformation> recipientsStore = (Collection<RecipientInformation>) envelopedMsg.getRecipientInfos().getRecipients();
        Map<BigInteger, KeyTransRecipientInformation> recipientsMap = new LinkedHashMap<>(recipientsStore.size());

        for (RecipientInformation recipient : recipientsStore) {
            if (!KeyTransRecipientInformation.class.isAssignableFrom(recipient.getClass())) {
                throw new MailCryptographyException(String.format("Recipient (class=%s) is not an instance of (class=%s).", ToolClassUtils.getName(recipient),
                    ToolClassUtils.getName(KeyTransRecipientInformation.class)));
            }
            recipientsMap.put(((KeyTransRecipientId) recipient.getRID()).getSerialNumber(), (KeyTransRecipientInformation) recipient);
        }

        return recipientsMap;
    }

    public static MimeMessage getMimeMessage(InputStream msgInStream) throws MailCryptographyException {
        try {
            MimeMessage mimeMessage = new MimeMessage(MailCryptographyUtils.getMailSession(), msgInStream);

            if (!mimeMessage.isMimeType(MimeType.PKCS7.getType()) && !mimeMessage.isMimeType(MimeType.X_PKCS7.getType())) {
                throw new MailCryptographyException(String.format("Invalid Content-Type (%s), expected %s or %s.", mimeMessage.getContentType(),
                    MimeType.PKCS7.getType(), MimeType.X_PKCS7.getType()));
            }

            return mimeMessage;
        } catch (MessagingException e) {
            throw new MailCryptographyException(String.format("Unable to convert input stream (class=%s) to message (class=%s).",
                ToolClassUtils.getName(msgInStream), ToolClassUtils.getName(MimeMessage.class)), e);
        }
    }

    public static void parseMessageHeaders(MimeMessage mimeMsg, EmailInfo emailInfo) throws MailCryptographyException {
        try {
            Address[] fromAddrs = mimeMsg.getFrom();
            emailInfo.setFromAddress(((InternetAddress) fromAddrs[0]).getAddress());

            Address[] toAddrs = mimeMsg.getAllRecipients();
            emailInfo.setToAddress(((InternetAddress) toAddrs[0]).getAddress());

            emailInfo.setSubject(mimeMsg.getSubject());
        } catch (MessagingException e) {
            throw new MailCryptographyException("Unable to parse headers from message.", e);
        }
    }

    public static SMIMEEnveloped getEnvelopedMsg(MimeMessage mimeMsg) throws MailCryptographyException {
        try {
            return new SMIMEEnveloped(mimeMsg);
        } catch (CMSException | MessagingException e) {
            throw new MailCryptographyException(String.format("Unable to get enveloped message from encrypted message (class=%s).",
                ToolClassUtils.getName(MimeMessage.class)), e);
        }
    }

    public static MimeMessage findMessagePart(Multipart msgMultiPart, MimeMessage origMimeMsg) throws MailCryptographyException {
        try {
            for (int a = 0; a < msgMultiPart.getCount(); a++) {
                try {
                    String contentType = msgMultiPart.getBodyPart(a).getContentType();

                    if (!contentType.contains(MimeType.PKCS7_SIGNATURE.getType()) && !contentType.contains(MimeType.X_PKCS7_SIGNATURE.getType())) {
                        return createMimeMessage(msgMultiPart, origMimeMsg, a, contentType);
                    }
                } catch (IOException e) {
                    throw new MailCryptographyException(String.format("Unable to get message part (index=%d) body content.", a), e);
                }
            }
        } catch (MessagingException e) {
            throw new MailCryptographyException("Unable to get the number of message parts.", e);
        }

        return null;
    }

    public static MimeMessage createMimeMessage(Multipart msgMultiPart, MimeMessage origMimeMsg, int a, String contentType) throws IOException,
        MessagingException {
        Object msgPart;
        msgPart = msgMultiPart.getBodyPart(a).getContent();
        MimeMessageWrapper newMsg = new MimeMessageWrapper(origMimeMsg);

        if (msgPart instanceof Multipart) {
            newMsg.setContent((Multipart) msgPart);
        } else {
            newMsg.setContent(msgPart, contentType);
        }

        newMsg.setMessageId(origMimeMsg.getMessageID());
        newMsg.saveChanges();

        LOGGER.debug(String.format("Found message after decryption of email (%s).", ToolMailCryptographyStringUtils.messageHeaderInfoToString(origMimeMsg)));

        return newMsg;
    }

    public static SMIMESigned findMailSignature(Multipart msgMultiPart) throws MailCryptographyException {
        String contentType = msgMultiPart.getContentType();

        if (contentType.contains(MimeType.MULTIPART_SIGNED.getType())
            && (contentType.contains(MimeType.PKCS7_SIGNATURE.getType()) || contentType.contains(MimeType.X_PKCS7_SIGNATURE.getType()))) {
            try {
                return new SMIMESigned((MimeMultipart) msgMultiPart);
            } catch (CMSException | MessagingException e) {
                throw new MailCryptographyException("Unable to find mail signature.", e);
            }
        }

        return null;
    }

    @SuppressWarnings("unchecked")
    public static CertificateInfo validateSignature(SMIMESigned signedEntity) throws MailCryptographyException {
        Store certStore = signedEntity.getCertificates();

        for (SignerInformation signerInfo : (Collection<SignerInformation>) signedEntity.getSignerInfos().getSigners()) {
            for (X509CertificateHolder certHolder : (Collection<X509CertificateHolder>) certStore.getMatches(signerInfo.getSID())) {
                try {
                    if (signerInfo.verify(new JcaSimpleSignerInfoVerifierBuilder().build(certHolder))) {
                        return new CertificateInfoImpl(new JcaX509CertificateConverter().getCertificate(certHolder));
                    }
                } catch (CMSException | OperatorCreationException | CertificateException ignored) {
                }
            }
        }

        throw new MailCryptographyException("Unable to validate signature.");
    }
}