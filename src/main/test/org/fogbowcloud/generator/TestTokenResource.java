package org.fogbowcloud.generator;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import junit.framework.Assert;

import org.apache.http.HttpEntity;
import org.apache.http.HttpStatus;
import org.apache.http.NameValuePair;
import org.apache.http.client.entity.UrlEncodedFormEntity;
import org.apache.http.message.BasicNameValuePair;
import org.fogbowcloud.generator.util.ConfigurationConstant;
import org.fogbowcloud.generator.util.HttpClientWrapper;
import org.fogbowcloud.generator.util.HttpResponseWrapper;
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
	
	@Before
	public void setUp() throws Exception {
		Properties properties = new Properties();
		// TODO create a private and public to test
		properties.put(ConfigurationConstant.ADMIN_PRIVATE_KEY, "/home/chicog/Downloads/private_key.pem");
		properties.put(ConfigurationConstant.ADMIN_PUBLIC_KEY, "/home/chicog/Downloads/public_key.pem");
		this.httpClientWrapper = new HttpClientWrapper();
		this.http = new Component();
		
		this.http.getServers().add(Protocol.HTTP, DEFAULT_HTTP_PORT);
		TokenGereratorApplication tokenGereratorApplication = 
				new TokenGereratorApplication(null);
		this.tokenGeneratorController = Mockito.spy(new TokenGeneratorController(properties));		
		tokenGereratorApplication
				.setTokenGeneratorController(tokenGeneratorController);
		
		this.http.getDefaultHost().attach(tokenGereratorApplication);
		this.http.start();
	}
	
	@After
	public void tearDown() throws Exception {
		this.http.stop();
	}
	
	@Test
	public void testPost() throws Exception { 
		List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
		urlParameters.add(new BasicNameValuePair(TokenResource.NAME_FORM, "Fulano"));
		urlParameters.add(new BasicNameValuePair(TokenResource.HOURS_FORM_POST, "2"));
		HttpEntity entity = new UrlEncodedFormEntity(urlParameters);
		HttpResponseWrapper httpResponseWrapper = this.httpClientWrapper.doRequest(
				PREFIX_URL + TOKEN_SUFIX_URL, HttpClientWrapper.POST, entity, null, null);
		Assert.assertEquals(HttpStatus.SC_OK, httpResponseWrapper.getStatusLine().getStatusCode());
		Assert.assertNotNull(httpResponseWrapper.getContent());
	}
	
	@Test
	public void testGet() throws Exception { 
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
		
		httpResponseWrapper = this.httpClientWrapper.doRequest(url + "?" + TokenResource.TOKEN_QUERY_GET + "=" 
				+ token + "&" + TokenResource.METHOD_PARAMETER + "=" 
				+ TokenResource.VALIDITY_CHECK_METHOD_GET , HttpClientWrapper.GET);
		Assert.assertEquals("Ok", httpResponseWrapper.getContent());
	}	
	
	@Test
	public void testPut() throws Exception {
		List<NameValuePair> urlParameters = new ArrayList<NameValuePair>();
		urlParameters.add(new BasicNameValuePair(TokenResource.NAME_FORM, "Fulano"));
		urlParameters.add(new BasicNameValuePair(TokenResource.DATE_REVOK_PUT, 
				"20/10/1990"));
		urlParameters.add(new BasicNameValuePair(TokenResource.METHOD_PARAMETER, 
				TokenResource.REVOKE_USER_METHOD_PUT));
		HttpEntity entity = new UrlEncodedFormEntity(urlParameters);
		HttpResponseWrapper httpResponseWrapper = this.httpClientWrapper.doRequest(
				PREFIX_URL + TOKEN_SUFIX_URL, HttpClientWrapper.PUT, entity, null, null);
		
		Assert.assertEquals("Ok", httpResponseWrapper.getContent());
	}
	
}
