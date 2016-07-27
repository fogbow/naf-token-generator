package org.fogbowcloud.generator;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.fogbowcloud.generator.util.ConfigurationConstant;
import org.restlet.Component;
import org.restlet.Server;
import org.restlet.data.Parameter;
import org.restlet.data.Protocol;
import org.restlet.util.Series;

public class Main {
	
	private static final Logger LOGGER = Logger.getLogger(Main.class);
	private static final int EXIT_ERROR_CODE = 128;

	public static void main(String[] args) throws Exception {
		LOGGER.debug("Starting Token Generation...");
		Properties properties = new Properties();
		properties.load(new FileInputStream(new File(args[0])));
				
		if (!checkProperties(properties)) {
			System.exit(EXIT_ERROR_CODE);
		}
		
		Component http = new Component();
		String httpsPort = properties.getProperty(ConfigurationConstant.HTTPS_PORT_KEY);
		String httpPort = properties.getProperty(ConfigurationConstant.HTTP_PORT_KEY);
		String requestHeaderSize = properties.getProperty(ConfigurationConstant.HTTP_REQUEST_HEADER_SIZE_KEY);
		if (requestHeaderSize == null) {
			requestHeaderSize = String.valueOf(ConfigurationConstant.DEFAULT_REQUEST_HEADER_SIZE);
		} else {
			requestHeaderSize = String.valueOf(Integer.parseInt(requestHeaderSize));
		}
		String responseHeaderSize = properties.getProperty(ConfigurationConstant.HTTP_RESPONSE_HEADER_SIZE_KEY);
		if (responseHeaderSize == null) {
			responseHeaderSize = String.valueOf(ConfigurationConstant.DEFAULT_RESPONSE_HEADER_SIZE);
		} else {
			responseHeaderSize = String.valueOf(Integer.parseInt(responseHeaderSize));
		}
		
		//Adding HTTP server
		Server httpServer = http.getServers().add(Protocol.HTTP, httpPort == null ? ConfigurationConstant.DEFAULT_HTTP_PORT : Integer.parseInt(httpPort));
		Series<Parameter> httpParameters = httpServer.getContext().getParameters();
		httpParameters.add("http.requestHeaderSize", requestHeaderSize);
		httpParameters.add("http.responseHeaderSize", responseHeaderSize);
		
		//Adding HTTPS server
		Server httpsServer = http.getServers().add(Protocol.HTTPS, httpsPort == null ? 
				ConfigurationConstant.DEFAULT_HTTPS_PORT : Integer.parseInt(httpsPort));
		
		@SuppressWarnings("rawtypes")
		Series parameters = httpsServer.getContext().getParameters();
		parameters.add("sslContextFactory", "org.restlet.engine.ssl.DefaultSslContextFactory");
		// create keystore
		parameters.add("keyStorePath", "/home/fogbowncj/fogbow.jks");
		parameters.add("keyStorePassword", "password");
		parameters.add("keyPassword", "password");
		parameters.add("keyStoreType", "JKS");
		parameters.add("http.requestHeaderSize", requestHeaderSize);
		parameters.add("http.responseHeaderSize", responseHeaderSize);		
		
		http.getDefaultHost().attach(new TokenGereratorApplication(properties));
		http.start();		
	}

	protected static boolean checkProperties(Properties properties) {
		LOGGER.debug("Checking main properties.");
		String adminPrivateKey = properties.getProperty(ConfigurationConstant.ADMIN_PRIVATE_KEY);
		if (adminPrivateKey == null || adminPrivateKey.isEmpty()) {
			LOGGER.error("Admin private key not especified in the properties.");			
			return false;
		}
		String adminPublicKey = properties.getProperty(ConfigurationConstant.ADMIN_PUBLIC_KEY);
		if (adminPublicKey == null || adminPublicKey.isEmpty()) {
			LOGGER.error("Admin public key not especified in the properties.");
			return false;
		}
		return true;
	}
	
	
}
