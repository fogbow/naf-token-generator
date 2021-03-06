package org.fogbowcloud.generator;

import java.util.Properties;

import junit.framework.Assert;

import org.fogbowcloud.generator.util.ConfigurationConstants;
import org.junit.Test;

public class TestMain {

	@Test
	public void testCheckProperties() {
		Properties properties = new Properties();
		properties.put(ConfigurationConstants.ADMIN_PRIVATE_KEY, "/path");
		properties.put(ConfigurationConstants.ADMIN_PUBLIC_KEY, "/path");
		Assert.assertTrue(Main.checkProperties(properties));
	}
	
	@Test
	public void testCheckPropertiesWithoutPrivateKey() {
		Properties properties = new Properties();
		properties.put(ConfigurationConstants.ADMIN_PUBLIC_KEY, "/path");
		Assert.assertFalse(Main.checkProperties(properties));
	}
	
	@Test
	public void testCheckPropertiesWithoutPublicKey() {
		Properties properties = new Properties();
		properties.put(ConfigurationConstants.ADMIN_PRIVATE_KEY, "/path");
		Assert.assertFalse(Main.checkProperties(properties));
	}	
	
}
