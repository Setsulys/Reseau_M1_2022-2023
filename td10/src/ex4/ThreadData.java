package ex4;

import java.io.IOException;
import java.nio.channels.SocketChannel;

public class ThreadData {

	private SocketChannel client;
	private int time;
	private static final int TICK = 5;
	private final Object lock = new Object();
	
	void setSocketChannel(SocketChannel client) {
		synchronized (lock) {
			this.client = client;
			this.time = 0;
		}
	}
	
	void tick() {
		synchronized (lock) {
			time = 0;
		}
	}
	
	void closeIfInactive(int timeout) {
		synchronized (lock) {
			if(client ==null) {
				return;
			}
			try {
				if(time > timeout/TICK) {
					close();
				}
				else {
					time++;
				}
			}catch (Exception e) {
				// TODO: handle exception
			}
		}
	}
	
	boolean isConnected() {
		synchronized (lock) {
			return client != null;
		}
		
	}
	void close() throws IOException {
		synchronized (lock) {
			client.close();	
		}
	}
}
