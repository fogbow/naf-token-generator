package org.fogbowcloud.generator;

import java.util.Properties;

public class TokenGeneratorController {

	private Properties properties;
	
	public TokenGeneratorController(Properties properties) {
		this.properties = properties;
	}

	public Properties getProperties() {
		return this.properties;
	}

	public boolean isRevoked(String jsonTokenSlice) {
		return false;
	}

	public void revoke(String name, String dateStr) {
	}

	public boolean authenticate() {
		return true;
	}
	
}
