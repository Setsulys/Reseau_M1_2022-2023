package fr.uge.ex2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.util.logging.*;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.util.Scanner;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ClientUpperCaseUDPTimeout {
	public static final int BUFFER_SIZE = 1024;
	public static final int TIMEOUT = 1000;
	private static final Logger logger = Logger.getLogger(ClientUpperCaseUDPRetry.class.getName());

	private static void usage() {
		System.out.println("Usage : NetcatUDP host port charset");
	}

	private static void timeout() {
		logger.info("Le serveur n'a pas répondu");
	}

	public static void main(String[] args) throws IOException {
    	
    	
    	var server = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
        var cs = Charset.forName(args[2]);
        
        
    	var queue = new ArrayBlockingQueue<ByteBuffer>(10);
        if (args.length != 3) {
            usage();
            return;
        }
 
        try(DatagramChannel dc = DatagramChannel.open();){
            dc.bind(null);
	        Thread.ofPlatform().start(() -> {
	            while (!Thread.interrupted()) {
	                try {
	                    var buffer2 = ByteBuffer.allocate(BUFFER_SIZE);
	                    
	                    dc.receive(buffer2);
	                    buffer2.flip();
	                    logger.info("received " + buffer2.remaining()); 
	                    queue.put(buffer2);
	                    buffer2.clear();
	                } catch (InterruptedException | IOException e) {
	                    return;
	                }
	            }
	        });
        	ByteBuffer msg;
            while (!Thread.interrupted()) {
                try (var scanner = new Scanner(System.in);){
                	var buffer = ByteBuffer.allocate(BUFFER_SIZE);
                    while (scanner.hasNextLine()) {
                        var line = scanner.nextLine();
                    	buffer = cs.encode(line);
	                    dc.send(buffer, server);
	                    msg = queue.poll(TIMEOUT,TimeUnit.MILLISECONDS);
	                    if(msg == null) {
	                    	timeout();
	                    	continue;
	                    }
	                    logger.info("String : <<<< " + cs.decode(msg).toString()+" >>>>");   
	                    buffer.clear();
                    }
                }catch(IOException | InterruptedException e) {
                   return;
                }
            }
        } 
    }
}

