package org.fogbowcloud.generator;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Properties;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.fogbowcloud.generator.util.ConfigurationConstant;
import org.fogbowcloud.generator.util.RSAUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.resource.ResourceException;

public class TokenGeneratorController {

	public static final String ETIME_TOKEN = "token_etime";
	public static final String CTIME_TOKEN = "token_ctime";	
	public static final String NAME_TOKEN = "name";
	public static final String ID_TOKEN = "id";
	public static final String SEPARATOR = "!#!";
	
	private static final Logger LOGGER = Logger.getLogger(TokenResource.class);		
	
	private Properties properties;
	
	public TokenGeneratorController(Properties properties) {
		this.properties = properties;
	}

	public Properties getProperties() {
		return this.properties;
	}

	public boolean isRevoked(String jsonTokenSlice) {
		return false;
	}

	public void revoke(String name, String dateStr) {
		
	}

	public boolean authenticate() {
		return true;
	}

	public String createToken(String name, String hours) {
		JSONObject createJsonTokenSlice = null;
		try {
			createJsonTokenSlice = createTokenJsonSlice(name, hours);
		} catch (JSONException e) {
			LOGGER.error("Token malformed.", e);
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, "Token malformed.");
		}
		
		// TODO check if private key exists
		RSAPrivateKey privateKey = null;
		try {
			privateKey = RSAUtils.getPrivateKey(properties.getProperty(ConfigurationConstant.ADMIN_PRIVATE_KEY));
		} catch (Exception e) {
			LOGGER.error("Invalid private key.", e);
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, "Invalid private key.");
		}
		String jsonTokenSliceSign = null;
		try {
			jsonTokenSliceSign = RSAUtils.sign(privateKey, createJsonTokenSlice.toString());
		} catch (Exception e) {
			LOGGER.error("Something worng when sign the token.", e);
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, "Something worng when sign the token.");
		}
		
		String token = null;
		try {
			token = new String(Base64.encodeBase64((createJsonTokenSlice.toString() 
					+ SEPARATOR + jsonTokenSliceSign).getBytes()));			
		} catch (Exception e) {
			// TODO: handle exception
		}
		
		return token;
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
	
	private JSONObject createTokenJsonSlice(String name, String hours) throws JSONException {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put(ID_TOKEN, UUID.randomUUID());
		jsonObject.put(NAME_TOKEN, name);
		long now = System.currentTimeMillis();
		jsonObject.put(CTIME_TOKEN, now);
		jsonObject.put(ETIME_TOKEN, now + (Long.parseLong(hours) * 60 * 60 * 1000));
		return jsonObject;
	}
	
}
