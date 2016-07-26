package org.fogbowcloud.generator;

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
		return router;
	}

	public void getUser() {
		// TODO Auto-generated method stub		
	}

	public Properties getProperties() {
		return this.tokenGeneratorController.getProperties();
	}

	public boolean isRevoked(String jsonTokenSlice) {
		return this.tokenGeneratorController.isRevoked(jsonTokenSlice);
	}

	public void revoke(String name, String dateStr) {
		this.tokenGeneratorController.revoke(name, dateStr);
	}

	public boolean authenticate() {
		return this.tokenGeneratorController.authenticate();
	}	
	
	public String createToken(String name, String hours) {
		return this.tokenGeneratorController.createToken(name, hours);
	}

	public boolean verifySignature(String tokenMessage, String signature) {
		return this.tokenGeneratorController.verifySign(tokenMessage, signature);
	}
	
}
