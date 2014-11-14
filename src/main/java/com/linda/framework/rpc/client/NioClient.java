package com.linda.framework.rpc.client;

import java.io.UnsupportedEncodingException;
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
import com.linda.framework.rpc.nio.RpcNioConnector;
import com.linda.framework.rpc.nio.RpcNioSelection;
import com.linda.framework.rpc.utils.RpcUtils.RpcType;

/**
 * @author chenbaoyu 2014-11-13下午4:16:40
 */
public class NioClient implements RpcCallListener {

	public static Logger logger = Logger.getLogger(NioClient.class);
	private RpcNioSelection selection;
	private String host = "127.0.0.1";
	private int port = 4332;
	private int threadCount;
	private AtomicInteger send = new AtomicInteger(0);
	private AtomicInteger receive = new AtomicInteger(0);
	private AtomicBoolean started = new AtomicBoolean(false);

	public NioClient(RpcNioSelection selection) {
		this.selection = selection;
	}

	public NioClient clone() {
		NioClient client = new NioClient(selection);
		client.host = host;
		client.port = port;
		client.threadCount = threadCount;
		return client;
	}

	public static void stopClients(List<NioClient> clients) {
		for (NioClient client : clients) {
			client.stopService();
		}
	}

	public static void printResult(List<NioClient> clients) {
		for (NioClient client : clients) {
			client.printResult();
		}
	}

	public static List<NioClient> createClients(RpcNioSelection selection,
			String ip, int port, int clientcount, int connectors,
			int threadCount) {
		List<NioClient> list = new LinkedList<NioClient>();
		int i = 0;
		while (i < clientcount) {
			NioClient client = new NioClient(selection);
			client.host = ip;
			client.port = port + i;
			client.threadCount = threadCount;
			list.add(client);
			// 克隆两个客户端
			int con = 0;
			while (con < connectors) {
				list.add(client.clone());
				con++;
			}
			i++;
		}
		return list;
	}


	public Thread startThread(AbstractRpcConnector connector) {
		Random random = new Random();
		int index = random.nextInt(20000);
		String dataInfo = "I have a dream 我有一个梦";
		SendThread thread = new SendThread(connector, dataInfo.getBytes(), index);
		thread.start();
		return thread;
	}

	/**
	 * 创建RPC对象
	 * 
	 * @param str
	 * @param id
	 * @param index
	 * @return
	 */
	public static RpcObject createRpc(byte[] obj, long id, int index) {
		RpcObject rpc = new RpcObject();
		rpc.setType(RpcType.INVOKE);
		rpc.setIndex(index);
		rpc.setThreadId(id);
		rpc.setData(obj);
		rpc.setLength(rpc.getData().length);
		return rpc;
	}

	/**
	 * 发送消息线程
	 * 
	 * @author chenbaoyu 2014-11-13下午4:17:06
	 */
	public class SendThread extends Thread {

		private AbstractRpcConnector connector;
		private  byte[] obj;
		private int index;

		public SendThread(AbstractRpcConnector connector, byte[] obj,int index) {
			this.connector = connector;
			this.obj = obj;
			this.index = index;
		}

		@Override
		public void run() {
			long threadId = Thread.currentThread().getId();
			RpcObject rpcObj = createRpc(obj, threadId, index);
			connector.sendRpcObject(rpcObj, 10000);
			NioClient.this.send.incrementAndGet();
		}
	}

	public void printResult() {
		logger.info(this.host + ":" + this.port + "  send:" + send.get()
				+ " receive:" + receive.get());
	}

	public RpcNioConnector getConnector(String host, int port) {

		RpcNioConnector connector = new RpcNioConnector(selection);
		connector.setHost(host);
		connector.setPort(port);
		connector.addRpcCallListener(this);
		return connector;

	}

	public void stopService() {
		
		// connector.stopService();
	}

	@Override
	public void onRpcMessage(RpcObject rpc, RpcSender sender) {
		try {
			String recString=new String(rpc.getData() ,"UTF-8");
			System.out.println(recString);
		} catch (UnsupportedEncodingException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		receive.incrementAndGet();
		printResult();
	}

}
