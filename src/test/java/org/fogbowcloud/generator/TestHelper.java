package org.fogbowcloud.generator;

import java.io.File;

public class TestHelper {

	public static final String DEFAULT_RESOURCE_PATH = "src/main/resources";
	public static final String DEFAULT_TOKEN_BD_PATH = DEFAULT_RESOURCE_PATH + "/token.bd";
	public static final String DEFAULT_FILE_PUBLIC_KEY_PATH = DEFAULT_RESOURCE_PATH + "/public.pem";
	public static final String DEFAULT_FILE_PRIVATE_KEY_PATH = DEFAULT_RESOURCE_PATH + "/private.pem";
	public static final String JDBC_SQLITE_MEMORY = "jdbc:sqlite:" + DEFAULT_TOKEN_BD_PATH;

	public static void removingFile(String path) {
		File dbFile = new File(path);
		if (dbFile.exists()) {
			dbFile.delete();
		}
	}
	
}
