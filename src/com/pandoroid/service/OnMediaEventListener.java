package com.pandoroid.service;

import com.pandoroid.pandora.Song;

public interface OnMediaEventListener {
	public abstract void onAlbumArtUpdated();
	public abstract void onNewSong(Song song);
	public abstract void onPause();
	public abstract void onPlay();
}
