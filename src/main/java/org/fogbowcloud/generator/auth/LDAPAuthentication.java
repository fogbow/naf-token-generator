package org.fogbowcloud.generator.auth;

import java.util.Arrays;
import java.util.Hashtable;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import javax.naming.Context;
import javax.naming.NamingEnumeration;
import javax.naming.NamingException;
import javax.naming.directory.DirContext;
import javax.naming.directory.InitialDirContext;
import javax.naming.directory.SearchControls;
import javax.naming.directory.SearchResult;

import org.apache.log4j.Logger;
import org.fogbowcloud.generator.resources.TokenResource;

public class LDAPAuthentication implements Authentication {
	private static final Logger LOGGER = Logger.getLogger(LDAPAuthentication.class);
	
	protected static final String ADMINS = "admins";
	protected static final String LDAP_URL = "ldap_url";
	protected static final String LDAP_BASE = "ldap_base";
	
	private Properties properties;
	private String ldapUrl;
	private String ldapBase;
	
	public LDAPAuthentication(Properties properties) {
		this.properties = properties;
		this.ldapUrl = properties.getProperty(LDAP_URL);
		this.ldapBase = properties.getProperty(LDAP_BASE);
	}

	public boolean isValid(Map<String, String> credentials) {
		String uid = credentials.get(TokenResource.NAME_FORM);
		String password = credentials.get(TokenResource.PASSWORD_FORM);
		
		if (uid == null || password == null) {
			return false;
		}
		
		try {
			ldapAuthenticate(uid, password);
			return true;
		} catch (Exception e) {
			LOGGER.debug("Could not authenticate with LDAP.", e);
		}
		return false;
	}

	public boolean isAdmin(Map<String, String> credentials) {
		String admins = properties.getProperty(ADMINS);
		if (admins == null) {
			return false;
		}
		List<String> adminsArray = Arrays.asList(admins.trim().split(","));
		
		if (adminsArray.contains(credentials.get(TokenResource.NAME_FORM))) {
			return true;
		}
		
		return false;
	}
	
	private void ldapAuthenticate(String uid, String password) throws Exception {

		Hashtable<String, String> env = new Hashtable<String, String>();
		env.put(Context.INITIAL_CONTEXT_FACTORY, "com.sun.jndi.ldap.LdapCtxFactory");
		env.put(Context.PROVIDER_URL, ldapUrl);
		env.put(Context.SECURITY_AUTHENTICATION, "simple");

		DirContext ctx = null;

		try {

			//password = encryptPassword(password);

			// Step 1: Bind anonymously
			ctx = new InitialDirContext(env);

			// Step 2: Search the directory
			String filter = "(&(objectClass=inetOrgPerson)(uid={0}))";
			SearchControls ctls = new SearchControls();
			ctls.setSearchScope(SearchControls.SUBTREE_SCOPE);
			ctls.setReturningAttributes(new String[0]);
			ctls.setReturningObjFlag(true);
			NamingEnumeration<SearchResult> enm = ctx.search(
					ldapBase, filter, new String[] { uid }, ctls);

			String dn = null;

			if (enm.hasMore()) {
				SearchResult result = (SearchResult) enm.next();
				dn = result.getNameInNamespace();

				LOGGER.debug("dn: " + dn);
			}

			if (dn == null || enm.hasMore()) {
				// uid not found or not unique
				throw new NamingException("Authentication failed");
			}

			// Step 3: Bind with found DN and given password
			ctx.addToEnvironment(Context.SECURITY_PRINCIPAL, dn);
			ctx.addToEnvironment(Context.SECURITY_CREDENTIALS, password);
			// Perform a lookup in order to force a bind operation with JNDI
			ctx.lookup(dn);
			LOGGER.debug("Authentication successful");

			enm.close();

		} catch (Exception e) {
			LOGGER.error("Error while trying to authenticate " + uid +" - Error: "+e.getMessage());
			throw e;
		} finally {
			ctx.close();
		}


	}

}
