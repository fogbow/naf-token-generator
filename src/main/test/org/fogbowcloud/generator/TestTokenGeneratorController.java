package org.fogbowcloud.generator;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import junit.framework.Assert;

import org.fogbowcloud.generator.auth.Authentication;
import org.fogbowcloud.generator.resources.TokenResource;
import org.fogbowcloud.generator.util.ConfigurationConstants;
import org.fogbowcloud.generator.util.DateUtils;
import org.fogbowcloud.generator.util.RSAUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.restlet.resource.ResourceException;

public class TestTokenGeneratorController {

	private static final String DEFAULT_PATH = "src/main/resources";
	private static final String DEFAULT_FILE_PUBLIC_KEY_PATH = DEFAULT_PATH + "/public.pem";
	private static final String DEFAULT_FILE_PRIVATE_KEY_PATH = DEFAULT_PATH + "/private.pem";
	
	private TokenGeneratorController tokenGeneratorController;
	private KeyPair keyPair;
	
	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		Properties properties = new Properties();
		
		try {
			this.keyPair = RSAUtils.generateKeyPair();
		} catch (NoSuchAlgorithmException e) {
			Assert.fail();
		}
		
		writeKeyToFile(RSAUtils.savePublicKey(this.keyPair.getPublic()), DEFAULT_FILE_PUBLIC_KEY_PATH);			
		writeKeyToFile(RSAUtils.savePrivateKey(this.keyPair.getPrivate()), DEFAULT_FILE_PRIVATE_KEY_PATH);	
		
		properties.put(ConfigurationConstants.ADMIN_PRIVATE_KEY, DEFAULT_FILE_PRIVATE_KEY_PATH);
		properties.put(ConfigurationConstants.ADMIN_PUBLIC_KEY, DEFAULT_FILE_PUBLIC_KEY_PATH);
		this.tokenGeneratorController = new TokenGeneratorController(properties);
		Authentication authentication = Mockito.mock(Authentication.class);
		Mockito.when(authentication.isValid(Mockito.anyMap())).thenReturn(true);
		Mockito.when(authentication.isAdmin(Mockito.anyMap())).thenReturn(true);
		this.tokenGeneratorController.setAuthentication(authentication);
	}
	
	@After
	public void tearDown() throws IOException{
		File dbFile = new File(DEFAULT_FILE_PUBLIC_KEY_PATH);
		if (dbFile.exists()) {
			dbFile.delete();
		}
		dbFile = new File(DEFAULT_FILE_PRIVATE_KEY_PATH);
		if (dbFile.exists()) {
			dbFile.delete();
		}		
	}		
	
	@Test
	public void testCreateToken() throws Exception {
		String name = "Fulano";
		String hours = "2";
		String infinite = "false";
		String password = "password";
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(TokenResource.NAME_FORM, name);
		parameters.put(TokenResource.PASSWORD_FORM, password);
		parameters.put(TokenResource.HOURS_FORM_POST, hours);
		parameters.put(TokenResource.INFINITE_FORM_POST, infinite);
						
		DateUtils dateUtils = Mockito.mock(DateUtils.class);
		this.tokenGeneratorController.setDateUtils(dateUtils);
		long now = System.currentTimeMillis();
		Mockito.when(dateUtils.currentTimeMillis()).thenReturn(now);
		
		String finalToken = this.tokenGeneratorController.createToken(parameters);
		Assert.assertEquals(1, this.tokenGeneratorController.getTokens().size());
		
		Token token = new Token();
		token.fromFinalToken(finalToken);
		Assert.assertEquals(now, token.getcTime());
		Assert.assertEquals(now + (Integer.parseInt(hours) * 
				TokenGeneratorController.HOURS_IN_MILISECONDS) , token.geteTime());
		
		Assert.assertTrue(this.tokenGeneratorController.verifySign(token.toJson().toString(), token.getSignature()));
		
		Assert.assertEquals(name, token.getName());
	}

	@SuppressWarnings("unchecked")
	@Test(expected=ResourceException.class)
	public void testCreateTokenNotAuthorized() {		
		Authentication authentication = Mockito.mock(Authentication.class);
		Mockito.when(authentication.isValid(Mockito.anyMap())).thenReturn(false);
		this.tokenGeneratorController.setAuthentication(authentication);
		
		this.tokenGeneratorController.createToken(new HashMap<String, String>());
	}
	
	@SuppressWarnings("unchecked")
	@Test(expected=ResourceException.class)
	public void testCreateInfiniteTokenNotAuthorized() {
		String name = "Fulano";
		String hours = "2";
		String infinite = "true";
		String password = "password";
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(TokenResource.NAME_FORM, name);
		parameters.put(TokenResource.PASSWORD_FORM, password);
		parameters.put(TokenResource.HOURS_FORM_POST, hours);
		parameters.put(TokenResource.INFINITE_FORM_POST, infinite);
		
		Authentication authentication = Mockito.mock(Authentication.class);
		Mockito.when(authentication.isValid(Mockito.anyMap())).thenReturn(true);
		Mockito.when(authentication.isAdmin(Mockito.anyMap())).thenReturn(false);
		this.tokenGeneratorController.setAuthentication(authentication);
		
		this.tokenGeneratorController.createToken(parameters);
	}

	@Test
	public void testIsDeleted() {
		Map<String, Token> tokens = new HashMap<String, Token>();
		tokens.put("001122", new Token());
		this.tokenGeneratorController.setTokens(tokens);
		Assert.assertTrue(this.tokenGeneratorController.isDeleted("123"));
	}
	
	@Test
	public void testIsNotDeleted() {
		Map<String, Token> tokens = new HashMap<String, Token>();
		String tokenId = "001122";
		tokens.put(tokenId, new Token());
		this.tokenGeneratorController.setTokens(tokens);
		Assert.assertFalse(this.tokenGeneratorController.isDeleted(tokenId));
	}
	
	@Test
	public void testDeleteExpiredTokens() {
		DateUtils dateUtils = Mockito.mock(DateUtils.class);
		this.tokenGeneratorController.setDateUtils(dateUtils);
		long now = System.currentTimeMillis();
		long stillAlive = now + 1; 
		long tokenExpired = now - 1; 
		Mockito.when(dateUtils.currentTimeMillis()).thenReturn(now);
		
		Map<String, Token> tokens = new HashMap<String, Token>();
		tokens.put("00", new Token("00", "name0", 0, stillAlive, false));
		tokens.put("11", new Token("11", "name1", 0, stillAlive, false));
		tokens.put("22", new Token("22", "name2", 0, tokenExpired, true));
		tokens.put("33", new Token("33", "name3", 0, tokenExpired, false));
		tokens.put("44", new Token("44", "name4", 0, tokenExpired, false));
		this.tokenGeneratorController.setTokens(tokens);
		Assert.assertEquals(5, this.tokenGeneratorController.getTokens().size());
		
		this.tokenGeneratorController.deleteExpiredToken();
		
		Assert.assertEquals(3, this.tokenGeneratorController.getTokens().size());
	}
	
	@SuppressWarnings("unchecked")
	@Test
	public void testDelete() {
		Authentication authentication = Mockito.mock(Authentication.class);
		Mockito.when(authentication.isValid(Mockito.anyMap())).thenReturn(true);
		Mockito.when(authentication.isAdmin(Mockito.anyMap())).thenReturn(true);
		this.tokenGeneratorController.setAuthentication(authentication);
		
		Map<String, Token> tokens = new HashMap<String, Token>();
		String tokenId = "00";
		tokens.put(tokenId, new Token(tokenId, "name0", 0, 0, false));
		this.tokenGeneratorController.setTokens(tokens);
		
		Map<String, String> parameters = new HashMap<String, String>();
		String name = "name";
		String password = "password";
		parameters.put(TokenResource.NAME_FORM, name);
		parameters.put(TokenResource.PASSWORD_FORM, password);
		
		Token token = new Token();
		token.setId(tokenId);
		Assert.assertEquals(1, this.tokenGeneratorController.getTokens().size());
		this.tokenGeneratorController.delete(parameters, token);
		Assert.assertEquals(0, this.tokenGeneratorController.getTokens().size());
	}
	
	@SuppressWarnings("unchecked")
	@Test(expected=ResourceException.class)
	public void testDeleteNotAuthorized() {
		Authentication authentication = Mockito.mock(Authentication.class);
		Mockito.when(authentication.isValid(Mockito.anyMap())).thenReturn(false);
		this.tokenGeneratorController.setAuthentication(authentication);
		
		Map<String, Token> tokens = new HashMap<String, Token>();
		String tokenId = "00";
		tokens.put(tokenId, new Token(tokenId, "name0", 0, 0, false));
		this.tokenGeneratorController.setTokens(tokens);
	
		Assert.assertEquals(1, this.tokenGeneratorController.getTokens().size());
		this.tokenGeneratorController.delete(new HashMap<String, String>(), new Token());
	}	
	
	public static void writeKeyToFile(String content, String path) throws IOException {
		File file = new File(path);

		if (!file.exists()) {
			file.createNewFile();
		}

		FileWriter fw = new FileWriter(file.getAbsoluteFile());
		BufferedWriter bw = new BufferedWriter(fw);
		bw.write(content);
		bw.close();
	}
	
}
