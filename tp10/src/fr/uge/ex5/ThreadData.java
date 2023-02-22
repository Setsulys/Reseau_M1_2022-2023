package fr.uge.ex5;

import java.io.IOException;
import java.nio.channels.SocketChannel;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;


public class ThreadData {

	 private static final Logger logger = Logger.getLogger(ThreadData.class.getName());
		private final ReentrantLock lock = new ReentrantLock();
		private long times;
		private SocketChannel sc;
		
		
		public void setSocketChannel(SocketChannel client) {
			lock.lock();
			try {
				this.sc = client;
				this.times = System.currentTimeMillis() ;
			}finally {
				lock.unlock();
			}
		}
		
		public void tick() {
			lock.lock();
			try {
				this.times = System.currentTimeMillis();
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
				if(times + timeout > System.currentTimeMillis()) {
					close();
				}
			}finally {
				lock.unlock();
			}
		}
		
		public void close() {
			try {
				if(sc!= null) {
					sc.close();
				}
			
			} catch (IOException e) {
				logger.info("IOException");;
			}
		}
		
		public boolean isConnected() {
			lock.lock();
			try {
				return sc != null;
			}finally {
				lock.unlock();
			}
		}
		
		
}
