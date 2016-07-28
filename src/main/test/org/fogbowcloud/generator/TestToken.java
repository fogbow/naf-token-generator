package org.fogbowcloud.generator;

import junit.framework.Assert;

import org.apache.commons.codec.binary.Base64;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.Test;

public class TestToken {

	@Test
	public void testToJson() throws JSONException {
		String id = "id";
		String name = "Fulano";
		long cTime = System.currentTimeMillis();
		long eTime = System.currentTimeMillis();
		boolean infinite = false;
		Token token = new Token(id, name, cTime, eTime, infinite);
		
		JSONObject jsonObject = token.toJson();
		Assert.assertEquals(id, jsonObject.getString(Token.ID_TOKEN));
		Assert.assertEquals(name, jsonObject.getString(Token.NAME_TOKEN));
		Assert.assertEquals(cTime, jsonObject.getLong(Token.CTIME_TOKEN));
		Assert.assertEquals(eTime, jsonObject.getLong(Token.ETIME_TOKEN));
		Assert.assertEquals(infinite, jsonObject.getBoolean(Token.INFITINE_TOKEN));
	}
	
	@Test
	public void testToJsonNullValues() throws JSONException {
		String id = null;
		String name = null;
		long cTime = System.currentTimeMillis();
		long eTime = System.currentTimeMillis();
		boolean infinite = true;
		Token token = new Token(id, name, cTime, eTime, infinite);
		
		JSONObject jsonObject = token.toJson();
		Assert.assertTrue(jsonObject.optString(Token.ID_TOKEN).isEmpty());
		Assert.assertTrue(jsonObject.optString(Token.NAME_TOKEN).isEmpty());
		Assert.assertEquals(cTime, jsonObject.getLong(Token.CTIME_TOKEN));
		Assert.assertEquals(eTime, jsonObject.getLong(Token.ETIME_TOKEN));
		Assert.assertEquals(infinite, jsonObject.getBoolean(Token.INFITINE_TOKEN));
	}
	
	@Test
	public void testFromJson() throws JSONException, CloneNotSupportedException {
		String id = "id";
		String name = "name";
		long cTime = System.currentTimeMillis();
		long eTime = System.currentTimeMillis();
		boolean infinite = true;
		Token token = new Token(id, name, cTime, eTime, infinite);
		
		JSONObject jsonObject = token.toJson();
		
		Token tokenTwo = (Token) token.clone();
		tokenTwo.fromJson(jsonObject.toString());

		Assert.assertEquals(token, tokenTwo);
	}
	
	@Test
	public void testGetFinalToken() {
		String id = "id";
		String name = "name";
		long cTime = System.currentTimeMillis();
		long eTime = System.currentTimeMillis();
		boolean infinite = true;
		Token token = new Token(id, name, cTime, eTime, infinite);
		String signature = "signature";
		token.setSignature(signature);
		
		String finalToken = token.toFinalToken();
		Assert.assertEquals(finalToken, new String(Base64.encodeBase64(
				(token.toJson().toString() + Token.SEPARATOR + signature).getBytes())));
	}
	
	@Test
	public void testFromFinalToken() throws Exception {
		String id = "id";
		String name = "name";
		long cTime = System.currentTimeMillis();
		long eTime = System.currentTimeMillis();
		boolean infinite = true;
		Token token = new Token(id, name, cTime, eTime, infinite);
		String signature = "signature";
		token.setSignature(signature);
		
		String finalToken = token.toFinalToken();		
		
		Token tokenTwo = new Token();
		tokenTwo.fromFinalToken(finalToken);
		
		Assert.assertEquals(token, tokenTwo);
	}
	
}
