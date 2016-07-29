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
import org.fogbowcloud.generator.auth.Authentication;
import org.fogbowcloud.generator.resources.TokenResource;
import org.fogbowcloud.generator.util.ConfigurationConstants;
import org.fogbowcloud.generator.util.DateUtils;
import org.fogbowcloud.generator.util.RSAUtils;
import org.json.JSONObject;
import org.restlet.resource.ResourceException;

public class TokenGeneratorController {
	
	protected static final int HOURS_IN_MILISECONDS = 60 * 60 *1000;

	private Map<String, Token> tokens;
	private DateUtils dateUtils;
	
	private static final Logger LOGGER = Logger.getLogger(TokenResource.class);
	
	private Properties properties;
	private Authentication authentication;
	
	public TokenGeneratorController(Properties properties) {
		this.properties = properties;
		this.tokens = new HashMap<String, Token>();
		this.dateUtils = new DateUtils();
	}

	public Map<String, Token> getTokens() {
		return tokens;
	}
	
	protected void setTokens(Map<String, Token> tokens) {
		this.tokens = tokens;
	}
	
	public Properties getProperties() {
		return this.properties;
	}
	
	public void setDateUtils(DateUtils dateUtils) {
		this.dateUtils = dateUtils;
	}
	
	public void setAuthentication(Authentication authentication) {
		this.authentication = authentication;
	}

	public boolean isDeleted(String tokenId) {
		return this.tokens.get(tokenId) == null ? true: false;
	}

	public void delete(Map<String, String> parameters, Token token) {
		if (!authentication.isValid(parameters) || !authentication.isAdmin(parameters)) {
			throw new ResourceException(HttpStatus.SC_UNAUTHORIZED, "Unauthorized.");
		}
		
		// TODO think about but this method in a thread
		deleteExpiredToken();
		
		if (this.tokens.get(token.getId()) != null) {
			this.tokens.remove(token.getId());
		}
	}
	
	public Map<String, Token> getAllTokens(Map<String, String> parameters) {
		if (!authentication.isValid(parameters) || !authentication.isAdmin(parameters)) {
			throw new ResourceException(HttpStatus.SC_UNAUTHORIZED, "Unauthorized.");
		}		
		
		return tokens;
	}
	
	protected void deleteExpiredToken() {
		Collection<Token> tokenValues = new ArrayList<Token>(this.tokens.values());
		for (Token token : tokenValues) {
			if (token.geteTime() < this.dateUtils.currentTimeMillis() && !token.isInfinite()) {
				this.tokens.remove(token.getId());
			}
		}
	}

	public String createToken(Map<String, String> parameters) {
		if (!this.authentication.isValid(parameters)) {
			throw new ResourceException(HttpStatus.SC_UNAUTHORIZED, "Unauthorized.");
		}
		
		String name = parameters.get(TokenResource.NAME_FORM);
		String hours = parameters.get(TokenResource.HOURS_FORM_POST);
		String infinite = parameters.get(TokenResource.INFINITE_FORM_POST);
		if (new Boolean(infinite) && !authentication.isAdmin(parameters)) {
			throw new ResourceException(HttpStatus.SC_UNAUTHORIZED, "Unauthorized.");
		}
				
		long now = this.dateUtils.currentTimeMillis();
		long expirationTime = now + (Integer.parseInt(hours) * HOURS_IN_MILISECONDS);
		Token token = new Token(UUID.randomUUID().toString(), name, 
				now, expirationTime, new Boolean(infinite));
		
		JSONObject tokenJson = null;		
		tokenJson = token.toJson();
		if (tokenJson == null) {
			LOGGER.error("Token malformed. " + token.toString());
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, "Token malformed.");
		}
		
		RSAPrivateKey privateKey = null;
		try {
			privateKey = RSAUtils.getPrivateKey(properties.getProperty(ConfigurationConstants.ADMIN_PRIVATE_KEY));
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
		RSAPublicKey publicKey = null;
		try {
			publicKey = RSAUtils.getPublicKey(this.properties.getProperty(
					ConfigurationConstants.ADMIN_PUBLIC_KEY));
		} catch (Exception e) {
			LOGGER.error("Invalid public key.", e);
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, "Invalid public key.");
		}		
		try {
			return RSAUtils.verify(publicKey, tokenMessage, signature);			
		} catch (Exception e) {
			LOGGER.error("Something wrong at verify.", e);
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, "Something wrong at verify. " 
					+ e.getMessage());
		}
	}

	public boolean isValidToken(Map<String, String> parameters, String finalToken) {
		LOGGER.error("Checking token : " + finalToken);
		
		if (!authentication.isValid(parameters)) {
			throw new ResourceException(HttpStatus.SC_UNAUTHORIZED, "Unauthorized.");
		}
		
		Token token = new Token();
		try {
			token.fromFinalToken(finalToken);
		} catch (Exception e) {
			LOGGER.error("Token malformed.", e);
			return false;
		}
		String tokenJson = token.toJson().toString();
				
		try {
			if (!verifySign(tokenJson, token.getSignature())) {
				LOGGER.error("Signature false.");
				return false;
			}			
		} catch (Exception e) {
			LOGGER.error("Signature false.", e);
			return false;
		}
		
		if (isDeleted(token.getId())) {
			LOGGER.error("Token deleted.");
			return false;
		}
		return true;
	}

}
