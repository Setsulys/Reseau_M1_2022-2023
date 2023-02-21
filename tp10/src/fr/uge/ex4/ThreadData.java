package fr.uge.ex4;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class ThreadData {

	 private static final Logger logger = Logger.getLogger(ThreadData.class.getName());
		private final ReentrantLock lock = new ReentrantLock();
		private long times;
		public final static int TICK = 5;
		private SocketChannel sc;
		
		
		public void setSocketChannel(SocketChannel client) {
			lock.lock();
			try {
				this.sc = client;
				this.times = 0 ;
			}finally {
				lock.unlock();
			}
		}
		
		public void tick() {
			lock.lock();
			try {
				this.times = 0;
			}finally {
				lock.unlock();
			}
		}
		
		public void closeIfInactive(long timeout) {
			lock.lock();
			try {
				if(sc == null) {
					return;
				}
				if(times > timeout/TICK) {
					close();
				}else {
					times++;
				}
			}finally {
				lock.unlock();
			}
		}
		
		public void close() {
			try {
				sc.close();
			} catch (IOException e) {
				logger.info("IOException");;
			}
		}
		
		
}
