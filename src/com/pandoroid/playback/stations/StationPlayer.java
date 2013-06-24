package com.pandoroid.playback.stations;

import java.io.IOException;
import java.util.Vector;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;

import android.os.AsyncTask;
import android.os.Handler;

import com.pandoroid.pandora.PandoraAPIException;
import com.pandoroid.pandora.PandoraAPIModifiedException;
import com.pandoroid.pandora.Song;
import com.pandoroid.pandora.StationMetaInfo;
import com.pandoroid.pandora.RPC.PandoraRemote;
import com.pandoroid.pandora.RPC.RPCException;
import com.pandoroid.playback.OnNewSongListener;
import com.pandoroid.playback.OnPlaybackStateChangedListener;
import com.pandoroid.playback.OnRPCErrorListener;
import com.pandoroid.playback.engine.MediaPlaybackController;
import com.pandoroid.service.RPCAsyncTasks;
import com.pandoroid.service.RPCAsyncTasks.PostTask;
import com.pandoroid.service.RPCAsyncTasks.Progress;

public class StationPlayer{
	
	private RPCAsyncTasks mAsyncRpc;
	private MediaPlaybackController mPlayer;
	private StationMetaInfo mStationMetaInfo;
	
	public StationPlayer(StationMetaInfo station, RPCAsyncTasks asyncRpc){
		mAsyncRpc = asyncRpc;
		mStationMetaInfo = station;
	}
	
	public Song getCurrentSong() throws Exception{
		return mPlayer.getSong();
	}
	
	public StationMetaInfo getStationMetaInfo(){
		return mStationMetaInfo;
	}
	
	public void pause(){
		mPlayer.pause();
	}
	
	public void play(){
		mPlayer.play();
	}
	
	public void registerListeners(OnNewSongListener newSongListener,
                                  OnPlaybackStateChangedListener pStateListener,
	                              OnRPCErrorListener onErrorListener){
		
	}
	
	public void stop(){
//		mPlayer.sleep();
	}

	public class SongGrabber{
		long mLastCallTime = 0;
		AsyncTask<String, RPCAsyncTasks.Progress, Vector<Song>> mLastAsyncTask;
		
		public void get() throws Exception{
			if (isGetPlaylistCallValid()){
				long currentTime = System.currentTimeMillis();
				long minNextCallTime = mLastCallTime 
			                           + (PandoraRemote.MIN_TIME_BETWEEN_PLAYLIST_CALLS * 1000L);
				if (currentTime < minNextCallTime){
					mLastAsyncTask = mAsyncRpc.getPlaylist(mStationMetaInfo.getToken(), 
							                                   new PostTask<Vector<Song>>(){
						@Override
						public void onException(Exception e) {
							exceptionHandler(e);
						}
						@Override
						public void onPostExecute(Vector<Song> arg) {
							mLastCallTime = System.currentTimeMillis();
							acquiredSongsHandler(arg);
						}
					});
				}
				else{ //Let's schedule for a call into the future then
					Handler handler = new Handler();
					handler.postDelayed(
						new Runnable(){
							public void run(){
								try {
									get();
								} catch (Exception e) {/*Ignore it*/}
							}
						}, minNextCallTime - currentTime);
				}
			}
			else{
				throw new Exception("Improper calls to song grabber are being made.");
			}
		}
		
		private void exceptionHandler(Exception e){
//			RPCException - when a Pandora RPC error has occurred.
//			IOException - when Pandora's remote servers could not be reached.
//			HttpResponseException - when an unexpected HTTP response occurs.
//			PandoraAPIModifiedException - if the API appears to have been modified.
//			PandoraAPIException - when an improper call has been made to the method.
//			Exception - when an unexpected fatal error occurs.
			if (e instanceof RPCException){
				RPCException rpcE = (RPCException) e;
				
			}
			else if (e instanceof PandoraAPIModifiedException){
				
			}
			else if (e instanceof PandoraAPIException){
				
			}
			else if (e instanceof HttpResponseException){
				HttpResponseException httpE = (HttpResponseException) e;
				int code = httpE.getStatusCode();
				if (code == HttpStatus.SC_REQUEST_TIMEOUT || code == HttpStatus.SC_BAD_REQUEST){
					//Try again.
					try{
						get();
					}
					catch(Exception newE){/*WTF? that shouldn't have happened*/}
				}
				else{
					//Something pretty bad happened.
				}
			}
			else if (e instanceof IOException){
				//Try again.
				try{
					get();
				}
				catch (Exception newE){/*WTF? that shouldn't have happened*/}
			}
			else{ //e instanceof Exception
				
			}
		}
		
		private void acquiredSongsHandler(Vector<Song> songs){
			mPlayer.submitSongs(songs);
		}
		
		private boolean isGetPlaylistCallValid(){			
			if (mLastAsyncTask != null){
				AsyncTask.Status status = mLastAsyncTask.getStatus();
				if (status == AsyncTask.Status.RUNNING 
					|| status == AsyncTask.Status.PENDING){
					return false;
				}
			}
			return true;
		}
	}
}
