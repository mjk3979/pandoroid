/* 
 * Pandoroid - An open source Pandora Internet Radio client for Android.
 * 
 * Copyright (C) 2011  Andrew Regner <andrew@aregner.com>
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
package com.pandoroid.pandora.RPC;

import java.util.HashMap;
import java.util.Map;
import java.util.Vector;
import java.io.IOException;
import java.security.GeneralSecurityException;

import org.apache.http.client.HttpResponseException;

import com.pandoroid.pandora.AudioUrl;
import com.pandoroid.pandora.PandoraAPIException;
import com.pandoroid.pandora.PandoraAPIModifiedException;
import com.pandoroid.pandora.Song;
import com.pandoroid.pandora.Station;
import com.pandoroid.pandora.SubscriberTypeException;


/**
 * Description: Uses Pandora's JSON v5 API. Documentation of the JSON API
 * 	can be found here: http://pan-do-ra-api.wikia.com/wiki/Json/5 
 *  A network connection is required before any operation can/should take place.
 *  
 * @author Dylan Powers <dylan.kyle.powers@gmail.com>
 */
public class PandoraRemote extends JsonRPC{
	
	public static final long PLAYLIST_VALIDITY_TIME = 3600 * 3;
	public static final String DEFAULT_AUDIO_FORMAT = "aacplus";
	public static final long MIN_TIME_BETWEEN_PLAYLIST_CALLS = 30; //seconds

	private String mUserAuthToken;
	private String mPartnerAuthToken;

	public PandoraRemote(boolean pandoraOneFlag, String userAgent) 
			         throws GeneralSecurityException{
		super(pandoraOneFlag, userAgent);			
	}
	
	
	/**
	 * Description: Disabled
	 */
	//public void bookmarkArtist(Station station, Song song) {
		//Vector<Object> args = new Vector<Object>(1);
		//args.add(song.getArtistMusicId());
		
		//doCall("station.createArtistBookmark", args, null);
	//}
	
	/**
	 * Description: Disabled
	 */
	//public void bookmarkSong(Station station, Song song) {
//		Vector<Object> args = new Vector<Object>(2);
//		args.add(String.valueOf(station.getId())); 
//		args.add(song.getId());
		
		//doCall("station.createBookmark", args, null);
	//}
	                                

	
	/**
	 * Description: Logs a user in.
	 * @param user -The user's username.
	 * @param password -The user's password.
	 * @throws RPCException when a Pandora RPC error occurs.
	 * @throws SubscriberTypeException when a Pandora user is identified as not
	 * 	being the expected subscriber type.
	 * @throws IOException when Pandora's servers can't be reached.
	 * @throws HttpResponseException when an unexpected HTTP response occurs.
	 * @throws PandoraAPIException when an improper call has been made to the function.
	 * @throws PandoraAPIModifiedException when the API has changed on the server side.
	 * @throws Exception when an irrecoverable error has occurred.
	 */
	public void userLogIn(String user, String password) 
		                  throws RPCException, SubscriberTypeException,
		                         IOException, HttpResponseException, 
		                         PandoraAPIException, 
		                         PandoraAPIModifiedException, Exception {
		if (!isPartnerAuthorized()){
			throw new PandoraAPIException("Improper call to connect(), " +
					                      "the application is not authorized.");
		}
		
		HashMap<String, Object> request_args = new HashMap<String, Object>(4);
		request_args.put("loginType", "user");
		request_args.put("username", user);
		request_args.put("password", password);
		request_args.put("partnerAuthToken", mPartnerAuthToken);
		
		Map<String, Object> result = doCall("auth.userLogin", request_args, 
				                        	true, true, null);
		
		Boolean hasAudioAds = (Boolean) result.get("hasAudioAds");
		if (hasAudioAds == null){
			throw new PandoraAPIModifiedException("Invalid RPC response result.");
		}
		
		//If this is a PandoraOne subscriber and the credentials aren't correct
		if (!hasAudioAds && !isPandoraOneCredentials()){
			throw new SubscriberTypeException(true, 
				"The subscriber is Pandora One and default device credentials were given.");
		} 
		// Please note that if we are using Pandora One credentials, 
		// it is impossible to see if the opposite is true.
		else{
			String userAuthToken = (String) result.get("userAuthToken");
			String userId = (String) result.get("userId");
			if (userAuthToken == null || userId == null){
				throw new PandoraAPIModifiedException("Invalid RPC response result.");
			}
			mStandardURLParams.put("auth_token", userAuthToken);
			mStandardURLParams.put("user_id", userId);
		}
	}	
	
	/**
	 * Description: Gets a list of songs to be played. This function should not
	 * 	be called on a particular station more frequently than 
	 * 	MIN_TIME_BETWEEN_PLAYLIST_CALLS allows or an error will result.
	 * @param station_token -A string representing the station's unique 
	 * 	identification token.
	 * @return a vector of songs.
	 * @throws RPCException when a Pandora RPC error has occurred.
	 * @throws IOException when Pandora's remote servers could not be reached.
	 * @throws HttpResponseException when an unexpected HTTP response occurs.
	 * @throws PandoraAPIModifiedException
	 * @throws PandoraAPIException
	 * @throws Exception when an unexpected fatal error occurs.
	 */
	public Vector<Song> getPlaylist(String station_token) 
			                       throws RPCException,
							       IOException,
							       HttpResponseException,
							       PandoraAPIModifiedException,
							       PandoraAPIException,
							       Exception{
		
		if (!isUserAuthorized()){
			throw new PandoraAPIException("Improper call to getPlaylist(), " +
					                      "the user has not been logged in yet.");
		}

		HashMap<String, Object> requestArgs = new HashMap<String, Object>(3);
		requestArgs.put("stationToken", station_token);
		requestArgs.put("userAuthToken", mUserAuthToken);
		
		//Order matters in this request. The same order given here is 
		//the order received.
		requestArgs.put("additionalAudioUrl", 
				         Song.MP3_128 + "," + Song.AAC_32);
		
		Map<String, Object> response = doCall("station.getPlaylist", 
		                                      requestArgs, 
				                              true, true, null);
		
		Vector<Song> songs;
		try{
			Vector<Object> songsReturned = keyGetCast(response, "items");
			songs = parseItems(songsReturned);
		}
		catch(ClassCastException e){
			throw new PandoraAPIModifiedException("Type error in the playlist RPC", e);
		}
		catch(NumberFormatException e){
			throw new PandoraAPIModifiedException("Type error in the playlist RPC", e);
		}
		
		if (songs.size() == 0){
			throw new PandoraAPIModifiedException("No songs were retrieved from the call");
		}
		
		return songs;
	}
		
	/**
	 * Description: Retrieves the available stations.
	 * @return The stations.
	 * @throws RPCException if the server call failed with an error code.
	 * @throws IOException if a connection could not be made with the server.
	 * @throws HttpResponseException if the HTTP server response is not equal to a 2XX code.
	 * @throws PandoraAPIModifiedException if the API appears to have been modified.
	 * @throws PandoraAPIException when an improper call has been made to the method.
	 * @throws Exception when a fatal error has occurred.
	 */
	@SuppressWarnings("unchecked")
	public Vector<Station> getStations() throws RPCException,
											    IOException,
											    HttpResponseException, 
											    PandoraAPIModifiedException,
											    PandoraAPIException,
											    Exception{
		if (!isUserAuthorized()){
			throw new PandoraAPIException("Improper call to getStations(), " +
					                      "the user has not been logged in yet.");
		}
		
		HashMap<String, Object> requestArgs = new HashMap<String, Object>(1);
		requestArgs.put("userAuthToken", mUserAuthToken);
		
		Map<String, Object> result = doCall("user.getStationList", requestArgs, 
				                            false, true, null);
		
		Vector<Station> stations = new Vector<Station>();		
		try{
			Vector<Object> resultStations = keyGetCast(result, "stations");
	
			//Run through the stations within the array, and pick out some of the
			//properties we want.
			for (int i = 0; i < resultStations.size(); ++i){
				Map<String, Object> singleStation = (Map<String, Object>) resultStations.get(i);
				stations.add(new Station(singleStation));
			}
		}
		catch (ClassCastException e){
			throw new PandoraAPIModifiedException("Type error in the stations RPC", e);
		}
		catch (NumberFormatException e){
			throw new PandoraAPIModifiedException("Type error in the stations RPC", e);
		}
		
		return stations;
	}
	
//	private boolean isGetPlaylistCallValid(String station_token){
//		if ((station_token.compareTo(last_acquired_playlist_station) == 0)
//				                             &&
//            (this.last_acquired_playlist_time > (		
//        		  (System.currentTimeMillis() / 1000L) - MIN_TIME_BETWEEN_PLAYLIST_CALLS
//		                                        )
//		    )
//		   ){
//			return false;
//			
//		}
//		return true;
//	}
	
	public boolean isPandoraOneCredentials(){
		return mCredentials.isPandoraOne();
	}
	
	public boolean isPartnerAuthorized(){
		return (mPartnerAuthToken != null);
	}
	
	public boolean isUserAuthorized(){
		return (mUserAuthToken != null);
	}
	
	/**
	 * Description: Gets a value from the map using the specified key. The
	 * 	value gets cast to the templated type and gets returned if it isn't null
	 * 	and the cast is successful.
	 * @param map -A map to get the key from.
	 * @param key -The key to get.
	 * @return The value of the mapping.
	 * @throws PandoraAPIModifiedException if the key is missing.
	 * @throws ClassCastException if the value could not be cast.
	 */
	@SuppressWarnings("unchecked")
	private <T> T keyGetCast(Map<String, Object> map, String key) 
			                throws PandoraAPIModifiedException,
			                       ClassCastException{
		T value = (T) map.get(key);
		if (value == null){
			throw new PandoraAPIModifiedException(
					"Missing the " + key + " key pair");
		}
		return value;
	}
	
	/**
	 * Description: Parses through the item vector from a playlist call and
	 * 	grabs the appropriate data for a song.
	 * @param items -The "items" returned from a playlist call.
	 * @return A vector holding the parsed songs.
	 * @throws PandoraAPIModifiedException if the API appears to be modified.
	 * @throws ClassCastException if an object could not be cast to a particular type.
	 */
	@SuppressWarnings("unchecked")
	private Vector<Song> parseItems(Vector<Object> items)
			                       throws PandoraAPIModifiedException, 
			                              ClassCastException{
		Vector<Song> songs = new Vector<Song>();
		
		for (int i = 0; i < items.size(); ++i){
			Map<String, Object> songData = (Map<String, Object>) items.get(i);
			
			if (songData.get("adToken") == null){
				Vector<AudioUrl> audioUrlMappings = new Vector<AudioUrl>(4);				
				Vector<String> audioUrls = keyGetCast(songData, 
						                              "additionalAudioUrl");
				
				//This has to be in the same order as the request.
				audioUrlMappings.add(new AudioUrl(Song.MP3_128, 128, audioUrls.get(0)));
				audioUrlMappings.add(new AudioUrl(Song.AAC_32, 32, audioUrls.get(1)));
				
				Map<String, Object> audioUrlMap = keyGetCast(songData, 
						                                     "audioUrlMap");
				
				//MP3_192 data
				if (isPandoraOneCredentials()){
					Map<String, Object> highQuality = keyGetCast(audioUrlMap, 
							                                     "highQuality");				
					audioUrlMappings.add(new AudioUrl(highQuality));
				}			
				
				//AAC_64 data
				Map<String, Object> mediumQuality = keyGetCast(audioUrlMap, 
						                                       "mediumQuality");		
				audioUrlMappings.add(new AudioUrl(mediumQuality));	
				
			    songs.add(new Song(songData, audioUrlMappings));
			}
		}		
		return songs;
	}

	/**
	 * Description: This is the authorization for the app itself.
	 * @throws RPCException if the server replied with a failed response.
	 * @throws IOException if a network problem occurred.
	 * @throws HttpResponseException if an HTTP server error occurred.
	 * @throws PandoraAPIModifiedException if the API appears to have been modified.
	 * @throws Exception for any most likely fatal uncaught extraneous errors.
	 */
	private void partnerLogin() throws RPCException,
								       IOException,
								       HttpResponseException,
								       PandoraAPIModifiedException,
								       Exception{
		Map<String, Object> partnerParams = new HashMap<String, Object>(4);
		partnerParams.put("username", mCredentials.username);
		partnerParams.put("password", mCredentials.password);
		partnerParams.put("deviceModel", mCredentials.deviceModel);
		partnerParams.put("version", PROTOCOL_VERSION);
		
		Map<String, Object> partnerReturn = doCall("auth.partnerLogin", 
				                                   partnerParams, 
                				                   true, false, null);
		try{
			mPartnerAuthToken = keyGetCast(partnerReturn, "partnerAuthToken");
			mStandardURLParams.put("auth_token", mPartnerAuthToken);
			mStandardURLParams.put("partner_id", (String) keyGetCast(partnerReturn, "partnerId"));
			setSync((String) keyGetCast(partnerReturn, "syncTime"));
		}
		catch(ClassCastException e){
			throw new PandoraAPIModifiedException("Type error in the partner login RPC", e);
		}
	}

	/**
	 * Description: Sends a song rating/feedback to the remote server.
	 * @param trackToken -The token/ID of the track to submit feedback on.
	 * @param isPositiveRating -Whether this is positive feedback or not.
	 * @return A 64bit integer that represents the feedback ID.
	 * @throws RPCException if the server replied with a failed response.
	 * @throws IOException if a network problem occurred.
	 * @throws HttpResponseException if an HTTP server error occurred.
	 * @throws PandoraAPIModifiedException if the API appears to have been modified.
	 * @throws Exception for any most likely fatal uncaught extraneous errors.
	 */
	public long rate(String trackToken, boolean isPositiveRating) 
			         throws RPCException, IOException, HttpResponseException,
				            PandoraAPIModifiedException, Exception{
		Map<String, Object> feedbackParams = new HashMap<String, Object>(3);
		feedbackParams.put("trackToken", trackToken);
		feedbackParams.put("isPositive", isPositiveRating);
		feedbackParams.put("userAuthToken", mUserAuthToken);
		Map<String, Object> response = doCall("station.addFeedback", 
				                              feedbackParams, false, true, null);
		try{
			Long ret = Long.getLong((String) keyGetCast(response, "feedbackId"));
			if (ret != null){
				return ret;
			}
			throw new PandoraAPIModifiedException(
					"Long.getLong failed with null on the \"feedbackId\" response");
		}
		catch(ClassCastException e){
			throw new PandoraAPIModifiedException(
					"Type error in the add song feedback RPC", e);
		}
	}
	
	/**
	 * Description: Disabled
	 */
//	public void tired(Station station, Song song) {
//		Vector<Object> args = new Vector<Object>(3);
//		args.add(song.getId()); 
//		//args.add(song.getUserSeed()); 
//		args.add(String.valueOf(station.getId()));
//		
//		//doCall("listener.addTiredSong", args, null);
//	}
}