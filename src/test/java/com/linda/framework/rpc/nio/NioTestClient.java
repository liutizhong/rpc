package com.linda.framework.rpc.nio;

import java.util.LinkedList;
import java.util.List;
import java.util.Random;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.log4j.Logger;

import com.linda.framework.rpc.RpcObject;
import com.linda.framework.rpc.net.AbstractRpcConnector;
import com.linda.framework.rpc.net.RpcCallListener;
import com.linda.framework.rpc.net.RpcSender;
import com.linda.framework.rpc.utils.RpcUtils.RpcType;

/**
 * @author chenbaoyu
 * 2014-11-13下午4:16:40
 */
public class NioTestClient implements RpcCallListener{
	
	public static Logger logger = Logger.getLogger(NioTestClient.class);
	private RpcNioSelection selection;
	private RpcNioConnector connector;
	private String host = "127.0.0.1";
	private int port = 4332;
	private int threadCount;
	private AtomicInteger send = new AtomicInteger(0);
	private AtomicInteger receive = new AtomicInteger(0);
	private List<Thread> threads;
	private AtomicBoolean started = new AtomicBoolean(false);
	
	public NioTestClient(RpcNioSelection selection){
		this.selection = selection;
	}
	
	public static void main(String[] args) throws InterruptedException {
		RpcNioSelection selector = new RpcNioSelection();
		String ip = "localhost";
		int basePort = 8000;
		int clientCount = 5; //模拟客户端的个数(并发请求)
		int connectors = 2; 
		int threadCount = 2; //开启线程个数
		List<NioTestClient> clients = createClients(selector,ip,basePort,clientCount,connectors,threadCount);
		//实际返回15个客户端
		startClients(clients);
		Thread.currentThread().sleep(1000);
		stopClients(clients);
		Thread.currentThread().sleep(1000);
		printResult(clients);
	}
	
	public NioTestClient clone(){
		NioTestClient client = new NioTestClient(selection);
		client.host = host;
		client.port = port;
		client.threadCount = threadCount;
		return client;
	}
	//开 启 多个客户端
	public static void startClients(List<NioTestClient> clients){
		int i = 0;
		for(NioTestClient client:clients){
			client.startService();
			i++;
		}
	}
	
	public static void stopClients(List<NioTestClient> clients){
		for(NioTestClient client:clients){
			client.stopService();
		}
	}
	
	public static void printResult(List<NioTestClient> clients){
		for(NioTestClient client:clients){
			client.printResult();
		}
	}
	
	public static List<NioTestClient> createClients(RpcNioSelection selection,String ip,int port,int clientcount,int connectors,int threadCount){
		List<NioTestClient> list = new LinkedList<NioTestClient>();
		int i=0;
		while(i<clientcount){
			NioTestClient client = new NioTestClient(selection);
			client.host = ip;
			client.port = port+i;
			client.threadCount = threadCount;
			list.add(client);
			//克隆两个客户端
			int con = 0;
			while(con<connectors){
				list.add(client.clone());
				con++;
			}
			i++;
		}
		return list;
	}
	
	private List<Thread> startThread(AbstractRpcConnector connector,int count){
		LinkedList<Thread> list = new LinkedList<Thread>();
		int c = 0;
		Random random = new Random();
		while(c<count){
			int interval = random.nextInt(200);
			int index = random.nextInt(20000);
			SendThread thread = new SendThread(connector,interval,index);
			list.add(thread);
			thread.start();
			c++;
		}
		return list;
	}

	
	/**
	 * 创建RPC对象
	 * @param str
	 * @param id
	 * @param index
	 * @return
	 */
	public static RpcObject createRpc(String str,long id,int index){
		RpcObject rpc = new RpcObject();
		rpc.setType(RpcType.INVOKE);
		rpc.setIndex(index);
		rpc.setThreadId(id);
		rpc.setData(str.getBytes());
		rpc.setLength(rpc.getData().length);
		return rpc;
	}
	/**
	 * 发送消息线程
	 * @author chenbaoyu
	 * 2014-11-13下午4:17:06
	 */
	public class SendThread extends Thread{

		private AbstractRpcConnector connector;
		private int interval;
		private int index;
		
		public SendThread(AbstractRpcConnector connector,int interval,int startIndex){
			this.connector = connector;
			this.interval = interval;
			this.index = startIndex;
		}
		
		@Override
		public void run() {
			String prefix = "I am the one! ";
			long threadId = Thread.currentThread().getId();
			logger.info("send thread:"+threadId+" start "+host+":"+port);
			while(true){
				RpcObject rpc = createRpc(prefix+index,threadId,index);
				connector.sendRpcObject(rpc, 10000);
				NioTestClient.this.send.incrementAndGet();
				index++;
				try {
					Thread.currentThread().sleep(interval);
				} catch (InterruptedException e) {
					break;
				}
			}
		}
	}

	public void printResult(){
		logger.info(this.host+":"+this.port+"  send:"+send.get()+" receive:"+receive.get());
	}
	
	public void startService() {
		if(!started.get()){
			started.set(true);
			connector = new RpcNioConnector(selection);
			connector.setHost(host);
			connector.setPort(port);
			connector.addRpcCallListener(this);
			connector.startService();
			threads = startThread(connector,threadCount);
		}

	}

	public void stopService() {
		for (Thread thread : threads) {
			thread.interrupt();
		}
		//connector.stopService();
	}
	
	@Override
	public void onRpcMessage(RpcObject rpc, RpcSender sender) {
		receive.incrementAndGet();
	}

	
}
