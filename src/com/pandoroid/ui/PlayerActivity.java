/* Pandoroid Radio - open source pandora.com client for android
 * Copyright (C) 2011  Andrew Regner <andrew@aregner.com>
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
package com.pandoroid.ui;

//import java.io.IOException;

//import org.apache.http.HttpStatus;
//import org.apache.http.client.HttpResponseException;

import com.actionbarsherlock.app.SherlockActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.actionbarsherlock.view.SubMenu;
import com.pandoroid.R.drawable;
import com.pandoroid.R.id;
import com.pandoroid.R.layout;
import com.pandoroid.R.string;
import com.pandoroid.R.style;
import com.pandoroid.pandora.Song;
import com.pandoroid.playback.OnNewSongListener;
import com.pandoroid.playback.OnPlaybackContinuedListener;
import com.pandoroid.playback.OnPlaybackHaltedListener;
import com.pandoroid.playback.engine.MediaPlaybackController;
import com.pandoroid.service.OnAlertListener;
import com.pandoroid.service.OnMediaEventListener;
import com.pandoroid.service.PandoroidMasterService;
//import com.pandoroid.service.PandoroidMasterService.ServerAsyncTask;
import com.pandoroid.AboutDialog;
import com.pandoroid.PandoroidLogin;
import com.pandoroid.PandoroidSettings;
import com.pandoroid.PandoroidStationSelect;
import com.pandoroid.R;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.ServiceConnection;
//import android.content.SharedPreferences;
//import android.media.MediaPlayer;
//import android.media.MediaPlayer.OnCompletionListener;
//import android.media.MediaPlayer.OnPreparedListener;
import android.net.Uri;
//import android.os.AsyncTask;
import android.os.Bundle;
import android.os.IBinder;
//import android.preference.PreferenceManager;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class PlayerActivity extends SherlockActivity {

//	public static final int REQUIRE_SELECT_STATION = 0x10;
//	public static final int REQUIRE_LOGIN_CREDS = 0x20;
//	public static final String RATING_BAN = "ban";
//	public static final String RATING_LOVE = "love";
//	public static final String RATING_NONE = null;

	// private static final String STATE_IMAGE = "imageDownloader";


	private static AlertDialog mAlert;
	private static boolean mAlertActiveFlag = false;
	private static int mSongHaltedReason = MediaPlaybackController.HALT_STATE_CLEAR;
	private boolean mIsServiceBound;
//	private SharedPreferences m_prefs;
//	private PartnerLoginTask m_partner_login_task;
	private static boolean mPartnerLoginFinishedFlag = true;
	private PandoroidMasterService mService;
	private ServiceConnection mServiceConnection = new PandoroidMasterServiceConnection();
//	private RetrieveStationsTask m_retrieve_stations_task;
	private static boolean mRetrieveStationsFinishedFlag = true;
//	private UserLoginTask m_user_login_task;
	private static boolean mUserLoginFinishedFlag = true;
	private static ProgressDialog mWaiting;
	
	/*
	 * Service connection specific stuff.
	 */
	void doBindService() {

		// This is the master service start
		startService(new Intent(this, PandoroidMasterService.class));

		// Establish a connection with the service. We use an explicit
		// class name because we want a specific service implementation that
		// we know will be running in our own process (and thus won't be
		// supporting component replacement by other applications).
		bindService(new Intent(this, PandoroidMasterService.class), 
				    mServiceConnection,
					Context.BIND_AUTO_CREATE);
	}

	void doUnbindService() {
		if (mIsServiceBound) {
			// Detach our existing connection.
			unbindService(mServiceConnection);
			mIsServiceBound = false;
		}
	}
	
	private class PandoroidMasterServiceConnection implements ServiceConnection{

		public void onServiceConnected(ComponentName name, IBinder service) {
			// This is called when the connection with the service has been
			// established, giving us the service object we can use to
			// interact with the service. Because we have bound to a explicit
			// service that we know is running in our own process, we can
			// cast its IBinder to a concrete class and directly access it.
			mService = ((PandoroidMasterService.PandoroidMasterServiceBinder) service).getService();
			mIsServiceBound = true;
			
			mService.setOnAlertListener(new PlayerOnAlertListener());
			
			mService.setOnMediaEventListener(new PlayerOnMediaEventListener());
			
			//Last step once everything has been configured.
			startup();
		}

		public void onServiceDisconnected(ComponentName name) {
			// This is called when the connection with the service has been
			// unexpectedly disconnected -- that is, its process crashed.
			// Because it is running in our same process, we should never
			// see this happen.
			mService = null;
			mIsServiceBound = false;
			
		}
		
	}
	/* End Service */
	
	/*
	 * Activity start and end specific stuff
	 */
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setTheme(R.style.Theme_Sherlock);
		setContentView(R.layout.player);

		//m_prefs = PreferenceManager.getDefaultSharedPreferences(this);
		doBindService();
	}

	@Override
	protected void onStart() {
		super.onStart();
	}

	@Override
	protected void onResume() {
		super.onResume();
		if (mIsServiceBound) {
			startup();
		} else {
			mWaiting = ProgressDialog.show(PlayerActivity.this, "",
										  getString(R.string.loading));
		}
	}

	protected void onPause() {
		super.onPause();
	}

	protected void onStop() {
		super.onStop();
		if (mAlertActiveFlag) {
			mAlert.dismiss();
		}
		dismissWaiting();
	}

	protected void onDestroy() {
		super.onDestroy();
		doUnbindService();
	}
	/* End Activity */

	
	public void controlButtonPressed(View button) {
		switch (button.getId()) {
		case R.id.player_ban:
			mService.rate(mService.getCurrentSong().getToken(), false);
			/*m_service.rate(RATING_BAN);*/
			Toast.makeText(getApplicationContext(),
			               getString(R.string.baned_song), 
			               Toast.LENGTH_SHORT).show();
			/*
			if (m_prefs.getBoolean("behave_nextOnBan", true)) {
				m_service.skip();
			}*/
			break;

		case R.id.player_love:
			mService.rate(mService.getCurrentSong().getToken(), true);
			/*
			m_service.rate(RATING_LOVE);
			*/
			Toast.makeText(getApplicationContext(),
			               getString(R.string.loved_song), 
			               Toast.LENGTH_SHORT).show();
			break;

		case R.id.player_pause:
			playPause();
			break;

		case R.id.player_next:
			mService.skip();
			break;
		}
	}

	/**
	 * Description: Removes alert dialogs.
	 */
	private void dismissAlert() {
		if (mAlertActiveFlag) {
			mAlert.dismiss();
			mAlertActiveFlag = false;
			mAlert = null;
		}
	}

	private void dismissSongHaltedProgress(){
		mSongHaltedReason = MediaPlaybackController.HALT_STATE_CLEAR;
		View progress = findViewById(R.id.progressUpdate);
		progress.setVisibility(View.INVISIBLE);
	}
	
	/**
	 * Description: Removes waiting prompts.
	 */
	private static void dismissWaiting() {
		if (mWaiting != null && mWaiting.isShowing()) {
			mWaiting.dismiss();
			mWaiting = null;
		}
	}

	public boolean onCreateOptionsMenu(Menu menu) {
		SubMenu sub = menu.addSubMenu("Options");
		sub.add(0, R.id.menu_stations, Menu.NONE, R.string.menu_stations);
		sub.add(0, R.id.menu_settings, Menu.NONE, R.string.menu_settings);
		sub.add(0, R.id.menu_logout, Menu.NONE, R.string.menu_logout);
		sub.add(0, R.id.menu_about, Menu.NONE, R.string.menu_about);

		MenuItem subMenu = sub.getItem();
		subMenu.setIcon(R.drawable.ic_sysbar_menu);
		subMenu.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
		return super.onCreateOptionsMenu(menu);
	}


	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {

		case R.id.menu_stations:
			spawnStationActivity();
			return true;

		case R.id.menu_logout:
			mService.signOut();
//			SharedPreferences.Editor prefs_edit = m_prefs.edit();
//			prefs_edit.remove("pandora_username");
//			prefs_edit.remove("pandora_password");
//			prefs_edit.remove("lastStationId");
//			prefs_edit.apply();
//			dismissSongHaltedProgress(); 
			userLogin();
			return true;

		case R.id.menu_settings:
			startActivity(new Intent(getApplicationContext(),
			              PandoroidSettings.class));
			return true;

		case R.id.menu_about:
			startActivity(new Intent(getApplicationContext(), 
			              AboutDialog.class));
			return true;

		default:
			return super.onOptionsItemSelected(item);
		}
	}
	
	private void playPause(){
		if (mService.isSet2Play()){
			mService.pause();
		}
		
		mService.play();
	}

	/**
	 * Description: Executes a PartnerLoginTask.
	 */
//	private void partnerLogin() {
//		if (m_partner_login_finished_flag == true) {
//			m_partner_login_finished_flag = false;
//			m_partner_login_task = new PartnerLoginTask();
//			boolean pandora_one_flag = m_prefs.getBoolean("pandora_one_flag", true);
//			m_partner_login_task.execute(pandora_one_flag);
//		}
//		else{
//			showWaiting("Authorizing the app...");
//		}
//	}

	/**
	 * Description: Closes down the app as much as is possible with android.
	 */
	private void quit() {
		doUnbindService();
		stopService(new Intent(PlayerActivity.this, PandoroidMasterService.class));
		mAlertActiveFlag = false;
		finish();
	}
	
	private void reportAction(){
		String issue_url = "https://github.com/dylanPowers/pandoroid/issues";
		Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(issue_url));
		startActivity(i);
	}
	
	private void retryAction() {
		mAlertActiveFlag = false;
		mService.play();
	}
	
	/**
	 * Description: Executes the RetrieveStationsTask.
	 */
//	private void setStation(){
//		if (m_retrieve_stations_finished_flag == true) {
//			m_retrieve_stations_finished_flag = false;
//			m_retrieve_stations_task = new RetrieveStationsTask();
//			m_retrieve_stations_task.execute();
//		}
//		else{
//			showWaiting("Acquiring a station...");
//		}
//	}

	/**
	 * Description: Shows an alert with the specified alert.
	 * @param new_alert -The AlertDialog to show.
	 */
	private void showAlert(AlertDialog new_alert) {
		dismissAlert();
		mAlert = new_alert;
		mAlert.show();
		mAlertActiveFlag = true;
	}
	
	private void showSongHaltedProgress(int reason){
		mSongHaltedReason = reason;
		View progress = findViewById(R.id.progressUpdate);
		TextView view_text = (TextView) findViewById(R.id.progressText);
		
		int reason_str;
		switch(reason){
			case MediaPlaybackController.HALT_STATE_NO_NETWORK:
				reason_str = R.string.no_network; 
				break;
			case MediaPlaybackController.HALT_STATE_NO_INTERNET:
				reason_str = R.string.no_internet;
				break;
			case MediaPlaybackController.HALT_STATE_BUFFERING:
				reason_str = R.string.buffering;
				break;
			case MediaPlaybackController.HALT_STATE_PREPARING:
				reason_str = R.string.preparing;
				break;
			case MediaPlaybackController.HALT_STATE_NO_SONGS:
				reason_str = R.string.no_songs;
				break;
			default:
				reason_str = R.string.buffering;
		}
	 
		view_text.setText(reason_str);
		progress.setVisibility(View.VISIBLE);	
		
	}

	/**
	 * Description: Shows a waiting/ProgressDialog with the specified message.
	 * @param message -The String to show in the ProgressDialog.
	 */
	private void showWaiting(String message) {
		dismissWaiting();
		mWaiting = ProgressDialog.show(this, "", message);
		mWaiting.show();
	}

	/**
	 * Description: Refreshes the view with the specified song. If song is null
	 * 	it will reset to the default configuration.
	 * @param song -The song to set the view to show.
	 */
	private void songRefresh(Song song) {
		TextView top = (TextView) findViewById(R.id.player_topText);
		ImageView image = (ImageView) findViewById(R.id.player_image);
		
		if (song != null){
			getSupportActionBar().setTitle(String.format("" + song.getTitle()));
			//mService.image_downloader.download(song.getAlbumArtUrl(), image);
			top.setText(String.format("%s\n%s", song.getArtist(), song.getAlbum()));
		}
		else{
			image.setImageResource(R.drawable.transparent);
			top.setText(R.string.loading);
			getSupportActionBar().setTitle(R.string.app_name);
		}

	}

	/**
	 * Description: Starts a login activity.
	 */
	private void spawnLoginActivity() {
		//m_prefs.edit().remove("pandora_password").apply();
		startActivity(new Intent(this, PandoroidLogin.class));
	}
	
	/**
	 * Description: Starts a new station selection activity.
	 */
	private void spawnStationActivity(){
		startActivity(new Intent(this, PandoroidStationSelect.class));
	}

	/**
	 * Description: This gets executed whenever the activity has to be
	 * 	restarted/resumed. 
	 */
	private void startup() {
		if (mAlertActiveFlag) {
			mAlert.show();
		}
		else {
//			if (mService.isSet2Play()){
//				mService.play();//else if (!m_service.isPartnerAuthorized()) {
//			}
//		}
//			partnerLogin();
//		} else if (!m_service.isUserAuthorized()) {
//			userLogin();
//		} 
//		else if (m_service.getCurrentStation() == null){
//			setStation();
//		}
		//else {
			if (mSongHaltedReason != MediaPlaybackController.HALT_STATE_CLEAR){
				showSongHaltedProgress(mSongHaltedReason);
			}
			try{
				songRefresh(mService.getCurrentSong());
			}
			catch(Exception e){
				songRefresh(null);
			}			
			dismissWaiting();
		}
	}

	/**
	 * Description: Updates a new song. This is mainly for OnNewSongListener
	 * 	purposes.
	 * @param song -The new Song.
	 */
	private void updateForNewSong(Song song) {
		//m_service.setNotification();
		songRefresh(song);
	}

	/**
	 * Description: Executes the UserLoginTask.
	 */
	private void userLogin() {
		spawnLoginActivity();
//		if (m_user_login_finished_flag == true) {
//			String username = m_prefs.getString("pandora_username", null);
//			String password = m_prefs.getString("pandora_password", null);
//
//			if (username == null || password == null) {
//				spawnLoginActivity();
//			} else {
//				m_user_login_finished_flag = false;
//				m_user_login_task = new UserLoginTask();
//				m_user_login_task.execute(username, password);
//			}
//		}
//		else{
//			showWaiting(getString(R.string.signing_in));
//		}
	}
	
	private class PlayerOnAlertListener implements OnAlertListener{
//		Context mContext;
//		
//		public PlayerAlertListener(Context context){
//			mContext = context;
//		}
		
		public void onAlert(AlertCode code) {
			switch(code){
				case ACTIVITY_ACQUIRING_STATIONS:
				case ACTIVITY_SIGNING_IN:
					handleOnActivityAlert(code);
					break;
				case ERROR_INVALID_USER_CREDENTIALS:
				case ERROR_NETWORK:
				case ERROR_REMOTE_SERVER:
				case ERROR_UNKNOWN:
				case ERROR_UNSUPPORTED_API:
					handleOnErrorAlert(code);
					break;				
			}
		}

		public void onRemoveAlert(AlertCode code) {
			if (code == AlertCode.ACTIVITY_ACQUIRING_STATIONS){
				dismissWaiting();
			}			
		}
		
		private void handleOnActivityAlert(AlertCode code){
//			if (code == AlertCode.ACTIVITY_ACQUIRING_STATIONS 
//				    || code == AlertCode.ACTIVITY_SIGNING_IN){
//					//showWaiting???
//			}
		}
		
		private void handleOnErrorAlert(AlertCode code){
			AlertDialog.Builder alert_builder = new AlertDialog.Builder(PlayerActivity.this);
			alert_builder.setCancelable(false);
			alert_builder.setPositiveButton("Quit",
					new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
							quit();
						}
					});

			switch (code){
				case ERROR_NETWORK:
				case ERROR_UNKNOWN:
				case ERROR_REMOTE_SERVER:
					alert_builder.setNeutralButton("Retry",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										            int which) {
									retryAction();
								}
							});
			}

			switch (code) {
				case ERROR_UNSUPPORTED_API:
					alert_builder.setNeutralButton("Report",
							new DialogInterface.OnClickListener() {
								public void onClick(DialogInterface dialog,
										            int which) {
									reportAction();
								}
							});
					alert_builder.setMessage("Please update the app. "
					                         + "The current Pandora API is unsupported.");
					break;
				case ERROR_NETWORK:
					alert_builder.setMessage("A network error has occurred.");
					break;
				case ERROR_UNKNOWN:
					alert_builder.setMessage("An unknown error has occurred.");
					break;
				case ERROR_REMOTE_SERVER:
					alert_builder.setMessage("Pandora's servers are having troubles. "
					                         + "Try again later.");
					break;
			}
		}
	}
	
	private class PlayerOnMediaEventListener implements OnMediaEventListener{

		public void onAlbumArtUpdated() {
			// TODO Auto-generated method stub
			
		}
		
		public void onNewSong(Song song) {
			/*
			 * 			mService.setListener(OnNewSongListener.class, new OnNewSongListener() {
				public void onNewSong(Song song) {
					updateForNewSong(song);
				}
			});
			 * 
			 * 
			 * 
			 */
			
		}

		public void onPause() {
			// TODO Auto-generated method stub
			
		}

		public void onPlay() {
			// TODO Auto-generated method stub
			
		}
		
	}

	/**
	 * Description: An abstract class of ServerAsyncTask that's specific to this
	 * 	activity.
	 * @param <Params> -Parameters specific to the doInBackground() execution.
	 */
//	private abstract class PandoroidPlayerServerTask<Params> extends ServerAsyncTask<Params>{
//		
//		protected AlertDialog.Builder buildErrorDialog(int error){
//			return super.buildErrorDialog(error, PandoroidPlayer.this);
//		}
//		
//		protected void quit(){
//			PandoroidPlayer.this.quit();
//		}
//		
//		protected void reportAction(){
//			String issue_url = "https://github.com/dylanPowers/pandoroid/issues";
//			Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(issue_url));
//			startActivity(i);
//		}
//		
//		protected void showAlert(AlertDialog alert){
//			PandoroidPlayer.this.showAlert(alert);
//		}
//	}

	/**
	 * Description: A login async task specific to authorizing the app itself
	 * 	for further communication with Pandora's servers.
	 */
//	private class PartnerLoginTask extends PandoroidPlayerServerTask<Boolean> {
//		protected void onPreExecute() {
//			showWaiting("Authorizing the app...");
//		}
//
//		protected Integer doInBackground(Boolean... subscriber_type) {
//			Integer success_flag = -1;
//			try {
//				m_service.runPartnerLogin(subscriber_type[0].booleanValue());
//				// exceptionTest();
//			} catch (RPCException e) {
//				Log.e("Pandoroid", "Error running partner login.", e);
//				if (e.code == RPCException.INVALID_PARTNER_CREDENTIALS) {
//					success_flag = ERROR_UNSUPPORTED_API;
//				} else {
//					success_flag = rpcExceptionHandler(e);
//				}
//			} catch (HttpResponseException e) {
//				Log.e("Pandoroid", "Error running partner login.", e);
//				success_flag = httpResponseExceptionHandler(e);
//			} catch (IOException e) {
//				Log.e("Pandoroid", "Error running partner login.", e);
//				success_flag = ioExceptionHandler(e);
//			} catch (Exception e) {
//				Log.e("Pandoroid", "Error running partner login.", e);
//				success_flag = generalExceptionHandler(e);
//			}
//
//			return success_flag;
//		}
//
//		protected void onPostExecute(Integer success_int) {
//			int success = success_int.intValue();
//			if (success >= 0) {
//				AlertDialog.Builder alert_builder = buildErrorDialog(success);
//				showAlert(alert_builder.create());
//			} else {
//				userLogin();
//			}
//			
//			m_partner_login_finished_flag = true;
//		}
//		
//		protected void retryAction() {
//			m_alert_active_flag = false;
//			partnerLogin();
//		}
//
//	}
	
	/**
	 * Description: A retrieving stations asynchronous task.
	 * @author Dylan Powers <dylan.kyle.powers@gmail.com>
	 *
	 */
//	private class RetrieveStationsTask extends PandoroidPlayerServerTask<Void>{
//		protected void onPreExecute(){
//			showWaiting("Acquiring a station...");
//
//		}
//		
//		protected Integer doInBackground(Void... massive_void){
//			Integer success_flag = -1;
//			try{
//				m_service.updateStations();
//			} catch (RPCException e) {
//				Log.e("Pandoroid", "Error fetching stations.", e);
//				success_flag = rpcExceptionHandler(e);
//			} catch (HttpResponseException e) {
//				Log.e("Pandoroid", "Error fetching stations.", e);
//				success_flag = httpResponseExceptionHandler(e);
//			} catch (IOException e) {
//				Log.e("Pandoroid", "Error fetching stations.", e);
//				success_flag = ioExceptionHandler(e);
//			} catch (Exception e) {
//				Log.e("Pandoroid", "Error fetching stations.", e);
//				success_flag = generalExceptionHandler(e);
//			}
//			
//			return success_flag;		
//		}
//		
//		protected void onPostExecute(Integer success_int){
//			if (success_int.intValue() < 0){
//				dismissWaiting();
//				String last_station_id = m_prefs.getString("lastStationId", null);
//				if (last_station_id != null && m_service.setCurrentStation(last_station_id)){
//					m_service.startPlayback();
//				}
//				else{
//					spawnStationActivity();
//				}
//			}
//			else {
//				AlertDialog.Builder alert_builder = buildErrorDialog(success_int);
//				showAlert(alert_builder.create());
//			}
//			
//			m_retrieve_stations_finished_flag = true;
//		}
//		
//		protected void retryAction(){
//			m_alert_active_flag = false;
//			setStation();
//		}
//	}

	/**
	 * Description: A login async task specific to logging in a user.
	 */
//	private class UserLoginTask extends PandoroidPlayerServerTask<String> {
//		private static final int ERROR_BAD_CREDENTIALS = 10;
//
//		protected void onPreExecute() {
//			showWaiting(getString(R.string.signing_in));
//		}
//		
//		protected Integer doInBackground(String... strings) {
//			Integer success_flag = -1;
//			try {
//				m_service.runUserLogin(strings[0], strings[1]);
//				// exceptionTest();
//			} catch (RPCException e) {
//				Log.e("Pandoroid", "RPC error running user login. " + e.getMessage());
//				if (e.code == RPCException.INVALID_USER_CREDENTIALS) {
//					success_flag = ERROR_BAD_CREDENTIALS;
//				} else {
//					success_flag = rpcExceptionHandler(e);
//				}
//			} catch (HttpResponseException e) {
//				Log.e("Pandoroid", "Error running user login.", e);
//				success_flag = httpResponseExceptionHandler(e);
//			} catch (IOException e) {
//				Log.e("Pandoroid", "Error running user login.", e);
//				success_flag = ioExceptionHandler(e);
//			} catch (Exception e) {
//				Log.e("Pandoroid", "Error running user login.", e);
//				success_flag = generalExceptionHandler(e);
//			}
//
//			return success_flag;
//		}
//
//		protected void onPostExecute(Integer success_int) {
//			int success = success_int.intValue();
//			if (success == ERROR_BAD_CREDENTIALS) {
//				int len = Toast.LENGTH_LONG;
//				CharSequence text = "Wrong username and/or password!";
//				Toast toasty = Toast.makeText(PandoroidPlayer.this, text, len);
//				toasty.show();
//				spawnLoginActivity();
//			} else if (success >= 0) {
//				AlertDialog.Builder alert_builder = buildErrorDialog(success);
//				showAlert(alert_builder.create());
//			} else {
//				setStation();
//			}
//			
//			m_user_login_finished_flag = true;
//		}
//
//		protected void retryAction() {
//			m_alert_active_flag = false;
//			userLogin();
//		}
//	}
}