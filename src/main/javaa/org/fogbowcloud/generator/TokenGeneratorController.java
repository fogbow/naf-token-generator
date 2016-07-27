package org.fogbowcloud.generator;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.UUID;

import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.fogbowcloud.generator.util.ConfigurationConstant;
import org.fogbowcloud.generator.util.RSAUtils;
import org.json.JSONObject;
import org.restlet.resource.ResourceException;

// TODO implements tests
public class TokenGeneratorController {
	
	private Map<String, Token> tokens;
	
	private static final Logger LOGGER = Logger.getLogger(TokenResource.class);
	
	private Properties properties;
	
	public TokenGeneratorController(Properties properties) {
		this.properties = properties;
		this.tokens = new HashMap<String, Token>();
	}

	public Map<String, Token> getTokens() {
		return tokens;
	}
	
	public Properties getProperties() {
		return this.properties;
	}

	public boolean isRevoked(String jsonTokenSlice) {
		return false;
	}

	public void delete(Token token) {
		// TODO think about but this method in a thread
		deleteExpiredToken();
		if (this.tokens.get(token.getId()) != null) {
			this.tokens.remove(token.getId());
		}
	}
	
	protected synchronized void deleteExpiredToken() {
		Collection<Token> tokenValues = new ArrayList<Token>(this.tokens.values());
		for (Token token : tokenValues) {
			if (token.geteTime() < System.currentTimeMillis()) {
				this.tokens.remove(token.getId());
			}
		}
	}

	public boolean authenticate() {
		return true;
	}

	// TODO check the admin for infinite tokens		
	// TODO check if private key exists
	public String createToken(Map<String, String> parameters) {
		JSONObject tokenJson = null;
		
		String name = parameters.get(TokenResource.NAME_FORM);
		String hours = parameters.get(TokenResource.HOURS_FORM_POST);
		String infinite = parameters.get(TokenResource.INFINITE_FORM_POST);
				
		long now = System.currentTimeMillis();
		
		Token token = new Token(UUID.randomUUID().toString(), name, now, 
				now + (Integer.parseInt(hours) * 60 * 60 *1000), new Boolean(infinite));
		
		tokenJson = token.toJson();
		if (tokenJson == null) {
			LOGGER.error("Token malformed. " + token.toString());
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, "Token malformed.");
		}
		
		RSAPrivateKey privateKey = null;
		try {
			privateKey = RSAUtils.getPrivateKey(properties.getProperty(ConfigurationConstant.ADMIN_PRIVATE_KEY));
		} catch (Exception e) {
			LOGGER.error("Invalid private key.", e);
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, "Invalid private key.");
		}
		String tokenSignature = null;
		try {
			tokenSignature = RSAUtils.sign(privateKey, tokenJson.toString());
			token.setSignature(tokenSignature);
		} catch (Exception e) {
			LOGGER.error("Something worng when sign the token.", e);
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, "Something worng when sign the token.");
		}				
		
		String finalToken = token.toFinalToken();
		this.tokens.put(token.getId(), token);	
		return finalToken;
	}
	
	public boolean verifySign(String tokenMessage, String signature) {
		// TODO check if private key exists
		RSAPublicKey publicKey = null;
		try {
			publicKey = RSAUtils.getPublicKey(properties.getProperty(ConfigurationConstant.ADMIN_PUBLIC_KEY));
		} catch (Exception e) {
			LOGGER.error("Invalid public key.", e);
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, "Invalid public key.");
		}		
		try {
			return RSAUtils.verify(publicKey, tokenMessage, signature);			
		} catch (Exception e) {
			LOGGER.error("Something wrong at verify.", e);
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, "Something wrong at verify. " + e.getMessage());
		}
	}

	public void getUser(String name) {
		// TODO Auto-generated method stub		
	}

}
