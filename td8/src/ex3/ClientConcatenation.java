package ex3;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.logging.Logger;
import java.util.ArrayList;
import java.util.Scanner;


public class ClientConcatenation {

	public static final Logger logger = Logger.getLogger(ClientConcatenation.class.getName());
	private static Charset UTF8 = StandardCharsets.UTF_8;
	
	
    static boolean readFully(SocketChannel sc, ByteBuffer buffer) throws IOException {
        while (buffer.hasRemaining()) {
            if (sc.read(buffer) == -1) {
                logger.info("Connection closed for reading");
                return false;
            }
        }
        buffer.flip();
        return true;
    }
    
    
    private static String requestConcatString(SocketChannel sc, ArrayList<String> lst) throws IOException{
    	ByteBuffer buffer = ByteBuffer.allocate(1024);
    	buffer.putInt(lst.size());
    	for(var e:lst) {
    		var toBuffer = UTF8.encode(e);
    		buffer.putInt(toBuffer.remaining());
    		buffer.put(toBuffer);
    	}
    	sc.write(buffer.flip());
    	
    	buffer = ByteBuffer.allocate(Integer.BYTES);
    	if(!readFully(sc, buffer)) {
    		logger.info("Integer Not Readfull");
    		return null;
    	}
    	var size = buffer.getInt();
    	if(size < Integer.BYTES) {
    		logger.info("Not An Integer");
    		return null;
    	}
    	
    	buffer = buffer.allocate(size);
    	if(!readFully(sc, buffer)){
    		logger.info("Not getting all concatenated string");
    		return null;
    	}
		return UTF8.decode(buffer).toString();
    	
    }
    
    public static void main(String[] args) throws IOException{
    	ArrayList<String> lst = new ArrayList<>();
		var server = new InetSocketAddress(args[0],Integer.parseInt(args[1]));
		try(var sc = SocketChannel.open(server)){
			var scan = new Scanner(System.in);
			String line;
			while(true) {
				do {
					line = scan.nextLine();
					if(line.isEmpty()) {
						break;
					}
					lst.add(line);
				}while(true);
				var msg = requestConcatString(sc, lst);
				if(msg == null) {
					logger.info("Connexion lost with server");
					return;
				}
				System.out.println(msg);
				lst.clear();
			}
		}
	}
}
