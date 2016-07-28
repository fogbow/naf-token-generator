package org.fogbowcloud.generator.resources;

import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.Assert;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.fogbowcloud.generator.TestTokenGeneratorController;
import org.fogbowcloud.generator.TokenGeneratorController;
import org.fogbowcloud.generator.TokenGereratorApplication;
import org.fogbowcloud.generator.auth.Authentication;
import org.fogbowcloud.generator.resources.TokenResource;
import org.fogbowcloud.generator.util.ConfigurationConstant;
import org.fogbowcloud.generator.util.HttpClientWrapper;
import org.fogbowcloud.generator.util.HttpResponseWrapper;
import org.fogbowcloud.generator.util.RSAUtils;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;
import org.restlet.Component;
import org.restlet.data.Protocol;

public class TestTokenResource {
	
	private static final int DEFAULT_HTTP_PORT = 9876;
	private static final String TOKEN_SUFIX_URL = "/token";
	private static final String PREFIX_URL = "http://0.0.0.0:" + DEFAULT_HTTP_PORT;
	private Component http;
	private HttpClientWrapper httpClientWrapper;
	private TokenGeneratorController tokenGeneratorController;
	
	private static final String DEFAULT_PATH = "src/main/resources";
	private static final String DEFAULT_FILE_PUBLIC_KEY_PATH = DEFAULT_PATH + "/public.pem";
	private static final String DEFAULT_FILE_PRIVATE_KEY_PATH = DEFAULT_PATH + "/private.pem";
	
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
		
		TestTokenGeneratorController.writeKeyToFile(
				RSAUtils.savePublicKey(this.keyPair.getPublic()),
				DEFAULT_FILE_PUBLIC_KEY_PATH);
		TestTokenGeneratorController.writeKeyToFile(
				RSAUtils.savePrivateKey(this.keyPair.getPrivate()),
				DEFAULT_FILE_PRIVATE_KEY_PATH);
		
		properties.put(ConfigurationConstant.ADMIN_PRIVATE_KEY, DEFAULT_FILE_PRIVATE_KEY_PATH);
		properties.put(ConfigurationConstant.ADMIN_PUBLIC_KEY, DEFAULT_FILE_PUBLIC_KEY_PATH);
		
		this.httpClientWrapper = new HttpClientWrapper();
		this.http = new Component();
		
		this.http.getServers().add(Protocol.HTTP, DEFAULT_HTTP_PORT);
		TokenGereratorApplication tokenGereratorApplication = 
				new TokenGereratorApplication(null);
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
		this.http.stop();
	}
	
	@Test
	public void testPost() throws Exception {
		Assert.assertEquals(0, this.tokenGeneratorController.getTokens().size());
		
		List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
		urlParameters.add(new BasicNameValuePair(TokenResource.NAME_FORM, "Fulano"));
		urlParameters.add(new BasicNameValuePair(TokenResource.HOURS_FORM_POST, "2"));
		HttpEntity entity = new UrlEncodedFormEntity(urlParameters);
		HttpResponseWrapper httpResponseWrapper = this.httpClientWrapper.doRequest(
				PREFIX_URL + TOKEN_SUFIX_URL, HttpClientWrapper.POST, entity, null, null);
		Assert.assertEquals(HttpStatus.SC_OK, httpResponseWrapper.getStatusLine().getStatusCode());
		Assert.assertNotNull(httpResponseWrapper.getContent());
		Assert.assertEquals(1, this.tokenGeneratorController.getTokens().size());
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
		System.out.println(httpResponseWrapper.getContent());
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
		Assert.assertEquals(0, this.tokenGeneratorController.getTokens().size());
		
		List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
		urlParameters.add(new BasicNameValuePair(TokenResource.NAME_FORM, "Fulano"));
		urlParameters.add(new BasicNameValuePair(TokenResource.HOURS_FORM_POST, "2"));
		HttpEntity entity = new UrlEncodedFormEntity(urlParameters);
		HttpResponseWrapper httpResponseWrapper = this.httpClientWrapper.doRequest(
				PREFIX_URL + TOKEN_SUFIX_URL, HttpClientWrapper.POST, entity, null, null);
		Assert.assertEquals(HttpStatus.SC_OK, httpResponseWrapper.getStatusLine().getStatusCode());
		Assert.assertNotNull(httpResponseWrapper.getContent());
		Assert.assertEquals(1, this.tokenGeneratorController.getTokens().size());
		
		httpResponseWrapper = this.httpClientWrapper.doRequest(
				PREFIX_URL + TOKEN_SUFIX_URL, HttpClientWrapper.POST, entity, null, null);
		Assert.assertEquals(HttpStatus.SC_OK, httpResponseWrapper.getStatusLine().getStatusCode());
		Assert.assertEquals(HttpStatus.SC_OK, httpResponseWrapper.getStatusLine().getStatusCode());
		Assert.assertNotNull(httpResponseWrapper.getContent());
		Assert.assertEquals(2, this.tokenGeneratorController.getTokens().size());	
		
		String finalToken = httpResponseWrapper.getContent();
		httpResponseWrapper = this.httpClientWrapper.doRequest(
				PREFIX_URL + TOKEN_SUFIX_URL + "/" + finalToken, HttpClientWrapper.DELETE, entity, null, null);
		Assert.assertEquals(TokenResource.OK_RESPONSE, httpResponseWrapper.getContent());
		
		Assert.assertEquals(1, this.tokenGeneratorController.getTokens().size());	
	}
	
}
