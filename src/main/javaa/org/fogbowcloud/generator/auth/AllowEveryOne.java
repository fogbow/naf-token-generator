package org.fogbowcloud.generator.auth;

import java.util.Map;

public class AllowEveryOne implements Authentication {

	public boolean isValid(Map<String, String> credentials) {
		return true;
	}

	public boolean isAdmin(Map<String, String> credentials) {
		return true;
	}

}
