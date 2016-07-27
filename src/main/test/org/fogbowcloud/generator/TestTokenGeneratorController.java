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

import org.fogbowcloud.generator.util.ConfigurationConstant;
import org.fogbowcloud.generator.util.RSAUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class TestTokenGeneratorController {

	private static final String DEFAULT_PATH = "src/main/resources";
	private static final String DEFAULT_FILE_PUBLIC_KEY_PATH = DEFAULT_PATH + "/public.pem";
	private static final String DEFAULT_FILE_PRIVATE_KEY_PATH = DEFAULT_PATH + "/private.pem";
	
	private TokenGeneratorController tokenGeneratorController;
	private KeyPair keyPair;
	
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
		
		properties.put(ConfigurationConstant.ADMIN_PRIVATE_KEY, DEFAULT_FILE_PRIVATE_KEY_PATH);
		properties.put(ConfigurationConstant.ADMIN_PUBLIC_KEY, DEFAULT_FILE_PUBLIC_KEY_PATH);
		this.tokenGeneratorController = new TokenGeneratorController(properties);
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
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(TokenResource.NAME_FORM, name);
		parameters.put(TokenResource.HOURS_FORM_POST, hours);
		parameters.put(TokenResource.INFINITE_FORM_POST, infinite);
						
		String finalToken = this.tokenGeneratorController.createToken(parameters);
		Token token = new Token();
		token.fromFinalToken(finalToken);
		
		Assert.assertTrue(this.tokenGeneratorController.verifySign(token.toJson().toString(), token.getSignature()));
		
		Assert.assertEquals(name, token.getName());
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
