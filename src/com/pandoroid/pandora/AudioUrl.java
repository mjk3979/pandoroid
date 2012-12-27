/* 
 * Pandoroid - An open source Pandora Internet Radio client for Android.
 * 
 * Copyright (C) 2011  Andrew Regner <andrew@aregner.com>
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

import java.util.Map;

/**
 * Description: Holds data relevant to the URL of a song retrieved from Pandora.
 * 
 * @author Andrew Regner <andrew@aregner.com>
 * @contributor Dylan Powers <dylan.kyle.powers@gmail.com>
 * 
 */
public class AudioUrl implements Comparable<AudioUrl>{
	public String type; //A format found in Song that's provided by Pandora.
	public int bitrate;
	public String urlStr;
	
	public AudioUrl(String type, int bitrate, String url){
		this.type = type;
		this.bitrate = bitrate;
		this.urlStr = url;
	}
	
	/**
	 * Description: A constructor that uses the data as returned from Pandora's
	 * 	servers to construct a URL.
	 * @param extended_audio_url
	 * @throws PandoraAPIModifiedException when the audio format can't be identified.
	 * @throws ClassCastException when a type cast could not be made.
	 */
	public AudioUrl(Map<String, Object> extendedAudioUrl) 
			        throws PandoraAPIModifiedException,
			               ClassCastException{
		if (is_AAC_64(extendedAudioUrl)){
			type = Song.AAC_64;					
		}
		else if (is_MP3_192(extendedAudioUrl)){
			type = Song.MP3_192;
		}
		else {
			throw new PandoraAPIModifiedException("Unidentified audio format found");
		}
		
		Object bitrateObject = extendedAudioUrl.get("bitrate");
		Object urlStrObject = extendedAudioUrl.get("audioUrl");
		if (bitrateObject instanceof String && urlStrObject instanceof String){		
			bitrate = Integer.parseInt((String) bitrateObject);
			urlStr = (String) urlStrObject;
		}
		else{
			throw new PandoraAPIModifiedException("The bitrate and/or url could " +
												  "not be found");
		}
	}	

	/**
	 * Description: Required for comparable.
	 */
	public int compareTo(AudioUrl comparable){
		if (bitrate == comparable.bitrate){
			return 0;
		}
		else if (bitrate < comparable.bitrate){
			return -1;
		}
		else { //if it's greater
			return 1;
		}
	}
	
	private boolean is_AAC_64(Map<String, Object> extendedAudioUrl){
		return (((String) extendedAudioUrl.get("bitrate")).compareTo("64") == 0
				                          &&
				((String) extendedAudioUrl.get("encoding")).compareTo("aacplus") == 0);
	}
	
	private boolean is_MP3_192(Map<String, Object> extendedAudioUrl){
		return (((String) extendedAudioUrl.get("bitrate")).compareTo("192") == 0
							         &&
	            ((String) extendedAudioUrl.get("encoding")).compareTo("mp3") == 0);
	}
	
	public String toString(){
		return urlStr;
	}
}
