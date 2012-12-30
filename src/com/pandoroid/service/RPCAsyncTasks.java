/* 
 * Pandoroid - An open source Pandora Internet Radio client for Android.
 * 
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
package com.pandoroid.service;

import java.util.Vector;

import com.pandoroid.pandora.Song;
import com.pandoroid.pandora.Station;
import com.pandoroid.pandora.RPC.PandoraRemote;

import android.annotation.TargetApi;
import android.os.AsyncTask;
import android.os.Build;

/**
 * Description: This is a class of static operations to run asynchronous 
 * 	PandoraRemote tasks. The general idea behind the operations here is to
 * 	extend the AsyncTask construct to make calls less messy and more simplified.
 * 
 * @author Dylan Powers <dylan.kyle.powers@gmail.com>
 *
 */
public class RPCAsyncTasks{
	private static PandoraRemote mRemote;
	
	/**
	 * Description: Runs an asynchronous getPlaylist call. 
	 * 	See {@link PandoraRemote#getPlaylist(String)}
	 * @param stationToken
	 * @param taskClass
	 */
	public static void getPlaylist(String stationToken, final PostTask<Vector<Song>> taskClass){
		RPCAsyncTask<String, Vector<Song>> task = new RPCAsyncTask<String, Vector<Song>>(taskClass){
			protected Vector<Song> doInBackground(String... params){
				Vector<Song> songs = null;
				try {
					songs = mRemote.getPlaylist(params[0]);
				} catch (Exception e) {
					mCaughtException = e;
				}
				
				return songs;
			}
		};

		
		task.executeOverride(stationToken);
	}
	
	/**
	 * Description: Runs an asynchronous getStations call.
	 * 	See {@link PandoraRemote#getStations()}
	 * @param taskClass
	 */
	public static void getStations(final PostTask<Vector<Station>> taskClass){
		RPCAsyncTask<Void, Vector<Station>> task = new RPCAsyncTask<Void, Vector<Station>>(taskClass){
			protected Vector<Station> doInBackground(Void... params){
				Vector<Station> stations = null;
				try{
					stations = mRemote.getStations();
				}
				catch (Exception e){
					mCaughtException = e;
				}
				
				return stations;
			}
		};
		
		task.executeOverride();
	}
	
	/**
	 * Description: Runs an asynchronous partnerLogIn call.
	 * 	See {@link PandoraRemote#partnerLogIn()}
	 * @param taskClass
	 */
	public static void partnerLogIn(final PostTask<Void> taskClass){
		RPCAsyncTask<Void, Void> task = new RPCAsyncTask<Void, Void>(taskClass){
			protected Void doInBackground(Void... params){
				try{
					mRemote.partnerLogIn();
				}
				catch (Exception e){
					mCaughtException = e;
				}
				
				return null;
			}
		};
		
		task.executeOverride();
	}
	
	/**
	 * Description: Runs an asynchronous rate call.
	 * 	See {@link PandoraRemote#rate(String, boolean)}
	 * @param trackToken
	 * @param isPositiveRating
	 * @param taskClass
	 */
	public static void rate(String trackToken, 
			                boolean isPositiveRating, 
			                final PostTask<Long> taskClass){
		RPCAsyncTask<Object, Long> task = new RPCAsyncTask<Object, Long>(taskClass){
			protected Long doInBackground(Object... params){
				String trackToken = (String) params[0];
				Boolean isPositive = (Boolean) params[1];
				Long feedbackId = null;
				try{
					feedbackId = mRemote.rate(trackToken, isPositive);
				}
				catch (Exception e){
					mCaughtException = e;
				}
				
				return feedbackId;
			}
		};
		
		task.executeOverride(trackToken, isPositiveRating);
	}
	
	/**
	 * Description: Sets the internal static PandoraRemote to the new, updated
	 * 	remote. This needs to be called if any modifications have been made to 
	 * 	the remote outside of the class or if the class is being used for
	 * 	the first time (null pointer exceptions will ensue if not).
	 * @param remote -An instance of a PandoraRemote.
	 */
	public static void setRemote(PandoraRemote remote){
		mRemote = remote;
	}

	/**
	 * Description: Runs an asynchronous userLogIn call.
	 * 	See {@link PandoraRemote#userLogIn(String, String)}
	 * @param user
	 * @param password
	 * @param taskClass
	 */
	public static void userLogIn(String user, String password, final PostTask<Void> taskClass){
		RPCAsyncTask<String, Void> task = new RPCAsyncTask<String, Void>(taskClass){
			protected Void doInBackground(String... params) {
				try {
					mRemote.userLogIn(params[0], params[1]);
				} catch (Exception e) {
					mCaughtException = e;
				}
				return null;
			}
		};
		
		task.executeOverride(user, password);
	}

	/**
	 * Description: This is the class to implement so that async tasks can call
	 * 	its member methods on post execution.
	 * 
	 * @author Dylan Powers <dylan.kyle.powers@gmail.com>
	 *
	 * @param <ReturnType> -The return type of the PandoraRemote method to be
	 * 	invoked.
	 */
	public abstract static class PostTask<ReturnType>{
		public abstract void onException(Exception e);
		public abstract void onPostExecute(ReturnType arg);
	}
	
	/**
	 * Description: This is the base class implemented by the methods in this 
	 * 	class. For implementing new methods use this format:
	 * 	public static void newMethod(...rpc-params..., PostTask<rpc-return-type>){
	 * 		RPCAsyncTask<rpc-params-type, rpc-return-type> task = new RPCA...etc.{
	 * 			protected Void doInBackground(etc...
	 * 		};
	 * 		task.executeOverride(...rpc-params...);
	 * 	}
	 * 
	 * @author Dylan Powers <dylan.kyle.powers@gmail.com>
	 *
	 * @param <Params>
	 * @param <ReturnType>
	 */
	private abstract static class RPCAsyncTask<Params, ReturnType>
	                              extends AsyncTask<Params, Void, ReturnType>{
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
		public final AsyncTask<Params, Void, ReturnType> executeOverride(Params... params){
			if (Build.VERSION.SDK_INT >= 11){
				return super.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, params);
			}
			else{
				return super.execute(params);
			}
		}
		
		protected void onPostExecute(ReturnType ret){
			if (mCaughtException != null){
				mTaskClass.onException(mCaughtException);
			}
			else{
				mTaskClass.onPostExecute(ret);
			}
		}		
	}
}

