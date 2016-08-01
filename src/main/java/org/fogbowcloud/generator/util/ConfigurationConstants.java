package org.fogbowcloud.generator.util;

public class ConfigurationConstants {

	public static final int DEFAULT_HTTP_PORT = 9000;
	public static final int DEFAULT_HTTPS_PORT = 9433;	
	public static final int DEFAULT_REQUEST_HEADER_SIZE = 1024 * 1024;
	public static final int DEFAULT_RESPONSE_HEADER_SIZE = 1024 * 1024;
		
	public static final String HTTPS_PORT_KEY = "https_port";
	public static final String HTTP_PORT_KEY = "http_port";
	public static final String HTTP_REQUEST_HEADER_SIZE_KEY = "http_request_header_size";
	public static final String HTTP_RESPONSE_HEADER_SIZE_KEY = "http_response_header_size";
	
	public static final String ADMIN_PUBLIC_KEY = "admin_public_key";
	public static final String ADMIN_PRIVATE_KEY = "admin_private_key";
	public static final String AUTHENTICATION_PLUGIN_KEY = "authentication_plugin";
	public static final String KEYSTORE_PATH = "keystore_path";
	public static final String KEYSTORE_PASSWORD = "keystore_password";
	public static final String MAXIMUM_HOURS_EXPIRATTION = "maximum_hours_expiration";
	public static final String TOKEN_EXPIRATION_SCHEDULER_PERIOD_KEY = "token_expiration_scheduler_period";
	
}
