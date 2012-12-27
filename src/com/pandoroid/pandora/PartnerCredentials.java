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
package com.pandoroid.pandora;

/**
 * Description: A simple credentials class.
 * 
 * @author Dylan Powers <dylan.kyle.powers@gmail.com>
 *
 */
public class PartnerCredentials {
	public enum CredentialType{
		PANDORA_ONE_JSON, STANDARD_USER_JSON
	}
	
	public String rpcURL;
	public String deviceModel;
	public String username;
	public String password;
	public String decryptKey;
	public String encryptKey;
	
	//Pandora One JSON specific credentials
	private static final String ONE_RPC_URL = "internal-tuner.pandora.com/services/json/";
	private static final String ONE_DEVICE_ID = "D01";
	private static final String ONE_PARTNER_USERNAME = "pandora one";
	private static final String ONE_PARTNER_PASSWORD = "TVCKIBGS9AO9TSYLNNFUML0743LH82D";
	private static final String ONE_DECRYPT_CIPHER = "U#IO$RZPAB%VX2";
	private static final String ONE_ENCRYPT_CIPHER = "2%3WCL*JU$MP]4";
	
	//Android standard user JSON specific credentials
	private static final String AND_RPC_URL = "tuner.pandora.com/services/json/";
	private static final String AND_DEVICE_ID = "android-generic";
	private static final String AND_PARTNER_USERNAME = "android";
	private static final String AND_PARTNER_PASSWORD = "AC7IBG09A3DTSYM4R41UJWL07VLN8JI7";
	private static final String AND_DECRYPT_CIPHER = "R=U!LH$O2B#";
	private static final String AND_ENCRYPT_CIPHER = "6#26FRL$ZWD";
	
	public PartnerCredentials(CredentialType type){
		if (type == CredentialType.PANDORA_ONE_JSON){
			rpcURL = ONE_RPC_URL;
			deviceModel = ONE_DEVICE_ID;
			username = ONE_PARTNER_USERNAME;
			password = ONE_PARTNER_PASSWORD;
			decryptKey = ONE_DECRYPT_CIPHER;
			encryptKey = ONE_ENCRYPT_CIPHER;
		}
		else if (type == CredentialType.STANDARD_USER_JSON){
			rpcURL = AND_RPC_URL;
			deviceModel = AND_DEVICE_ID;
			username = AND_PARTNER_USERNAME;
			password = AND_PARTNER_PASSWORD;
			decryptKey = AND_DECRYPT_CIPHER;
			encryptKey = AND_ENCRYPT_CIPHER;
		}
		else{
			//Everything gets left null.
		}
	}
	
	public boolean isPandoraOne(){
		return deviceModel.equals(ONE_DEVICE_ID);
	}
}
