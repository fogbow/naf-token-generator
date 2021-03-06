package org.fogbowcloud.generator.resources;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import junit.framework.Assert;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.fogbowcloud.generator.TestHelper;
import org.fogbowcloud.generator.TestTokenGeneratorController;
import org.fogbowcloud.generator.TokenGeneratorController;
import org.fogbowcloud.generator.TokenGereratorApplication;
import org.fogbowcloud.generator.auth.Authentication;
import org.fogbowcloud.generator.util.ConfigurationConstants;
import org.fogbowcloud.generator.util.HttpClientWrapper;
import org.fogbowcloud.generator.util.HttpResponseWrapper;
import org.fogbowcloud.generator.util.RSAUtils;
import org.fogbowcloud.generator.util.ResponseConstants;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.restlet.Component;
import org.restlet.data.Protocol;
import org.restlet.resource.ResourceException;

public class TestTokenResource {
	
	private static final int DEFAULT_HTTP_PORT = 9876;
	private static final String TOKEN_SUFIX_URL = "/token";
	private static final String PREFIX_URL = "http://0.0.0.0:" + DEFAULT_HTTP_PORT;
	private Component http;
	private HttpClientWrapper httpClientWrapper;
	private TokenGeneratorController tokenGeneratorController;
	
	private static final String DEFAULT_FILE_PUBLIC_KEY_PATH = TestHelper.DEFAULT_RESOURCE_PATH + "/public.pem";
	private static final String DEFAULT_FILE_PRIVATE_KEY_PATH = TestHelper.DEFAULT_RESOURCE_PATH + "/private.pem";
	private static final String JDBC_SQLITE_MEMORY = TestHelper.JDBC_SQLITE_MEMORY;
	
	private KeyPair keyPair;
	
	@SuppressWarnings("unchecked")
	@Before
	public void setUp() throws Exception {
		TestHelper.removingFile(TestHelper.DEFAULT_TOKEN_BD_PATH);
		
		Properties properties = new Properties();
		
		
		try {
			this.keyPair = RSAUtils.generateKeyPair();
		} catch (NoSuchAlgorithmException e) {
			Assert.fail();
		}
		
		TestTokenGeneratorController.writeKeyToFile(
				RSAUtils.savePublicKey(this.keyPair.getPublic()),
				DEFAULT_FILE_PUBLIC_KEY_PATH);
		TestTokenGeneratorController.writeKeyToFile(
				RSAUtils.savePrivateKey(this.keyPair.getPrivate()),
				DEFAULT_FILE_PRIVATE_KEY_PATH);
		
		properties.put(ConfigurationConstants.ADMIN_PRIVATE_KEY, DEFAULT_FILE_PRIVATE_KEY_PATH);
		properties.put(ConfigurationConstants.ADMIN_PUBLIC_KEY, DEFAULT_FILE_PUBLIC_KEY_PATH);
		properties.setProperty(ConfigurationConstants.TOKEN_DATASTORE_URL, JDBC_SQLITE_MEMORY);
		
		this.httpClientWrapper = new HttpClientWrapper();
		this.http = new Component();
		
		this.http.getServers().add(Protocol.HTTP, DEFAULT_HTTP_PORT);
		TokenGereratorApplication tokenGereratorApplication = 
				new TokenGereratorApplication(properties);
		this.tokenGeneratorController = Mockito.spy(new TokenGeneratorController(properties));		
		tokenGereratorApplication
				.setTokenGeneratorController(tokenGeneratorController);
		
		Authentication authentication = Mockito.mock(Authentication.class);
		Mockito.when(authentication.isValid(Mockito.anyMap())).thenReturn(true);
		Mockito.when(authentication.isAdmin(Mockito.anyMap())).thenReturn(true);
		this.tokenGeneratorController.setAuthentication(authentication);		
		
		this.http.getDefaultHost().attach(tokenGereratorApplication);
		this.http.start();
	}
	
	@After
	public void tearDown() throws Exception {
		TestHelper.removingFile(TestHelper.DEFAULT_TOKEN_BD_PATH);
		TestHelper.removingFile(TestHelper.DEFAULT_FILE_PUBLIC_KEY_PATH);
		TestHelper.removingFile(TestHelper.DEFAULT_FILE_PRIVATE_KEY_PATH);
		this.http.stop();
	}
	
	@Test
	public void testPost() throws Exception {
		
		Assert.assertEquals(0, tokenGeneratorController.getAllTokens(new HashMap<String, String>()).size());
		
		List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
		urlParameters.add(new BasicNameValuePair(TokenResource.NAME_FORM, "Fulano"));
		urlParameters.add(new BasicNameValuePair(TokenResource.HOURS_FORM_POST, "2"));
		HttpEntity entity = new UrlEncodedFormEntity(urlParameters);
		HttpResponseWrapper httpResponseWrapper = this.httpClientWrapper.doRequest(
				PREFIX_URL + TOKEN_SUFIX_URL, HttpClientWrapper.POST, entity, null, null);
		Assert.assertEquals(HttpStatus.SC_OK, httpResponseWrapper.getStatusLine().getStatusCode());
		Assert.assertNotNull(httpResponseWrapper.getContent());
		Assert.assertEquals(1, tokenGeneratorController.getAllTokens(new HashMap<String, String>()).size());
		Assert.assertTrue(!tokenGeneratorController
				.getAllTokens(new HashMap<String, String>()).get(0).getSignature().isEmpty());		
	}
	
	@Test
	public void testGetSpecificToken() throws Exception { 
		List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
		urlParameters.add(new BasicNameValuePair(TokenResource.NAME_FORM, "Fulano"));
		urlParameters.add(new BasicNameValuePair(TokenResource.HOURS_FORM_POST, "2"));
		HttpEntity entity = new UrlEncodedFormEntity(urlParameters);
		String url = PREFIX_URL + TOKEN_SUFIX_URL;
		HttpResponseWrapper httpResponseWrapper = this.httpClientWrapper.doRequest(
				url, HttpClientWrapper.POST, entity, null, null);
		Assert.assertEquals(HttpStatus.SC_OK, httpResponseWrapper.getStatusLine().getStatusCode());
		String token = httpResponseWrapper.getContent();
		Assert.assertNotNull(token);
		
		httpResponseWrapper = this.httpClientWrapper.doRequest(url + "/" + token + "?" + TokenResource.METHOD_PARAMETER + "=" 
				+ TokenResource.VALIDITY_CHECK_METHOD_GET , HttpClientWrapper.GET);
		Assert.assertEquals(TokenResource.VALID_RESPONSE, httpResponseWrapper.getContent());
	}	
	
	@Test
	public void testGetSpecificInvalidToken() throws Exception { 
		List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
		urlParameters.add(new BasicNameValuePair(TokenResource.NAME_FORM, "Fulano"));
		urlParameters.add(new BasicNameValuePair(TokenResource.HOURS_FORM_POST, "2"));
		HttpEntity entity = new UrlEncodedFormEntity(urlParameters);
		String url = PREFIX_URL + TOKEN_SUFIX_URL;
		HttpResponseWrapper httpResponseWrapper = this.httpClientWrapper.doRequest(
				url, HttpClientWrapper.POST, entity, null, null);
		Assert.assertEquals(HttpStatus.SC_OK, httpResponseWrapper.getStatusLine().getStatusCode());
		String token = httpResponseWrapper.getContent();
		Assert.assertNotNull(token);
		
		httpResponseWrapper = this.httpClientWrapper.doRequest(url + "/" + "worng" + "?" + TokenResource.METHOD_PARAMETER + "=" 
				+ TokenResource.VALIDITY_CHECK_METHOD_GET , HttpClientWrapper.GET);
		Assert.assertEquals(TokenResource.INVALID_RESPONSE, httpResponseWrapper.getContent());
	}		
	
	@Test
	public void testGetTokens() throws Exception { 
		List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
		urlParameters.add(new BasicNameValuePair(TokenResource.NAME_FORM, "Fulano"));
		urlParameters.add(new BasicNameValuePair(TokenResource.HOURS_FORM_POST, "2"));
		HttpEntity entity = new UrlEncodedFormEntity(urlParameters);
		String url = PREFIX_URL + TOKEN_SUFIX_URL;
		
		// create token one		
		HttpResponseWrapper httpResponseWrapper = this.httpClientWrapper.doRequest(
				url, HttpClientWrapper.POST, entity, null, null);
		Assert.assertEquals(HttpStatus.SC_OK, httpResponseWrapper.getStatusLine().getStatusCode());
		String content = httpResponseWrapper.getContent();
		Assert.assertNotNull(content);
		
		// create token two
		httpResponseWrapper = this.httpClientWrapper.doRequest(
				url, HttpClientWrapper.POST, entity, null, null);
		Assert.assertEquals(HttpStatus.SC_OK, httpResponseWrapper.getStatusLine().getStatusCode());
		content = httpResponseWrapper.getContent();
		Assert.assertNotNull(content);		
		
		// TODO check more
		httpResponseWrapper = this.httpClientWrapper.doRequest(url, HttpClientWrapper.GET);
		Assert.assertEquals(2, httpResponseWrapper.getContent().split("\n").length);
	}		
	
	@Test
	public void testDelete() throws Exception {
		
		Assert.assertEquals(0, tokenGeneratorController.getAllTokens(new HashMap<String, String>()).size());
		
		List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
		urlParameters.add(new BasicNameValuePair(TokenResource.NAME_FORM, "Fulano"));
		urlParameters.add(new BasicNameValuePair(TokenResource.HOURS_FORM_POST, "2"));
		HttpEntity entity = new UrlEncodedFormEntity(urlParameters);
		HttpResponseWrapper httpResponseWrapper = this.httpClientWrapper.doRequest(
				PREFIX_URL + TOKEN_SUFIX_URL, HttpClientWrapper.POST, entity, null, null);
		Assert.assertEquals(HttpStatus.SC_OK, httpResponseWrapper.getStatusLine().getStatusCode());
		Assert.assertNotNull(httpResponseWrapper.getContent());
		Assert.assertEquals(1, tokenGeneratorController.getAllTokens(new HashMap<String, String>()).size());
		
		httpResponseWrapper = this.httpClientWrapper.doRequest(
				PREFIX_URL + TOKEN_SUFIX_URL, HttpClientWrapper.POST, entity, null, null);
		Assert.assertEquals(HttpStatus.SC_OK, httpResponseWrapper.getStatusLine().getStatusCode());
		Assert.assertEquals(HttpStatus.SC_OK, httpResponseWrapper.getStatusLine().getStatusCode());
		Assert.assertNotNull(httpResponseWrapper.getContent());
		Assert.assertEquals(2, tokenGeneratorController.getAllTokens(new HashMap<String, String>()).size());	
		
		String finalToken = httpResponseWrapper.getContent();
		httpResponseWrapper = this.httpClientWrapper.doRequest(
				PREFIX_URL + TOKEN_SUFIX_URL + "/" + finalToken, HttpClientWrapper.DELETE, entity, null, null);
		Assert.assertEquals(TokenResource.OK_RESPONSE, httpResponseWrapper.getContent());
		
		Assert.assertEquals(1, tokenGeneratorController.getAllTokens(new HashMap<String, String>()).size());	
	}
	
	@Test
	public void testCheckValues() {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(TokenResource.NAME_FORM, "cicleno");
		parameters.put(TokenResource.HOURS_FORM_POST, "2");
		parameters.put(TokenResource.INFINITE_FORM_POST, "true");
		TokenResource.checkValues(parameters);			
	}
	
	@Test
	public void testCheckValuesNameEmpty() {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(TokenResource.NAME_FORM, "");
		parameters.put(TokenResource.HOURS_FORM_POST, "2");
		parameters.put(TokenResource.INFINITE_FORM_POST, "true");
		try {
			TokenResource.checkValues(parameters);						
		} catch (ResourceException e) {
			Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getStatus().getCode());
			Assert.assertTrue(e.getMessage().contains(ResponseConstants.ATTRIBUTE_NAME_IS_EMPTY));
			return;
		}
		Assert.fail();
	}	
	
	@Test
	public void testCheckValuesHoursInvalidValue() {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(TokenResource.NAME_FORM, "fulano");
		parameters.put(TokenResource.HOURS_FORM_POST, "wrong");
		parameters.put(TokenResource.INFINITE_FORM_POST, "true");
		try {
			TokenResource.checkValues(parameters);						
		} catch (ResourceException e) {
			Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getStatus().getCode());
			Assert.assertTrue(e.getMessage().contains(ResponseConstants.ATTRIBUTE_HOURS_IS_NOT_A_INTEGER));
			return;
		}
		Assert.fail();
	}		
	
	@Test
	public void testCheckValuesInfiniteInvalidValue() {
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(TokenResource.NAME_FORM, "fulano");
		parameters.put(TokenResource.HOURS_FORM_POST, "2");
		parameters.put(TokenResource.INFINITE_FORM_POST, "wrong");
		try {
			TokenResource.checkValues(parameters);						
		} catch (ResourceException e) {
			Assert.assertEquals(HttpStatus.SC_BAD_REQUEST, e.getStatus().getCode());
			Assert.assertTrue(e.getMessage().contains(ResponseConstants.
					ATTRIBUTE_INFINITE_IS_NOT_TRUE_NOR_FALSE));
			return;
		}
		Assert.fail();
	}		
	
}
