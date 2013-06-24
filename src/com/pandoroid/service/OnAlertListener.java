package com.pandoroid.service;

public interface OnAlertListener {
	//Type => activity/benign or error
	//Task => Generic, sign in, playback, stations
	public abstract void onAlert(/*type, task,*/ AlertCode code);
	
	public abstract void onRemoveAlert(AlertCode code);
	
	public static enum AlertCode{
		ACTIVITY_ACQUIRING_STATIONS,
		ACTIVITY_RATING,
		ACTIVITY_SIGNING_IN,
		
		ERROR_APPLICATION,
		ERROR_AUDIO_FOCUS,
		ERROR_INVALID_USER_CREDENTIALS,
		ERROR_MISSING_STATION_SELECTION,
		ERROR_MISSING_USER_CREDENTIALS,
		ERROR_NETWORK,
		ERROR_REMOTE_SERVER,
		ERROR_UNKNOWN,
		ERROR_UNSUPPORTED_API,
		ERROR_WAN
	}
}
