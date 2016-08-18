package org.fogbowcloud.generator;

import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.TimerTask;
import java.util.UUID;
import java.util.concurrent.Executors;

import org.apache.http.HttpStatus;
import org.apache.log4j.Logger;
import org.fogbowcloud.generator.auth.Authentication;
import org.fogbowcloud.generator.resources.TokenResource;
import org.fogbowcloud.generator.util.ConfigurationConstants;
import org.fogbowcloud.generator.util.DateUtils;
import org.fogbowcloud.generator.util.ManagerTimer;
import org.fogbowcloud.generator.util.RSAUtils;
import org.fogbowcloud.generator.util.ResponseConstants;
import org.json.JSONObject;
import org.restlet.resource.ResourceException;

public class TokenGeneratorController {
		
	private final ManagerTimer tokenExpirationSchedulerTimer;
	private DateUtils dateUtils;
	
	protected static final int HOURS_IN_MILISECONDS = 60 * 60 *1000;
	
	private static final int DEFAULT_MAXIMUM_HOURS_EXPIRATION = 24; // 24 hours
	private static final Long DEFAULT_SCHEDULER_PERIOD = 120000l; // 2 minutes

	private static final Logger LOGGER = Logger.getLogger(TokenResource.class);
	
	private Properties properties;
	private Authentication authentication;
	
	private TokenDataStore tokenDs;
	
	public TokenGeneratorController(Properties properties) {
		this.properties = properties;
		this.dateUtils = new DateUtils();
		this.tokenExpirationSchedulerTimer = new ManagerTimer(Executors.newScheduledThreadPool(1)); 
		this.tokenDs = new TokenDataStore(properties);
		triggerExpiredTokenScheduler();
	}
	
	public Properties getProperties() {
		return this.properties;
	}
	
	public void setDateUtils(DateUtils dateUtils) {
		this.dateUtils = dateUtils;
	}
	
	public TokenDataStore getTokenDs() {
		return tokenDs;
	}
	
	public void setAuthentication(Authentication authentication) {
		this.authentication = authentication;
	}
	
	private void triggerExpiredTokenScheduler() {
		String tokenExpirationPeriodStr = this.properties.getProperty(ConfigurationConstants
				.TOKEN_EXPIRATION_SCHEDULER_PERIOD_KEY);
		long tokenExpirationPeriod = tokenExpirationPeriodStr == null ? DEFAULT_SCHEDULER_PERIOD : Long.valueOf(tokenExpirationPeriodStr);
		tokenExpirationSchedulerTimer.scheduleAtFixedRate(new TimerTask() {
			@Override
			public void run() {
				try {
					checkExpiredToken();			
				} catch (Throwable e) {
					LOGGER.error("Error while checking token expired.", e);
				}
			}
		}, 0, tokenExpirationPeriod);
	}	

	public boolean isDeleted(String tokenId) {
		
		Token token = null;
		
		try{
			token = tokenDs.getTokenByID(tokenId);
		}catch(Exception e){
			LOGGER.error("Error while checking token deleted.", e);
		}
		
		return token == null ? true: false;
	}

	public void delete(Map<String, String> parameters, Token token) throws Exception {
		if (!authentication.isValid(parameters) || !authentication.isAdmin(parameters)) {
			throw new ResourceException(HttpStatus.SC_UNAUTHORIZED, ResponseConstants.UNAUTHORIZED);
		}
		try{
			tokenDs.removeTokenById(token.getId());
		}catch(Exception e){
			throw new Exception("Error while delete token.", e);
		}
	}
	
	public List<Token> getAllTokens(Map<String, String> parameters) throws Exception {
		if (!authentication.isValid(parameters) || !authentication.isAdmin(parameters)) {
			throw new ResourceException(HttpStatus.SC_UNAUTHORIZED, ResponseConstants.UNAUTHORIZED);
		}		
		
		try{
			return tokenDs.getAllTokens();
		}catch(Exception e){
			throw new Exception("Error while getting all tokens.", e);
		}
	}
	
	protected void checkExpiredToken() throws Exception {
		LOGGER.debug("Checking expired tokens.");
		for (Token token : tokenDs.getAllTokens()) {
			if (token.geteTime() < this.dateUtils.currentTimeMillis() && !token.isInfinite()) {
				LOGGER.debug(token.toJson() + " expired.");
				tokenDs.removeTokenById(token.getId());
			}
		}
	}

	public Token createToken(Map<String, String> parameters) throws Exception {
		if (!this.authentication.isValid(parameters)) {
			throw new ResourceException(HttpStatus.SC_UNAUTHORIZED, ResponseConstants.UNAUTHORIZED);
		}
		
		String name = parameters.get(TokenResource.NAME_FORM);
		String hoursStr = parameters.get(TokenResource.HOURS_FORM_POST);
		int hours = (Integer.parseInt(hoursStr));
		// TODO implement tests
		int maximumHoursExpiration = getMaximumHoursExpiration();
		if (hours > maximumHoursExpiration) {
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, ResponseConstants.HOURS_INVALID 
					+ maximumHoursExpiration);
		}
		String infinite = parameters.get(TokenResource.INFINITE_FORM_POST);
		if (new Boolean(infinite) && !authentication.isAdmin(parameters)) {
			throw new ResourceException(HttpStatus.SC_UNAUTHORIZED, ResponseConstants.UNAUTHORIZED);
		}
				
		long now = this.dateUtils.currentTimeMillis();
		long expirationTime = now + (hours * HOURS_IN_MILISECONDS);
		Token token = new Token(UUID.randomUUID().toString(), name, 
				now, expirationTime, new Boolean(infinite));
		
		JSONObject tokenJson = null;		
		tokenJson = token.toJson();
		if (tokenJson == null) {
			LOGGER.error("Token malformed. " + token.toString());
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, ResponseConstants.TOKEN_MALFORMED);
		}
		
		RSAPrivateKey privateKey = null;
		try {
			privateKey = RSAUtils.getPrivateKey(properties.getProperty(ConfigurationConstants.ADMIN_PRIVATE_KEY));
		} catch (Exception e) {
			LOGGER.error(ResponseConstants.INVALID_PRIVATE_KEY, e);
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, ResponseConstants.INVALID_PRIVATE_KEY);
		}
		String tokenSignature = null;
		try {
			tokenSignature = RSAUtils.sign(privateKey, tokenJson.toString());
			token.setSignature(tokenSignature);
		} catch (Exception e) {
			LOGGER.error(ResponseConstants.ERROR_WHEN_SIGN, e);
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, ResponseConstants.ERROR_WHEN_SIGN);
		}				
		
		try {
 			tokenDs.addToken(token);
		} catch (Exception e) {
			LOGGER.error("Error while saving token :"+token.toFinalToken(), e);
			throw new Exception("Error while saving token :"+token.toFinalToken(), e);
		}
		
		return token;
	}

	private int getMaximumHoursExpiration() {
		int maximumHoursExpiration = DEFAULT_MAXIMUM_HOURS_EXPIRATION;
		try {
			String maximumHoursExpirationPropertiesStr = this.properties.getProperty(
					ConfigurationConstants.MAXIMUM_HOURS_EXPIRATTION);
			maximumHoursExpiration = Integer.parseInt(maximumHoursExpirationPropertiesStr);			
		} catch (Exception e) {
			LOGGER.warn("Maximum hours expiration propeties wrong. Using the default: " + DEFAULT_MAXIMUM_HOURS_EXPIRATION, e);
		}
		return maximumHoursExpiration;
	}
	
	public boolean verifySign(String tokenMessage, String signature) {
		RSAPublicKey publicKey = null;
		try {
			publicKey = RSAUtils.getPublicKey(this.properties.getProperty(
					ConfigurationConstants.ADMIN_PUBLIC_KEY));
		} catch (Exception e) {
			LOGGER.error(ResponseConstants.INVALID_PUBLIC_KEY, e);
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, ResponseConstants.INVALID_PUBLIC_KEY);
		}		
		try {
			return RSAUtils.verify(publicKey, tokenMessage, signature);			
		} catch (Exception e) {
			LOGGER.error("Something wrong at verify.", e);
			throw new ResourceException(HttpStatus.SC_BAD_REQUEST, ResponseConstants.SOMETHING_WRONG_AT_VERIFY 
					+ e.getMessage());
		}
	}

	public boolean isValidToken(Map<String, String> parameters, String finalToken) {
		LOGGER.error("Checking token : " + finalToken);
		
		if (!authentication.isValid(parameters)) {
			throw new ResourceException(HttpStatus.SC_UNAUTHORIZED, ResponseConstants.UNAUTHORIZED);
		}
		
		Token token = new Token();
		try {
			token.fromFinalToken(finalToken);
		} catch (Exception e) {
			LOGGER.error(ResponseConstants.TOKEN_MALFORMED, e);
			return false;
		}
		String tokenJson = token.toJson().toString();
				
		try {
			if (!verifySign(tokenJson, token.getSignature())) {
				LOGGER.error(ResponseConstants.SIGNATURE_FALSE);
				return false;
			}			
		} catch (Exception e) {
			LOGGER.error(ResponseConstants.SIGNATURE_FALSE, e);
			return false;
		}
		
		if (isDeleted(token.getId())) {
			LOGGER.error(ResponseConstants.TOKEN_THERE_IS_NOT);
			return false;
		}
		return true;
	}

}
