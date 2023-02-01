package fr.uge.ex3;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Objects;
import java.util.logging.Logger;

public class ServerEchoMultiPort {
    private static final Logger logger = Logger.getLogger(ServerEchoMultiPort.class.getName());
    private final Selector selector;
    private int LowerPort;
    private int UpperPort;
    
    public class Context {
    	private final int BUFFER_SIZE = 1024;
    	private final ByteBuffer contextBuffer = ByteBuffer.allocate(BUFFER_SIZE);
    	private InetSocketAddress sender;
    	private DatagramChannel dchannel;
    	
    	
    	public Context(DatagramChannel dc) {
    		this.dchannel = Objects.requireNonNull(dc);
    	}
    	
    	public void read(SelectionKey key) throws IOException {
    		contextBuffer.clear();
        	sender = (InetSocketAddress) dchannel.receive(contextBuffer);
        	contextBuffer.flip();
        	if(sender == null) {
        		logger.info("Nothing received");
        		return;
        	}
        	key.interestOps(SelectionKey.OP_WRITE);
    	}
    	
    	public void write(SelectionKey key) throws IOException {
            dchannel.send(contextBuffer, sender);
            if(contextBuffer.hasRemaining()) {
            	logger.info("Packet not sended");
            	return;
            }
            key.interestOps(SelectionKey.OP_READ);
            contextBuffer.clear();
    	}
    }
    
    public ServerEchoMultiPort(int start,int end) throws IOException {
        this.LowerPort = start;
        this.UpperPort = end;
        selector = Selector.open();
        for(var port = LowerPort;port <= UpperPort;port++) {
        	var dc = DatagramChannel.open();
        	dc.bind(new InetSocketAddress(port));
        	dc.configureBlocking(false);
            dc.register(selector, SelectionKey.OP_READ,new Context(dc));
            
        }
    }

    public void serve() throws IOException {
        logger.info("ServerEcho started from port " + LowerPort + " to port " + UpperPort);
        while (!Thread.interrupted()) {
        	try {
            selector.select(this::treatKey);
        	}catch(UncheckedIOException e) {
        		throw e.getCause();
        	}
        	
        }
    }

    private void treatKey(SelectionKey key) {
        try {
            if (key.isValid() && key.isWritable()) {
                doWrite(key);
            }
            if (key.isValid() && key.isReadable()) {
                doRead(key);
            }
        } catch (IOException e) {
        	throw new UncheckedIOException(e);
        }

    }

    private void doRead(SelectionKey key) throws IOException {
    	var context = (Context)key.attachment();
    	context.read(key);
    	
    }

    private void doWrite(SelectionKey key) throws IOException {
        var context = (Context) key.attachment();
        context.write(key);
    }

    public static void usage() {
        System.out.println("Usage : ServerEchoMultiPort LowerPort UpperPort");
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            usage();
            return;
        }
        new ServerEchoMultiPort(Integer.parseInt(args[0]),Integer.parseInt(args[1])).serve();
    }
}
