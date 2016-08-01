package org.fogbowcloud.generator.util;

import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.codec.Charsets;
import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

public class HttpClientWrapper {

	public static final String POST = "POST";
	public static final String GET = "GET";
	public static final String DELETE = "DELETE";
	public static final String PUT = "PUT";
	
	private HttpClient httpClient;
	
	public HttpResponseWrapper doRequest(String url, String method) throws Exception {
		return this.doRequest(url, method, null, null, null);
	}
	
	// TODO refact for other method.
	public HttpResponseWrapper doRequest(String url, String method, HttpEntity entity,
			SSLConnectionSocketFactory sslSocketFactory, 
			Map<String, String> headers) throws Exception {
		HttpRequestBase request = null;
		if (method.equals(POST)) {
			request = new HttpPost(url);
			if (entity != null) {
				((HttpPost) request).setEntity(entity);
			}
		} else if (method.equals(GET)) {
			request = new HttpGet(url);
		} else if (method.equals(DELETE)) {
			request = new HttpDelete(url);
		} else if (method.equals(PUT)) {
			request = new HttpPut(url);
			if (entity != null) {
				((HttpPut) request).setEntity(entity);
			}			
		}
		
		if (headers != null) {
			for (Entry<String, String> header : headers.entrySet()) {
				request.setHeader(header.getKey(), header.getValue());
			}
		}
		HttpResponse response = null;
		String responseStr = null;
		try {
			response = getClient(sslSocketFactory).execute(request);
			responseStr = EntityUtils.toString(response.getEntity(),
					Charsets.UTF_8);
		} catch (Exception e) {
			throw e;
		} finally {
			try {
				response.getEntity().getContent().close();
			} catch (Exception e) {
				// Best effort
			}
		}
		Header firstHeader = response.getFirstHeader("Location");
		return new HttpResponseWrapper(response.getStatusLine(), responseStr,
				firstHeader != null ? firstHeader.getValue() : null);
	}	
	
	private HttpClient getClient(SSLConnectionSocketFactory sslSocketFactory) {
		if (this.httpClient == null) {
			if (sslSocketFactory == null) {
				this.httpClient = HttpClients.createMinimal();
			} else {
				this.httpClient = HttpClients.custom().setSSLSocketFactory(
						sslSocketFactory).build();
			}
		}
		return this.httpClient;
	}	
	
}
