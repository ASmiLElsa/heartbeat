package client;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.net.UnknownHostException;
import java.util.concurrent.ConcurrentHashMap;

import vo.KeepAlive;

public class Client {

	public static interface ObjectAction {
		void doAction(Object obj, Client client);
	}
	
	public static final class DefaultObjectAction implements ObjectAction {

		@Override
		public void doAction(Object obj, Client client) {
			System.out.println("处理：\t" + obj.toString());
		}		
	}
	
	public static void main(String[] args) throws UnknownHostException, IOException {
		String serverIP = "192.241.223.236";
		int port = 60000;
		Client client = new Client(serverIP,port);
		client.start();
	}
	
	private String serverIP;
	private int port;
	private Socket socket;
	private boolean running = false;//连接状态
	
	private long lastSendTime;//最后一次发送数据的时间
	
	private ConcurrentHashMap<Class, ObjectAction> actionMapping = new ConcurrentHashMap<>();
	
	public Client(String serverIP, int port) {
		this.serverIP = serverIP;
		this.port = port;
	}
	
	public void start() throws UnknownHostException, IOException {
		if(running) return;
		socket = new Socket(serverIP, port);
		System.out.println("本地端口："+socket.getLocalPort());
		lastSendTime = System.currentTimeMillis();
		running = true;
		new Thread(new KeepAliveWatchDog()).start();//保持长连接线程，每隔2s向服务器发一个一个保持连接的心跳消息
		new Thread(new ReceiveWatchDog()).start();//接收消息的线程，处理消息
	}
	
	public void stop() {
		if(running) running = false;
	}
	
	public void addActionMap(Class<Object> cls, ObjectAction action) {
		actionMapping.put(cls, action);
	}
	
	public void sendObject(Object obj) throws IOException {
		ObjectOutputStream oos = new ObjectOutputStream(socket.getOutputStream());
		oos.writeObject(obj);
		System.out.println("发送：\t" + obj);
		oos.flush();
	}
	
	class KeepAliveWatchDog implements Runnable {
		long checkDelay = 10;
		long keepAliveDelay = 2000;
		public void run() {
			while(running) {
				if(System.currentTimeMillis()-lastSendTime>keepAliveDelay) {
					try {
						Client.this.sendObject(new KeepAlive());
					} catch (IOException e) {
						e.printStackTrace();
						Client.this.stop();
					}
					lastSendTime = System.currentTimeMillis();
				}
				else {
					try {
						Thread.sleep(checkDelay);
					} catch (InterruptedException e) {
						e.printStackTrace();
						Client.this.stop();
					}
				}
			}
		}
	}
	
	class ReceiveWatchDog implements Runnable {
		public void run() {
			while(running) {
				try {
					InputStream in = socket.getInputStream();
					if(in.available()>0) {
						ObjectInputStream ois =new ObjectInputStream(in);
						Object obj = ois.readObject();
						System.out.println("接收：\t"+obj);
						ObjectAction oa = actionMapping.get(obj.getClass());
						oa = oa==null ? new DefaultObjectAction(): oa;
						oa.doAction(obj, Client.this);
					}
					else {
						Thread.sleep(10);
					}
				} catch (Exception e) {
					e.printStackTrace();
					Client.this.stop();
				}
			}
		}
	}
}
