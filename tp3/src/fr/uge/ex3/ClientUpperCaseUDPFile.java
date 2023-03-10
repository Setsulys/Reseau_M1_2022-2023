package fr.uge.ex3;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import fr.uge.ex2.ClientUpperCaseUDPRetry;

import static java.nio.file.StandardOpenOption.*;

public class ClientUpperCaseUDPFile {
    private final static Charset UTF8 = StandardCharsets.UTF_8;
    private final static int BUFFER_SIZE = 1024;
    private static final Logger logger = Logger.getLogger(ClientUpperCaseUDPRetry.class.getName());

    private static void usage() {
        System.out.println("Usage : ClientUpperCaseUDPFile in-filename out-filename timeout host port ");
    }
    
	private static void timeout() {
		logger.info("Le serveur n'a pas répondu");
	}
	
    public static void main(String[] args) throws IOException, InterruptedException {
        if (args.length != 5) {
            usage();
            return;
        }

        var inFilename = args[0];
        var outFilename = args[1];
        var timeout = Integer.parseInt(args[2]);
        var server = new InetSocketAddress(args[3], Integer.parseInt(args[4]));

        // Read all lines of inFilename opened in UTF-8
        var lines = Files.readAllLines(Path.of(inFilename), UTF8);
        var upperCaseLines = new ArrayList<String>();
        
        // TODO
        var queue = new ArrayBlockingQueue<ByteBuffer>(10);
        var cs = Charset.forName("utf-8");
        try(DatagramChannel dc = DatagramChannel.open()){
        	dc.bind(null);
	        Thread.ofPlatform().start(()->{
	        	ByteBuffer msg;
	        	var buffer = ByteBuffer.allocate(BUFFER_SIZE);
	        	try {
		        	try {
						for(var line : lines) {
			        		buffer = cs.encode(line);
			        		do {
			        			dc.send(buffer, server);
			        			msg = queue.poll(timeout,TimeUnit.MILLISECONDS);
			        			if(msg ==null) {
			        				timeout();
			        			}
			        		}while(msg == null);
			        		buffer.flip();
		                    logger.info("String : <<<< " + msg+ " >>>>");
		                    upperCaseLines.add(cs.decode(msg).toString());
			        	}
					} catch (IOException e) {
						return;
					}
	        	}catch (InterruptedException e) {
	        		return;
	        	}
	        });
	        
        	while(!Thread.interrupted()) {
        		try {
        			var buffer2 = ByteBuffer.allocate(BUFFER_SIZE);
        			dc.receive(buffer2);
        			buffer2.flip();
        			logger.info("received " + buffer2.remaining());
        			queue.put(buffer2);
        			buffer2.clear();
        		} catch(InterruptedException | IOException e) {
        			return;
        		}
        	}
	        
        }

        // Write upperCaseLines to outFilename in UTF-8
        Files.write(Path.of(outFilename), upperCaseLines, UTF8, CREATE, WRITE, TRUNCATE_EXISTING);
    }
}