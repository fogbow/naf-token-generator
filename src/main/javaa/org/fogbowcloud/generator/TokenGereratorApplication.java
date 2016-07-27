package org.fogbowcloud.generator;

import java.util.Map;
import java.util.Properties;

import org.restlet.Application;
import org.restlet.Restlet;
import org.restlet.routing.Router;

public class TokenGereratorApplication extends Application {
	
	private TokenGeneratorController tokenGeneratorController;
	
	public TokenGereratorApplication(Properties properties) {
		this.tokenGeneratorController = new TokenGeneratorController(properties);
	}
	
	protected void setTokenGeneratorController(
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

	public void getUser(String name) {
		this.tokenGeneratorController.getUser(name);
	}

	public Properties getProperties() {
		return this.tokenGeneratorController.getProperties();
	}

	public boolean isRevoked(String jsonTokenSlice) {
		return this.tokenGeneratorController.isRevoked(jsonTokenSlice);
	}

	public void delete(Token token) {
		this.tokenGeneratorController.delete(token);
	}

	public boolean authenticate() {
		return this.tokenGeneratorController.authenticate();
	}	
	
	public String createToken(Map<String, String> parameters) {
		return this.tokenGeneratorController.createToken(parameters);
	}

	public boolean verifySignature(String tokenMessage, String signature) {
		return this.tokenGeneratorController.verifySign(tokenMessage, signature);
	}

	public Map<String, Token> getTokens() {
		return this.tokenGeneratorController.getTokens();
	}
	
}
