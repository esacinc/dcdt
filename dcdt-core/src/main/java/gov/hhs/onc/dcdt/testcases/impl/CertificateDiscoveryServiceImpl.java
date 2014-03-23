package gov.hhs.onc.dcdt.testcases.impl;

import gov.hhs.onc.dcdt.beans.impl.AbstractToolBean;
import gov.hhs.onc.dcdt.dns.DnsRecordType;
import gov.hhs.onc.dcdt.mail.BindingType;
import gov.hhs.onc.dcdt.mail.MailAddress;
import gov.hhs.onc.dcdt.testcases.CertificateDiscoveryService;
import gov.hhs.onc.dcdt.testcases.steps.ToolTestcaseCertificateStep;
import gov.hhs.onc.dcdt.testcases.results.ToolTestcaseCertificateResultType;
import gov.hhs.onc.dcdt.testcases.steps.ToolTestcaseDnsLookupStep;
import gov.hhs.onc.dcdt.testcases.results.ToolTestcaseResultException;
import gov.hhs.onc.dcdt.testcases.steps.ToolTestcaseStep;
import org.springframework.stereotype.Component;
import java.util.ArrayList;
import java.util.List;

@Component("certificateDiscoveryServiceImpl")
public class CertificateDiscoveryServiceImpl extends AbstractToolBean implements CertificateDiscoveryService {
    @Override
    public List<ToolTestcaseStep> runCertificateDiscoverySteps(MailAddress directAddr, List<ToolTestcaseStep> certDiscoverySteps)
        throws ToolTestcaseResultException {
        List<ToolTestcaseStep> infoSteps = new ArrayList<>(certDiscoverySteps.size());
        ToolTestcaseStep prevStep = null;

        for (ToolTestcaseStep infoStep : certDiscoverySteps) {
            infoSteps.add(infoStep);

            boolean successful = false;

            if (infoStep.getBindingType() != BindingType.ADDRESS || directAddr.getBindingType() != BindingType.DOMAIN) {
                successful = infoStep.execute(directAddr, prevStep);
                prevStep = infoStep;
            }

            infoStep.setSuccessful(successful);

            switch (infoStep.getResultType()) {
                case CERT_LOOKUP:
                    ToolTestcaseCertificateStep certInfoStep = ((ToolTestcaseCertificateStep) infoStep);
                    if (successful && certInfoStep.hasCertificateInfo()
                        || (!certInfoStep.hasCertificateInfo() && certInfoStep.getCertificateStatus() != ToolTestcaseCertificateResultType.NO_CERT)) {
                        return infoSteps;
                    }
                    break;
                case DNS_LOOKUP:
                    if (!successful && ((ToolTestcaseDnsLookupStep) infoStep).getDnsRecordType() == DnsRecordType.SRV) {
                        return infoSteps;
                    }
                    break;
                case LDAP_CONNECTION:
                    if (!successful) {
                        return infoSteps;
                    }
                    break;
                default:
                    break;
            }
        }

        return infoSteps;
    }
}
