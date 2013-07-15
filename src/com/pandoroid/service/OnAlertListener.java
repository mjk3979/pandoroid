package com.pandoroid.service;

public interface OnAlertListener {
	//Type => activity/benign or error
	//Task => Generic, sign in, playback, stations
	public abstract void onAlert(/*type, task,*/ Alert alert);
	
	public abstract void onRemoveAlert();
	
	public static enum ActionCode {
		ACQUIRING_STATIONS,
		FATAL, //Some mundane unrecoverable action.
		PLAYING,
		RATING,
		SIGNING_IN
	}
	
	public static enum AlertCode{
		RUNNING,
		
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
	
	public static class Alert {
		public final ActionCode action;
		public final AlertCode alert;
		public Alert(ActionCode action, AlertCode alert) {
			this.action = action;
			this.alert = alert;
		}
		
		public boolean equals(Alert other) {
			if (action == other.action && alert == other.alert) {
				return true;
			}
			return false;
		}
	}
}
