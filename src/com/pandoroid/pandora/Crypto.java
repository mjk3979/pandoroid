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

import java.security.GeneralSecurityException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

/**
 * Description: The encryption handler for Pandora's remote server API. All
 * 	encryption by the JsonRPC component is handled here.
 * 
 * @author Dylan Powers <dylan.kyle.powers@gmail.com>
 *
 */
public class Crypto {

	private Cipher mDecode;
	private Cipher mEncode;
	
	/**
	 * Description: Our cryptography constructor.
	 * @param encryptKey -The key to be used for encryption.
	 * @param decryptKey -The key to be used for decryption.
	 * @throws GeneralSecurityException when an internal error occurred in the 
	 * 	initialization.
	 */
	public Crypto(byte[] encryptKey, byte[] decryptKey) throws GeneralSecurityException{
		SecretKeySpec keySpec = new SecretKeySpec(encryptKey, "Blowfish");		
		mEncode = Cipher.getInstance("Blowfish/ECB/NoPadding");
		mEncode.init(Cipher.ENCRYPT_MODE, keySpec);

		keySpec = new SecretKeySpec(decryptKey, "Blowfish");		
		mDecode = Cipher.getInstance("Blowfish/ECB/NoPadding");
		mDecode.init(Cipher.ENCRYPT_MODE, keySpec);
	}

	/**
	 * Description: This takes a Blowfish encrypted, hexadecimal string, and 
	 *  decrypts it to a plain string form.
	 */
	public String decrypt(String hex) throws GeneralSecurityException {
		byte[] bytes = hex.getBytes();
		bytes = fromHextoRaw(bytes);
		bytes = mDecode.doFinal(bytes);
		return new String(bytes);		
	}
	
	/**
	 * Description: This function encrypts a string using a Blowfish cipher, 
	 * 	and returns	the hexadecimal representation of the encryption.
	 */
	public String encrypt(String plain) throws GeneralSecurityException{
		byte[] bytes = plain.getBytes();
		
		// Before we encode we may need to do some padding.
		if (bytes.length % 8 != 0){
			int paddingSize = 8 - (bytes.length % 8);
			byte[] tmpBytes = new byte[bytes.length + paddingSize];
			System.arraycopy(bytes, 0, tmpBytes, 0, bytes.length);
			bytes = tmpBytes;
		}
		
		byte[] encodeRaw = mEncode.doFinal(bytes);
		return new String(fromRawtoHex(encodeRaw));		
	}
	

	
	/**
	 * Description: Self explanatory function that converts from a hex encoded 
	 *  byte array to a raw byte array. One complicated portion of this to mention
	 *  is that String types can do something rather odd when conversions are made
	 *  from byte arrays to Strings and back again. They don't like to work 
	 *  out perfectly so lets not use them.
	 * @param hex -A byte array holding the hex values we want to convert.
	 * @return -A byte array holding the raw 8 bit equivalent to the 4 bit input.
	 */
	private byte[] fromHextoRaw(byte[] hex) {
		int hexLen = hex.length;
		byte[] raw = new byte[hexLen / 2];
		for (int i = 0; i < hexLen; i += 2){
			raw[i / 2] = (byte) ((Character.digit(hex[i], 16) * 16 /*Shift the bits by 4 left*/)
					             + Character.digit(hex[i + 1], 16));
		}
		return raw;
	}
	
	/**
	 * Description: I had to look far and wide to find this implementation.
	 * 	Java's builtin Integer.toHexString() function is absolutely atrocious.
	 *  It can't be depended on for any kind of formatting predictability.
	 *  For speed's sake, having the HEX_CHARS constant is a necessity.
	 * @param raw -A byte array holding the raw 8 bit values we want to convert.
	 * @return -A byte array holding the 4 bit hex equivalent to the 8 bit input.
	 */
	private static final byte[] HEX_CHARS = "0123456789ABCDEF".getBytes();
	private byte[] fromRawtoHex(byte[] raw){
		byte[] hex = new byte[2 * raw.length];
        for (int i = 0; i < raw.length; ++i){
            hex[2 * i] = HEX_CHARS[(raw[i] & 0xF0) >>> 4];
            hex[2 * i + 1] = HEX_CHARS[raw[i] & 0x0F];
        }		
		return hex;
	}
}
