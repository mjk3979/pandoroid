package com.pandoroid.playback.stations;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import com.pandoroid.pandora.StationMetaInfo;
import com.pandoroid.playback.OnErrorListener;
import com.pandoroid.playback.OnNewSongListener;
import com.pandoroid.playback.OnPlaybackStateChangedListener;
import com.pandoroid.service.RPCAsyncTasks;

/**
 * Description: Think of this as a radio tuner.
 *  
 * @author Dylan Powers <dylan.kyle.powers@gmail.com>
 *
 */
public class StationTuner {

	private RPCAsyncTasks mAsyncRpc;
	private StationPlayer mCurrentStation;
	private OnErrorListener mErrorListener;
	private ArrayList<StationMetaInfo> mMetaInfo;
	private OnNewSongListener mNewSongListener;
	private OnPlaybackStateChangedListener mPlayStateListener;
	private Map<String, StationPlayer> mStations;	
	
	public StationTuner(ArrayList<StationMetaInfo> stations, 
	                    RPCAsyncTasks asyncRpc,
	                    OnPlaybackStateChangedListener pStateChangedListener,
	                    OnNewSongListener newSongListener,
	                    OnErrorListener errorListener){
		mAsyncRpc = asyncRpc;
		mErrorListener = errorListener;
		mMetaInfo = stations;
		mNewSongListener = newSongListener;
		mPlayStateListener = pStateChangedListener;
		mStations = new HashMap<String, StationPlayer>(stations.size());
		
		String tokenTmp;
		StationPlayer playerTmp;
		for (int stationIndex = 0; stationIndex < stations.size(); ++stationIndex){
			tokenTmp = stations.get(stationIndex).getToken();
			playerTmp = new StationPlayer(stations.get(stationIndex), asyncRpc);
			
			mStations.put(tokenTmp, playerTmp);
		}
	}
		
	public StationPlayer changeStations(String stationToken) throws Exception{
		if (mCurrentStation != null){
			mCurrentStation.stop();
		}
		
		mCurrentStation = mStations.get(stationToken);
		
		if (mCurrentStation == null){
			throw new Exception("Station Token not found");
		}
		
		return mCurrentStation;
	}
	
	public ArrayList<StationMetaInfo> getStations(){
		return mMetaInfo;
	}
	
	public void killAll(){
		
	}
	
	public void update(ArrayList<StationMetaInfo> stations){
		
		//First, lets create a table of our old stations so we can mark them as
		//being alive or not. Note: Our goal is to be able to reuse all or
		//our old stations. 
		HashMap<String, Void> outDatedStations = new HashMap<String, Void>(mMetaInfo.size());
		for (int metaIndex = 0; metaIndex < mMetaInfo.size(); ++metaIndex){
			outDatedStations.put(mMetaInfo.get(metaIndex).getToken(), null);
		}	
		
		//Here we are adding stations we don't already have, and marking the
		//ones that shall stay (aka they're still alive).
		String tokenTmp;
		StationPlayer playerTmp;
		for (int stationIndex = 0; stationIndex < stations.size(); ++stationIndex){
			if (!mStations.containsKey(stations.get(stationIndex))){
				tokenTmp = stations.get(stationIndex).getToken();
				playerTmp = new StationPlayer(stations.get(stationIndex), mAsyncRpc);
				
				mStations.put(tokenTmp, playerTmp);
			}
			else{
				outDatedStations.remove(stations.get(stationIndex).getToken());
			}
		}
		
		//Here we are removing stations that are no longer needed.
		Iterator<String> iter = outDatedStations.keySet().iterator();
		while(iter.hasNext()){
			mStations.remove(iter.next());
		}
		
		//Lastly we want to reset our meta info
		mMetaInfo = stations;
	}
}
