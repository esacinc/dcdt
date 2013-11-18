package gov.hhs.onc.dcdt.testcases.discovery.impl;


import gov.hhs.onc.dcdt.testcases.discovery.DiscoveryTestcase;
import gov.hhs.onc.dcdt.testcases.discovery.DiscoveryTestcaseCertificate;
import gov.hhs.onc.dcdt.testcases.discovery.DiscoveryTestcaseResult;
import gov.hhs.onc.dcdt.testcases.impl.AbstractToolTestcase;
import java.util.List;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;

@Entity(name = "testcase_discovery")
@Table(name = "testcases_discovery")
public class DiscoveryTestcaseImpl extends AbstractToolTestcase<DiscoveryTestcaseResult> implements DiscoveryTestcase {
    @Transient
    private List<DiscoveryTestcaseCertificate> certs;
    
    @Transient
    private String mailAddr;

    @Override
    public List<DiscoveryTestcaseCertificate> getCertificates() {
        return this.certs;
    }

    @Override
    public void setCertificates(List<DiscoveryTestcaseCertificate> certs) {
        this.certs = certs;
    }

    @Override
    public String getMailAddress() {
        return this.mailAddr;
    }

    @Override
    public void setMailAddress(String mailAddr) {
        this.mailAddr = mailAddr;
    }
}
