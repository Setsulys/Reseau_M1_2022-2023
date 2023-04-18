package ex2;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.concurrent.Semaphore;
import java.util.logging.Logger;

public class BoundedOnDemandConcurrentLongSumServer {
	
	private static final Logger logger = Logger.getLogger(OnDemandConcurrentLongSumServer.class.getName());
	private final ServerSocketChannel ssc;
	private int maxClient;
	
	
    public BoundedOnDemandConcurrentLongSumServer(int intPort,int maxClients) throws IOException {
		// TODO Auto-generated constructor stub
    	ssc = ServerSocketChannel.open();
    	ssc.bind(new InetSocketAddress(intPort));
    	maxClient = maxClients;
    	logger.info(this.getClass().getName() + " starts on port " + intPort);
	}

    
    
    public void launch()throws IOException, InterruptedException {
    	logger.info("server started");
    	Semaphore semaphore = new Semaphore(maxClient);
    	while(!Thread.interrupted()) {
    		semaphore.acquire();
    		SocketChannel client = ssc.accept();
    		Thread.ofPlatform().start(()->{
	    		try {
	    			logger.info("Connection accepted from " + client.getRemoteAddress());
	    			serve(client);
	    		}catch(IOException e) {
	    			logger.info("Connection terminated with IOException" + e.getCause());
	    		}finally {
	    			silentlyClose(client);
	    			semaphore.release();
	    		}
    		});
    	}
    }
    
    private void serve(SocketChannel sc) throws IOException{
    	while(true) {
	    	Long result =0L;
	    	ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
	    	if(!readFully(sc, buffer)) {
	    		logger.info("Integer Not readfull or Connexion closedt");
	    		return;
	    	}
	    	buffer.flip();
	    	var nbToSum = buffer.getInt();
	    	if(nbToSum < Integer.BYTES) {
	    		logger.info("Not an Integer");
	    	}
	    	buffer = ByteBuffer.allocate(Long.BYTES * nbToSum);
	    	if(!readFully(sc, buffer)) {
	    		logger.info("Long to sum not ReadFull");
	    		return;
	    	}
	    	buffer.flip();
	    	for(var i = 0; i < nbToSum; i++) {
	    		result+= buffer.getLong();
	    	}
	    	buffer = ByteBuffer.allocate(Long.BYTES);
	    	buffer.putLong(result);
	    	sc.write(buffer.flip());
    	}
    }
    
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
        try {
			server.launch();
		} catch (IOException ioe) {
			logger.info("Server" + Thread.currentThread().getName() + " problem\n " +ioe);
		} catch (InterruptedException e) {
			logger.info("InterruptedServer");
		}
    }
}