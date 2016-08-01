package org.fogbowcloud.generator.resources;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.fogbowcloud.generator.Token;
import org.fogbowcloud.generator.TokenGereratorApplication;
import org.fogbowcloud.generator.util.ResponseConstants;
import org.restlet.data.Form;
import org.restlet.data.Header;
import org.restlet.engine.adapter.HttpRequest;
import org.restlet.representation.Representation;
import org.restlet.representation.StringRepresentation;
import org.restlet.resource.Delete;
import org.restlet.resource.Get;
import org.restlet.resource.Post;
import org.restlet.resource.ResourceException;
import org.restlet.resource.ServerResource;
import org.restlet.util.Series;

public class TokenResource extends ServerResource  {

	protected static final String OK_RESPONSE = "Ok";
	protected static final String VALID_RESPONSE = "Valid";
	protected static final String INVALID_RESPONSE = "Invalid";
	protected static final String REVOKE_USER_METHOD_PUT = "revokeUser";
	protected static final String DATE_REVOK_PUT = "dateRevoked";
	
	protected static final String VALIDITY_CHECK_METHOD_GET = "validityCheck";
	protected static final String METHOD_PARAMETER = "method";
	protected static final String TOKEN_QUERY_GET = "token";
	
	public static final String INFINITE_FORM_POST = "infinite";
	public static final String HOURS_FORM_POST = "hours";
	public static final String NAME_FORM = "name";
	public static final String PASSWORD_FORM = "password";
	
	private static final Logger LOGGER = Logger.getLogger(TokenResource.class);		
	
	@Post
	public StringRepresentation post(Representation entity) {
		LOGGER.info("Posting a new token.");
		TokenGereratorApplication application = (TokenGereratorApplication) getApplication();
		
		final Form form = new Form(entity);
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(NAME_FORM, form.getFirstValue(NAME_FORM));
		parameters.put(PASSWORD_FORM, form.getFirstValue(PASSWORD_FORM));
		parameters.put(HOURS_FORM_POST, form.getFirstValue(HOURS_FORM_POST));
		parameters.put(INFINITE_FORM_POST, form.getFirstValue(INFINITE_FORM_POST));
		
		checkValues(parameters);
		
		return new StringRepresentation(application.createToken(parameters));
	}

	protected static boolean checkValues(Map<String, String> parameters) {
		String name = parameters.get(NAME_FORM);
		String hours = parameters.get(HOURS_FORM_POST);
		String infinite = parameters.get(INFINITE_FORM_POST);
		if (name == null || name.isEmpty()) {
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, 
					ResponseConstants.ATTRIBUTE_NAME_IS_EMPTY);			
		}
		try {
			if (hours == null || hours.isEmpty()) {
				throw new ResourceException(HttpStatus.SC_BAD_REQUEST, 
						ResponseConstants.ATTRIBUTE_NAME_IS_EMPTY);
			}
			Integer.parseInt(hours);
		} catch (Exception e) {
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, 
					ResponseConstants.ATTRIBUTE_HOURS_IS_NOT_A_INTEGER);
		}
		if (infinite != null && (!infinite.endsWith(Boolean.TRUE.toString())) 
				&& !infinite.equals(Boolean.FALSE.toString())) {
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, 
					ResponseConstants.ATTRIBUTE_INFINITE_IS_NOT_TRUE_NOR_FALSE);
		}
		return true;
	}

	@Get
	public StringRepresentation fetch() throws IOException, GeneralSecurityException {
		TokenGereratorApplication application = (TokenGereratorApplication) getApplication();
		HttpRequest req = (HttpRequest) getRequest();
		
		Series<Header> headers = req.getHeaders();
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(NAME_FORM, headers.getFirstValue(NAME_FORM));
		parameters.put(PASSWORD_FORM, headers.getFirstValue(PASSWORD_FORM));		
		
		String finalToken = (String) getRequestAttributes().get("token");		
		if (finalToken == null || finalToken.isEmpty()) {
			return getTokens(application, parameters);
		} else {
			return getSpecificToken(application, finalToken, parameters);			
		}
	}

	private StringRepresentation getSpecificToken(TokenGereratorApplication application,
			String finalToken, Map<String, String> parameters) {
		String method = getQueryValue(METHOD_PARAMETER);
		if (!method.equals(VALIDITY_CHECK_METHOD_GET)) {
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, 
					ResponseConstants.METHOD_THERE_IS_NOT);
		}	
		
		if (!application.isValidToken(parameters, finalToken)) {
			return new StringRepresentation(INVALID_RESPONSE);
		}
		
		return new StringRepresentation(VALID_RESPONSE);
	}
	
	private StringRepresentation getTokens(TokenGereratorApplication application,
			Map<String, String> parameters) {
		
		Collection<Token> tokens = application.getTokens(parameters).values();
		StringBuilder stringBuilder = new StringBuilder();
		for (Token token : tokens) {
			stringBuilder.append(token.toJson());
			stringBuilder.append(" " + Token.SEPARATOR + " ");
			stringBuilder.append(token.toFinalToken());
			stringBuilder.append("\n");
		}
		
		return new StringRepresentation(stringBuilder.toString().trim());
	}

	@Delete
	public StringRepresentation put(Representation entity) {
		TokenGereratorApplication application = (TokenGereratorApplication) getApplication();		
		String finalToken = (String) getRequestAttributes().get("token");
		LOGGER.info("Deleting token: " + finalToken);
		
		final Form form = new Form(entity);
		Map<String, String> parameters = new HashMap<String, String>();
		parameters.put(NAME_FORM, form.getFirstValue(NAME_FORM));
		parameters.put(PASSWORD_FORM, form.getFirstValue(PASSWORD_FORM));		
		
		Token token = new Token();
		try {
			token.fromFinalToken(finalToken);
		} catch (Exception e) {
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, 
					ResponseConstants.TOKEN_MALFORMED);
		}
		application.delete(parameters, token);
		
		return new StringRepresentation(OK_RESPONSE);
	}	
	
}
