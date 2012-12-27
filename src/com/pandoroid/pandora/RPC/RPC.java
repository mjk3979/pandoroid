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
package com.pandoroid.pandora.RPC;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.net.HttpURLConnection;
import java.util.Map;
import org.apache.http.client.HttpResponseException;

/**
 * Description: This is the RPC client implementation for interfacing with 
 * 	Pandora's servers. At the moment it uses Pandora's JSON API, but will
 *  hopefully be useful for whatever Pandora throws at us in the future.
 *  
 * @author Dylan Powers <dylan.kyle.powers@gmail.com>
 */
public class RPC {
	private static final String MIME_TYPE = "text/plain";

	private String mRequestURL;
	private String mUserAgent;
	
	/**
	 * Description: Our constructor class. This will set our default parameters
	 * 	for subsequent http requests. setURL() must be called shortly after
	 * 	construction.
	 */
	protected RPC(String userAgent){
		mUserAgent = userAgent;				
	}
	
	/**
	 * Description: This function contacts the remote server with a string
	 * 	type data package (could be JSON), and returns the remote server's 
	 * 	response in a string.
	 * @throws Exception if urlParams or entityData is empty/null or some other
	 * 	miscellaneous unspecified error occurs.
	 * @throws HttpResponseException if response is not equal to a 2XX code.
	 * @throws IOException if a connection to the remote server can't be made.
	 */
	protected String call(Map<String, String> urlParams, 
			              String entityData,
			              boolean requireSecure) throws Exception, 
			                                            HttpResponseException,
			                                            IOException{
		
		if (urlParams == null || urlParams.size() == 0){
			throw new Exception("Missing URL paramaters");
		}
		
		if (entityData == null){
			throw new Exception("Missing data for HTTP entity.");
		}
		
		// Set up the url.
		HttpURLConnection conn = getURLConnection(requireSecure, urlParams);
		
		// Add the user agent to the header.
		if (mUserAgent != null){
			conn.setRequestProperty("User-Agent", mUserAgent);
		}
		
		// Add the content type to the header.
		conn.setRequestProperty("Content-Type", MIME_TYPE);
		
		// Why yes we would like to do output!
		conn.setDoOutput(true);
		
		// More HTTP POST prep work.
		byte[] outBytes = entityData.getBytes();
		conn.setFixedLengthStreamingMode(outBytes.length);
		
		// Connect
		OutputStream post = new BufferedOutputStream(conn.getOutputStream());
		
		// Send
		post.write(outBytes);
		post.flush();
		
		// Receive server response code from the response headers.
		int httpStatusCode = conn.getResponseCode();		
		if (httpStatusCode / 100 != 2){
			throw new HttpResponseException(httpStatusCode, 
											"HTTP status code: " + httpStatusCode 
											+ " != 2XX");
		}
		
		// Receive our response.
		String response;
		try{
			InputStream in = new BufferedInputStream(conn.getInputStream());
			response = readResponse(in);
		}
		finally{
			conn.disconnect(); // Remember to clean up when you're done like a good boy.
		}
			
		return response;
	}
	
	/**
	 * Description: Gets a URL connection using the specified parameters.
	 * @param httpSecure -Flag for whether this is an HTTPS or HTTP connection.
	 * @param getParams	-The parameters that would go in a server GET request.
	 * @return -A URLConnection typecasted as an httpURLConnection that was created
	 * 	from URL parameters.
	 * @throws MalformedURLException when a problem occurs creating the URL
	 * @throws IOException if an error occurs while trying to create a URLConnection.
	 */
	private HttpURLConnection getURLConnection(boolean httpSecure,
	                                           Map<String, String> getParams) 
	                                          throws MalformedURLException,
	                                                 IOException{
		String urlString;
		if (httpSecure){
			urlString = "https://";
		}
		else{
			urlString = "http://";
		}
		
		urlString += mRequestURL;
		urlString += makeURLParamString(getParams);
		
		URL url = new URL(urlString);
		
		return (HttpURLConnection) url.openConnection();
	}
	
	/**
	 * Description: Here we create a URL method string with the parameters
	 * 	given. It automatically applies the '?' character to the beginning
	 *  of strings, so multiple calls to this function will create an invalid 
	 *  URL request string.
	 */
	private String makeURLParamString(Map<String, String> mappedURLParams){
		String urlString = "?";
		boolean firstLoop = true;
		for (Map.Entry<String, String> entry : mappedURLParams.entrySet()){
			if (!firstLoop){
				urlString += "&";
			}
			else{
				firstLoop = false;
			}
			urlString += URLEncoder.encode(entry.getKey()) + "=" 
			              + URLEncoder.encode(entry.getValue());			
		}
		
		return urlString;
	}
	
	/**
	 * Description: Reads the response body from a server call.
	 * @param in -An input stream that holds the response body.
	 * @return -The response in a string.
	 * @throws IOException if a problem occurred while reading.
	 */
	private String readResponse(InputStream in) throws IOException {
		final int BUFFER_BYTE_SIZE = 512;
		
		String responseString = new String();
		byte[] bytes = new byte[BUFFER_BYTE_SIZE];
		
		int bytesRead = 0;
		
		//Rather than read an arbitrary amount of bytes, lets be sure to get
		//it all.
		while((bytesRead = in.read(bytes, 0, BUFFER_BYTE_SIZE)) != -1){				
			responseString += new String(bytes, 0, bytesRead);
		}
		
		return responseString;
	}
	
	/**
	 * Description: This must be called along with the constructor. It sets
	 * 	the partial URL for the server (i.e. 'tuner.pandora.com/path/to/request/')
	 * @param url
	 */
	protected void setURL(String url){
		mRequestURL = url;
	}
}
