package fr.uge.ex3;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class FixedPrestartedLongSumServer {

    private static final Logger logger = Logger.getLogger(FixedPrestartedLongSumServer.class.getName());
    private final ServerSocketChannel serverSocketChannel;

    public FixedPrestartedLongSumServer(int port) throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        logger.info(this.getClass().getName() + " starts on port " + port);
    }

    /**
     * Iterative server main loop
     *
     * @throws IOException
     */

    public void worker(int val) throws IOException {
        logger.info("Server started");
    	
        for(var i = 0; i < val; i++) {
        	Thread.ofPlatform().start(()->{
        		while (!Thread.interrupted()) {
					try {
						SocketChannel client = serverSocketChannel.accept();
				    	try {
				            logger.info("Connection accepted from " + client.getRemoteAddress());
				            serve(client);
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
    private void serve(SocketChannel sc) throws IOException {
    	while(true) {
    		long sum=0;
	    	var buffer = ByteBuffer.allocate(Integer.BYTES);
	    	if(!readFully(sc, buffer)) {
	    			logger.info("Not readfull");
	    		return;
	    	}
	    	buffer.flip();
	    	var oct = buffer.getInt();
	    	buffer = ByteBuffer.allocate(oct*Long.BYTES);
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
        var server = new FixedPrestartedLongSumServer(Integer.parseInt(args[0]));
		server.worker(Integer.parseInt(args[1]));

    }
}