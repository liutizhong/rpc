package com.linda.framework.rpc.net;

import java.util.List;

import com.linda.framework.rpc.RpcObject;


public interface RpcCallListener {
	
	public void onRpcMessage(RpcObject rpc,RpcSender sender);

}
