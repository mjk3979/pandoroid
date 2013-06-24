package com.pandoroid.playback;

import com.pandoroid.playback.engine.PlaybackHaltCode;

public interface OnPlaybackStateChangedListener {
	public void onPlaybackContinued();
	public void onPlaybackHalted(PlaybackHaltCode haltCode); 
}
