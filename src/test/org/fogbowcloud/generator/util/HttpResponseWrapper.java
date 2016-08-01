package org.fogbowcloud.generator.util;

import org.apache.http.StatusLine;

public class HttpResponseWrapper {

	private StatusLine statusLine;
	private String content;
	private String url;
	
	public HttpResponseWrapper(StatusLine statusLine, String content, String url) {
		this.statusLine = statusLine;
		this.content = content;
		this.url = url;
	}
	
	public HttpResponseWrapper(StatusLine statusLine, String content) {
		this(statusLine, content, null);
	}	

	public StatusLine getStatusLine() {
		return statusLine;
	}

	public void setStatusLine(StatusLine statusLine) {
		this.statusLine = statusLine;
	}

	public String getContent() {
		return content;
	}

	public void setContent(String content) {
		this.content = content;
	}

	public String getUrl() {
		return url;
	}

	public void setUrl(String url) {
		this.url = url;
	}
		
}
