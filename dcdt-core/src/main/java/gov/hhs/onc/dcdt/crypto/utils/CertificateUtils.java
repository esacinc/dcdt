package gov.hhs.onc.dcdt.crypto.utils;

import java.io.*;
import java.math.BigInteger;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.SecureRandom;
import java.security.cert.*;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.StringUtils;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.GeneralNames;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.cert.CertIOException;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.crypto.params.AsymmetricKeyParameter;
import org.bouncycastle.crypto.util.PrivateKeyFactory;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.operator.DefaultDigestAlgorithmIdentifierFinder;
import org.bouncycastle.operator.DefaultSignatureAlgorithmIdentifierFinder;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.bc.BcRSAContentSignerBuilder;

import gov.hhs.onc.dcdt.crypto.CertificateInfo;
import gov.hhs.onc.dcdt.crypto.CertificateName;
import gov.hhs.onc.dcdt.crypto.CryptographyException;
import gov.hhs.onc.dcdt.crypto.constants.CertificateType;
import gov.hhs.onc.dcdt.crypto.constants.DataEncoding;
import gov.hhs.onc.dcdt.crypto.constants.PemType;

public abstract class CertificateUtils {
    private final static int SERIAL_NUM_SEED_SIZE = 8;

    public static X509Certificate generateCertificate(CertificateInfo cert) throws CryptographyException {
        X509Certificate certificate;

        try {
            AlgorithmIdentifier sigAlgId = new DefaultSignatureAlgorithmIdentifierFinder().find(cert.getSigAlgName());
            AlgorithmIdentifier digAlgId = new DefaultDigestAlgorithmIdentifierFinder().find(sigAlgId);
            PrivateKey signerKey = cert.getIssuerPrivateKey();
            AsymmetricKeyParameter keyParam = PrivateKeyFactory.createKey(signerKey.getEncoded());

            X509v3CertificateBuilder certGen = generateCertBuilder(cert);
            X509CertificateHolder certHolder = certGen.build(new BcRSAContentSignerBuilder(sigAlgId, digAlgId).build(keyParam));

            certificate = (X509Certificate) CertificateFactory.getInstance(CertificateType.X509, BouncyCastleProvider.PROVIDER_NAME).generateCertificate(
                new ByteArrayInputStream(certHolder.getEncoded()));
        } catch (CertificateException | OperatorCreationException | IOException | NoSuchProviderException e) {
            throw new CryptographyException("Certificate is not valid", e);
        }

        return certificate;
    }

    private static X509v3CertificateBuilder generateCertBuilder(CertificateInfo cert) throws CertIOException {
        CertificateName issuer = cert.getIssuer();

        X509v3CertificateBuilder certGen = new X509v3CertificateBuilder(issuer.toX500Name(), cert.getSerialNumber(), cert.getValidInterval().getNotBefore(),
            cert.getValidInterval().getNotAfter(), cert.getSubject().toX500Name(), cert.getPublicKey());

        certGen.addExtension(X509Extension.basicConstraints, false, new BasicConstraints(cert.getIsCa()));

        GeneralNames subjAltNames = cert.getSubject().getSubjAltNames();

        if (!cert.getIsCa()) {
            certGen.addExtension(X509Extension.authorityKeyIdentifier, false, cert.getAuthKeyId());
            certGen.addExtension(X509Extension.subjectKeyIdentifier, false, cert.getSubjKeyId());

            GeneralNames issuerAltNames = issuer.getSubjAltNames();

            if (issuerAltNames != null) {
                certGen.addExtension(X509Extension.issuerAlternativeName, false, issuerAltNames);
            }
        }

        if (subjAltNames != null) {
            certGen.addExtension(X509Extension.subjectAlternativeName, false, subjAltNames);
        }
        return certGen;
    }

    public static X509Certificate readCertificate(InputStream stream, String encoding) throws CryptographyException {
        try {
            return readCertificate(IOUtils.toByteArray(stream), encoding);
        } catch (IOException e) {
            throw new CryptographyException("Unable to read X509 certificate from stream: encoding=" + encoding, e);
        }
    }

    public static X509Certificate readCertificate(File file, String encoding) throws CryptographyException {
        try {
            return readCertificate(new FileInputStream(file), encoding);
        } catch (IOException e) {
            throw new CryptographyException("Unable to read X509 certificate from file (" + file + "): encoding=" + encoding, e);
        }
    }

    public static X509Certificate readCertificate(byte[] data, String encoding) throws CryptographyException {
        try {
            switch (StringUtils.lowerCase(encoding)) {
                case DataEncoding.PEM:
                    data = CryptographyUtils.readPemContent(new ByteArrayInputStream(data));

                case DataEncoding.DER:
                    Certificate cert = getX509CertFactory().generateCertificate(new ByteArrayInputStream(data));

                    if (!(cert instanceof X509Certificate)) {
                        throw new CryptographyException("Certificate (type=" + cert.getType() + ") is not a X509 certificate: " + cert);
                    }

                    return (X509Certificate) cert;
                default:
                    throw new CryptographyException("Unknown X509 certificate data encoding: " + encoding);
            }
        } catch (CertificateException e) {
            throw new CryptographyException("Unable to read X509 certificate from data (length=" + ArrayUtils.getLength(data) + "): encoding=" + encoding, e);
        }
    }

    public static void writeCertificate(File file, X509Certificate cert, String encoding) throws CryptographyException {
        try {
            writeCertificate(new FileOutputStream(file), cert, encoding);
        } catch (IOException e) {
            throw new CryptographyException("Unable to write X509 certificate to file (" + file + "): encoding=" + encoding, e);
        }
    }

    public static void writeCertificate(OutputStream stream, X509Certificate cert, String encoding) throws CryptographyException {
        try {
            byte[] data = cert.getEncoded();

            switch (StringUtils.lowerCase(encoding)) {
                case DataEncoding.PEM:
                    CryptographyUtils.writePemContent(stream, PemType.X509_CERTIFICATE, data);
                    break;

                case DataEncoding.DER:
                    IOUtils.write(data, stream);
                    break;

                default:
                    throw new CryptographyException("Unknown X509 certificate data encoding: " + encoding);
            }
        } catch (CertificateEncodingException | IOException e) {
            throw new CryptographyException("Unable to write X509 certificate to stream: encoding=" + encoding, e);
        }
    }

    public static CertificateFactory getX509CertFactory() throws CryptographyException {
        try {
            return CertificateFactory.getInstance(CertificateType.X509, CryptographyUtils.BOUNCY_CASTLE_PROVIDER);
        } catch (CertificateException e) {
            throw new CryptographyException("Unable to get X509 certificate factory (type=" + CertificateType.X509
                + ") instance from BouncyCastle provider (name= " + CryptographyUtils.BOUNCY_CASTLE_PROVIDER.getName() + " ).", e);
        }
    }

    public static BigInteger generateSerialNum(Set<BigInteger> existingSerialNums) throws CryptographyException {
        SecureRandom rand = CryptographyUtils.getRandom(SERIAL_NUM_SEED_SIZE);
        BigInteger serialNum;

        do {
            serialNum = BigInteger.valueOf(rand.nextLong()).abs();
        } while (existingSerialNums != null && existingSerialNums.contains(serialNum));

        if (existingSerialNums != null) {
            existingSerialNums.add(serialNum);
        }

        return serialNum;
    }
}
