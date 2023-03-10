package fr.uge.ex1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Objects;
import java.util.concurrent.SynchronousQueue;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import static java.nio.file.StandardOpenOption.*;

public class ClientIdUpperCaseUDPOneByOne {

	private static Logger logger = Logger.getLogger(ClientIdUpperCaseUDPOneByOne.class.getName());
	private static final Charset UTF8 = StandardCharsets.UTF_8;
	private static final int BUFFER_SIZE = 1024;

	private record Response(long id, String message) {
		
		public ByteBuffer buffer() {
			var buffer = ByteBuffer.allocate(BUFFER_SIZE);
			buffer.putLong(id);
			buffer.put(UTF8.encode(message));
			return buffer;
		}
		
		public String toString() {
			return message;
		}
	};

	private final String inFilename;
	private final String outFilename;
	private final long timeout;
	private final InetSocketAddress server;
	private final DatagramChannel dc;
	private final SynchronousQueue<Response> queue = new SynchronousQueue<>();

	public static void usage() {
		System.out.println("Usage : ClientIdUpperCaseUDPOneByOne in-filename out-filename timeout host port ");
	}

	public ClientIdUpperCaseUDPOneByOne(String inFilename, String outFilename, long timeout, InetSocketAddress server)
			throws IOException {
		this.inFilename = Objects.requireNonNull(inFilename);
		this.outFilename = Objects.requireNonNull(outFilename);
		this.timeout = timeout;
		this.server = server;
		this.dc = DatagramChannel.open();
		dc.bind(null);
	}

	private void listenerThreadRun() {
		try {
			
			while(!Thread.interrupted()) {
				var buffer = ByteBuffer.allocate(BUFFER_SIZE);
				dc.receive(buffer);
				buffer.flip();
				if(buffer.remaining() < 8) {
					logger.info("Corrupted Package");
					return;
				}
				queue.put(new Response(buffer.getLong(), UTF8.decode(buffer).toString()));			
			}
		}catch(InterruptedException e) {
			logger.info("Interrupted by listenerThreadRun");
		}catch(AsynchronousCloseException e) {
			logger.info("AsynchronousException of listenerThreadRun");
		}catch(IOException e) {
			logger.severe("IOException of listenerThreadRun");
		} finally {
			logger.info("End of listenerThreadRun");
		}
	}

	public void launch() throws IOException, InterruptedException {
		try {

			var listenerThread = Thread.ofPlatform().start(this::listenerThreadRun);
			
			// Read all lines of inFilename opened in UTF-8
			var lines = Files.readAllLines(Path.of(inFilename), UTF8);

			var upperCaseLines = new ArrayList<String>();

			Response msg;
			var id =0;
			for(var line : lines) {
				var datas = new Response(id, line);
				var buffer = datas.buffer();
				do {
					dc.send(buffer, server);
					buffer.flip(); //on flip le buffer pour revenir sur la zone de travail
					msg = waitForAnswer(timeout,id);
				}while(msg == null);
				logger.info("Datas : " + msg.toString());
				
				id++;
				upperCaseLines.add(msg.toString());
			}

			listenerThread.interrupt();
			Files.write(Paths.get(outFilename), upperCaseLines, UTF8, CREATE, WRITE, TRUNCATE_EXISTING);
		}catch(AsynchronousCloseException e) {
			logger.info("AsynchronousException of Launch");
		}catch(IOException e) {
			logger.severe("IOException of Launch");
		}finally {
			dc.close();
			logger.info("End of Launch");
		}
	}

	public Response waitForAnswer( long timeout, long id) {
        try {
            Response rep;
            long start;
            do {
                start = System.currentTimeMillis();
                rep = queue.poll(timeout,TimeUnit.MILLISECONDS);
                if(rep == null || timeout <= 0) {
                    return null;
                }
                timeout = timeout - (System.currentTimeMillis() - start);

            }while(rep.id != id);
            return rep;

        } catch (InterruptedException e) {
            logger.info("interrupted");
        }
        return null;
    }
	
	public static void main(String[] args) throws IOException, InterruptedException {
		if (args.length != 5) {
			usage();
			return;
		}

		var inFilename = args[0];
		var outFilename = args[1];
		var timeout = Long.parseLong(args[2]);
		var server = new InetSocketAddress(args[3], Integer.parseInt(args[4]));
		try(DatagramChannel dc = DatagramChannel.open()){
			dc.bind(null);
			// Create client with the parameters and launch it
			new ClientIdUpperCaseUDPOneByOne(inFilename, outFilename, timeout, server).launch();
		}


	}
}
