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

import java.util.Vector;

import com.pandoroid.pandora.Song;
import com.pandoroid.pandora.StationMetaInfo;
import com.pandoroid.pandora.RPC.PandoraRemote;

import android.annotation.TargetApi;
import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.AsyncTask;
import android.os.Build;

/**
 * Description: This is a class of operations to run asynchronous 
 * 	PandoraRemote tasks. The general idea behind the operations here is to
 * 	extend the AsyncTask construct to make calls less messy and more simplified.
 * 
 * @author Dylan Powers <dylan.kyle.powers@gmail.com>
 *
 */
public class RPCAsyncTasks{
	public enum Progress{
		NO_NETWORK, CONNECTING 
	}
	
	private ConnectivityManager mConnectivity;
	private PandoraRemote mRemote;
	
	public RPCAsyncTasks(PandoraRemote remote, ConnectivityManager connectivity){
		mRemote = remote;
		mConnectivity = connectivity;
	}
	
	/**
	 * Description: Runs an asynchronous getPlaylist call. 
	 * 	See {@link PandoraRemote#getPlaylist(String)}
	 * @param stationToken
	 * @param taskClass
	 */
	public AsyncTask<String, Progress, Vector<Song>> getPlaylist(String stationToken, 
			                                                     final PostTask<Vector<Song>> taskClass){
		RPCAsyncTask<String, Vector<Song>> task = new RPCAsyncTask<String, Vector<Song>>(taskClass){
			protected Vector<Song> doInBackground(String... params){
				Vector<Song> songs = null;
				try {
					networkConnectWait();
					songs = mRemote.getPlaylist(params[0]);
				} catch (Exception e) {
					mCaughtException = e;
				}
				
				return songs;
			}
		};
		
		return task.executeOverride(stationToken);
	}
	
	/**
	 * Description: Runs an asynchronous getStations call.
	 * 	See {@link PandoraRemote#getStations()}
	 * @param taskClass
	 */
	public AsyncTask<Void, Progress, Vector<StationMetaInfo>> getStations(final PostTask<Vector<StationMetaInfo>> taskClass){
		RPCAsyncTask<Void, Vector<StationMetaInfo>> task = new RPCAsyncTask<Void, Vector<StationMetaInfo>>(taskClass){
			protected Vector<StationMetaInfo> doInBackground(Void... params){
				Vector<StationMetaInfo> stations = null;
				try{
					networkConnectWait();
					stations = mRemote.getStations();
				}
				catch (Exception e){
					mCaughtException = e;
				}
				
				return stations;
			}
		};
		
		return task.executeOverride();
	}
		
	/**
	 * Description: Runs an asynchronous partnerLogIn call.
	 * 	See {@link PandoraRemote#partnerLogIn()}
	 * @param taskClass
	 */
	public AsyncTask<Void, Progress, Void> partnerLogIn(final PostTask<Void> taskClass){
		RPCAsyncTask<Void, Void> task = new RPCAsyncTask<Void, Void>(taskClass){
			protected Void doInBackground(Void... params){
				try{
					networkConnectWait();
					mRemote.partnerLogIn();
				}
				catch (Exception e){
					mCaughtException = e;
				}
				
				return null;
			}
		};
		
		return task.executeOverride();
	}
	
	/**
	 * Description: Runs an asynchronous rate call.
	 * 	See {@link PandoraRemote#rate(String, boolean)}
	 * @param trackToken
	 * @param isPositiveRating
	 * @param taskClass
	 */
	public AsyncTask<Object, Progress, Long> rate(String trackToken, 
			                                      boolean isRatingPositive, 
			                                      final PostTask<Long> taskClass){
		RPCAsyncTask<Object, Long> task = new RPCAsyncTask<Object, Long>(taskClass){
			protected Long doInBackground(Object... params){
				String trackToken = (String) params[0];
				Boolean isPositive = (Boolean) params[1];
				Long feedbackId = null;
				try{
					networkConnectWait();
					feedbackId = mRemote.rate(trackToken, isPositive);
				}
				catch (Exception e){
					mCaughtException = e;
				}
				
				return feedbackId;
			}
		};
		
		return task.executeOverride(trackToken, isRatingPositive);
	}
	
	/**
	 * Description: Sets the internal static PandoraRemote to the new, updated
	 * 	remote. This needs to be called if any modifications have been made to 
	 * 	the remote outside of the class or if the class is being used for
	 * 	the first time (null pointer exceptions will ensue if not).
	 * @param remote -An instance of a PandoraRemote.
	 */
	public void setRemote(PandoraRemote remote){
		mRemote = remote;
	}
	
	/**
	 * Description: Runs an asynchronous userLogIn call.
	 * 	See {@link PandoraRemote#userLogIn(String, String)}
	 * @param user
	 * @param password
	 * @param taskClass
	 */
	public AsyncTask<String, Progress, Void> userLogIn(String user, 
			                                           String password, 
			                                           final PostTask<Void> taskClass){
		RPCAsyncTask<String, Void> task = new RPCAsyncTask<String, Void>(taskClass){
			protected Void doInBackground(String... params) {
				try {
					networkConnectWait();
					mRemote.userLogIn(params[0], params[1]);
				} catch (Exception e) {
					mCaughtException = e;
				}
				return null;
			}
		};
		
		return task.executeOverride(user, password);
	}

	/**
	 * Description: This is the class to implement so that async tasks have
	 * 	something to call when they complete.
	 * 
	 * @author Dylan Powers <dylan.kyle.powers@gmail.com>
	 *
	 * @param <ReturnType> -The return type of the PandoraRemote method to be
	 * 	invoked.
	 */
	public abstract static class PostTask<ReturnType>{
		
		/**
		 * Description: Gets called when an exception occurs.
		 * @param e -The exception that was thrown.
		 */
		public abstract void onException(Exception e);
		
		/**
		 * Description: Called upon successful completion of the task.
		 * @param arg -What is to be returned.
		 */
		public abstract void onSuccess(ReturnType arg);

		/**
		 * Description: A method that always gets called at the end of a task.
		 * 	This method is optional to implement.
		 */
		public void always() {}
		
		/**
		 * Description: A method that gets called on progress updates. 
		 * 	This method is optional to implement.
		 * @param progress
		 */
		@SuppressWarnings("unused")
		public void onProgressUpdate(Progress progress) {}
	}
	
	/**
	 * Description: This is the base class implemented by the methods in this 
	 * 	class. For implementing new methods use this format:
	 * 	public static RPCAsyncTask newMethod(...rpc-params..., PostTask<rpc-return-type>){
	 * 		RPCAsyncTask<rpc-params-type, rpc-return-type> task = new RPCA...etc.{
	 * 			protected Void doInBackground(etc...
	 * 		};
	 * 		task.executeOverride(...rpc-params...);
	 * 		return task;
	 * 	}
	 * 
	 * @author Dylan Powers <dylan.kyle.powers@gmail.com>
	 *
	 * @param <Params>
	 * @param <ReturnType>
	 */
	private abstract class RPCAsyncTask<Params, ReturnType>
	                              extends AsyncTask<Params, Progress, ReturnType>{
		protected Exception mCaughtException;
		private PostTask<ReturnType> mTaskClass;
		
		public RPCAsyncTask(PostTask<ReturnType> taskClass){
			super();
			mTaskClass = taskClass;
		}
		
		protected abstract ReturnType doInBackground(Params... params);
		
		/**
		 * Description: The implementation of execute changed between Android
		 * 	API's 10 (Gingerbread) and 11 (Honeycomb). For the sake of 
		 * 	consistency, let's stick to the old Gingerbread behavior.
		 * @param params
		 * @return
		 */
		@TargetApi(11)
		public final AsyncTask<Params, Progress, ReturnType> executeOverride(Params... params){
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB){
				return super.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
			}
			else{
				return super.execute(params);
			}
		}
		
		protected void networkConnectWait(){
			NetworkInfo activeNet = mConnectivity.getActiveNetworkInfo();
			while(activeNet == null || !activeNet.isConnected()){
				publishProgress(Progress.NO_NETWORK);
				try {
					wait(1 * 1000); // Sleep 1 second then try again
				} catch (InterruptedException e) {/*We don't care*/}
				activeNet = mConnectivity.getActiveNetworkInfo();
			}
			publishProgress(Progress.CONNECTING);
		}
		
		protected void onPostExecute(ReturnType ret){
			if (mCaughtException != null){
				mTaskClass.onException(mCaughtException);
			}
			else{
				mTaskClass.onSuccess(ret);
			}
			mTaskClass.always();
		}	
		
		protected void onProgressUpdate(Progress prog){
			mTaskClass.onProgressUpdate(prog);
		}
	}
}

