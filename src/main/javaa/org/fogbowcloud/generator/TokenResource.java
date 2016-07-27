package org.fogbowcloud.generator;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.restlet.data.Form;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;

public class TokenResource extends ServerResource  {
	
	protected static final String OK_RESPONSE = "Ok";
	protected static final String REVOKE_USER_METHOD_PUT = "revokeUser";
	protected static final String DATE_REVOK_PUT = "dateRevoked";
	
	protected static final String VALIDITY_CHECK_METHOD_GET = "validityCheck";
	protected static final String METHOD_PARAMETER = "method";
	protected static final String TOKEN_QUERY_GET = "token";
	
	protected static final String INFINITE_FORM_POST = "infinite";
	protected static final String HOURS_FORM_POST = "hours";
	protected static final String NAME_FORM = "name";
	
	private static final Logger LOGGER = Logger.getLogger(TokenResource.class);		
	
	@Post
	public StringRepresentation post(Representation entity) {
		LOGGER.info("Posting a new token.");
		TokenGereratorApplication application = (TokenGereratorApplication) getApplication();
		
		final Form form = new Form(entity);		
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(NAME_FORM, form.getFirstValue(NAME_FORM));
		parameters.put(HOURS_FORM_POST, form.getFirstValue(HOURS_FORM_POST));
		parameters.put(INFINITE_FORM_POST, form.getFirstValue(INFINITE_FORM_POST));
		
		// TODO implemente tests
		checkValues(parameters);
		
		// TODO authentication
//		application.getUser(name);
		
		return new StringRepresentation(application.createToken(parameters));
	}

	private boolean checkValues(Map<String, String> parameters) {
		String name = parameters.get(NAME_FORM);
		String hours = parameters.get(HOURS_FORM_POST);
		String infinite = parameters.get(INFINITE_FORM_POST);
		if (name == null || name.isEmpty()) {
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, "Attribute (name) is empty.");			
		}
		try {
			if (hours == null || hours.isEmpty()) {
				throw new ResourceException(HttpStatus.SC_BAD_REQUEST, "Attribute (name) is empty.");
			}
			Integer.parseInt(hours);
		} catch (Exception e) {
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, "Attribute (hours) is not a integer.");
		}
		if (infinite != null && !infinite.equals(Boolean.TRUE) && !infinite.equals(Boolean.TRUE)) {
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, "Attribute (infinite) is not true nor false.");
		}
		return true;
	}

	@Get
	public StringRepresentation fetch() throws IOException, GeneralSecurityException {
		TokenGereratorApplication application = (TokenGereratorApplication) getApplication();
		
		String finalToken = (String) getRequestAttributes().get("token");
		
		if (finalToken == null || finalToken.isEmpty()) {
			return getTokens(application);
		} else {
			return getSpecificToken(application, finalToken);			
		}
	}

	private StringRepresentation getSpecificToken(TokenGereratorApplication application,
			String finalToken) {
		String method = getQueryValue(METHOD_PARAMETER);
		if (!method.equals(VALIDITY_CHECK_METHOD_GET)) {
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, "Method there is not.");
		}
		
		Token token = new Token();
		try {
			token.fromFinalToken(finalToken);
		} catch (Exception e) {
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, "Token malformed.");
		}
		String tokenJson = token.toJson().toString();
				
		if (!application.verifySignature(tokenJson, token.getSignature())) {
			throw new ResourceException(HttpStatus.SC_UNAUTHORIZED, "Signature false.");
		}
		
		if (application.isRevoked(tokenJson)) {
			throw new ResourceException(HttpStatus.SC_UNAUTHORIZED, "Token revoked.");
		}
		
		return new StringRepresentation(OK_RESPONSE);
	}
	
	private StringRepresentation getTokens(TokenGereratorApplication application) {
		String method = getQueryValue(METHOD_PARAMETER);
		
		Collection<Token> tokens = application.getTokens().values();
		StringBuilder stringBuilder = new StringBuilder();
		for (Token token : tokens) {
			stringBuilder.append(token.toFinalToken());
			stringBuilder.append("/n");
		}
		
		return new StringRepresentation(stringBuilder.toString().trim());
	}

	@Delete
	public StringRepresentation put(Representation entity) {
		TokenGereratorApplication application = (TokenGereratorApplication) getApplication();		
		String finalToken = (String) getRequestAttributes().get("token");
		LOGGER.info("Deleting token: " + finalToken);
		
//		TODO implement when discovey who works the LDAP
//		final Form form = new Form(entity);
//		String name = form.getFirstValue(NAME_FORM);
				
		if (!application.authenticate()) {
			throw new ResourceException(HttpStatus.SC_UNAUTHORIZED, "Not authenticate.");
		}
		
		Token token = new Token();
		try {
			token.fromFinalToken(finalToken);
		} catch (Exception e) {
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, "Token malformed.");
		}
		application.delete(token);
		
		return new StringRepresentation(OK_RESPONSE);
	}	
	
}
