package gov.hhs.onc.dcdt.config.impl;

import com.fasterxml.jackson.annotation.JsonTypeName;
import gov.hhs.onc.dcdt.beans.impl.AbstractToolDomainBean;
import gov.hhs.onc.dcdt.beans.utils.ToolBeanFactoryUtils;
import gov.hhs.onc.dcdt.config.InstanceDnsConfig;
import gov.hhs.onc.dcdt.crypto.CryptographyException;
import gov.hhs.onc.dcdt.crypto.DataEncoding;
import gov.hhs.onc.dcdt.crypto.credentials.CredentialInfo;
import gov.hhs.onc.dcdt.crypto.keys.KeyInfo;
import gov.hhs.onc.dcdt.crypto.utils.CertificateUtils;
import gov.hhs.onc.dcdt.dns.DnsException;
import gov.hhs.onc.dcdt.dns.DnsKeyAlgorithmType;
import gov.hhs.onc.dcdt.dns.DnsRecordType;
import gov.hhs.onc.dcdt.dns.DnsRuntimeException;
import gov.hhs.onc.dcdt.dns.config.ARecordConfig;
import gov.hhs.onc.dcdt.dns.config.CertRecordConfig;
import gov.hhs.onc.dcdt.dns.config.CnameRecordConfig;
import gov.hhs.onc.dcdt.dns.config.DnsRecordConfig;
import gov.hhs.onc.dcdt.dns.config.MxRecordConfig;
import gov.hhs.onc.dcdt.dns.config.NsRecordConfig;
import gov.hhs.onc.dcdt.dns.config.SoaRecordConfig;
import gov.hhs.onc.dcdt.dns.config.SrvRecordConfig;
import gov.hhs.onc.dcdt.dns.config.TargetedDnsRecordConfig;
import gov.hhs.onc.dcdt.dns.utils.ToolDnsNameUtils;
import gov.hhs.onc.dcdt.dns.utils.ToolDnsRecordUtils;
import gov.hhs.onc.dcdt.mail.MailAddress;
import gov.hhs.onc.dcdt.mail.ToolMailAddressException;
import gov.hhs.onc.dcdt.testcases.discovery.DiscoveryTestcase;
import gov.hhs.onc.dcdt.testcases.discovery.DiscoveryTestcase.DiscoveryTestcaseCredentialsExtractor;
import gov.hhs.onc.dcdt.testcases.discovery.credentials.DiscoveryTestcaseCredential;
import gov.hhs.onc.dcdt.testcases.discovery.credentials.DiscoveryTestcaseCredentialLocation;
import gov.hhs.onc.dcdt.utils.ToolArrayUtils;
import gov.hhs.onc.dcdt.utils.ToolCollectionUtils;
import gov.hhs.onc.dcdt.utils.ToolIteratorUtils;
import gov.hhs.onc.dcdt.utils.ToolMapUtils;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.IteratorUtils;
import org.apache.commons.collections4.Predicate;
import org.apache.commons.collections4.Transformer;
import org.apache.commons.lang3.tuple.MutablePair;
import org.springframework.beans.BeansException;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.AbstractApplicationContext;
import org.xbill.DNS.Name;
import org.xbill.DNS.Record;

@JsonTypeName("instanceDnsConfig")
public class InstanceDnsConfigImpl extends AbstractToolDomainBean implements InstanceDnsConfig {
    private class DiscoveryTestcaseCredentialCertRecordConfigTransformer implements Transformer<DiscoveryTestcaseCredential, CertRecordConfig> {
        @Override
        public CertRecordConfig transform(DiscoveryTestcaseCredential discoveryTestcaseCred) {
            try {
                CertRecordConfig certRecordConfig = ToolBeanFactoryUtils.createBeanOfType(InstanceDnsConfigImpl.this.appContext, CertRecordConfig.class);
                // noinspection ConstantConditions
                certRecordConfig.setName(ToolDnsNameUtils.toAbsolute(discoveryTestcaseCred.getLocation().getMailAddress().toAddressName()));

                // noinspection ConstantConditions
                KeyInfo discoveryTestcaseCredKeyInfo = discoveryTestcaseCred.getCredentialInfo().getKeyDescriptor();
                // noinspection ConstantConditions
                DnsKeyAlgorithmType discoveryTestcaseCredKeyAlgType = discoveryTestcaseCredKeyInfo.getKeyAlgorithm().getDnsAlgorithmType();
                certRecordConfig.setKeyAlgorithmType(discoveryTestcaseCredKeyAlgType);
                // noinspection ConstantConditions
                certRecordConfig.setKeyTag(ToolDnsRecordUtils.getKeyTag(discoveryTestcaseCredKeyAlgType, discoveryTestcaseCredKeyInfo.getPublicKey()));

                // noinspection ConstantConditions
                certRecordConfig.setCertificateData(CertificateUtils.writeCertificate(discoveryTestcaseCred.getCredentialInfo().getCertificateDescriptor()
                    .getCertificate(), DataEncoding.DER));

                return certRecordConfig;
            } catch (CryptographyException | DnsException | ToolMailAddressException e) {
                throw new DnsRuntimeException(String.format("Unable to transform Discovery testcase credential (name=%s) into DNS CERT record configuration.",
                    discoveryTestcaseCred.getName()), e);
            }
        }
    }

    private class DiscoveryTestcaseCredentialCertRecordPredicate implements Predicate<DiscoveryTestcaseCredential> {
        @Override
        public boolean evaluate(DiscoveryTestcaseCredential discoveryTestcaseCred) {
            DiscoveryTestcaseCredentialLocation discoveryTestcaseCredLoc;
            MailAddress discoveryTestcaseCredLocMailAddr;
            CredentialInfo discoveryTestcaseCredInfo;
            KeyInfo discoveryTestcaseCredKeyInfo;

            try {
                // noinspection ConstantConditions
                return discoveryTestcaseCred.hasLocation() && (discoveryTestcaseCredLoc = discoveryTestcaseCred.getLocation()).getType().isDns()
                    && discoveryTestcaseCredLoc.hasMailAddress()
                    && (discoveryTestcaseCredLocMailAddr = discoveryTestcaseCredLoc.getMailAddress()).getBindingType().isBound()
                    && ToolDnsNameUtils.toAbsolute(discoveryTestcaseCredLocMailAddr.getDomainName()).equals(InstanceDnsConfigImpl.this.domainName)
                    && discoveryTestcaseCred.hasCredentialInfo() && (discoveryTestcaseCredInfo = discoveryTestcaseCred.getCredentialInfo()).hasKeyDescriptor()
                    && (discoveryTestcaseCredKeyInfo = discoveryTestcaseCredInfo.getKeyDescriptor()).hasKeyAlgorithm()
                    && discoveryTestcaseCredKeyInfo.hasPublicKey() && discoveryTestcaseCredInfo.hasCertificateDescriptor()
                    && discoveryTestcaseCredInfo.getCertificateDescriptor().hasCertificate();
            } catch (DnsException | ToolMailAddressException e) {
                throw new DnsRuntimeException(String.format("Unable to evaluate whether Discovery testcase credential (name=%s) can be a DNS CERT record.",
                    discoveryTestcaseCred.getName()), e);
            }
        }
    }

    private AbstractApplicationContext appContext;
    private List<ARecordConfig> aRecordsConfigs;
    private List<CertRecordConfig> certRecordConfigs;
    private List<CnameRecordConfig> cnameRecordConfigs;
    private List<MxRecordConfig> mxRecordConfigs;
    private List<NsRecordConfig> nsRecordConfigs;
    private SoaRecordConfig soaRecordConfig;
    private List<SrvRecordConfig> srvRecordConfigs;

    @Override
    public <T extends Record> Collection<T> findAnswers(T questionRecord) {
        return ToolDnsRecordUtils.findAnswers(questionRecord, this.mapRecordConfigs().get(ToolDnsRecordUtils.findByType(questionRecord.getType())));
    }

    @Override
    public Map<DnsRecordType, List<? extends DnsRecordConfig<? extends Record>>> mapRecordConfigs() {
        return ToolMapUtils.putAll(new LinkedHashMap<DnsRecordType, List<? extends DnsRecordConfig<? extends Record>>>(DnsRecordType.values().length),
            new MutablePair<>(DnsRecordType.A, this.aRecordsConfigs), new MutablePair<>(DnsRecordType.CERT, this.certRecordConfigs), new MutablePair<>(
                DnsRecordType.CNAME, this.cnameRecordConfigs), new MutablePair<>(DnsRecordType.MX, this.mxRecordConfigs), new MutablePair<>(DnsRecordType.NS,
                this.nsRecordConfigs), new MutablePair<>(DnsRecordType.SOA, ToolArrayUtils.asList(this.soaRecordConfig)), new MutablePair<>(DnsRecordType.SRV,
                this.srvRecordConfigs));
    }

    @Override
    public boolean isAuthoritative(Record questionRecord) {
        return this.isAuthoritative() && questionRecord.getName().subdomain(this.domainName);
    }

    @Override
    public boolean isAuthoritative() {
        return this.hasDomainName() && this.hasSoaRecordConfig();
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        if (this.hasDomainName()) {
            this.domainName = ToolDnsNameUtils.toAbsolute(this.domainName);

            this.certRecordConfigs =
                ToolCollectionUtils.nullIfEmpty(CollectionUtils.collect(CollectionUtils.select(
                    IteratorUtils.asIterable(ToolIteratorUtils.chainedIterator(CollectionUtils.collect(
                        ToolBeanFactoryUtils.getBeansOfType(this.appContext, DiscoveryTestcase.class), DiscoveryTestcaseCredentialsExtractor.INSTANCE))),
                    new DiscoveryTestcaseCredentialCertRecordPredicate()), new DiscoveryTestcaseCredentialCertRecordConfigTransformer(),
                    new ArrayList<CertRecordConfig>()));

            Map<DnsRecordType, List<? extends DnsRecordConfig<? extends Record>>> recordConfigsMap = this.mapRecordConfigs();
            List<? extends DnsRecordConfig<? extends Record>> recordConfigs;
            Name recordName;
            TargetedDnsRecordConfig<? extends Record> targetedRecordConfig;
            SoaRecordConfig soaRecordConfig;

            for (DnsRecordType recordType : recordConfigsMap.keySet()) {
                if (CollectionUtils.isEmpty((recordConfigs = recordConfigsMap.get(recordType)))) {
                    continue;
                }

                for (DnsRecordConfig<? extends Record> recordConfig : recordConfigs) {
                    if (((recordName = recordConfig.getName()) == null) || !recordName.isAbsolute()) {
                        recordConfig.setName(ToolDnsNameUtils.toAbsolute(ToolDnsNameUtils.fromLabels(recordName, this.domainName)));
                    }

                    switch (recordType) {
                        case CNAME:
                        case MX:
                        case NS:
                        case SRV:
                            if ((targetedRecordConfig = (TargetedDnsRecordConfig<? extends Record>) recordConfig).getTarget() == null) {
                                targetedRecordConfig.setTarget(this.domainName);
                            }

                            targetedRecordConfig.setTarget(ToolDnsNameUtils.toAbsolute(targetedRecordConfig.getTarget()));
                            break;

                        case SOA:
                            (soaRecordConfig = ((SoaRecordConfig) recordConfig)).setAdmin(ToolDnsNameUtils.toAbsolute(soaRecordConfig.getAdmin()));
                            soaRecordConfig.setHost(ToolDnsNameUtils.toAbsolute(soaRecordConfig.getHost()));
                            break;
                    }
                }
            }
        }
    }

    @Override
    public void setApplicationContext(ApplicationContext appContext) throws BeansException {
        this.appContext = (AbstractApplicationContext) appContext;
    }

    @Override
    public boolean hasARecordConfigs() {
        return !CollectionUtils.isEmpty(this.aRecordsConfigs);
    }

    @Nullable
    @Override
    public List<ARecordConfig> getARecordConfigs() {
        return this.aRecordsConfigs;
    }

    @Override
    public void setARecordConfigs(@Nullable List<ARecordConfig> aRecordConfigs) {
        this.aRecordsConfigs = aRecordConfigs;
    }

    @Override
    public boolean hasCertRecordConfigs() {
        return !CollectionUtils.isEmpty(this.certRecordConfigs);
    }

    @Nullable
    @Override
    public List<CertRecordConfig> getCertRecordConfigs() {
        return this.certRecordConfigs;
    }

    @Override
    public boolean hasCnameRecordConfigs() {
        return !CollectionUtils.isEmpty(this.cnameRecordConfigs);
    }

    @Nullable
    @Override
    public List<CnameRecordConfig> getCnameRecordConfigs() {
        return this.cnameRecordConfigs;
    }

    @Override
    public void setCnameRecordConfigs(@Nullable List<CnameRecordConfig> cnameRecordConfigs) {
        this.cnameRecordConfigs = cnameRecordConfigs;
    }

    @Override
    public boolean hasMxRecordConfigs() {
        return !CollectionUtils.isEmpty(this.mxRecordConfigs);
    }

    @Nullable
    @Override
    public List<MxRecordConfig> getMxRecordConfigs() {
        return this.mxRecordConfigs;
    }

    @Override
    public void setMxRecordConfigs(@Nullable List<MxRecordConfig> mxRecordConfigs) {
        this.mxRecordConfigs = mxRecordConfigs;
    }

    @Override
    public boolean hasNsRecordConfigs() {
        return !CollectionUtils.isEmpty(this.nsRecordConfigs);
    }

    @Nullable
    @Override
    public List<NsRecordConfig> getNsRecordConfigs() {
        return this.nsRecordConfigs;
    }

    @Override
    public void setNsRecordConfigs(@Nullable List<NsRecordConfig> nsRecordConfigs) {
        this.nsRecordConfigs = nsRecordConfigs;
    }

    @Override
    public boolean hasSoaRecordConfig() {
        return this.soaRecordConfig != null;
    }

    @Nullable
    @Override
    public SoaRecordConfig getSoaRecordConfig() {
        return this.soaRecordConfig;
    }

    @Override
    public void setSoaRecordConfig(@Nullable SoaRecordConfig soaRecordConfig) {
        this.soaRecordConfig = soaRecordConfig;
    }

    @Override
    public boolean hasSrvRecordConfigs() {
        return !CollectionUtils.isEmpty(this.srvRecordConfigs);
    }

    @Nullable
    @Override
    public List<SrvRecordConfig> getSrvRecordConfigs() {
        return this.srvRecordConfigs;
    }

    @Override
    public void setSrvRecordConfigs(@Nullable List<SrvRecordConfig> srvRecordConfigs) {
        this.srvRecordConfigs = srvRecordConfigs;
    }
}