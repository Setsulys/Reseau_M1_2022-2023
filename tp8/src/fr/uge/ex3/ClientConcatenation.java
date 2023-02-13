package fr.uge.ex3;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Scanner;
import java.util.logging.Logger;

public class ClientConcatenation {

	
	private static final ArrayList<String> lines = new ArrayList<>();
	private static final Logger logger = Logger.getLogger(ClientConcatenation.class.getName());
	private static final Charset UTF8 = StandardCharsets.UTF_8;
	private static final int BUFFER_SIZE =1024;
	private static final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
	
	
    static boolean readFully(SocketChannel sc, ByteBuffer buffer) throws IOException {
    	int read = 0;
    	while(buffer.hasRemaining() && read !=-1) {
    		read = sc.read(buffer);
    	}
        return read!=-1;
    }
	
	private static String message(SocketChannel sc) throws IOException {
		buffer.clear();
		buffer.putInt(lines.size());
		for(var e:lines) {
			buffer.putInt(UTF8.encode(e).remaining());
			buffer.put(UTF8.encode(e));
		}
		sc.write(buffer.flip());
		sc.shutdownOutput();
		buffer.clear();
		
		if(!readFully(sc,buffer)) {
			return null;
		}
		buffer.flip();
		var size = buffer.getInt();
		if(size < Integer.BYTES) {
			logger.info("Malformed packet");
			return null;
		}
		buffer.clear();
		readFully(sc,buffer);
		buffer.flip();
		return UTF8.decode(buffer).toString();
	}
	
	public static void main(String[] args) throws IOException {
		var server = new InetSocketAddress(args[0],Integer.parseInt(args[1]));
		try(var sc = SocketChannel.open(server)){
			var scan  = new Scanner(System.in);
			String line;
			while(true) {
				do {
					line  = scan.nextLine();
					if (!line.isEmpty()) {
						lines.add(line);
					}
				}while(!line.isEmpty());
				
				var msg = message(sc);
				System.out.println(msg);
				if(msg == null) {
					logger.warning("Connection with server lost");
					return;
				}
				logger.info("Everything seems ok");
			}
		}
	}
}
