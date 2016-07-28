package org.fogbowcloud.generator;

import java.util.Map;
import java.util.Properties;

import org.fogbowcloud.generator.resources.TokenResource;
import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

public class TokenGereratorApplication extends Application {
	
	private TokenGeneratorController tokenGeneratorController;
	
	public TokenGereratorApplication(Properties properties) {
		this.tokenGeneratorController = new TokenGeneratorController(properties);
	}
	
	public void setTokenGeneratorController(
			TokenGeneratorController tokenGeneratorController) {
		this.tokenGeneratorController = tokenGeneratorController;
	}
	
	@Override
	public Restlet createInboundRoot() {
		Router router = new Router(getContext());
		router.attach("/token", TokenResource.class);
		router.attach("/token/", TokenResource.class);
		router.attach("/token/{token}", TokenResource.class);
		return router;
	}

	public Properties getProperties() {
		return this.tokenGeneratorController.getProperties();
	}

	public boolean isDeleted(String tokenId) {
		return this.tokenGeneratorController.isDeleted(tokenId);
	}

	public void delete(Map<String, String> parameters, Token token) {
		this.tokenGeneratorController.delete(parameters, token);
	}
	
	public String createToken(Map<String, String> parameters) {
		return this.tokenGeneratorController.createToken(parameters);
	}

	public boolean verifySignature(String tokenMessage, String signature) {
		return this.tokenGeneratorController.verifySign(tokenMessage, signature);
	}

	public Map<String, Token> getTokens(Map<String, String> parameters) {
		return this.tokenGeneratorController.getAllTokens(parameters);
	}

	public boolean isValidToken(Map<String, String> parameters, String finalToken) {
		return this.tokenGeneratorController.isValidToken(parameters, finalToken);		
	}
	
}
