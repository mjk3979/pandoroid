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

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;

import android.util.Log;

/**
 * A class heavily related to the Pandora API that holds all song data.
 * 
 * @author Andrew Regner <andrew@aregner.com>
 * @contributor Dylan Powers <dylan.kyle.powers@gmail.com>
 *
 */
public class Song {
	
	//Normal API stuff
	private String mAlbum;
	private String mAlbumArtUrl;
	private String mAlbumDetailUrl;
	private boolean mAllowFeedback;
	private String mArtist;
	private String mArtistDetailUrl;
	private String mDetailUrl; 
	public int rating; //0 for none, (-) for down, (+) for up
	private long mStationId;
	private String mTitle;
	private String mToken;
	private float mTrackGain;
	
	//Extra stuff for us.
	public long lastFeedbackId;
	private long mTimeAcquired; //In seconds since UNIX epoch!
	public boolean tired;
	
	private HashMap<String, AudioUrl> mAudioUrls;
	
	private static final int MAX_TIME_ALIVE = 60 * 60; //Represented in seconds.
	
	//Available formats from Pandora
	public static final String AAC_32 = "HTTP_32_AACPLUS";
	public static final String AAC_64 = "HTTP_64_AACPLUS";
	public static final String MP3_128 = "HTTP_128_MP3";
	public static final String MP3_192 = "HTTP_192_MP3";

	public Song(Map<String, Object> data, Vector<AudioUrl> audioUrls) 
			   throws ClassCastException, NumberFormatException {
		mAlbum = (String) data.get("albumName");
		mAlbumArtUrl = (String) data.get("albumArtUrl");
		mAlbumDetailUrl = (String) data.get("albumDetailURL");
		mAllowFeedback = (Boolean) data.get("allowFeedback");
		mArtist = (String) data.get("artistName");
		mArtistDetailUrl = (String) data.get("artistDetailUrl");
		mDetailUrl = (String) data.get("songDetailURL");
		rating = (Integer) data.get("songRating");
		mStationId = Long.parseLong((String) data.get("stationId"));
		mTitle = (String) data.get("songName");
		mToken = (String) data.get("trackToken");
		mTrackGain = Float.parseFloat((String) data.get("trackGain"));
		
		lastFeedbackId = 0;
		mTimeAcquired = System.currentTimeMillis() * 1000;
		tired = false;
		
		mAudioUrls = new HashMap<String, AudioUrl>(audioUrls.size());
		for (int i = 0; i < audioUrls.size(); ++i){
			AudioUrl tmp = audioUrls.get(i);
			mAudioUrls.put(tmp.type, tmp);
		}
	}
	
	/**
	 * Description: Compares two audio format strings.
	 * @param value -The value making the comparison.
	 * @param relative_to -The value being compared to.
	 * @return An integer that's positive when value is greater than relative_to,
	 * 	negative when value is less than relative_to, and 0 when they're 
	 * 	equivalent.
	 * @throws Exception when the strings are invalid to be making comparisons
	 * 	against (One or both is not one of the defined constants).
	 */
	public static int audioQualityCompare(String value, String relativeTo) throws Exception{
		int str1Magnitude = getRelativeAudioQualityMagnitude(value);
		int str2Magnitude = getRelativeAudioQualityMagnitude(relativeTo);
		if (str1Magnitude != -1 && str2Magnitude != -1){
			return (str1Magnitude - str2Magnitude);
		}		
		else{
			throw new Exception("Invalid strings to compare");
		}
	}
	
	/**
	 * Description: Gets what it says.
	 * @param audioFormatString -A string that consists of the above const.
	 * @return -A value related to the inputed format string. If the input is
	 * 	invalid then -1 is output.
	 */
	private static int getRelativeAudioQualityMagnitude(String audioFormatString){
		if (audioFormatString.compareTo(MP3_192) == 0){
			return 4;
		}
		if (audioFormatString.compareTo(MP3_128) == 0){
			return 3;
		}
		if (audioFormatString.compareTo(AAC_64) == 0){
			return 2;
		}
		if (audioFormatString.compareTo(AAC_32) == 0){
			return 1;
		}
		return -1;
	}

	/**
	 * Description: Checks to see if a song is still valid. If it is invalid
	 * 	then a request to the remote server to play the song will not work.
	 * @return a boolean if true or not.
	 */
	public boolean isStillValid() {
		return (System.currentTimeMillis() * 1000 - mTimeAcquired) < MAX_TIME_ALIVE;
	}
	
	//All of our self explanatory getters.
	public String getAudioUrl(String audioQuality) {
		return mAudioUrls.get(audioQuality).urlStr;
	}	
	public String getAlbum() {
		return mAlbum;
	}
	public String getAlbumArtUrl() {
		return mAlbumArtUrl;
	}	
	public String getAlbumDetailUrl() {
		return mAlbumDetailUrl;
	}
	public boolean allowFeedback(){
		return mAllowFeedback;
	}
	public String getArtist() {
		return mArtist;
	}
	public String getArtistDetailUrl(){
		return mArtistDetailUrl;
	}	
	public String getDetailUrl(){
		return mDetailUrl;
	}
	public long getStationId() {
		return mStationId;
	}	
	public String getTitle() {
		return mTitle;
	}
	public String getToken(){
		return mToken;
	}
	public float getTrackGain() {
		return mTrackGain;
	}
	//End getters.
}
