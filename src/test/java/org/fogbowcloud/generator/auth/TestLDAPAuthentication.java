package org.fogbowcloud.generator.auth;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import junit.framework.Assert;

import org.fogbowcloud.generator.resources.TokenResource;
import org.junit.Before;
import org.junit.Test;

public class TestLDAPAuthentication {
	private static final String USERNAME1 = "username1";
	private static final String PASSWORD1 = "password1";
	private static final String USERNAME2 = "username2";
	private static final String PASSWORD2 = "password2";
	
	private Properties properties;
	
	@Before
	public void setUp() {
		properties = new Properties();
		properties.put(LDAPAuthentication.ADMINS, USERNAME1 + "," + USERNAME2);
		properties.put(LDAPAuthentication.LDAP_BASE, "dc=lsd,dc=ufcg,dc=edu,dc=br");
		properties.put(LDAPAuthentication.LDAP_URL, "ldap://fake.ldap:389");
	}
	
	@Test
	public void testIsAdmin() {
		LDAPAuthentication ldapAuthentication = new LDAPAuthentication(properties);
		Map<String, String> credentials = new HashMap<String, String>();
		credentials.put(TokenResource.NAME_FORM, USERNAME1);
		credentials.put(TokenResource.PASSWORD_FORM, PASSWORD1);
		boolean admin = ldapAuthentication.isAdmin(credentials);
		
		Assert.assertTrue(admin);
		
		Map<String, String> invalidCredentials = new HashMap<String, String>();
		invalidCredentials.put(TokenResource.NAME_FORM, "invalid");
		invalidCredentials.put(TokenResource.PASSWORD_FORM, "invalid");
		admin = ldapAuthentication.isAdmin(invalidCredentials);
		
		Assert.assertFalse(admin);
	}
	
	@Test
	public void testIsValid() {
		LDAPAuthentication ldapAuthentication = new LDAPAuthentication(properties);
		Map<String, String> credentials = new HashMap<String, String>();
		credentials.put(TokenResource.NAME_FORM, USERNAME1);
		credentials.put(TokenResource.PASSWORD_FORM, PASSWORD1);
		boolean valid = ldapAuthentication.isValid(credentials);
		
		Assert.assertTrue(valid);
	}

}
