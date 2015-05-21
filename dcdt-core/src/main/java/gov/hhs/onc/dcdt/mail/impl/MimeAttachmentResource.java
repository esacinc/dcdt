package gov.hhs.onc.dcdt.mail.impl;

import gov.hhs.onc.dcdt.mail.MailContentTransferEncoding;
import javax.annotation.Nullable;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.util.MimeType;

public class MimeAttachmentResource extends ByteArrayResource {
    private MimeType contentType;
    private MailContentTransferEncoding contentXferEnc;
    private String filename;

    public MimeAttachmentResource(byte[] content, @Nullable String desc, MimeType contentType, @Nullable MailContentTransferEncoding contentXferEnc,
        @Nullable String filename) {
        super(content, desc);

        this.contentType = contentType;
        this.contentXferEnc = contentXferEnc;
        this.filename = filename;
    }

    public boolean hasContentType() {
        return (this.contentType != null);
    }

    @Nullable
    public MimeType getContentType() {
        return this.contentType;
    }

    public boolean hasContentXferEncoding() {
        return (this.contentXferEnc != null);
    }

    @Nullable
    public MailContentTransferEncoding getContentXferEncoding() {
        return this.contentXferEnc;
    }

    public boolean hasDescription() {
        return !StringUtils.isBlank(this.getDescription());
    }

    public boolean hasFilename() {
        return !StringUtils.isBlank(this.filename);
    }

    @Nullable
    @Override
    public String getFilename() {
        return this.filename;
    }

    @Override
    @SuppressWarnings({ "EqualsWhichDoesntCheckParameterClass" })
    public boolean equals(Object obj) {
        return EqualsBuilder.reflectionEquals(this, obj, false);
    }

    @Override
    public int hashCode() {
        return HashCodeBuilder.reflectionHashCode(this, false);
    }
}
