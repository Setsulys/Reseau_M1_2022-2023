package fr.uge.ex1;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.util.Objects;
import java.util.logging.Logger;

public class ServerSumOneShot {

	private static final int BUFFER_SIZE = 2 * Integer.BYTES;
	private static final Logger logger = Logger.getLogger(ServerSumOneShot.class.getName());

	private final ServerSocketChannel serverSocketChannel;
	private final Selector selector;

	
//	public class Context {
//		private final ByteBuffer buffer = ByteBuffer.allocate(BUFFER_SIZE);
//		private SocketChannel channel;
//		
//		
//		public Context (SocketChannel channel) {
//			this.channel = Objects.requireNonNull(channel);
//		}
//		
//		
//		public void read(SelectionKey key) throws IOException {
//			buffer.clear();
//			if(channel.read(buffer) ==-1) {
//				channel.close();
//				logger.info("Not all value readed");
//				return;
//			}
//			buffer.flip();
//			var fst = buffer.getInt();
//			var snd = buffer.getInt();
//			if(fst != Integer.BYTES ||snd != Integer.BYTES ) {
//				logger.info("malformed packet");
//			}
//			buffer.clear();
//			buffer.getInt(fst+snd);
//			buffer.flip();
//			key.interestOps(SelectionKey.OP_WRITE);
//		}
//		
//		public void write(SelectionKey key) throws IOException {
//			channel.write(buffer);
//			if(buffer.hasRemaining()) {
//				logger.info("Packet not sended");
//				return;
//			}
//			key.interestOps(SelectionKey.OP_READ);
//			buffer.clear();
//		}
//		
//	}
	
	public ServerSumOneShot(int port) throws IOException {
		serverSocketChannel = ServerSocketChannel.open();
		serverSocketChannel.bind(new InetSocketAddress(port));
		selector = Selector.open();
	}

	public void launch() throws IOException {
		serverSocketChannel.configureBlocking(false);
		serverSocketChannel.register(selector, SelectionKey.OP_ACCEPT);
		while (!Thread.interrupted()) {
			Helpers.printKeys(selector); // for debug
			System.out.println("Starting select");
			try {
				selector.select(this::treatKey);
			}catch(UncheckedIOException tunneled) {
				logger.info("Unchecked from treatKey");
				throw tunneled.getCause();
			}
			System.out.println("Select finished");
		}
	}

	private void treatKey(SelectionKey key) {
		try {
			Helpers.printSelectedKey(key); // for debug
			if (key.isValid() && key.isAcceptable()) {
				doAccept(key);
			}
			if (key.isValid() && key.isWritable()) {
				doWrite(key);
			}
			if (key.isValid() && key.isReadable()) {
				doRead(key);
			}
		}catch(IOException ioe) {
			throw new UncheckedIOException(ioe);
		}
	}

	private void doAccept(SelectionKey key) throws IOException {
		// TODO
		ServerSocketChannel ssc = (ServerSocketChannel)  key.channel();
		SocketChannel sc = ssc.accept();
		if(sc == null) {
			logger.info("Selector gave bad hint");
			return;
		}
		sc.configureBlocking(false);
		//sc.register(selector, SelectionKey.OP_READ,new Context(sc));
		sc.register(selector, SelectionKey.OP_READ,ByteBuffer.allocate(BUFFER_SIZE));

	}

	private void doRead(SelectionKey key) throws IOException {
		// TODO
		var channel = (SocketChannel) key.channel();
		var buffer = (ByteBuffer) key.attachment();
		if(channel.read(buffer)==-1) {
			channel.close();
			logger.info("Not readfull");
			return;
		}
		if(buffer.hasRemaining()) {
			logger.info("remain place in the buffer");
			return;
		}
		buffer.flip();
		var sum = buffer.getInt() + buffer.getInt();
		buffer.clear();
		buffer.putInt(sum);
		buffer.flip();
		key.interestOps(SelectionKey.OP_WRITE);
		
		
	}

	private void doWrite(SelectionKey key) throws IOException {
		var channel = (SocketChannel) key.channel();
		var buffer = (ByteBuffer) key.attachment();
		channel.write(buffer);
		if(buffer.hasRemaining()) {
			logger.info("Not sendfully");
			return;
		}
		silentlyClose(key);
		
	}

	private void silentlyClose(SelectionKey key) {
		var sc = (Channel) key.channel();
		try {
			sc.close();
		} catch (IOException e) {
			// ignore exception
			logger.info("SilentlyClose IOE");
		}
	}

	public static void main(String[] args) throws NumberFormatException, IOException {
		if (args.length != 1) {
			usage();
			return;
		}
		new ServerSumOneShot(Integer.parseInt(args[0])).launch();
	}

	private static void usage() {
		System.out.println("Usage : ServerSumOneShot port");
	}
}