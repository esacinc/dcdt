package gov.hhs.onc.dcdt.ldap;

import gov.hhs.onc.dcdt.beans.LdapService;
import gov.hhs.onc.dcdt.ldap.filter.StringEqualityNode;
import gov.hhs.onc.dcdt.test.ldap.AbstractTestNgLdapTest;
import java.util.List;
import org.apache.commons.collections.CollectionUtils;
import org.apache.directory.api.ldap.model.constants.SchemaConstants;
import org.apache.directory.api.ldap.model.exception.LdapInvalidAttributeValueException;
import org.apache.directory.api.ldap.model.message.SearchScope;
import org.apache.directory.api.ldap.model.name.Dn;
import org.apache.directory.server.annotations.CreateLdapServer;
import org.apache.directory.server.annotations.CreateTransport;
import org.apache.directory.server.core.annotations.CreateDS;
import org.testng.Assert;
import org.testng.annotations.AfterGroups;
import org.testng.annotations.BeforeGroups;
import org.testng.annotations.Test;

@CreateDS(name = "dcdt.ldap.test.apacheds", allowAnonAccess = true)
@CreateLdapServer(name = "dcdt.ldap.test.apacheds.ldap.server", transports = { 
	@CreateTransport(protocol = "LDAP", port = 10389), 
	@CreateTransport(protocol = "LDAP", port = 11389)
})
@Test(enabled = false, groups = { "dcdt.all", "dcdt.ldap", "dcdt.ldap.all" })
public class LdapServiceWrapperTest extends AbstractTestNgLdapTest
{
	public static LdapServiceWrapper serviceWrapper;
	public static List<Dn> baseDns;
	
	@BeforeGroups({ "dcdt.ldap" })
	public void setUpGroup() throws Exception
	{
		this.initClassAnnotations();
		
		this.startLdapServer();
	}
	
	@AfterGroups({ "dcdt.ldap" })
	public void tearDownGroup() throws Exception
	{
		this.stopLdapServer();
	}
	
	@Test(enabled = false, dependsOnMethods = { "testGetBaseDns" })
	public void testSearch() throws LdapInvalidAttributeValueException, ToolLdapException
	{
		Assert.assertFalse(CollectionUtils.isEmpty(serviceWrapper.search(baseDns, new StringEqualityNode(SchemaConstants.UID_AT, "admin"), 
			SearchScope.SUBTREE)), "Search failed to find admin object.");
	}
	
	@Test(enabled = false, dependsOnMethods = { "testBind" })
	public void testGetBaseDns() throws ToolLdapException
	{
		baseDns = serviceWrapper.getBaseDns();
		
		Assert.assertFalse(CollectionUtils.isEmpty(serviceWrapper.search(baseDns, new StringEqualityNode(SchemaConstants.UID_AT, "admin"), 
			SearchScope.SUBTREE)), "Search failed to find any base DN(s).");
	}
	
	@Test(enabled = false, dataProvider = "ldapServiceDataProvider", dataProviderClass = LdapServiceDataProvider.class)
	public void testBind(LdapService service) throws ToolLdapException
	{
		serviceWrapper = new LdapServiceWrapper(service);
		serviceWrapper.bind();
		
		Assert.assertTrue(serviceWrapper.isBound(), "Failed to bind to LDAP service.");
	}
}