package com.pandoroid.playback.stations;

import com.pandoroid.pandora.RPC.RPCException;
import com.pandoroid.playback.engine.PlaybackHaltCode;

public interface stationsListener {
	public void onNewSong(Song song);
	public void onError(Throwable e);
	public void onRPCError(RPCException e);
	public void onPlaybackContinued();
	public void onPlaybackHalted(PlaybackHaltCode haltCode); 
}
