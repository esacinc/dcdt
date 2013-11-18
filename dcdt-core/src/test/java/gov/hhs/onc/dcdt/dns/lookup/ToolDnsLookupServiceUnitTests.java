package gov.hhs.onc.dcdt.dns.lookup;


import gov.hhs.onc.dcdt.dns.DnsResolver;
import gov.hhs.onc.dcdt.dns.DnsResolverType;
import gov.hhs.onc.dcdt.test.ToolTestNgTests;
import org.springframework.beans.factory.annotation.Autowired;
import org.testng.Assert;
import org.testng.annotations.Test;
import org.xbill.DNS.Name;
import org.xbill.DNS.TextParseException;

@Test(dependsOnGroups = { "dcdt.test.unit.config.all", "dcdt.test.unit.testcases.all" }, groups = { "dcdt.test.all", "dcdt.test.unit.all",
    "dcdt.test.unit.dns.all", "dcdt.test.unit.dns.lookup" })
public class ToolDnsLookupServiceUnitTests extends ToolTestNgTests {
    @Autowired
    @DnsResolver(DnsResolverType.EXTERNAL)
    private ToolDnsLookupService dnsLookupServiceExternal;

    @Test
    public void testLookupAddress() throws TextParseException {
        Assert.assertNotNull(this.dnsLookupServiceExternal.lookupAddress(new Name("google.com")).getAddress(), "Unable to lookup address.");
    }
}
