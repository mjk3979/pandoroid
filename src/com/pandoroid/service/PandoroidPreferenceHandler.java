/* 
 * Pandoroid - An open source Pandora Internet Radio client for Android.
 * 
 * Copyright (C) 2013  Dylan Powers <dylan.kyle.powers@gmail.com>
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
package com.pandoroid.service;

import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.util.Log;

public class PandoroidPreferenceHandler {
	
	private static final String TAG = "Pandoroid Prefs";
	
	private SharedPreferences mPrefs;
	
	public PandoroidPreferenceHandler(SharedPreferences prefs, int versionCode){
		mPrefs = prefs;
		int currentPrefsVersion = 0;
		Upgrader upgrader = new Upgrader();
		try{
			currentPrefsVersion = mPrefs.getInt(Keys.VERSION, 0);
		}
		catch(ClassCastException e){/*An upgrade definitely needs to be done*/}
		
		upgrader.doUpgrade(currentPrefsVersion, versionCode);
	}
	
	public boolean getBehaviorSkipOnBan(){
		try{
			return mPrefs.getBoolean(Keys.BEHAVE_SKIP_ON_BAN, Default.BEHAVE_SKIP_ON_BAN);
		}
		catch(ClassCastException e){
			Log.w(TAG, e.getMessage());
			return Default.BEHAVE_SKIP_ON_BAN;
		}
	}
	
	/**
	 * Description: There are many things to note here. First and foremost,
	 * 	192kbps MP3 audio is ONLY available to Pandora One subscribers. Secondly,
	 * 	it's been found that having more bitrate steps results in poor performance.
	 *  Thus we'll default everything to have two steps. The user of course
	 *  will be able to change this to their liking, but let's give them the
	 *  best configuration possible by default. Next, on Android versions below
	 *  Jelly Bean (4.1), there are issues in the AAC decoder that cause crackling
	 *  artifacts in the audio. Without these artifacts, 64kbps AAC is (according
	 *  to my ear) indistinguishable from 128kbps MP3. For this reason we'll 
	 *  prefer the lower bitrate audio. Again, this is still configurable by the
	 *  user if they beg to differ.
	 * 	
	 * @return
	 */
	public AudioBitratesPref getAudioBitrates(){
		boolean mp3_192 = Default.PANDORA_BITRATE_MP3_192;
		boolean mp3_128 = Default.PANDORA_BITRATE_MP3_128;
		boolean aac_64 = Default.PANDORA_BITRATE_AAC_64;
		boolean aac_32 = Default.PANDORA_BITRATE_AAC_32;
		
		if (Build.VERSION.SDK_INT >= 16/*Build.VERSION_CODES.JELLY_BEAN*/){
			mp3_128 = false;
			aac_64 = true;
			aac_32 = true;
		}
		
		if (getPandoraOneFlag()){
			mp3_192 = true;
			mp3_128 = false;
			aac_64 = false;
			aac_32 = true;
		}
		
		AudioBitratesPref bitratesPref = new AudioBitratesPref();
		try{
			bitratesPref.mp3_192 = mPrefs.getBoolean(Keys.PANDORA_BITRATE_MP3_192, mp3_192);
			bitratesPref.mp3_128 = mPrefs.getBoolean(Keys.PANDORA_BITRATE_MP3_128, mp3_128);
			bitratesPref.aac_64 = mPrefs.getBoolean(Keys.PANDORA_BITRATE_AAC_64, aac_64);
			bitratesPref.aac_32 = mPrefs.getBoolean(Keys.PANDORA_BITRATE_AAC_32, aac_32);
		}
		catch (ClassCastException e){
			Log.w(TAG, e.getMessage());
			bitratesPref.mp3_192 = mp3_192;
			bitratesPref.mp3_128 = mp3_128;
			bitratesPref.aac_64 = aac_64;
			bitratesPref.aac_32 = aac_32;
		}
		
		return bitratesPref;
	}
	
	public String getLastStationToken(){
		try{
			return mPrefs.getString(Keys.PANDORA_LAST_STATION_TOKEN, Default.PANDORA_LAST_STATION_TOKEN);
		}
		catch(ClassCastException e){
			Log.w(TAG, e.getMessage());
			return Default.PANDORA_LAST_STATION_TOKEN;
		}
	}
	
	public boolean getPandoraOneFlag(){
		try{
			return mPrefs.getBoolean(Keys.PANDORA_ONE_FLAG, Default.PANDORA_ONE_FLAG);
		}
		catch(ClassCastException e){
			Log.w(TAG, e.getMessage());
			return Default.PANDORA_ONE_FLAG;
		}
	}
	
	public String getPassword(){
		try{
			return mPrefs.getString(Keys.PANDORA_PASSWORD, Default.PANDORA_PASSWORD);
		}
		catch(ClassCastException e){
			Log.w(TAG, e.getMessage());
			return Default.PANDORA_PASSWORD;
		}
	}
	
	public boolean getPauseOnHeadphoneDisconnect(){
		try{
			return mPrefs.getBoolean(Keys.BEHAVE_PAUSE_ON_HEADPHONE_DISCONNECT, Default.BEHAVE_PAUSE_ON_HEADPHONE_DISCONNECT);
		}
		catch(ClassCastException e){
			Log.w(TAG, e.getMessage());
			return Default.BEHAVE_PAUSE_ON_HEADPHONE_DISCONNECT;
		}
	}
	
	public String getUsername(){
		try{
			return mPrefs.getString(Keys.PANDORA_USERNAME, Default.PANDORA_USERNAME);
		}
		catch(ClassCastException e){
			Log.w(TAG, e.getMessage());
			return Default.PANDORA_USERNAME;
		}
	}
	
	public void removeLastStationToken(){
		mPrefs.edit().remove(Keys.PANDORA_LAST_STATION_TOKEN).apply();
	}
		
	public void removePandoraOneFlag(){
		mPrefs.edit().remove(Keys.PANDORA_ONE_FLAG).apply();
	}
	
	public void removePassword(){
		mPrefs.edit().remove(Keys.PANDORA_PASSWORD).apply();
	}
	
	public void removeUsername(){
		mPrefs.edit().remove(Keys.PANDORA_USERNAME).apply();
	}
	
	public void setLastStationToken(String stationToken){
		mPrefs.edit().putString(Keys.PANDORA_LAST_STATION_TOKEN, stationToken).apply();	
	}
	
	public void setPandoraOneFlag(boolean flag){
		mPrefs.edit().putBoolean(Keys.PANDORA_ONE_FLAG, flag).apply();
	}
	
	public void setPassword(String password){
		mPrefs.edit().putString(Keys.PANDORA_PASSWORD, password).apply();
	}
	
	public void setUsername(String username){
		mPrefs.edit().putString(Keys.PANDORA_USERNAME, username).apply();
	}
	
	
	private static class Default{
		public static final boolean BEHAVE_PAUSE_ON_HEADPHONE_DISCONNECT = true;
		public static final boolean BEHAVE_RESUME_ON_HANGUP = true;
		public static final boolean BEHAVE_SKIP_ON_BAN = true;
		
		public static final boolean PANDORA_BITRATE_MP3_192 = false;
		public static final boolean PANDORA_BITRATE_MP3_128 = true;
		public static final boolean PANDORA_BITRATE_AAC_64 = false;
		public static final boolean PANDORA_BITRATE_AAC_32 = true;
		public static final boolean PANDORA_ONE_FLAG = false;
		public static final String PANDORA_PASSWORD = null;
		public static final String PANDORA_LAST_STATION_TOKEN = null;
		public static final String PANDORA_USERNAME = null;				
	}
	
	private static class Keys{
		public static final String BEHAVE_PAUSE_ON_HEADPHONE_DISCONNECT = "behave_pauseOnHeadphoneDisconnect";
		public static final String BEHAVE_RESUME_ON_HANGUP = "behave_resumeOnHangup";
		public static final String BEHAVE_SKIP_ON_BAN = "behave_skipOnBan";
		
		public static final String PANDORA_BITRATE_MP3_192 = "pandoraBitrateMP3_192";
		public static final String PANDORA_BITRATE_MP3_128 = "pandoraBitrateMP3_128";
		public static final String PANDORA_BITRATE_AAC_64 = "pandoraBitrateAAC_64";
		public static final String PANDORA_BITRATE_AAC_32 = "pandoraBitrateAAC_32";
		public static final String PANDORA_ONE_FLAG = "pandoraOneFlag";
		public static final String PANDORA_PASSWORD = "pandoraPassword";
		public static final String PANDORA_LAST_STATION_TOKEN = "pandoraLastStationToken";
		public static final String PANDORA_USERNAME = "pandoraUsername";
		
		public static final String VERSION = "version";
	}
	
	private class Upgrader{
		private String mPassword = Default.PANDORA_PASSWORD;
		private boolean mPandoraOneFlag = Default.PANDORA_ONE_FLAG;
		private boolean mSkipOnBan = Default.BEHAVE_SKIP_ON_BAN;
		private String mStationToken = Default.PANDORA_LAST_STATION_TOKEN;
		private String mUsername = Default.PANDORA_USERNAME;
		
		public void doUpgrade(int currentPrefsVersion, int newVersion){
			if (currentPrefsVersion < 15){
				upgradeTo15();
				
				toFinal(newVersion);
			}
		}
		
		/**
		 * Changes in this version:
		 * 	Renaming of PANDORA_USERNAME, PANDORA_PASSWORD, 
		 * 		PANDORA_LAST_STATION_TOKEN, and PANDORA_ONE_FLAG keys.
		 *  A bug was found in the value of PANDORA_ONE_FLAG so it will be reset.
		 */
		private void upgradeTo15(){
			try{
				mUsername = mPrefs.getString("pandora_username", Default.PANDORA_USERNAME);
				mPassword = mPrefs.getString("pandora_password", Default.PANDORA_PASSWORD);
				mSkipOnBan = mPrefs.getBoolean("behave_nextOnBan", Default.BEHAVE_SKIP_ON_BAN);
				mStationToken = mPrefs.getString("lastStationId", Default.PANDORA_LAST_STATION_TOKEN);
			}
			catch (ClassCastException e){/*Ignore it*/}
			
			mPrefs.edit().remove("pandora_username")
			             .remove("pandora_password")
			             .remove("pandora_one_flag")
			             .remove("behave_nextOnBan")
			             .remove("lastStationId")
			             .apply();
		}
		
		private void toFinal(int upgradeVersion){
			Editor prefsEditor = mPrefs.edit();
			if (!mPassword.equals(Default.PANDORA_PASSWORD)){
				prefsEditor.putString(Keys.PANDORA_PASSWORD, mPassword);
			}
			if (mPandoraOneFlag != Default.PANDORA_ONE_FLAG){
			    prefsEditor.putBoolean(Keys.PANDORA_ONE_FLAG, mPandoraOneFlag);
			}
			if (mSkipOnBan != Default.BEHAVE_SKIP_ON_BAN){
				prefsEditor.putBoolean(Keys.BEHAVE_SKIP_ON_BAN, mSkipOnBan);
			}
			if (!mStationToken.equals(Default.PANDORA_LAST_STATION_TOKEN)){
				prefsEditor.putString(Keys.PANDORA_LAST_STATION_TOKEN, mStationToken);
			}
			if (!mUsername.equals(Default.PANDORA_USERNAME)){
				prefsEditor.putString(Keys.PANDORA_USERNAME, mUsername);
			}
			prefsEditor.putInt(Keys.VERSION, upgradeVersion);
			prefsEditor.apply();
		}
	}
}
