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
import java.util.logging.Logger;

public class ServerEchoMultiPort {
    private static final Logger logger = Logger.getLogger(ServerEchoMultiPort.class.getName());

    private final Selector selector;
    private final int BUFFER_SIZE = 1024;
    private int LowerPort;
    private int UpperPort;
    
    public class Context {
    	private final ByteBuffer ContextBuffer = ByteBuffer.allocate(BUFFER_SIZE);
    	private InetSocketAddress sender;
    	}
    
    public ServerEchoMultiPort(int start,int end) throws IOException {
        this.LowerPort = start;
        this.UpperPort = end;
        selector = Selector.open();
        for(var port = LowerPort;port <= UpperPort;port++) {
        	var dc = DatagramChannel.open();
        	dc.bind(new InetSocketAddress(port));
        	dc.configureBlocking(false);
            dc.register(selector, SelectionKey.OP_READ,new Context());
            
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
    	context.ContextBuffer.clear();
    	var dchannel =(DatagramChannel) key.channel();
    	var sender = dchannel.receive(context.ContextBuffer);
    	context.ContextBuffer.flip();
    	if(sender == null) {
    		logger.info("Nothing received");
    		return;
    	}
    	context.sender = (InetSocketAddress)sender;
    	key.interestOps(SelectionKey.OP_WRITE);
    }

    private void doWrite(SelectionKey key) throws IOException {
        var context = (Context) key.attachment();
        var dchannel = (DatagramChannel) key.channel();
        dchannel.send(context.ContextBuffer, context.sender);
        if(context.ContextBuffer.hasRemaining()) {
        	logger.info("Packet not sended");
        	return;
        }
        key.interestOps(SelectionKey.OP_READ);
        context.ContextBuffer.clear();
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
