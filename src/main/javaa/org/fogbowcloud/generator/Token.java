package org.fogbowcloud.generator;

import org.apache.commons.codec.binary.Base64;
import org.apache.log4j.Logger;
import org.json.JSONException;
import org.json.JSONObject;

public class Token {

	public static final String ETIME_TOKEN = "token_etime";
	public static final String CTIME_TOKEN = "token_ctime";
	public static final String NAME_TOKEN = "name";
	public static final String ID_TOKEN = "id";
	public static final String SEPARATOR = "!#!";
	public static final String INFITINE_TOKEN = "infinite";
	private static final Logger LOGGER = Logger.getLogger(Token.class);

	private String id;
	private String name;
	private long cTime;
	private long eTime;
	private boolean infinite;

	private String signature;

	public Token() {
	}
	
	// TODO implement tests
	public Token(String id, String name, long cTime, long eTime,
			boolean infinite) {
		super();
		this.id = id;
		this.name = name;
		this.cTime = cTime;
		this.eTime = eTime;
		this.infinite = infinite;
	}

	public JSONObject toJson() {
		JSONObject jsonObject = null;
		try {
			jsonObject = new JSONObject();			
			jsonObject.put(ID_TOKEN, id);
			jsonObject.put(NAME_TOKEN, name);
			jsonObject.put(CTIME_TOKEN, cTime);
			jsonObject.put(ETIME_TOKEN, eTime);
			jsonObject.put(INFITINE_TOKEN, new Boolean(infinite));
			return jsonObject;
		} catch (Exception e) {
			LOGGER.warn("Error while generation json format.");
		}
		return null;
	}

	public Token fromJson(String jsonStr) throws JSONException {
		JSONObject jsonObject = new JSONObject(jsonStr);
		setId(jsonObject.optString(ID_TOKEN));
		setName(jsonObject.optString(NAME_TOKEN));
		setcTime(jsonObject.optLong(CTIME_TOKEN));
		seteTime(jsonObject.optLong(ETIME_TOKEN));
		setInfinite(jsonObject.optBoolean(INFITINE_TOKEN));
		return null;
	}
	
	public String toFinalToken() {
		return new String(Base64.encodeBase64((toJson() + SEPARATOR + signature).getBytes()));
	}
	
	public void fromFinalToken(String finalToken) throws Exception {
		if (finalToken == null) {
			// TODO review this exception
			throw new Exception("Final token can not null.");
		}
		String decoded = new String(Base64.decodeBase64(finalToken.getBytes()));
		String[] tokenSlices = decoded.split(SEPARATOR);
		if (tokenSlices.length != 2) {
			throw new Exception("Final token malformed.");
		}
		fromJson(tokenSlices[0]);
		setSignature(tokenSlices[1]);
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}

	public String getName() {
		return name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public long getcTime() {
		return cTime;
	}

	public void setcTime(long cTime) {
		this.cTime = cTime;
	}

	public long geteTime() {
		return eTime;
	}

	public void seteTime(long eTime) {
		this.eTime = eTime;
	}

	public boolean isInfinite() {
		return infinite;
	}

	public void setInfinite(boolean infinite) {
		this.infinite = infinite;
	}

	public String getSignature() {
		return signature;
	}

	public void setSignature(String signature) {
		this.signature = signature;
	}

	@Override
	public String toString() {
		return "Token [id=" + id + ", name=" + name + ", cTime=" + cTime
				+ ", eTime=" + eTime + ", infinite=" + infinite
				+ ", signature=" + signature + "]";
	}	
}
