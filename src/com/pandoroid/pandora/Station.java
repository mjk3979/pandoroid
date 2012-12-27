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
package com.pandoroid.pandora;

import java.io.Serializable;
import java.util.Map;
import java.util.Vector;

import android.util.Log;

/**
 * Description: Station information as given by Pandora.
 * 
 * @author Andrew Regner <andrew@aregner.com>
 * @contributor Dylan Powers <dylan.kyle.powers@gmail.com>
 *
 */
public class Station {
	
	private long mId; //The id and token are currently implemented by Pandora
	private String mToken; // as being essentially the same thing.
	
	private boolean mAllowAddMusic;
	private boolean mAllowRename;
	private String mDetailUrl;
	private boolean mIsQuickMix;
	private String mName;


	public Station(Map<String, Object> data) throws ClassCastException,
		                                            NumberFormatException{
		mId = Long.parseLong((String) data.get("stationId"));
		mToken = (String) data.get("stationIdToken");
		
		mAllowAddMusic = (Boolean) data.get("allowAddMusic");
		mAllowRename = (Boolean) data.get("allowRename");
		mDetailUrl = (String) data.get("detailUrl");
		mIsQuickMix = (Boolean) data.get("isQuickMix");
		mName = (String) data.get("stationName");
	}

	public boolean allowAddMusic(){
		return mAllowAddMusic;
	}
	public boolean allowRename(){
		return mAllowRename;
	}
	public String getDetailUrl(){
		return mDetailUrl;
	}	
	public long getId() {
		return mId;
	}
	public String getName() {
		return mName;
	}
	public boolean isQuickMix() {
		return mIsQuickMix;
	}
	public String getToken() {
		return mToken;
	}
}
