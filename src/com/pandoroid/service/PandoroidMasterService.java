/* 
 * Pandoroid - An open source Pandora Internet Radio client for Android.
 * 
 * Copyright (C) 2011  Andrew Regner <andrew@aregner.com>
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

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.NoSuchElementException;
import java.util.Vector;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;

import android.R;
import android.annotation.TargetApi;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.PackageManager.NameNotFoundException;
import android.media.AudioManager;
import android.media.AudioManager.OnAudioFocusChangeListener;
import android.media.RemoteControlClient;
import android.net.ConnectivityManager;
import android.os.AsyncTask;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.util.Log;
import android.view.KeyEvent;

import com.pandoroid.pandora.Song;
import com.pandoroid.pandora.StationMetaInfo;
import com.pandoroid.pandora.SubscriberTypeException;
import com.pandoroid.pandora.RPC.PandoraRemote;
import com.pandoroid.pandora.RPC.RPCException;
import com.pandoroid.playback.stations.StationPlayer;
import com.pandoroid.playback.stations.StationTuner;
import com.pandoroid.service.OnAlertListener.Alert;
import com.pandoroid.service.OnAlertListener.ActionCode;
import com.pandoroid.service.OnAlertListener.AlertCode;
import com.pandoroid.ui.PlayerActivity;


/**
 * Description: Someone really needs to give this class some loving, document
 *  it up, organize it, and make it thread safe.
 */
public class PandoroidMasterService extends Service {
	
	private static final String USER_AGENT = "";//= "com.pandoroid_x.x.x-suffix"
	private static final String TAG = "Pandroid Service";
    
	private OnAlertListener mAlertCallback;
	private ArrayDeque<Alert> mAlertStack = new ArrayDeque<Alert>();
	private AudioControlReceiver mAudioControlReceiver = new AudioControlReceiver();
	private ComponentName mAudioControlIdentifier;
	private AudioFocusChangeListener mAudioFocusChangeListener = 
			new AudioFocusChangeListener();
	private AudioManager mAudioManager;
	
  // This is the object that receives interactions from clients of the service. 
	private final IBinder mBinder = new PandoroidMasterServiceBinder();
	
	private StationPlayer mCurrentStation;
	private OnMediaEventListener mMediaEventCallback;
	private RPCAsyncTasks mPandoraRPCAsync;
	private PandoroidPreferenceHandler mPrefs;
	private PandoraRemote mRemote;
	private RemoteControlClient mRemoteControlClient;
	private RunningAsyncTasks mRunningAsyncTasks = new RunningAsyncTasks();
	private boolean mSet2PlayFlag = true;
	private StationTuner mTuner;	

	
	/* 
	 * Standard service necessities
	 */
	
	/**
	 * Class for clients to access.  Because we know this service always
	 * runs in the same process as its clients, we don't need to deal with
	 * IPC.
	 */
	public class PandoroidMasterServiceBinder extends Binder {
		public PandoroidMasterService getService() {
			return PandoroidMasterService.this;
		}
	}
    
	@Override
	public IBinder onBind(Intent intent) {
		return mBinder;
	}
    
	@Override
	public void onCreate() {
		
		mAudioControlIdentifier = new ComponentName(getPackageName(), 
		                                           AudioControlReceiver.class.getName());
		
		//Register ourselves to get media button events.
		mAudioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
		mAudioManager.registerMediaButtonEventReceiver(mAudioControlIdentifier);
		
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.ICE_CREAM_SANDWICH) {
			registerRemoteClient();
		}
		
		registerReceiver(mAudioControlReceiver, 
				             new IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY));
		
		SharedPreferences sharedPrefs = PreferenceManager.getDefaultSharedPreferences(this);
		int versionCode = 0;
		try {
			versionCode = getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
		} catch (NameNotFoundException e) {
			Log.w(TAG, "Mysteriously unable to get the version code.");
		}
		
		mPrefs = new PandoroidPreferenceHandler(sharedPrefs, versionCode);			

		try {
			ConnectivityManager connectivityManager = 
					(ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
			mRemote = new PandoraRemote(mPrefs.getPandoraOneFlag(), USER_AGENT);
			mPandoraRPCAsync = new RPCAsyncTasks(mRemote, connectivityManager);
		} catch (GeneralSecurityException e) {
			Log.e(TAG, 
			      "A fatal exception occurred while initializing the remote", 
			      e);
			raiseAlert(new Alert(ActionCode.FATAL, AlertCode.ERROR_APPLICATION));
		}
		
//		// Register the listener with the telephony manager
//		telephonyManager.listen(new PhoneStateListener() {
//			boolean pausedForRing = false;
//			@Override
//			public void onCallStateChanged(int state, String incomingNumber) {
//				switch(state) {
//
//				case TelephonyManager.CALL_STATE_IDLE:
//					if(pausedForRing && m_song_playback != null) {
//						if(m_prefs.getBoolean("behave_resumeOnHangup", true)) {
//							if(m_song_playback != null && !m_paused){
//								m_song_playback.play();
//							}
//						}
//					}
//					
//					pausedForRing = false;
//					break;
//
//				case TelephonyManager.CALL_STATE_OFFHOOK:
//				case TelephonyManager.CALL_STATE_RINGING:
//					if(m_song_playback != null) {
//						m_song_playback.pause();
//					}					
//
//					pausedForRing = true;						
//					break;
//				}
//			}
//		}, PhoneStateListener.LISTEN_CALL_STATE);
//		
		
		play();
	}	
	
	@Override
	public void onDestroy() {
		//TODO: Other fatal stopping tasks need to be done.
//		if (m_song_playback != null){
//			m_song_playback.stop();
//		}
//		this.unregisterReceiver(m_music_intent_receiver);
		mAudioManager.unregisterMediaButtonEventReceiver(mAudioControlIdentifier);
//		stopForeground(true);
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}	

	/* End service necessities */
	
	
	private void abandonAudioFocus() {
		mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);
	}
	
	private void dismissAlert(OnAlertListener.Alert alert) {
		if (alert.equals(mAlertStack.peekLast())) {
			mAlertStack.pop();
			mAlertCallback.onRemoveAlert();
			if (!mAlertStack.isEmpty()) {
				raiseAlert(mAlertStack.peekLast(), false);
			}
		} else { //Now we have to do some digging.
			if (!mAlertStack.isEmpty()) {
				if (!mAlertStack.removeLastOccurrence(alert)) {
					//Let's not fail, but at least log it if nothing is found.
					//This sort of thing is an error, but an error we can live
					//with.
					Log.d(TAG, 
						  "dismissAlert() called on an alert that doesn't" +
					      "exist.\nActionCode: " + alert.action + "\n" +
					      "AlertCode: " + alert.alert);
				}
			}
		}
	}
	
	private void executeSignIn(String username, String password) {
		if (!mRemote.isPartnerAuthorized()){
			partnerAuthorization();
		}
		else{
			if (username != null && password != null){
				final Alert signInAlert = new Alert(ActionCode.SIGNING_IN, 
						                            AlertCode.RUNNING);
				raiseAlert(signInAlert);
				if (mRunningAsyncTasks.userLogIn == null){
					mRunningAsyncTasks.userLogIn = mPandoraRPCAsync.userLogIn(username, 
						                                                      password,
						new RPCAsyncTasks.PostTask<Void>() {
				
							@Override
							public void onException(Exception e) {
								if (e instanceof SubscriberTypeException){
									switchSubscriberTypes();
								}
								// TODO Auto-generated method stub							
							}
				
							@Override
							public void onSuccess(Void arg) {
								// TODO Auto-generated method stub
								//Set the station
								
							}

							@Override
							public void always() {
								dismissAlert(signInAlert);
								mRunningAsyncTasks.userLogIn = null;
							}
						
					});	
				}
			}
			else{
				raiseAlert(new Alert(ActionCode.SIGNING_IN, 
						             AlertCode.ERROR_MISSING_USER_CREDENTIALS));
			}
		}
	}
	
	private void fetchStations(){
		if (mRunningAsyncTasks.getStations == null){
			final Alert stationsFetchAlert = new Alert(ActionCode.ACQUIRING_STATIONS, 
					                                   AlertCode.RUNNING);
			raiseAlert(stationsFetchAlert);
			mRunningAsyncTasks.getStations = mPandoraRPCAsync.getStations(
				new RPCAsyncTasks.PostTask<Vector<StationMetaInfo>>() {

					@Override
					public void onException(Exception e) {
						// TODO Auto-generated method stub
						
					}
	
					@Override
					public void onSuccess(Vector<StationMetaInfo> arg) {
						if (mTuner == null){
							//TODO: Setup the stationTuner
							//mStationsHandler = new StationsHandler(arg, mPandoraRPCAsync);
						}
						else{
							mTuner.update(arg);
						}
						
						if (mCurrentStation == null){
							String token = mPrefs.getLastStationToken();
							if (token != null){
								try{
									mCurrentStation = mTuner.changeStations(token);
								}
								catch(Exception e){
									mPrefs.removeLastStationToken();
									raiseAlert(new Alert(ActionCode.ACQUIRING_STATIONS,
											             AlertCode.ERROR_MISSING_STATION_SELECTION));
								}
							}
							else{
								raiseAlert(new Alert(ActionCode.ACQUIRING_STATIONS,
										             AlertCode.ERROR_MISSING_STATION_SELECTION));
							}
						}
					}

					@Override
					public void always() {
						dismissAlert(stationsFetchAlert);
						mRunningAsyncTasks.getStations = null;						
					}
					
			});
		}
	}
	
	public Song getCurrentSong() throws Exception{
		return mCurrentStation.getCurrentSong();
	}
	
	public StationMetaInfo getCurrentStation() {
		return mCurrentStation.getStationMetaInfo();
	}
	
	public ArrayList<StationMetaInfo> getStations(){
		if (mTuner == null){
			fetchStations();
			return new ArrayList<StationMetaInfo>();
		}
		else{
			//TODO: Change all vectors to arrayList
			return new ArrayList<StationMetaInfo>();//mTuner.getStations();
		}
	}
	
	
	public boolean isSet2Play(){		
		return mSet2PlayFlag; //Paused, playing
	}
	
	private void issueNotification(Song meta) {
//	tmp_song = m_song_playback.getSong();
//	Notification notification = new Notification(R.drawable.icon, 
//                  									 "Pandoroid Radio", 
//                  									 System.currentTimeMillis());
//		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
//		builder.setSmallIcon(R.drawable.icon)
//		       .setContentTitle(getString(R.string.app_name))
//		       .setContentText(meta.getTitle());		
//		Intent notificationIntent = new Intent(this, PlayerActivity.class);
//	PendingIntent contentIntent = PendingIntent.getActivity(this, 
//                             										NOTIFICATION_SONG_PLAYING, 
//                         											notificationIntent, 
//                         											0);
//	notification.flags |= Notification.FLAG_ONGOING_EVENT | 
//						  Notification.FLAG_FOREGROUND_SERVICE;
//	notification.setLatestEventInfo(getApplicationContext(), 
//									tmp_song.getTitle(),
//									tmp_song.getArtist() + " on " + tmp_song.getAlbum(), 
//									contentIntent);
//	startForeground(NOTIFICATION_SONG_PLAYING, notification);
	}
	
	
	private void partnerAuthorization() {
		if (mRunningAsyncTasks.partnerLogIn == null) {
			final Alert signInAlert = new Alert(ActionCode.SIGNING_IN, 
                    AlertCode.RUNNING);
			raiseAlert(signInAlert);
			mRunningAsyncTasks.partnerLogIn = mPandoraRPCAsync.partnerLogIn(
				new RPCAsyncTasks.PostTask<Void>(){	
					@Override
					public void onException(Exception e) {
						if (e instanceof RPCException && 
								((RPCException) e).getCode() == 
									RPCException.INVALID_PARTNER_CREDENTIALS) {
							raiseAlert(new Alert(ActionCode.SIGNING_IN,
									             AlertCode.ERROR_UNSUPPORTED_API));
							Log.e(TAG, "Invalid partner credentials");
						} else {
							raiseAlert(new Alert(ActionCode.SIGNING_IN,
									             mRunningAsyncTasks.exceptionHandler(e)));
						}
					}
		
					@Override
					public void onSuccess(Void arg) {
						signIn();
					}

					@Override
					public void always() {
						dismissAlert(signInAlert);
						mRunningAsyncTasks.partnerLogIn = null;
					}
				});
		}
	}

	//TODO: Notification shade.
	public void pause() {
		mSet2PlayFlag = false;
		if (mCurrentStation != null) {
			mCurrentStation.pause();
		}
		abandonAudioFocus();
	}
	
	/*
	 * Idea: When play is called, it also checks to make sure that the application
	 * 	is authorized, the user is signed in, and a station has been selected. 
	 * 	If not, let's do them automatically. This way, the main activity has no 
	 * 	perception of being "signed in". If some problem occurs that requires
	 * 	user interaction, then we'll simply call the appropriate listener (the
	 * 	alert listener) to notify the main activity that a problem has occurred
	 * 	and it needs to take appropriate action.  
	 */
	public void play() {
		mSet2PlayFlag = true;
		if (!mRemote.isUserAuthorized()){
			signIn();
		}		
		else if (mCurrentStation != null){
			mCurrentStation.play();
		} else {		
			if (requestAudioFocus()) {
				mCurrentStation.play();
				mMediaEventCallback.onPlay();
				//TODO: Notification shade support
			} else {
				mSet2PlayFlag = false;
				raiseAlert(new Alert(ActionCode.PLAYING,
						             AlertCode.ERROR_AUDIO_FOCUS));
			}
		}
	}
	
	/**
	 * Description: Pushes the alert to the alert stack and notifies the 
	 * 	registered listener. Optionally, a boolean flag can be set whether
	 * 	the code should be pushed to the alert stack. This normally defaults
	 *  to true.
	 * @param code
	 */
	private void raiseAlert(OnAlertListener.Alert alert) {
		raiseAlert(alert, true);
	}	
	private void raiseAlert(OnAlertListener.Alert alert, boolean push) {
		if (push) {
			mAlertStack.push(alert);
		}
		if (mAlertCallback != null) {			
			mAlertCallback.onAlert(alert);
		}
	}

	
	public void rate(String trackToken, boolean isRatingPositive) {
		final Alert ratingAlert = new Alert(ActionCode.RATING, AlertCode.RUNNING);
		raiseAlert(ratingAlert);
		mPandoraRPCAsync.rate(trackToken, 
				              isRatingPositive, 
				              new RPCAsyncTasks.PostTask<Long>(){

			@Override
			public void onException(Exception e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onSuccess(Long arg) {
				// TODO Auto-generated method stub
			}

			@Override
			public void always() {
				dismissAlert(ratingAlert);
			}			
		});
	}
	
	@TargetApi(Build.VERSION_CODES.ICE_CREAM_SANDWICH)
	private void registerRemoteClient() {
		//Setup our intent to take media button control.
		Intent mediaButtonIntent = new Intent(Intent.ACTION_MEDIA_BUTTON);
		mediaButtonIntent.setComponent(mAudioControlIdentifier);
		PendingIntent mediaPendingIntent = 
				PendingIntent.getBroadcast(this, 0, mediaButtonIntent, 0);		
		//Create and register the control client.
		RemoteControlClient mRemoteControlClient = 
				new RemoteControlClient(mediaPendingIntent);
		mAudioManager.registerRemoteControlClient(mRemoteControlClient);
	}

	
	/**
	 * Description: Unregisters the previously set OnAlertListener(). This must
	 * be called any time an activity that registers a listener leaves scope.
	 */
	public void removeOnAlertListener() {
		mAlertCallback = null;
	}
	
	/**
	 * Description: Unregisters the previously set OnMediaEventListener(). 
	 * This must be called any time an activity that registers a listener 
	 * leaves scope.
	 */
	public void removeOnMediaEventListener() {
		mMediaEventCallback = null;
	}
	
	private boolean requestAudioFocus() {
		int result = mAudioManager.requestAudioFocus(mAudioFocusChangeListener,
		                                             AudioManager.STREAM_MUSIC,
		                                             AudioManager.AUDIOFOCUS_GAIN);
		return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
	}
	
	
	
	public void setCurrentStation(String stationToken) {
		try {
			mCurrentStation = mTuner.changeStations(stationToken);
			mPrefs.setLastStationToken(stationToken);
		} catch (Exception e) {
			// TODO Auto-generated catch block			e.printStackTrace();
			raiseAlert(new Alert(ActionCode.FATAL,
					             AlertCode.ERROR_APPLICATION));
		}
	}
	
	//Includes setting and removing alerts.
	public void setOnAlertListener(OnAlertListener listener){
		mAlertCallback = listener;
		if (!mAlertStack.isEmpty()) {
			raiseAlert(mAlertStack.peekLast(), false);
		}
	}
	
	//This includes onPlay, onPause, and onNewSong listeners.
	public void setOnMediaEventListener(OnMediaEventListener listener){
		mMediaEventCallback = listener;
	}	
	
	/**
	 * Description: Signs a user in with the specified username and password.
	 * @param username
	 * @param password
	 */
	public void signIn(String username, String password) {
		if ((username == null || username == "") ||
		    (password == null || password == "")) {
			raiseAlert(new Alert(ActionCode.SIGNING_IN, 
					             AlertCode.ERROR_INVALID_USER_CREDENTIALS));
		} else {
			mPrefs.setPassword(password);
			mPrefs.setUsername(username);
			executeSignIn(username, password);
		}
	}
	
	/**
	 * Description: Signs a user in with the previously saved username and 
	 * 	password.
	 */
	private void signIn(){
		String username = mPrefs.getUsername();
		String password = mPrefs.getPassword();
		
		executeSignIn(username, password);
	}
	
	public void signOut() {
		//First we shall clean out the preferences
		mPrefs.removeLastStationToken();
		mPrefs.removePandoraOneFlag();
		mPrefs.removePassword();
		mPrefs.removeUsername();
		
		//Now kill the currently running processes
		if (mRunningAsyncTasks.partnerLogIn != null){
			mRunningAsyncTasks.partnerLogIn.cancel(true);
		}		
		if (mRunningAsyncTasks.userLogIn != null){
			mRunningAsyncTasks.userLogIn.cancel(true);
		}
		if (mRunningAsyncTasks.getStations != null){
			mRunningAsyncTasks.getStations.cancel(true);
		}
		stop();
		mTuner.killAll();
		
		//Lastly, reset everything to their default states
		mRunningAsyncTasks.partnerLogIn = null;
		mRunningAsyncTasks.userLogIn = null;
		mRunningAsyncTasks.getStations = null;
		mTuner = null;
		mCurrentStation = null;
		try {
			mRemote = new PandoraRemote(mPrefs.getPandoraOneFlag(), USER_AGENT);
			mPandoraRPCAsync.setRemote(mRemote);
		} catch (GeneralSecurityException e) {
			Log.e(TAG, 
				  "A fatal exception occurred while initializing the " +
			      "PandoraRemote during signOut",
				  e);
			raiseAlert(new Alert(ActionCode.FATAL, AlertCode.ERROR_APPLICATION));
		}
	}
	
	public void skip(){
		//TODO: mCurrentStation.skip();
	}
	
	private void stop(){
		pause();
		//Then remove all notifications.
		//TODO Other fancy stuff
	}
	
	private void switchSubscriberTypes(){
		if (mRemote.isPandoraOneCredentials()){
			mPrefs.setPandoraOneFlag(false);
		}
		else{
			mPrefs.setPandoraOneFlag(true);
			try {
				mRemote = new PandoraRemote(true, USER_AGENT);
				mPandoraRPCAsync.setRemote(mRemote);
			} catch (GeneralSecurityException e) {
				Log.e(TAG, 
					  "A fatal exception occurred while initializing the " +
				      "PandoraRemote during switchSubscriberTypes",
					  e);
				raiseAlert(new Alert(ActionCode.FATAL, AlertCode.ERROR_APPLICATION));
			}
		}
	}
	
	public void updateStations(){
		fetchStations();
	}

	public class AudioControlReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)){
				if (mSet2PlayFlag && mPrefs.getPauseOnHeadphoneDisconnect()){
					pause();
				}
			}
			else if (action.equals(Intent.ACTION_MEDIA_BUTTON)){
				KeyEvent event = (KeyEvent) intent.getParcelableExtra(Intent.EXTRA_KEY_EVENT);
				switch (event.getKeyCode()){
					case KeyEvent.KEYCODE_MEDIA_PLAY:
						play();
						break;
					case KeyEvent.KEYCODE_MEDIA_PAUSE:
						pause();
						break;
					case KeyEvent.KEYCODE_MEDIA_NEXT:
						skip();
						break;
												
				}
			}
			
		}

	}
	
	public class AudioFocusChangeListener implements OnAudioFocusChangeListener{

		public void onAudioFocusChange(int focusChange) {
			switch(focusChange){
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
					pause();
					break;
				case AudioManager.AUDIOFOCUS_LOSS:
					stop();
					break;
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
					//TODO Figure out this part later
					pause();
					break;
				case AudioManager.AUDIOFOCUS_GAIN:
					play();
					break;
			}
		}		
	}
	
	public class PlaybackOnErrorListener extends com.pandoroid.playback.OnErrorListener{
		public void onError(String error_message, 
                			Throwable e, 
            				boolean remote_error_flag,
            				int rpc_error_code){
//			if (remote_error_flag){
//				if (rpc_error_code == RPCException.INVALID_AUTH_TOKEN){
//					m_pandora_remote.disconnect();
//					OnInvalidAuthListener 
//						listener = (OnInvalidAuthListener) listeners.get(OnInvalidAuthListener.class);
//					if (listener != null){
//						listener.onInvalidAuth();
//					}
//				}
//			}			
		}
	}
	
	private class RunningAsyncTasks {
		public AsyncTask<Void, RPCAsyncTasks.Progress, Vector<StationMetaInfo>> getStations;
		public AsyncTask<Void, RPCAsyncTasks.Progress, Void> partnerLogIn;
		public AsyncTask<String, RPCAsyncTasks.Progress, Void> userLogIn;
		
		public AlertCode exceptionHandler(Exception e) {
			AlertCode errorCode = AlertCode.ERROR_UNKNOWN;
			if (e instanceof HttpResponseException) {
				errorCode = httpResponseExceptionHandler((HttpResponseException) e);
			} else if (e instanceof IOException) {
				errorCode = ioExceptionHandler((IOException) e);
			} else	if (e instanceof RPCException) {
				errorCode = rpcExceptionHandler((RPCException) e);
			}
			
			return errorCode;
		}
		
		private AlertCode httpResponseExceptionHandler(HttpResponseException e) {
			AlertCode errorCode = AlertCode.ERROR_UNKNOWN;
			if (e.getStatusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
				errorCode = AlertCode.ERROR_REMOTE_SERVER;
			} else {
				errorCode = AlertCode.ERROR_NETWORK;
			}
		
			return errorCode;
		}
		
		@SuppressWarnings("unused")
		private AlertCode ioExceptionHandler(IOException e) {
			return AlertCode.ERROR_NETWORK;
		}
		
		private AlertCode rpcExceptionHandler(RPCException e) {
			AlertCode errorCode = AlertCode.ERROR_UNKNOWN;
			if (RPCException.URL_PARAM_MISSING_METHOD <= e.getCode() && 
					e.getCode() <= RPCException.API_VERSION_NOT_SUPPORTED) {
				errorCode = AlertCode.ERROR_UNSUPPORTED_API;
				Log.e(TAG, "Unsupported API", e);
			} else if (e.getCode() == RPCException.INTERNAL || 
					       e.getCode() == RPCException.MAINTENANCE_MODE) {
				errorCode = AlertCode.ERROR_REMOTE_SERVER;
				Log.w(TAG, "RPC error in Pandora's servers", e);
			}
		
			return errorCode;
		}	
	}
}
