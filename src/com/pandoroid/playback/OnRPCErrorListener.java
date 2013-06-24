package com.pandoroid.playback;

import com.pandoroid.pandora.RPC.RPCException;

public interface OnRPCErrorListener {
	public void onError(RPCException e);
}
