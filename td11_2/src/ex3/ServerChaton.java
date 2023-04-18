package ex3;

import java.util.logging.Logger;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.Channel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.ServerSocketChannel;
import java.nio.channels.SocketChannel;
import java.nio.charset.StandardCharsets;
import java.util.ArrayDeque;

public class ServerChaton {
	static private class Context{
		private final SelectionKey key;
		private final SocketChannel sc;
		private final ByteBuffer bufferIn = ByteBuffer.allocate(BUFFER_SIZE);
		private final ByteBuffer bufferOut = ByteBuffer.allocate(BUFFER_SIZE);
		private final ArrayDeque<Message> queue = new ArrayDeque<>();
		private final MessageReader reader = new MessageReader();
		private ServerChaton server;
		private boolean closed = false;
		
		private Context(ServerChaton server, SelectionKey key) {
			this.key = key;
			this.sc = (SocketChannel) key.channel();
			this.server = server;
		}
		
		private void processIn() {
			for(;;) {
				Reader.ProcessStatus status = reader.process(bufferIn);
				switch(status) {
					case DONE ->{
						var msg = reader.get();
						server.broadCast(msg);
						reader.reset();
						break;
					}
					case REFILL ->{
						return;
					}
					case ERROR ->{
						silentlyClose();
						return;
					}
					default ->{
						return;
					}
				}
			}
		}
		
		private void processOut() {
			if (bufferOut.remaining() < Integer.BYTES) {
				return;
			}
			var message = queue.poll();
			if(message == null) {
				logger.info("nothing to poll");
				return;
			}
			var login = StandardCharsets.UTF_8.encode(message.login());
			var text = StandardCharsets.UTF_8.encode(message.text());
			bufferOut.putInt(login.remaining());
			bufferOut.put(login);
			bufferOut.putInt(text.remaining());
			bufferOut.put(text);
		}
		
		private void updateInterestOps(){
			var interestOps = 0;
			if(bufferIn.hasRemaining() && !closed) {
				interestOps |= SelectionKey.OP_READ;
			}
			if(bufferOut.position() != 0) {
				interestOps |= SelectionKey.OP_WRITE;
			}
			if(interestOps == 0) {
				silentlyClose();
				return;
			}
			key.interestOps(interestOps);
		}
		
		private void queueMessage(Message msg) {
			queue.add(msg);
			if(bufferOut.hasRemaining()) {
				processOut();
			}
			updateInterestOps();
		}
		
		private void doRead() throws IOException {
			if(sc.read(bufferIn) == -1) {
				logger.info("Connection closed");
				closed = true;
				return;
			}
			processIn();
			//updateInterestOps();
		}
		
		private void doWrite() throws IOException {
			bufferOut.flip();
			sc.write(bufferOut);
			bufferOut.compact();
			updateInterestOps();
		}
		
		private void silentlyClose() {
			try {
				sc.close();
			}catch(IOException e) {
				
			}
		}
	}

	private static int BUFFER_SIZE = 1024;
	private static final Logger logger = Logger.getLogger(ServerChaton.class.getName());
	
	private final ServerSocketChannel ssc;
	private final Selector selector;
	
	public ServerChaton(int port) throws IOException {
		this.ssc = ServerSocketChannel.open();
		ssc.bind(new InetSocketAddress(port));
		selector = Selector.open();
	}
	
	public void launch() throws IOException{
		ssc.configureBlocking(false);
		ssc.register(selector, SelectionKey.OP_ACCEPT);
		while(!Thread.interrupted()) {
			System.out.println("Starting Select");
			try {
				selector.select(this::treatKey);
			}catch(UncheckedIOException tunneled) {
				throw tunneled.getCause();
			}
			System.out.println("Select finished");
		}
	}
	
	public void treatKey(SelectionKey key) {
		try {
			if(key.isValid() && key.isAcceptable()) {
				doAccept(key);
			}
			
		}catch(IOException e) {
			throw new UncheckedIOException(e);
		}
		try {
			if(key.isValid() && key.isWritable()) {
				((Context) key.attachment()).doWrite();
			}
			if(key.isValid() && key.isReadable()) {
				((Context) key.attachment()).doRead();
			}
		}catch(IOException e) {
			// TODO: handle exception
			logger.info("Conection closed with client IOE");
			silentlyClose(key);
		}
	}
	
	public void doAccept(SelectionKey key) throws IOException {
		ServerSocketChannel ssc = (ServerSocketChannel) key.channel();
		SocketChannel sc = ssc.accept();
		if(sc == null) {
			logger.info("Selector Gave Bad Hint");
			return;
		}
		sc.configureBlocking(false);
		var newKey = sc.register(selector, SelectionKey.OP_READ | SelectionKey.OP_WRITE);
		newKey.attach(new Context(this,newKey));
	}
	
	public void silentlyClose(SelectionKey key) {
		Channel sc = (Channel) key.channel();
		try {
			sc.close();
		}catch (IOException e) {
			//ignore exception
		}
	}
	
	public void broadCast(Message msg) {
		for(var key : selector.keys()) {
			Context context = (Context) key.attachment();
			if(context != null) {
				context.queueMessage(msg);
			}
		}
	}
	
	public static void main(String[] args) throws NumberFormatException, IOException {
		if(args.length != 1) {
			usage();
			return;
		}
		new ServerChaton(Integer.parseInt(args[0])).launch();
	}
	
	private static void usage() {
		System.out.println("Usage : ServerSumBetter port");
	}
	
	
	
	
	
	
	
	
}
