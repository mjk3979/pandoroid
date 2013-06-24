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
import java.util.Vector;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;

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
import com.pandoroid.playback.stations.StationsHandler;
import com.pandoroid.service.OnAlertListener.AlertCode;




/**
 * Description: Someone really needs to give this class some loving, document
 *  it up, organize it, and make it thread safe.
 */
public class PandoroidMasterService extends Service {

//	private static final int NOTIFICATION_SONG_PLAYING = 1;
	
	//                                 
	private static final String USER_AGENT = "";//= "com.pandoroid_x.x.x-suffix"
	private static final String TAG = "Pandroid Service";
    
	// tools this service uses
//	private PandoraRemote mPandoraRemote;
//	private HashMap<Long, StationHandler> mStations = new HashMap<Long, StationHandler>();
	

//	private ConnectivityManager mConnectivityManager;
	
	private AudioControlReceiver mAudioControlReceiver = new AudioControlReceiver();
	private ComponentName mAudioControlIdentifier;
	private AudioFocusChangeListener mAudioFocusChangeListener = 
			new AudioFocusChangeListener();
	private AudioManager mAudioManager;
	
    // This is the object that receives interactions from clients. 
	private final IBinder mBinder = new PandoroidMasterServiceBinder();
	
	private StationPlayer mCurrentStation;
	private OnMediaEventListener mMediaEventCallback;
	private RPCAsyncTasks mPandoraRPCAsync;
	private PandoroidPreferenceHandler mPrefs;
	private PandoraRemote mRemote;
	private RemoteControlClient mRemoteControlClient;
	private RunningAsyncTasks mRunningAsyncTasks = new RunningAsyncTasks();
	private boolean mSet2PlayFlag = true;
	private StationsHandler mStationsHandler;
//	private boolean mIsUserActionPlay = true;
	
//	private TelephonyManager mTelephonyManager;
	
	// tracking/organizing what we are doing
//	private StationHandler mCurrentStation;
//	private String m_audio_quality;
//	private boolean mPaused;
	
	//We'll use this for now as the database implementation is garbage.
//	private ArrayList<Station> m_stations; 
//	private HashMap<Class<?>,Object> listeners = new HashMap<Class<?>,Object>();

//	protected PandoraDB db;

	
	// static usefulness
//	private static Object lock = new Object();
//	private static Object pandora_lock = new Object();
	
//	public PandoroidMasterService(){
//		super();
//		
////		String versionName = "com.pandoroid";
////		try {
////			versionName = getPackageName() + "_"
////			             + getPackageManager().getPackageInfo(getPackageName(), 0)
////			             .versionName;
////		} catch (NameNotFoundException e) {
////			Log.e(TAG, e.getMessage());
////		}
////		
////		USER_AGENT = versionName;
//	}

	//Taken straight from the Android service reference
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


    //End service reference
    
    
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
			Log.e(TAG, "Couldn't initialize the remote.", e);
			raiseAlert(AlertCode.ERROR_APPLICATION);
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
	public int onStartCommand(Intent intent, int flags, int startId) {
		return START_STICKY;
	}
	
	public void onDestroy() {
//		if (m_song_playback != null){
//			m_song_playback.stop();
//		}
//		this.unregisterReceiver(m_music_intent_receiver);
		mAudioManager.unregisterMediaButtonEventReceiver(mAudioControlIdentifier);
//		stopForeground(true);
	}
	
	private void abandonAudioFocus() {
		mAudioManager.abandonAudioFocus(mAudioFocusChangeListener);
	}

	
	public Song getCurrentSong() throws Exception{
//		return m_song_playback.getSong();
		return mCurrentStation.getCurrentSong();
	}
	
	public StationMetaInfo getCurrentStation() {
		return mCurrentStation.getStationMetaInfo();
	}
	
//	public Vector<Song> getLastSongs(int rangeBegin, int rangeEnd){
//		
//	}
	

	
	public Vector<StationMetaInfo> getStations(){
		if (mStationsHandler == null){
			serverStationUpdate();
			return null;
		}
		else{
			return mStationsHandler.getStations();
		}
	}
	

	
//	public ArrayList<Station> getStations(){
////		return m_stations;
//	}
	
//	private boolean isPartnerAuthorized(){
////		return m_pandora_remote.isPartnerAuthorized();
//	}
	
	//Gets the play state. Is it paused or playing?
	//Maybe this could be named:
	// isSet2Play();
	// isPlayControlSet();
	// isPlaying(); <== Seems ambiguous to me.
	//public int getPlayState(){
	public boolean isSet2Play(){		
		return mSet2PlayFlag; //Paused, playing
	}
	
	private void issueNotification(Song meta) {
//	tmp_song = m_song_playback.getSong();
//	Notification notification = new Notification(R.drawable.icon, 
//                  									 "Pandoroid Radio", 
//                  									 System.currentTimeMillis());
		NotificationCompat.Builder builder = new NotificationCompat.Builder(this);
		
//	Intent notificationIntent = new Intent(this, PandoroidPlayer.class);
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
	
//	private boolean isUserAuthorized(){
////		return m_pandora_remote.isUserAuthorized();
//	}
	
	private void partnerAuthorization(){
		if (mRunningAsyncTasks.partnerLogIn == null){
			mRunningAsyncTasks.partnerLogIn = mPandoraRPCAsync.partnerLogIn(
					new RPCAsyncTasks.PostTask<Void>(){	
						@Override
						public void onException(Exception e) {
							if (e instanceof RPCException && 
									((RPCException) e).getCode() == 
											RPCException.INVALID_PARTNER_CREDENTIALS) {
								raiseAlert(AlertCode.ERROR_UNSUPPORTED_API);
								Log.e(TAG, "Invalid partner credentials");
							} else {
								raiseAlert(mRunningAsyncTasks.exceptionHandler(e));
							}
						}
			
						@Override
						public void onPostExecute(Void arg) {
							mRunningAsyncTasks.partnerLogIn = null;
							signIn();
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
				raiseAlert(AlertCode.ERROR_AUDIO_FOCUS);
			}
		}
		
//		m_paused = false;
//		m_song_playback.play();
//		setNotification();
	}
	
	private void raiseAlert(OnAlertListener.AlertCode code){
		
	}
	
	public void rate(String trackToken, boolean isRatingPositive) {
		raiseAlert(AlertCode.ACTIVITY_RATING);
		mPandoraRPCAsync.rate(trackToken, isRatingPositive, new RPCAsyncTasks.PostTask<Long>(){

			@Override
			public void onException(Exception e) {
				// TODO Auto-generated method stub
				
			}

			@Override
			public void onPostExecute(Long arg) {
				// TODO Auto-generated method stub
				removeAlert(AlertCode.ACTIVITY_RATING);
			}
			
		});
//		if(rating == PandoroidPlayer.RATING_NONE) {
//			// cannot set rating to none
//			return;
//		}
//		
//		(new RateTask()).execute(rating);
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
	
	private void removeAlert(OnAlertListener.AlertCode code){
		
	}
	
	public void removeRating(String trackToken){
		
	}
	
	private boolean requestAudioFocus() {
		int result = mAudioManager.requestAudioFocus(mAudioFocusChangeListener,
		                                             AudioManager.STREAM_MUSIC,
		                                             AudioManager.AUDIOFOCUS_GAIN);
		return result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED;
	}
	
//	public StationHandler getStation(long stationId, StationHandler previouslyActiveStation){
//		
//	}
	
//	public void runPartnerLogin(boolean pandora_one_subscriber_flag) throws RPCException, 
//																			IOException,
//																			HttpResponseException,
//																			Exception{
//		Log.i("Pandoroid", 
//			  "Running a partner login for a " +
//			  (pandora_one_subscriber_flag ? "Pandora One": "standard Pandora") +
//		          " subscriber.");
//		m_pandora_remote.runPartnerLogin(pandora_one_subscriber_flag);
//	}
	
//	private void runUserLogin(String user, String password) throws HttpResponseException, 
//																  RPCException,
//																  IOException, 
//																  Exception{
//		boolean needs_partner_login = false;
//		boolean is_pandora_one_user = m_pandora_remote.isPandoraOneCredentials();
//		boolean failure_but_not_epic_failure = true;
//		while (failure_but_not_epic_failure){
//			try{
//				if (needs_partner_login){
//					m_prefs.edit().putBoolean("pandora_one_flag", is_pandora_one_user).apply();
//					runPartnerLogin(is_pandora_one_user);
//					needs_partner_login = false;
//				}
//				
//				if (is_pandora_one_user){
//					m_audio_quality = PandoraRemote.MP3_192;
//				}
//				else {
//					m_audio_quality = PandoraRemote.MP3_128;
//				}
//				Log.i("Pandoroid", "Running a user login.");
//				m_pandora_remote.connect(user, password);
//				failure_but_not_epic_failure = false; //Or any type of fail for that matter.
//			}
//			catch (SubscriberTypeException e){
//				needs_partner_login = true;
//				is_pandora_one_user = e.is_pandora_one;
//				Log.i("Pandoroid", 
//					  "Wrong subscriber type. User is a " +
//					  (is_pandora_one_user? "Pandora One": "standard Pandora") +
//			          " subscriber.");
//			}
//			catch (RPCException e){
//				if (e.code == RPCException.INVALID_AUTH_TOKEN){
//					needs_partner_login = true;
//					Log.e("Pandoroid", e.getMessage());
//				}
//				else{
//					throw e;
//				}
//			}
//		}
//	}

	
//	public void setListener(Class<?> klass, Object listener) {
//		listeners.put(klass, listener);
//	}	
//	private void setNotification() {
//		if (!m_paused){
//			try {
//				Song tmp_song;
//				tmp_song = m_song_playback.getSong();
//				Notification notification = new Notification(R.drawable.icon, 
//                        									 "Pandoroid Radio", 
//                        									 System.currentTimeMillis());
//				Intent notificationIntent = new Intent(this, PandoroidPlayer.class);
//				PendingIntent contentIntent = PendingIntent.getActivity(this, 
//                                   										NOTIFICATION_SONG_PLAYING, 
//                               											notificationIntent, 
//                               											0);
//				notification.flags |= Notification.FLAG_ONGOING_EVENT | 
//									  Notification.FLAG_FOREGROUND_SERVICE;
//				notification.setLatestEventInfo(getApplicationContext(), 
//												tmp_song.getTitle(),
//												tmp_song.getArtist() + " on " + tmp_song.getAlbum(), 
//												contentIntent);
//				startForeground(NOTIFICATION_SONG_PLAYING, notification);
//			} catch (Exception e) {}
//		}
//	}
	
	private void serverSignIn(String username, String password){
		if (!mRemote.isPartnerAuthorized()){
			partnerAuthorization();
		}
		else{
			if (username != null && password != null){
				raiseAlert(AlertCode.ACTIVITY_SIGNING_IN);
				if (mUserLogInTask == null){
					mUserLogInTask = mPandoraRPCAsync.userLogIn(username, 
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
						public void onPostExecute(Void arg) {
							removeAlert(AlertCode.ACTIVITY_SIGNING_IN);
							mUserLogInTask = null;
							// TODO Auto-generated method stub
							//Set the station
							
						}
					});	
				}
			}
			else{
				raiseAlert(AlertCode.ERROR_MISSING_USER_CREDENTIALS);
			}
		}
	}
	
	private void serverStationUpdate(){
		if (mGetStationsTask == null){
			raiseAlert(AlertCode.ACTIVITY_ACQUIRING_STATIONS);
			mGetStationsTask = mPandoraRPCAsync.getStations(new RPCAsyncTasks.PostTask<Vector<StationMetaInfo>>() {

				@Override
				public void onException(Exception e) {
					// TODO Auto-generated method stub
					
				}

				@Override
				public void onPostExecute(Vector<StationMetaInfo> arg) {
					removeAlert(AlertCode.ACTIVITY_ACQUIRING_STATIONS);
					mGetStationsTask = null;
					if (mStationsHandler == null){
						mStationsHandler = new StationsHandler(arg, mPandoraRPCAsync);
					}
					else{
						mStationsHandler.update(arg);
					}
					
					if (mCurrentStation == null){
						String token = mPrefs.getLastStationToken();
						if (token != null){
							try{
								mCurrentStation = mStationsHandler.changeStations(token);
							}
							catch(Exception e){
								mPrefs.removeLastStationToken();
								raiseAlert(AlertCode.ERROR_MISSING_STATION_SELECTION);
							}
						}
						else{
							raiseAlert(AlertCode.ERROR_MISSING_STATION_SELECTION);
						}
					}
				}			
			});
		}
	}
	
	
	public void setCurrentStation(String stationToken) {
		try {
			mCurrentStation = mStationsHandler.changeStations(stationToken);
			mPrefs.setLastStationToken(stationToken);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			raiseAlert(AlertCode.ERROR_APPLICATION);
		}
		
//		for(int i = 0; i < m_stations.size(); ++i){
//			Station tmp_station = m_stations.get(i);
//			if (tmp_station.compareTo(station_id) == 0){
//				m_current_station = tmp_station;
//				stopForeground(true);
//				setPlaybackController();
//				m_prefs.edit().putString("lastStationId", station_id).apply();
//				return true;
//			}
//		}
//		
//		return false;
	}
	
	//Includes setting and removing alerts.
	public void setOnAlertListener(OnAlertListener listener){
		
	}
	
	//This includes onPlay, onPause, and onNewSong listeners.
	public void setOnMediaEventListener(OnMediaEventListener listener){
		
	}
	
	
//	public void setTired(String trackToken){
//		
//	}	
	
	

	// Precondition: Username and password have already been checked to be
	//		somewhat valid.
	public void signIn(String username, String password){
		mPrefs.setPassword(password);
		mPrefs.setUsername(username);
		serverSignIn(username, password);
	}
	
	private void signIn(){
		String username = mPrefs.getUsername();
		String password = mPrefs.getPassword();
		
		serverSignIn(username, password);
	}
	
	public void signOut() {
		//First we shall clean out the preferences
		mPrefs.removeLastStationToken();
		mPrefs.removePandoraOneFlag();
		mPrefs.removePassword();
		mPrefs.removeUsername();
		
		//Now kill the currently running processes
		if (mPartnerLogInTask != null){
			mPartnerLogInTask.cancel(true);
		}		
		if (mUserLogInTask != null){
			mUserLogInTask.cancel(true);
		}
		if (mGetStationsTask != null){
			mGetStationsTask.cancel(true);
		}
		stop();
		mStationsHandler.killAll();
		
		//Lastly, reset everything to their default states
		mPartnerLogInTask = null;
		mUserLogInTask = null;
		mGetStationsTask = null;
		mStationsHandler = null;
		mCurrentStation = null;
		try {
			mRemote = new PandoraRemote(mPrefs.getPandoraOneFlag(), USER_AGENT);
			mPandoraRPCAsync.setRemote(mRemote);
		} catch (GeneralSecurityException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			raiseAlert(AlertCode.ERROR_APPLICATION);
		}
	}
	
	public void skip(){
//		m_song_playback.skip();
	}
	
	private void stop(){
		pause();
		//Then remove all notifications.
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
				raiseAlert(AlertCode.ERROR_APPLICATION);
			}
		}
	}
	
	public void updateStations(){
		
	}
	
//	public boolean isAlive() {
//		return m_pandora_remote.isAlive();
//	}
	

	

	
//	public void updateStations() throws HttpResponseException, 
//	RPCException, 
//	IOException, 
//	Exception {
////m_stations = m_pandora_remote.getStations();
//}
	

	
//	public void playPause(){
//		if (m_song_playback != null){
//			if (!m_paused){
//				pause();
//			}
//			else{
//				play();
//			}
//		}
//	}


	

	
	

	

	
//	public void resetPlaybackListeners(){
//		if (m_song_playback != null){
//			try {
//				m_song_playback.setOnNewSongListener(
//						(OnNewSongListener) listeners.get(OnNewSongListener.class)
//						                          );
//				m_song_playback.setOnPlaybackContinuedListener(
//						(OnPlaybackContinuedListener) listeners.get(OnPlaybackContinuedListener.class)
//															   );
//				m_song_playback.setOnPlaybackHaltedListener(
//						(OnPlaybackHaltedListener) listeners.get(OnPlaybackHaltedListener.class)
//														   );
//				m_song_playback.setOnErrorListener(new PlaybackOnErrorListener());
//
//			} 
//			catch (Exception e) {
//				Log.e("Pandoroid", e.getMessage(), e);
//			}
//		}
//	}
	
//	private void setPlaybackController(){
//		try{	
//			if (m_song_playback == null){		
//				m_song_playback = new MediaPlaybackController(m_current_station.getStationIdToken(),
//						                                    PandoraRemote.AAC_32,
//						                                    m_audio_quality,
//						                                    m_pandora_remote,
//						                                    connectivity_manager);
//
//				
//			}
//			else{
//				m_song_playback.reset(m_current_station.getStationIdToken(), m_pandora_remote);
//				
//			}
//			resetPlaybackListeners();
//		} 
//		catch (Exception e) {
//			Log.e("Pandoroid", e.getMessage(), e);
//			m_song_playback = null;
//		}
//	}
	

	
//	public void startPlayback(){		
//		if (m_song_playback != null){
//			Thread t = new Thread(m_song_playback);
//			t.start();
//		}		
//	}
	
//	public void stopPlayback(){
//		if (m_song_playback != null){
//			m_song_playback.stop();
//		}
//		stopForeground(true);
//	}
	public class AudioControlReceiver extends BroadcastReceiver {

		@Override
		public void onReceive(Context context, Intent intent) {
			String action = intent.getAction();
			if (action.equals(AudioManager.ACTION_AUDIO_BECOMING_NOISY)){
				if (mSet2PlayFlag && mPrefs.getPauseOnHeadphoneDisconnect()){
					pause();
				}
//				if (m_song_playback != null){
//					pause();
//				}
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
	
//	public abstract static class OnInvalidAuthListener{
//		public abstract void onInvalidAuth();
//	}
	
	public class AudioFocusChangeListener implements OnAudioFocusChangeListener{

		public void onAudioFocusChange(int focusChange) {
			switch(focusChange){
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
					break;
				case AudioManager.AUDIOFOCUS_LOSS:
					break;
				case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
					break;
				case AudioManager.AUDIOFOCUS_GAIN:
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
	


	
//	public class RateTask extends AsyncTask<String, Void, Void>{
//		public void onPreExecute(){
//			try {
//				this.m_song = m_song_playback.getSong();
//			} catch (Exception e) {
//				Log.e("Pandoroid", "No song to rate.");
//			}
//		}
//		public Void doInBackground(String... ratings){
//			if (m_song != null){
//				String rating = ratings[0];				
//				boolean rating_bool = rating.equals(PandoroidPlayer.RATING_LOVE) ? true : false;
//				try {
//					m_pandora_remote.rate(this.m_song, rating_bool);
//					Log.i("Pandoroid", "A " + 
//									   (rating_bool ? "thumbs up" : "thumbs down") +
//									   " rating for the song " +
//									   this.m_song.getTitle() +
//									   " was successfully sent.");
//				//We'll have to do more later, but this works for now.
////				} catch (HttpResponseException e) {
////				} catch (RPCException e) {
////				} catch (IOException e) {
//				} 
//				catch (Exception e) {
//					Log.e("Pandoroid", "Exception while sending a song rating.", e);
//				}
//			}
//			return null;
//		}
//		
//		private Song m_song;
//	}
	
	/**
	 * Description: An abstract asynchronous task for doing a generic login. 
	 * @param <Params> -Parameters specific for the doInBackground() execution.
	 */
//	public abstract static class ServerAsyncTask<Params> extends AsyncTask<Params, 
//																		   Void, 
//																		   Integer> {
//		protected static final int ERROR_UNSUPPORTED_API = 0;
//		protected static final int ERROR_NETWORK = 1;
//		protected static final int ERROR_UNKNOWN = 2;
//		protected static final int ERROR_REMOTE_SERVER = 3;
//
//
//		/**
//		 * Description: The required AsyncTask.doInBackground() function.
//		 */
//		protected abstract Integer doInBackground(Params... args);
//		
//		protected abstract void quit();
//		
//		protected abstract void reportAction();
//		
//		/**
//		 * Description: A function that specifies the action to be taken
//		 * 	when a user clicks the retry button in an alert dialog.
//		 */
//		protected abstract void retryAction();
//		
//		protected abstract void showAlert(AlertDialog new_alert);
//
//		/**
//		 * Description: Builds an alert dialog necessary for all login tasks.
//		 * @param error -The error code.
//		 * @return An alert dialog builder that can be converted into an alert
//		 * 	dialog.
//		 */
//		protected AlertDialog.Builder buildErrorDialog(int error, final Context context) {
//			AlertDialog.Builder alert_builder = new AlertDialog.Builder(context);
//			alert_builder.setCancelable(false);
//			alert_builder.setPositiveButton("Quit",
//					new DialogInterface.OnClickListener() {
//						public void onClick(DialogInterface dialog, int which) {
//							quit();
//						}
//					});
//
//			switch (error) {
//			case ERROR_NETWORK:
//			case ERROR_UNKNOWN:
//			case ERROR_REMOTE_SERVER:
//				alert_builder.setNeutralButton("Retry",
//						new DialogInterface.OnClickListener() {
//							public void onClick(DialogInterface dialog,
//									int which) {
//								retryAction();
//							}
//						});
//			}
//
//			switch (error) {
//			case ERROR_UNSUPPORTED_API:
//				alert_builder.setNeutralButton("Report",
//						new DialogInterface.OnClickListener() {
//							public void onClick(DialogInterface dialog,
//									int which) {
//								reportAction();
//							}
//						});
//				alert_builder.setMessage("Please update the app. "
//						+ "The current Pandora API is unsupported.");
//				break;
//			case ERROR_NETWORK:
//				alert_builder.setMessage("A network error has occurred.");
//				break;
//			case ERROR_UNKNOWN:
//				alert_builder.setMessage("An unknown error has occurred.");
//				break;
//			case ERROR_REMOTE_SERVER:
//				alert_builder
//						.setMessage("Pandora's servers are having troubles. "
//								+ "Try again later.");
//				break;
//			}
//
//			return alert_builder;
//		}
//
//		/**
//		 * Description: A test to show off different exceptions.
//		 * @throws RPCException
//		 * @throws HttpResponseException
//		 * @throws IOException
//		 * @throws Exception
//		 */
//		public void exceptionTest() throws RPCException, HttpResponseException,
//				IOException, Exception {
//			switch (1) {
//				case 0:
//					throw new RPCException(
//							RPCException.API_VERSION_NOT_SUPPORTED,
//							"Invalid API test");
//				case 1:
//					throw new HttpResponseException(
//							HttpStatus.SC_INTERNAL_SERVER_ERROR,
//							"Internal server error test");
//				case 2:
//					throw new IOException("IO exception test");
//				case 3:
//					throw new Exception("Generic exception test");
//			}
//		}
//
//		/**
//		 * Description: A handler that must be called when an RPCException 
//		 * 	has occurred.
//		 * @param e
//		 * @return
//		 */
//		protected int rpcExceptionHandler(RPCException e) {
//			int success_flag = ERROR_UNKNOWN;
//			if (RPCException.URL_PARAM_MISSING_METHOD <= e.code
//					&& e.code <= RPCException.API_VERSION_NOT_SUPPORTED) {
//				success_flag = ERROR_UNSUPPORTED_API;
//			} else if (e.code == RPCException.INTERNAL
//					|| e.code == RPCException.MAINTENANCE_MODE) {
//				success_flag = ERROR_REMOTE_SERVER;
//			} else {
//				success_flag = ERROR_UNKNOWN;
//			}
//
//			return success_flag;
//		}
//
//		/**
//		 * Description: A handler that must be called when an HttpResponseException
//		 * 	has occurred.
//		 * @param e
//		 * @return
//		 */
//		protected int httpResponseExceptionHandler(HttpResponseException e) {
//			int success_flag = ERROR_UNKNOWN;
//			if (e.getStatusCode() == HttpStatus.SC_INTERNAL_SERVER_ERROR) {
//				success_flag = ERROR_REMOTE_SERVER;
//			} else {
//				success_flag = ERROR_NETWORK;
//			}
//
//			return success_flag;
//		}
//
//		/**
//		 * Description: A handler that must be called when an IOException
//		 * 	has been encountered.
//		 * @param e
//		 * @return
//		 */
//		protected int ioExceptionHandler(IOException e) {
//			return ERROR_NETWORK;
//		}
//
//		/**
//		 * Description: A handler that must be called when a generic Exception has
//		 * 	been encountered.
//		 * @param e
//		 * @return
//		 */
//		protected int generalExceptionHandler(Exception e) {
//			return ERROR_UNKNOWN;
//		}
//	}
	
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
