/* 
 * Pandoroid - An open source Pandora Internet Radio client for Android.
 * 
 * Copyright (C) 2012  Dylan Powers <dylan.kyle.powers@gmail.com>
 * 
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.pandoroid.pandora.RPC;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.HashMap;
import java.util.Map;

import javax.crypto.Cipher;

import org.apache.http.client.HttpResponseException;
import org.json.JSONException;
import org.json.JSONObject;

import com.pandoroid.JSON.JSONHelper;
import com.pandoroid.pandora.Crypto;
import com.pandoroid.pandora.PandoraAPIModifiedException;
import com.pandoroid.pandora.PartnerCredentials;
import com.pandoroid.pandora.PartnerCredentials.CredentialType;

/**
 * Description: A class that implements the JSON specific aspects of the RPC.
 *  If Pandora decides to stop using JSON, this is the part to rewrite!
 * 
 * @author Dylan Powers <dylan.kyle.powers@gmail.com>
 *
 */
public abstract class JsonRPC extends RPC{	
	protected static final String PROTOCOL_VERSION = "5";

	private Crypto mCipher;
	protected PartnerCredentials mCredentials;
	protected HashMap<String, String> mStandardURLParams = new HashMap<String, String>();
	private long mSyncTime = 0;
	private long mSyncObtainedTime = 0;
	
	protected JsonRPC(boolean pandoraOneFlag, String userAgent) 
			         throws GeneralSecurityException{
		super(userAgent);
		if (pandoraOneFlag){
			mCredentials = new PartnerCredentials(CredentialType.PANDORA_ONE_JSON);
		}
		else{
			mCredentials = new PartnerCredentials(CredentialType.STANDARD_USER_JSON);
		}
		mCipher = new Crypto(mCredentials.encryptKey.getBytes(), 
		                     mCredentials.decryptKey.getBytes());
		super.setURL(mCredentials.rpcURL);
	}
	
	/**
	 * Description: Keeps track of the remote server's sync time.
	 * @return An integer for the current sync time.
	 */
	private int calcSync(){
		return (int) (mSyncTime 
				      + ((System.currentTimeMillis() / 1000L) 
				         - mSyncObtainedTime));
	}
	
	
	/**
	 * Description: Here we are making our remote procedure call specifically
	 * 	using Pandora's JSON protocol. This will return a Map<String, Object> holding
	 * 	the contents of the results key in the response. If an error occurs
	 * 	(ie "stat":"fail") an exception with the message body will be thrown.
	 * Caution: When debugging, be sure to note that most data that flows 
	 *  through here is time sensitive, and if stopped in the wrong places,
	 *  it will cause "stat":"fail" responses from the remote server.
	 *  @throws PandoraAPIModifiedException if an unexpected server response has occurred.
	 *  	This could denote a change in the API.
	 *  @throws RPCException if the server call failed in a predictable way with
	 *  	an error code as the API suggests.
	 *  @throws IOException if a network problem occurred or a connection can't be made.
	 *  @throws HttpResponseException if the HTTP server response is not equal
	 *  	to a 2XX code.
	 *  @throws Exception when an unrecoverable error has occurred.
	 */
	protected Map<String, Object> doCall(String method, 
			                  			 Map<String, Object> jsonParams,
			                             boolean httpSecureFlag, 
			                             boolean encrypt,
						                 Map<String, String> optURLParams) 
						                throws PandoraAPIModifiedException, 
						                       RPCException,
						                       IOException,
						                       HttpResponseException,
						                       Exception{
		JSONObject response = null;
		JSONObject request = null;
		if (jsonParams != null){
			request = new JSONObject(jsonParams);
		}
		else{
			request = new JSONObject();
		}
		
		Map<String, String> 
			urlParams = new HashMap<String, String>(mStandardURLParams);
		urlParams.put("method", method);
		if (optURLParams != null){
			urlParams.putAll(optURLParams);
		}
		
		if (mSyncTime != 0){
			request.put("syncTime", calcSync());			
		}
		
		String requestString = request.toString();
		if (encrypt){
			requestString = mCipher.encrypt(requestString);
		}		
		
		String responseString = call(urlParams, requestString, httpSecureFlag);
		response = new JSONObject(responseString);
		JSONObject result;
		try{
			if (response.getString("stat").compareTo("ok") != 0){
				if (response.getString("stat").compareTo("fail") == 0){
					throw new RPCException(response.getInt("code"),
										   response.getString("message"));
				}
				else{
					throw new PandoraAPIModifiedException(
							"RPC unknown error result. stat: " + 
				            response.getString("stat"));
				}
			}
			
			result = response.getJSONObject("result");
		}
		catch (JSONException e){
			throw new PandoraAPIModifiedException(
					"A parameter is missing from the RPC response.", e);
		}
		return JSONHelper.toMap(result);
	}
	
	
	/**
	 * Description: The sync time from the remote server is rather special (or maybe not).
	 * 	It comes in hexadecimal form from which it must be dehexed to byte form,
	 * 	then it must be decrypted with the Blowfish decryption. From there,
	 *  it's hidden inside a string with 4 bytes of junk characters at the 
	 *  beginning, and two white space characters at the end.
	 *  All that, and using the system time works just fine LOL.
	 * @param encodedSyncTime -The sync time in an encrypted hex form.
	 */
	protected void setSync(String encodedSyncTime){
		mSyncObtainedTime = System.currentTimeMillis() / 1000L; 
		
//		//This time stamp contains a lot of junk in the string it's in.
//		String junk = pandoraDecrypt(encoded_sync_time); //First decrypt the hex
//		//Don't use junk.substring()! Java Strings use 2 byte characters.
//		//Remove the first 4 bytes of junk. 
//		//Trim off the predictable white space chunks at the end.
//		this.sync_time = Long.parseLong(junk);
		
		//As long as our system clocks are accurate, using the system clock
		//is a suitable (and potentially long term) solution to this issue.
		mSyncTime = mSyncObtainedTime;
	}

}
