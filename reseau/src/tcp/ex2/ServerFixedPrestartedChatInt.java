package tcp.ex2;

import java.io.Closeable;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.logging.Level;
import java.util.logging.Logger;

public class ServerFixedPrestartedChatInt {
    private static final Logger logger = Logger.getLogger(ServerFixedPrestartedChatInt.class.getName());
    private final ServerSocketChannel serverSocketChannel;
    private final int nbClients;
    private final ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
    private final HashSet<SocketChannel> clientChannel;
    private boolean close =false;
    
    public ServerFixedPrestartedChatInt(int port, int nbClients) throws IOException {
        this.serverSocketChannel = ServerSocketChannel.open();
        this.nbClients = nbClients;
        serverSocketChannel.bind(new InetSocketAddress(port));
        logger.info(this.getClass().getName()
                + " starts on port " + port);
        this.clientChannel  = new HashSet<SocketChannel>(nbClients);
    }

    public void serve(SocketChannel client) throws IOException{
		if(client.read(buffer)==-1) {
			close =true;
			return ;
		}
		buffer.flip();
		for(var channel : clientChannel) {
			channel.write(buffer);
			buffer.flip();
		}
    }
    
    public void launch(){
        // TODO
    	try {
    	SocketChannel client = serverSocketChannel.accept();
		clientChannel.add(client);
    	while(!Thread.interrupted()) {
    		try {
    	        logger.info("Connection accepted from " + client.getRemoteAddress());
    	        serve(client);
    	    } catch (IOException ioe) {
    	        logger.log(Level.INFO,"Connection terminated by IOException ", ioe.getCause());
    	        break;
    	    }finally {
    	        silentlyClose(client);
    	    }
    	}
    	}catch (IOException e) {
			System.exit(0);
		}
    }
    
    public void silentlyClose(Closeable sc) {
    	if(sc != null && close) {
    		try {
    			sc.close();
    		}catch(IOException e) {
    			//Do nothing
    		}
    	}
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("usage: java ServerFixedPrestartedChatInt port nbClients");
            return;
        }
        var server = new ServerFixedPrestartedChatInt(Integer.parseInt(args[0]), Integer.parseInt(args[1]));
        server.launch();
    }
}