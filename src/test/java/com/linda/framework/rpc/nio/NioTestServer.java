package com.linda.framework.rpc.nio;

import java.util.Enumeration;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.linda.framework.rpc.RpcObject;
import com.linda.framework.rpc.net.RpcCallListener;
import com.linda.framework.rpc.net.RpcSender;

public class NioTestServer implements RpcCallListener{
	
	private static Logger logger = Logger.getLogger(NioTestServer.class);
	RpcNioAcceptor acceptor;
	private RpcNioSelection selection;
	private String host;
	private int port;
	//使用 AtomicBoolean 高效并发处理 “只初始化一次” 的功能要求：
	private AtomicInteger receive = new AtomicInteger(0);
	private AtomicBoolean started = new AtomicBoolean(false);
	private ConcurrentHashMap<String, AtomicInteger> count = new ConcurrentHashMap<String, AtomicInteger>();
	
	public NioTestServer(RpcNioSelection selection){
		this.selection = selection;
	}
	//server端开启服务
	public void startService(){
		if(!started.get()){
			acceptor = new RpcNioAcceptor(selection);
			acceptor.setHost(host);
			acceptor.setPort(port);
			acceptor.addRpcCallListener(this);
			acceptor.startService();
			started.set(true);
		}
	}
	
	public void printResult(){
		String hostname = host+":"+port;
		logger.info(hostname+" receive count all:"+receive.get());
		Enumeration<String> keys = count.keys();
		int i=1;
		while(keys.hasMoreElements()){
			String sender = keys.nextElement();
			AtomicInteger c = count.get(sender);
			logger.info("host:"+hostname+" client "+sender+" count:"+c.get());
			i++;
		}
	}
	
	public static void main(String[] args) throws InterruptedException {
		//
		RpcNioSelection selection = new RpcNioSelection();
		String ip = "localhost";
		int port = 8000;
		int count = 5; //开启的server个数
		//5个NioTestServer对象
		List<NioTestServer> servers = createServers(selection,count,ip,port);
		startServer(servers);
		Thread.currentThread().sleep(80000);
		printResult(servers);
	}
	/**
	 * 构建NioTestServer对象
	 * @param selection
	 * @param c server个数
	 * @param ip 
	 * @param basePort 
	 * @return
	 */
	public static List<NioTestServer> createServers(RpcNioSelection selection,int c,String ip,int basePort){
		if(selection==null){
			selection = new RpcNioSelection();
		}
		List<NioTestServer> servers = new LinkedList<NioTestServer>();
		int i = 0;
		while(i<c){
			NioTestServer server = new NioTestServer(selection);
			server.host = ip;
			server.port = basePort+i;
			i++;
			servers.add(server);
		}
		return servers;
	}
	/**
	 * 启动多个NioTestServer
	 * @param servers
	 */
	public static void startServer(List<NioTestServer> servers){
		for(NioTestServer server:servers){
			server.startService();
		}
	}
	
	public static void printResult(List<NioTestServer> servers){
		for(NioTestServer server:servers){
			server.printResult();
		}
	}
	
	public void stopService(){
		acceptor.stopService();
	}
	
	@Override
	public void onRpcMessage(RpcObject rpc, RpcSender sender) {
		logger.info(rpc.toString()+"****");
		sender.sendRpcObject(rpc, 1000);
		RpcNioConnector connector = (RpcNioConnector)sender;
		String clientKey = connector.getRemoteHost()+":"+connector.getRemotePort();
		AtomicInteger c = count.get(clientKey);
		if(c==null){
			c = new AtomicInteger(1);
			count.put(clientKey, c);
		}else{
			c.incrementAndGet();
		}
		receive.incrementAndGet();
	}
}
