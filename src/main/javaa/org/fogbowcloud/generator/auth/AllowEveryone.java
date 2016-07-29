package org.fogbowcloud.generator.auth;

import java.util.Map;
import java.util.Properties;

public class AllowEveryone implements Authentication {
	
	@SuppressWarnings("unused")
	private Properties properties;

	public AllowEveryone(Properties properties) {
		this.properties = properties;
	}
	
	public boolean isValid(Map<String, String> credentials) {
		return true;
	}

	public boolean isAdmin(Map<String, String> credentials) {
		return true;
	}

}
