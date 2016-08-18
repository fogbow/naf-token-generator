package org.fogbowcloud.generator;

import java.io.File;
import java.io.FileInputStream;
import java.util.Properties;

import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Logger;
import org.fogbowcloud.generator.auth.Authentication;
import org.fogbowcloud.generator.util.ConfigurationConstants;
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
		configureLog4j();
		Properties properties = new Properties();
		properties.load(new FileInputStream(new File(args[0])));
				
		if (!checkProperties(properties)) {
			System.exit(EXIT_ERROR_CODE);
		}
		
		Authentication authentication = null;
		try {
			authentication = (Authentication) createInstance(
					ConfigurationConstants.AUTHENTICATION_PLUGIN_KEY, properties);
		} catch (Exception e) {
			LOGGER.warn("Authentication not especified in the properties.", e);
			System.exit(EXIT_ERROR_CODE);
		}		
		
		Component http = new Component();
		String httpsPort = properties.getProperty(ConfigurationConstants.HTTPS_PORT_KEY);
		String httpPort = properties.getProperty(ConfigurationConstants.HTTP_PORT_KEY);
		String requestHeaderSize = properties.getProperty(ConfigurationConstants.HTTP_REQUEST_HEADER_SIZE_KEY);
		if (requestHeaderSize == null || requestHeaderSize.isEmpty()) {
			requestHeaderSize = String.valueOf(ConfigurationConstants.DEFAULT_REQUEST_HEADER_SIZE);
		} else {
			requestHeaderSize = String.valueOf(Integer.parseInt(requestHeaderSize));
		}
		String responseHeaderSize = properties.getProperty(ConfigurationConstants.HTTP_RESPONSE_HEADER_SIZE_KEY);
		if (responseHeaderSize == null || responseHeaderSize.isEmpty()) {
			responseHeaderSize = String.valueOf(ConfigurationConstants.DEFAULT_RESPONSE_HEADER_SIZE);
		} else {
			responseHeaderSize = String.valueOf(Integer.parseInt(responseHeaderSize));
		}
		
		//Adding HTTP server
		Server httpServer = http.getServers().add(Protocol.HTTP, httpPort == null ? ConfigurationConstants.DEFAULT_HTTP_PORT : Integer.parseInt(httpPort));
		Series<Parameter> httpParameters = httpServer.getContext().getParameters();
		httpParameters.add("http.requestHeaderSize", requestHeaderSize);
		httpParameters.add("http.responseHeaderSize", responseHeaderSize);
		
		//Adding HTTPS server
		Server httpsServer = http.getServers().add(Protocol.HTTPS, httpsPort == null ? 
				ConfigurationConstants.DEFAULT_HTTPS_PORT : Integer.parseInt(httpsPort));
		
		@SuppressWarnings("rawtypes")
		Series parameters = httpsServer.getContext().getParameters();
		parameters.add("sslContextFactory", "org.restlet.engine.ssl.DefaultSslContextFactory");
		// put in the properties
		String keyStorePath = properties.getProperty(ConfigurationConstants.KEYSTORE_PATH);
		parameters.add("keyStorePath", keyStorePath);
		String keyStorePassword = properties.getProperty(ConfigurationConstants.KEYSTORE_PASSWORD);
		parameters.add("keyStorePassword", keyStorePassword);
		parameters.add("keyPassword", keyStorePassword);
		parameters.add("keyStoreType", "JKS");
		parameters.add("http.requestHeaderSize", requestHeaderSize);
		parameters.add("http.responseHeaderSize", responseHeaderSize);		
		
		TokenGereratorApplication tokenGereratorApplication = new TokenGereratorApplication(properties);
		tokenGereratorApplication.getTokenGeneratorController().setAuthentication(authentication);
		http.getDefaultHost().attach(tokenGereratorApplication);
		http.start();
	}

	protected static boolean checkProperties(Properties properties) {
		LOGGER.debug("Checking main properties.");
		String adminPrivateKey = properties.getProperty(ConfigurationConstants.ADMIN_PRIVATE_KEY);
		if (adminPrivateKey == null || adminPrivateKey.isEmpty()) {
			LOGGER.error("Admin private key not especified in the properties.");			
			return false;
		}
		String adminPublicKey = properties.getProperty(ConfigurationConstants.ADMIN_PUBLIC_KEY);
		if (adminPublicKey == null || adminPublicKey.isEmpty()) {
			LOGGER.error("Admin public key not especified in the properties.");
			return false;
		}
		return true;
	}
	
	protected static Object createInstance(String propName, Properties properties) throws Exception {
		return Class.forName(properties.getProperty(propName)).getConstructor(Properties.class)
				.newInstance(properties);
	}
	
	private static void configureLog4j() {
		ConsoleAppender console = new ConsoleAppender();
		console.setThreshold(org.apache.log4j.Level.OFF);
		console.activateOptions();
		Logger.getRootLogger().addAppender(console);
		
		Component component = new Component();
		component.setLogService(new org.restlet.service.LogService(false));
	}	
	
}
