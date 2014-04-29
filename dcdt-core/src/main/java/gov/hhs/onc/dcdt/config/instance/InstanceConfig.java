package gov.hhs.onc.dcdt.config.instance;

import gov.hhs.onc.dcdt.beans.ToolDomainBean;
import java.net.InetAddress;
import javax.annotation.Nullable;

public interface InstanceConfig extends ToolDomainBean {
    public boolean isConfigured();

    public boolean hasIpAddress();

    @Nullable
    public InetAddress getIpAddress();

    public void setIpAddress(@Nullable InetAddress ipAddr);
}
