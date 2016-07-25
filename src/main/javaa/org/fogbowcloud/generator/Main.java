package org.fogbowcloud.generator;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.restlet.Component;
import org.restlet.data.Protocol;

public class Main {

	private static final Logger LOGGER = Logger.getLogger(Main.class);
	// TODO think better
	private static final int DEFAULT_HTTP_PORT = 8888;

	public static void main(String[] args) throws Exception {
		LOGGER.debug("Starting Token Generation...");
		Properties properties = new Properties();
		properties.load(new FileInputStream(new File(args[0])));
		
		// TODO get from properties
		String httpPortStr = null;
		
		Component http = new Component();
		
		http.getServers().add(Protocol.HTTP,
				httpPortStr == null ? DEFAULT_HTTP_PORT : Integer.parseInt(httpPortStr));
		http.getDefaultHost().attach(new TokenGereratorApplication(properties));
		http.start();		
	}
	
}
