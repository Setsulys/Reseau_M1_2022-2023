package fr.uge.ex1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.util.logging.Logger;

public class ClientEOS {

    public static final Charset UTF8_CHARSET = Charset.forName("UTF-8");
    public static final int BUFFER_SIZE = 1024;
    public static final Logger logger = Logger.getLogger(ClientEOS.class.getName());

    /**
     * This method: 
     * - connect to server 
     * - writes the bytes corresponding to request in UTF8 
     * - closes the write-channel to the server 
     * - stores the bufferSize first bytes of server response 
     * - return the corresponding string in UTF8
     *
     * @param request
     * @param server
     * @param bufferSize
     * @return the UTF8 string corresponding to bufferSize first bytes of server
     *         response
     * @throws IOException
     */

    public static String getFixedSizeResponse(String request, SocketAddress server, int bufferSize) throws IOException {
        // TODO 	
    	SocketChannel sc = SocketChannel.open();
    	var buffer = ByteBuffer.allocate(bufferSize);
    	int read = 0;
    	
    	sc.connect(server);
    	sc.write(UTF8_CHARSET.encode(request));
    	sc.shutdownOutput();
    	while(buffer.remaining() != 0) {
    		read = sc.read(buffer);
    	}
    	buffer.flip();
    	if (read == -1){
    	    System.out.println("Connection closed for reading");
    	    return null;
    	  } else {
    	    System.out.println("Read "+ read +" bytes");
    	  }
    	sc.close();
        return UTF8_CHARSET.decode(buffer).toString();
    }

    /**
     * This method: 
     * - connect to server 
     * - writes the bytes corresponding to request in UTF8 
     * - closes the write-channel to the server 
     * - reads and stores all bytes from server until read-channel is closed 
     * - return the corresponding string in UTF8
     *
     * @param request
     * @param server
     * @return the UTF8 string corresponding the full response of the server
     * @throws IOException
     */

    public static String getUnboundedResponse(String request, SocketAddress server) throws IOException {
        // TODO
    	SocketChannel sc = SocketChannel.open();
    	var buffer = ByteBuffer.allocate(BUFFER_SIZE);
    	//int read=0;
    	
    	sc.connect(server);
    	sc.write(UTF8_CHARSET.encode(request));
    	sc.shutdownOutput();
//    	while(read >=0) {
//    		if(!buffer.hasRemaining()) {
//    			buffer.flip();
//    			buffer = store(buffer);
//    		}
//    		read = sc.read(buffer);	
//    	}
    	while(readFully(sc, buffer)) {
    		if(!buffer.hasRemaining()) {
    			buffer.flip();
    			buffer = store(buffer);
    		}
    	}
    	buffer.flip();
    	sc.close();
        return UTF8_CHARSET.decode(buffer).toString();
    }
    
    public static ByteBuffer store(ByteBuffer stored) {
    	var tempBuffer = ByteBuffer.allocate(stored.remaining()*2);
    	return tempBuffer.put(stored);
    }

    /**
     * Fill the workspace of the Bytebuffer with bytes read from sc.
     *
     * @param sc
     * @param buffer
     * @return false if read returned -1 at some point and true otherwise
     * @throws IOException
     */
    static boolean readFully(SocketChannel sc, ByteBuffer buffer) throws IOException {
        // TODO
    	int read = 0;
    	while(buffer.hasRemaining() && read !=-1) {
    		read = sc.read(buffer);
    	}
        return read!=-1;
    }

    public static void main(String[] args) throws IOException {
        var google = new InetSocketAddress("www.google.fr", 80);
        //System.out.println(getFixedSizeResponse("GET / HTTP/1.1\r\nHost: www.google.fr\r\n\r\n", google, 512));
         System.out.println(getUnboundedResponse("GET / HTTP/1.1\r\nHost: www.google.fr\r\n\r\n", google));
    }
}
