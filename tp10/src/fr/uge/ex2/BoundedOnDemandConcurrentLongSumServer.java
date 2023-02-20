package fr.uge.ex2;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Semaphore;
import java.util.logging.Level;
import java.util.logging.Logger;

public class BoundedOnDemandConcurrentLongSumServer {
	    private static final Logger logger = Logger.getLogger(BoundedOnDemandConcurrentLongSumServer.class.getName());
	    private final ServerSocketChannel serverSocketChannel;
	    private final ByteBuffer intBuffer = ByteBuffer.allocate(Integer.BYTES);
	    private final int nbPermitsMax;

    public BoundedOnDemandConcurrentLongSumServer(int port,int nbPermits) throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        logger.info(this.getClass().getName() + " starts on port " + port);
        nbPermitsMax = nbPermits;
    }

    /**
     * Iterative server main loop
     *
     * @throws IOException
     * @throws InterruptedException 
     */

    public void launch() throws IOException {
        logger.info("Server started");
        while (!Thread.interrupted()) {
        	Semaphore semaphore =  new Semaphore(nbPermitsMax);
        	
            SocketChannel client = serverSocketChannel.accept();
            Thread.ofPlatform().start(()-> {
	            try {
	            	semaphore.acquire();
	                logger.info("Connection accepted from " + client.getRemoteAddress());
	                serve(client);
	            } catch(InterruptedException e) {
	            	logger.info("Interrupted");
	            }catch (IOException ioe) {
	                logger.log(Level.SEVERE, "Connection terminated with client by IOException", ioe.getCause());
	            } finally {
	                silentlyClose(client);
	                semaphore.release();
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
    private void serve(SocketChannel sc) throws IOException {
    	boolean finished = false;
    	while(true) {
    		long sum=0;
	    	intBuffer.clear();
	    	if(!readFully(sc, intBuffer)) {
	    		if(finished == true) {
	    			logger.info("Finished");
	    		}
	    		else {
	    			logger.info("Not readfull");
	    		}
	    		return;
	    	}
	    	intBuffer.flip();
	    	var oct = intBuffer.getInt();
	    	var opBuffer = ByteBuffer.allocate(oct*Long.BYTES);
	    	if(!readFully(sc, opBuffer)) {
	    		logger.info("Not readFull2");
	    		return;
	    	}
	    	opBuffer.flip();
	    	for(var i = 0; i < oct; i++) {
	    		if(opBuffer.remaining() < Long.BYTES) {
	    			logger.info("Not a Long");
	    			return;
	    		}
	    		sum+= opBuffer.getLong();
	    	}
	    	opBuffer.clear();
	    	sc.write(opBuffer.putLong(sum).flip());
	    	finished=true;
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
        var server = new BoundedOnDemandConcurrentLongSumServer(Integer.parseInt(args[0]),Integer.parseInt(args[1]));
        server.launch();
    }
	
}
