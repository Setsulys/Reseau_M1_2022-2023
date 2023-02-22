package fr.uge.ex4;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;


public class FixedPrestartedConcurrentLongSumServerWithTimeout{

    private static final Logger logger = Logger.getLogger(FixedPrestartedConcurrentLongSumServerWithTimeout.class.getName());
    private final ServerSocketChannel serverSocketChannel;
    private final long timeout;	
    private final int MAX_CLIENT =4;
    private static final ArrayList<ThreadData> td = new ArrayList<>();

    public FixedPrestartedConcurrentLongSumServerWithTimeout(int port,int time) throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        logger.info(this.getClass().getName() + " starts on port " + port);
        timeout = time;
    }

    /**
     * Iterative server main loop
     *
     * @throws IOException
     */

    public void worker() throws IOException {
        logger.info("Server started");
    	
        for(var i = 0; i < MAX_CLIENT; i++) {
        	Thread.ofPlatform().start(()->{
        		var data = new ThreadData();
            	td.add(data);
        		while (!Thread.interrupted()) {
					try {
						SocketChannel client = serverSocketChannel.accept();
						data.setSocketChannel(client);
				    	try {
				            logger.info("Connection accepted from " + client.getRemoteAddress());
				            serve(client,data);
				            
				        }catch(AsynchronousCloseException e) {
				        	logger.info("Connection terminated, afk client");
				        } catch (IOException ioe) {
				            logger.log(Level.SEVERE, "Connection terminated with client by IOException", ioe.getCause());
				        } finally {
				            silentlyClose(client);
				        }
					}catch(IOException e) {
						logger.log(Level.SEVERE,"Cannot continue server",e.getCause());
						return;
					}
				}
        	});
        }
    }

    /**
     * Treat the connection sc applying the protocol. All IOException are thrown
     *
     * @param sc
     * @throws IOException
     */
    private void serve(SocketChannel sc,ThreadData data) throws IOException {
    	while(true) {
    		long sum=0;
	    	var buffer = ByteBuffer.allocate(Integer.BYTES);
	    	data.tick();
	    	if(!readFully(sc, buffer)) {
	    			logger.info("Not readfull");
	    		return;
	    	}
	    	buffer.flip();
	    	var oct = buffer.getInt();
	    	buffer = ByteBuffer.allocate(oct*Long.BYTES);
	    	data.tick();
	    	if(!readFully(sc, buffer)) {
	    		logger.info("Not readFull2");
	    		return;
	    	}
	    	buffer.flip();
	    	for(var i = 0; i < oct; i++) {
	    		if(buffer.remaining() < Long.BYTES) {
	    			logger.info("Not a Long");
	    			return;
	    		}
	    		sum+= buffer.getLong();
	    	}
	    	buffer.clear();
	    	sc.write(buffer.putLong(sum).flip());
    	}
    }

    /**
     * Close a SocketChannel while ignoring IOExecption
     *
     * @param sc
     */

    private void silentlyClose(Closeable sc) {
        if (sc != null) {
            try {
                sc.close();
            } catch (IOException e) {
                // Do nothing
            }
        }
    }

    static boolean readFully(SocketChannel sc, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            if (sc.read(buffer) == -1) {
                logger.info("Input stream closed");
                return false;
            }
        }
        return true;
    }

    public static void main(String[] args) throws NumberFormatException, IOException {
        var server = new FixedPrestartedConcurrentLongSumServerWithTimeout(Integer.parseInt(args[0]),Integer.parseInt(args[1]));
		server.worker();

		Thread.ofPlatform().start(()->{
			while(!Thread.interrupted()) {
				try {
					for(var elements : td) {
						elements.closeIfInactive(server.timeout);
						Thread.sleep(server.timeout/2);
					}
				}catch(InterruptedException e) {
					logger.info("Manager Interrupted");
					return;
				}
			}
		});
    }
}