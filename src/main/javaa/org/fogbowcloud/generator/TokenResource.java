package org.fogbowcloud.generator;

import java.io.IOException;
import java.security.GeneralSecurityException;

import org.apache.commons.codec.binary.Base64;
import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.restlet.data.Form;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.Put;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

public class TokenResource extends ServerResource  {
	
	protected static final String REVOKE_USER_METHOD_PUT = "revokeUser";
	protected static final String DATE_REVOK_PUT = "dateRevoked";
	
	protected static final String VALIDITY_CHECK_METHOD_GET = "validityCheck";
	protected static final String METHOD_PARAMETER = "method";
	protected static final String TOKEN_QUERY_GET = "token";
	
	protected static final String HOURS_FORM_POST = "hours";
	protected static final String NAME_FORM = "name";
	
	private static final Logger LOGGER = Logger.getLogger(TokenResource.class);		
	
	@Post
	public StringRepresentation post(Representation entity) {
		LOGGER.info("Posting a new token.");
		TokenGereratorApplication application = (TokenGereratorApplication) getApplication();
		
		// TODO check values
		// TODO implement tests
		final Form form = new Form(entity);
		String name = form.getFirstValue(NAME_FORM);
		String hours = form.getFirstValue(HOURS_FORM_POST);
		
		if (!checkValues(name, hours)) {
			// TODO check
		}
		
		application.getUser();
		
		return new StringRepresentation(application.createToken(name, hours));
	}

	private boolean checkValues(String name, String hours) {
		return true;
	}

	@Get
	public StringRepresentation fetch() throws IOException, GeneralSecurityException {
		TokenGereratorApplication application = (TokenGereratorApplication) getApplication();
		String token = getQueryValue(TOKEN_QUERY_GET);
		String method = getQueryValue(METHOD_PARAMETER);
		if (!method.equals(VALIDITY_CHECK_METHOD_GET)) {
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, "Method there is not.");
		}
		
		// TODO refact to application ?		
		token = new String(Base64.decodeBase64(token.getBytes()));
		String[] tokenSlices = token.split(TokenGeneratorController.SEPARATOR);
		if (tokenSlices.length != 2) {
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, "Token malformed.");
		}
		String jsonTokenSlice = tokenSlices[0];
		String signatureTokenSlice = tokenSlices[1];
		
				
		if (!application.verifySignature(jsonTokenSlice, signatureTokenSlice)) {
			throw new ResourceException(HttpStatus.SC_UNAUTHORIZED, "Signature false.");
		}
		
		if (application.isRevoked(jsonTokenSlice)) {
			throw new ResourceException(HttpStatus.SC_UNAUTHORIZED, "Token revoked.");
		}
		
		return new StringRepresentation("Ok");
	}
	
	@Put
	public StringRepresentation put(Representation entity) {
		TokenGereratorApplication application = (TokenGereratorApplication) getApplication();
		
		final Form form = new Form(entity);
		String name = form.getFirstValue(NAME_FORM);
		String method = form.getFirstValue(METHOD_PARAMETER);
		String dateStr = form.getFirstValue(DATE_REVOK_PUT);
		
		if (!method.equals(REVOKE_USER_METHOD_PUT)) {
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, "Method there is not.");
		}
		
		if (!application.authenticate()) {
			throw new ResourceException(HttpStatus.SC_UNAUTHORIZED, "Not authenticate.");
		}
		
		application.revoke(name, dateStr);
		
		return new StringRepresentation("Ok");
	}	
	
}
