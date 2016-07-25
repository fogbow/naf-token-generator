package org.fogbowcloud.generator;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.PublicKey;
import java.security.interfaces.RSAPrivateKey;
import java.util.UUID;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.fogbowcloud.generator.util.RSAUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.restlet.data.Form;
import org.restlet.engine.adapter.HttpRequest;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ServerResource;

public class TokenResource extends ServerResource  {
	
	private static final String ADMIN_PUBLIC_KEY = "admin_public_key";
	private static final String ADMIN_PRIVATE_KEY = "admin_private_key";

	private static final Logger LOGGER = Logger.getLogger(TokenResource.class);
	
	private static final String ETIME_TOKEN = "token_etime";
	private static final String NAME_TOKEN = "name";
	private static final String ID_TOKEN = "name";
	private static final String CTIME_TOKEN = "token_ctime";

	private static final String SEPARATOR = "!#!";
	
	@Post
	public StringRepresentation post(Representation entity) {
		LOGGER.info("Posting a new token.");
		TokenGereratorApplication application = (TokenGereratorApplication) getApplication();
		HttpRequest req = (HttpRequest) getRequest();
		
		// TODO check values
		// TODO implement tests
		final Form form = new Form(entity);
		String name = form.getFirstValue("name");
		String hours = form.getFirstValue("hours");
		
		application.getUser();
		
		JSONObject createJsonTokenSlice = null;
		try {
			createJsonTokenSlice = createJsonTokenSlice(name, hours);
		} catch (JSONException e) {
			// TODO
		}
		
		// TODO check if private key exists
		RSAPrivateKey privateKey = null;
		try {
			privateKey = RSAUtils.getPrivateKey(application.getProperties().getProperty(ADMIN_PRIVATE_KEY));
		} catch (Exception e) {}
		String jsonTokenSliceSign = null;
		try {
			jsonTokenSliceSign = RSAUtils.sign(privateKey, createJsonTokenSlice.toString());
		} catch (Exception e) {
		}
						
		String toBase64 = new String(Base64.encodeBase64((createJsonTokenSlice.toString() + SEPARATOR + jsonTokenSliceSign).getBytes()));
		return new StringRepresentation(toBase64);
	}
		
	private JSONObject createJsonTokenSlice(String name, String hours) throws JSONException {
		JSONObject jsonObject = new JSONObject();
		jsonObject.put(ID_TOKEN, UUID.randomUUID());
		jsonObject.put(NAME_TOKEN, name);
		long now = System.currentTimeMillis();
		jsonObject.put(CTIME_TOKEN, now);
		jsonObject.put(ETIME_TOKEN, now + (Long.parseLong(hours) * 60 * 60 * 1000));
		return jsonObject;
	}

	@Get
	public StringRepresentation fetch() throws IOException, GeneralSecurityException {
		TokenGereratorApplication application = (TokenGereratorApplication) getApplication();
		String token = getQueryValue("token");
		String[] tokenSlices = token.split(SEPARATOR);
		if (tokenSlices.length != 2) {
			// TODO throw new Exception
		}
		
		PublicKey publicKey = RSAUtils.getPublicKey(ADMIN_PUBLIC_KEY);
		String jsonTokenSlice = tokenSlices[0];
		String signTokenSlice = tokenSlices[1];
		if (!RSAUtils.verify(publicKey, jsonTokenSlice, signTokenSlice)) {
			// TODO throw new Exception
		}
		
		if (application.isRevoked(jsonTokenSlice)) {
			// TODO throw new Exception
		}
		
		return new StringRepresentation("Ok");
	}
	
	@Put
	public StringRepresentation put(Representation entity) {
		TokenGereratorApplication application = (TokenGereratorApplication) getApplication();
		
		final Form form = new Form(entity);
		String name = form.getFirstValue("name");
		String dateStr = form.getFirstValue("date");
		
		if (!application.authenticate()) {
			// TODO throw new Exception
		}
		
		application.revoke(name, dateStr);
		
		return new StringRepresentation("Ok");
	}	
	
}
