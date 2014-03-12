package gov.hhs.onc.dcdt.mail;

import gov.hhs.onc.dcdt.mail.utils.ToolMailContentTypeUtils;
import gov.hhs.onc.dcdt.utils.ToolStringUtils;
import javax.mail.internet.ContentType;
import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import org.springframework.util.MimeTypeUtils;

public final class MailContentTypes {
    public final static String DELIM_TYPE = "/";
    public final static String DELIM_PARAM = "; ";
    public final static String DELIM_PARAM_VALUE = "=";

    public final static String APP_TYPE = "application";

    public final static String NAME_PARAM_NAME = "name";
    public final static String NAME_SMIME_MIME_PARAM_VALUE = ToolStringUtils.quote("smime.p7m");
    public final static Pair<String, String> NAME_SMIME_MIME_PARAM = new ImmutablePair<>(NAME_PARAM_NAME, NAME_SMIME_MIME_PARAM_VALUE);
    public final static String NAME_SMIME_SIG_PARAM_VALUE = ToolStringUtils.quote("smime.p7s");
    public final static Pair<String, String> NAME_SMIME_SIG_PARAM = new ImmutablePair<>(NAME_PARAM_NAME, NAME_SMIME_SIG_PARAM_VALUE);

    public final static String SMIME_TYPE_PARAM_NAME = "smime-type";
    public final static String SMIME_TYPE_ENV_DATA_PARAM_VALUE = "enveloped-data";
    public final static Pair<String, String> SMIME_TYPE_ENV_DATA_PARAM = new ImmutablePair<>(SMIME_TYPE_PARAM_NAME, SMIME_TYPE_ENV_DATA_PARAM_VALUE);
    public final static String SMIME_TYPE_SIGNED_DATA_PARAM_VALUE = "signed-data";
    public final static Pair<String, String> SMIME_TYPE_SIGNED_DATA_PARAM = new ImmutablePair<>(SMIME_TYPE_PARAM_NAME, SMIME_TYPE_SIGNED_DATA_PARAM_VALUE);

    public final static String APP_PKCS7_MIME_SUBTYPE = "pkcs7-mime";
    public final static ContentType APP_PKCS7_MIME_ENV = ToolMailContentTypeUtils.getContentType(APP_TYPE, APP_PKCS7_MIME_SUBTYPE, SMIME_TYPE_ENV_DATA_PARAM,
        NAME_SMIME_MIME_PARAM);
    public final static ContentType APP_PKCS7_MIME_SIGNED = ToolMailContentTypeUtils.getContentType(APP_TYPE, APP_PKCS7_MIME_SUBTYPE,
        SMIME_TYPE_SIGNED_DATA_PARAM);

    public final static String APP_X_PKCS7_MIME_SUBTYPE = "x-" + APP_PKCS7_MIME_SUBTYPE;
    public final static ContentType APP_X_PKCS7_MIME_ENV = ToolMailContentTypeUtils.getContentType(APP_TYPE, APP_X_PKCS7_MIME_SUBTYPE,
        SMIME_TYPE_ENV_DATA_PARAM, NAME_SMIME_MIME_PARAM);
    public final static ContentType APP_X_PKCS7_MIME_SIGNED = ToolMailContentTypeUtils.getContentType(APP_TYPE, APP_X_PKCS7_MIME_SUBTYPE,
        SMIME_TYPE_SIGNED_DATA_PARAM);

    public final static String APP_PKCS7_SIG_SUBTYPE = "pkcs7-signature";
    public final static ContentType APP_PKCS7_SIG = ToolMailContentTypeUtils.getContentType(APP_TYPE, APP_PKCS7_SIG_SUBTYPE, NAME_SMIME_SIG_PARAM);

    public final static String APP_X_PKCS7_SIG_SUBTYPE = "x-" + APP_PKCS7_SIG_SUBTYPE;
    public final static ContentType APP_X_PKCS7_SIG = ToolMailContentTypeUtils.getContentType(APP_TYPE, APP_X_PKCS7_SIG_SUBTYPE, NAME_SMIME_SIG_PARAM);

    public final static String MULTIPART_TYPE = "multipart";
    public final static ContentType MULTIPART = ToolMailContentTypeUtils.getContentType(MULTIPART_TYPE);

    public final static String MULTIPART_MIXED_SUBTYPE = "mixed";
    public final static ContentType MULTIPART_MIXED = ToolMailContentTypeUtils.getContentType(MULTIPART_TYPE, MULTIPART_MIXED_SUBTYPE);

    public final static String MULTIPART_SIGNED_SUBTYPE = "signed";
    public final static String MULTIPART_SIGNED_PROTOCOL_PARAM_NAME = "protocol";
    public final static String MULTIPART_SIGNED_PROTOCOL_X_PKCS7_SIG_PARAM_VALUE = ToolStringUtils.quote(APP_X_PKCS7_SIG.getBaseType());
    public final static Pair<String, String> MULTIPART_SIGNED_PROTOCOL_X_PKCS7_SIG_PARAM = new ImmutablePair<>(MULTIPART_SIGNED_PROTOCOL_PARAM_NAME,
        MULTIPART_SIGNED_PROTOCOL_X_PKCS7_SIG_PARAM_VALUE);
    public final static ContentType MULTIPART_SIGNED = ToolMailContentTypeUtils.getContentType(MULTIPART_TYPE, MULTIPART_SIGNED_SUBTYPE,
        MULTIPART_SIGNED_PROTOCOL_X_PKCS7_SIG_PARAM);
    public final static String MULTIPART_SIGNED_MSG_DIGEST_ALG_PARAM_NAME = "micalg";

    public final static ContentType TEXT_HTML = ToolMailContentTypeUtils.getContentType(MimeTypeUtils.TEXT_HTML);

    public final static ContentType TEXT_PLAIN = ToolMailContentTypeUtils.getContentType(MimeTypeUtils.TEXT_PLAIN);

    private MailContentTypes() {
    }
}
