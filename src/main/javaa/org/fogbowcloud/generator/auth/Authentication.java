package org.fogbowcloud.generator.auth;

import java.util.Map;

public interface Authentication {
	
	public boolean isValid(Map<String, String> credentials);
		
	public boolean isAdmin(Map<String, String> credentials);
	
}
