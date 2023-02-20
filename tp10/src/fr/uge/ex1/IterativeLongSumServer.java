package fr.uge.ex1;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;

public class IterativeLongSumServer {

    private static final Logger logger = Logger.getLogger(IterativeLongSumServer.class.getName());
    private final ServerSocketChannel serverSocketChannel;
    private final ByteBuffer intBuffer = ByteBuffer.allocate(Integer.BYTES);

    public IterativeLongSumServer(int port) throws IOException {
        serverSocketChannel = ServerSocketChannel.open();
        serverSocketChannel.bind(new InetSocketAddress(port));
        logger.info(this.getClass().getName() + " starts on port " + port);
    }

    /**
     * Iterative server main loop
     *
     * @throws IOException
     */

    public void launch() throws IOException {
        logger.info("Server started");
        while (!Thread.interrupted()) {
            SocketChannel client = serverSocketChannel.accept();
            	try {
                    logger.info("Connection accepted from " + client.getRemoteAddress());
                    serve(client);
                } catch (IOException ioe) {
                    logger.log(Level.SEVERE, "Connection terminated with client by IOException", ioe.getCause());
                } finally {
                    silentlyClose(client);
                }            
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
        var server = new IterativeLongSumServer(Integer.parseInt(args[0]));
        server.launch();
    }
}