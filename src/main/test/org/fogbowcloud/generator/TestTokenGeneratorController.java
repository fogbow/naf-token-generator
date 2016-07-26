package org.fogbowcloud.generator;

import java.util.Properties;

import junit.framework.Assert;

import org.apache.commons.codec.binary.Base64;
import org.fogbowcloud.generator.util.ConfigurationConstant;
import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;

public class TestTokenGeneratorController {

	private TokenGeneratorController tokenGeneratorController;
	
	@Before
	public void setUp() {
		Properties properties = new Properties();
		// TODO create private key and public key
		properties.put(ConfigurationConstant.ADMIN_PRIVATE_KEY, "/home/chicog/Downloads/private_key.pem");
		properties.put(ConfigurationConstant.ADMIN_PUBLIC_KEY, "/home/chicog/Downloads/public_key.pem");
		this.tokenGeneratorController = new TokenGeneratorController(properties);
	}
	
	@Test
	public void testCreateToken() {
		String name = "Fulano";
		String hours = "2";
		String token = this.tokenGeneratorController.createToken(name, hours);
		String tokenDecoded = new String(Base64.decodeBase64(token.getBytes()));
		String[] tokenSlices = tokenDecoded.split(TokenGeneratorController.SEPARATOR);
		
		String tokenJsonSlice = tokenSlices[0];
		Assert.assertTrue(this.tokenGeneratorController.verifySign(tokenJsonSlice, tokenSlices[1]));
		
		Assert.assertEquals(name, new Token(tokenJsonSlice).getName());
	}
	
	private class Token {
		
		private String name;
		private String eTime;
		
		public Token(String tokenJsonSlice) {
			JSONObject jsonObject = null;
			try {
				jsonObject = new JSONObject(tokenJsonSlice);				
			} catch (Exception e) {
				return;
			}
			this.name = jsonObject.optString(TokenGeneratorController.NAME_TOKEN);
			this.eTime = jsonObject.optString(TokenGeneratorController.ETIME_TOKEN);
		}
		
		public String getName() {
			return name;
		}
		
		public String geteTime() {
			return eTime;
		}
		
	}
	
}
