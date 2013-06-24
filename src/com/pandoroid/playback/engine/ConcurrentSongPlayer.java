package com.pandoroid.playback.engine;

import java.io.IOException;

import com.pandoroid.pandora.AudioUrl;
import com.pandoroid.pandora.Song;

import android.media.MediaPlayer;
import android.media.MediaPlayer.OnSeekCompleteListener;

/**
 * Synchronized to work most efficiently with the MediaPlaybackEngine.
 * @author Dylan Powers <dylan.kyle.powers@gmail.com>
 *
 */
public class ConcurrentSongPlayer extends MediaPlayer{
	
	//A constant for setting the percentage of a song's total length that needs
	//to be played before it is determined as being effectively finished.
	public static final int MINIMUM_SONG_COMPLETENESS = 95;
	
	private final Object BUFFER_LOCK = new Object();
	private final Object LOCK = this;	
	private final Object PLAYER_LOCK = new Object();
	
	public volatile Boolean mBufferCompleteFlag = false;
	
	private volatile boolean mAlive = false;	
	private volatile int mBufferingCounter = -1;
	private volatile int mNum_100_BufferUpdates = 0;
	private volatile boolean mSeekingFlag;
	private volatile Song mSong;
	private volatile AudioUrl mUrl;
//	private volatile MediaPlayer mPlayer;
	
	public ConcurrentSongPlayer(){
		super();
	}
	
	/**
	 * Description: Overloaded constructor for setting the song upon creation.
	 * @param song -The song to initialize to.
	 */
	public ConcurrentSongPlayer(Song song){
		super();
		setSong(song);
	}
	
//	/**
//	 * Description: Overloaded constructor for setting the initialized song, 
//	 * 	and the MediaPlayer to use.
//	 * @param song -The Song to initialize to.
//	 * @param mp -The MediaPlayer to initialize to.
//	 */
//	public ConcurrentSongPlayer(Song song, MediaPlayer mp){
//		m_player = mp;
//		setSong(song);
//	}
	
//	/**
//	 * Description: Copies the contents of another ConcurrentSongMediaPlayer
//	 * 	into itself. 
//	 * @param other_player -The ConcurrentSongMediaPlayer to copy from.
//	 */
//	public void copy(ConcurrentSongPlayer other_player){
//		if (this != other_player){
//			release();
//			synchronized(this){
//				m_player = other_player.getPlayer();
//			}
//			m_song = other_player.getSong();
//			m_buffer_complete_flag = other_player.m_buffer_complete_flag;
//			m_alive = other_player.m_alive;
//			m_url = other_player.getUrl();
//			m_num_100_buffer_updates = other_player.m_num_100_buffer_updates;
//		}
//	}
	
	/**
	 * Description: Synchronized method that retrieves the audio session id from
	 * 	the underlying MediaPlayer.
	 * @return The audio session id.
	 */
	public int getAudioSessionId(){		
		try{
			synchronized(PLAYER_LOCK){
				return super.getAudioSessionId();
			}
		}
		catch(IllegalStateException e){
			return 0;
		}
	}
	
	/**
	 * Description: Synchronized method that retrieves the current position
	 * 	from the underlying MediaPlayer.
	 * @return
	 */
	public int getCurrentPosition(){
		synchronized(PLAYER_LOCK){
			return super.getCurrentPosition();
		}
	}
	
	/**
	 * Description: Synchronized method that retrieves the duration of the 
	 * 	song from the underlying MediaPlayer.
	 * TODO Why do I check for it being alive?
	 * @return
	 */
	public int getDuration(){
		synchronized(LOCK){
			if (mAlive){
				synchronized(PLAYER_LOCK){
					return super.getDuration();
				}
			}
			else{
				return -1; //Signifying an error.
			}
		}
	}
	
//	/**
//	 * Description: Synchronized method that returns the underlying MediaPlayer.
//	 * @return
//	 */
//	public MediaPlayer getPlayer(){
//		synchronized(PLAYER_LOCK){
//			return m_player;
//		}
//	}
	
	/**
	 * Description: Returns the song.
	 * @return
	 */
	public Song getSong(){
		return mSong;
	}
	
	/**
	 * Description: Gets the url.
	 * @return
	 */
	public AudioUrl getUrl(){
		return mUrl;
	}
	
	/**
	 * Description: If this player is in fact buffering, then for every 5 calls,
	 * 	it will return true;
	 * @return
	 */
	public boolean isBuffering(){
		synchronized(BUFFER_LOCK){
			if (m_buffering_counter > 0){
				--m_buffering_counter;
			}
			else if(m_buffering_counter == 0){
				m_buffering_counter = 4;
				return true;
			}
		}
		
		return false;
	}
	
	/**
	 * Description: Checks to see if playback can be counted as complete.
	 * @return A boolean of true if it's complete, else false.
	 */
	public boolean isPlaybackComplete(){
		int song_length = getDuration();
		int current_pos = getCurrentPosition();
		
		int end_song_position = (int) ((song_length - 
				                        (
					song_length * ((float) 1F/MINIMUM_SONG_COMPLETENESS)
				                        )) / 1000F);
		
		if ((current_pos / 1000) < end_song_position){
			return false;
		}
		
		return true;
	}
	
	/**
	 * Description: Synchronized method that determines if the underlying 
	 * 	MediaPlayer is playing.
	 * @return
	 */
	public boolean isPlaying(){
		synchronized(this){
			return m_player.isPlaying();
		}
	}
	
	public boolean isSeeking(){
		return m_seeking_flag;
	}
	
	public int noBufferUpdatesHack(boolean kill){
		if (kill){
			m_num_100_buffer_updates = -1;
		}
		else if (m_num_100_buffer_updates != -1){
			++m_num_100_buffer_updates;
		}
		return m_num_100_buffer_updates;
	}
	
	/**
	 * Description: Synchronized method that pauses the underlying MediaPlayer.
	 */
	public void pause(){
		synchronized(this){
			m_player.pause();
		}
	}
	
	/**
	 * Description: This is an all in one function that resets, sets the data 
	 * 	source, prepares, and if appropriate seeks to the appropriate position 
	 *  for the MediaPlayer. After a successful call, start can be called.
	 * @param url -The url to set the player to use.
	 * @throws IllegalArgumentException
	 * @throws SecurityException
	 * @throws IllegalStateException
	 * @throws IOException
	 */
	public void prepare(AudioUrl url) throws IllegalArgumentException, 
													SecurityException, 
													IllegalStateException, 
													IOException{
		int prev_playback_pos = -1;
		
		synchronized(this){
			m_url = url;
			if (m_alive){
				prev_playback_pos = getCurrentPosition();
			}
			reset();
			
			synchronized(playerLock){
				m_player.setDataSource(url.toString());
				m_player.prepare();
				if (prev_playback_pos > 0){
					m_seeking_flag = true;
					setOnSeekCompleteListener();
					m_player.seekTo(prev_playback_pos);
				}
			}
			m_alive = true;
			setBuffering(false);
		}
	}
	
	/**
	 * Description: Synchronized method that releases the underlying MediaPlayer.
	 */
	public void release(){
		synchronized(playerLock){
			m_player.release();
		}
		m_alive = false;
	}
	
	/**
	 * Description: Synchronized method that resets the underlying MediaPlayer.
	 */
	private void reset(){
//		synchronized(this){
			m_player.reset();
//		}
		m_alive = false;
	}
	
	/**
	 * Description: Synchronized method that sends a seekTo() command to the
	 * 	underlying MediaPlayer.
	 * @param msec -The milliseconds to seek to.
	 */
	public void seekTo(int msec){
		synchronized(this){
			m_player.seekTo(msec);
		}
	}
	
	/**
	 * Description: Sets an internal buffer flag.
	 * @param bool
	 */
	public void setBuffering(boolean bool){
		synchronized(BUFFER_LOCK){
			if (bool){
				m_buffering_counter = 0;
			}
			else{
				m_buffering_counter = -1;
			}
		}
	}
	
	public void setOnBufferingUpdateListener(MediaPlayer.OnBufferingUpdateListener listener){
		synchronized(this){
			m_player.setOnBufferingUpdateListener(listener);
		}
	}
	
	public void setOnCompletionListener(MediaPlayer.OnCompletionListener listener){
		synchronized(this){
			m_player.setOnCompletionListener(listener);
		}
	}
	
	public void setOnErrorListener(MediaPlayer.OnErrorListener listener){
		synchronized(this){
			m_player.setOnErrorListener(listener);
		}
	}
	
	public void setOnInfoListener(MediaPlayer.OnInfoListener listener){
		synchronized(this){
			m_player.setOnInfoListener(listener);
		}
	}
	
	/**
	 * Description: Resets the player with the specified song.
	 * @param song -The Song to set to.
	 */
	public void setSong(Song song){
		synchronized(LOCK){
			mSong = song;
			mBufferCompleteFlag = false;
			mBufferingCounter = -1;
			synchronized(PLAYER_LOCK){
				reset();
			}
		}
	}
	
	/**
	 * Description: Synchronized method that starts the underlying media player.
	 */
	public void start(){
		synchronized(PLAYER_LOCK){
			super.start();
		}
	}
	

	
	private void setOnSeekCompleteListener(){
		mPlayer.setOnSeekCompleteListener(new OnSeekCompleteListener(){
			public void onSeekComplete(MediaPlayer mp) {
				m_seeking_flag = false;
			}
			
		});
	}
}
