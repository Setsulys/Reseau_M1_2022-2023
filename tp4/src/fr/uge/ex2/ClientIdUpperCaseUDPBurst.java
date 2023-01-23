package fr.uge.ex2;


import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.BitSet;
import java.util.List;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

public class ClientIdUpperCaseUDPBurst {

        private static Logger logger = Logger.getLogger(ClientIdUpperCaseUDPBurst.class.getName());
        private static final Charset UTF8 = StandardCharsets.UTF_8;
        private static final int BUFFER_SIZE = 1024;
        private final List<String> lines;
        private final int nbLines;
        private final String[] upperCaseLines; //
        private final int timeout;
        private final String outFilename;
        private final InetSocketAddress serverAddress;
        private final DatagramChannel dc;
        private final AnswersLog answersLog;         // Thread-safe structure keeping track of missing responses

        public static void usage() {
            System.out.println("Usage : ClientIdUpperCaseUDPBurst in-filename out-filename timeout host port ");
        }

        public ClientIdUpperCaseUDPBurst(List<String> lines,int timeout,InetSocketAddress serverAddress,String outFilename) throws IOException {
            this.lines = lines;
            this.nbLines = lines.size();
            this.timeout = timeout;
            this.outFilename = outFilename;
            this.serverAddress = serverAddress;
            this.dc = DatagramChannel.open();
            dc.bind(null);
            this.upperCaseLines = new String[nbLines];
            this.answersLog = new AnswersLog(nbLines);
        }

        private void senderThreadRun() {
        	try {
				var buffer = ByteBuffer.allocate(BUFFER_SIZE);
        		while(answersLog.missingAnswers()) {
        			var lineNumber = 0;
        			for(var line : lines) {
        				if(!answersLog.getAnswer(lineNumber)) {
        					buffer.putLong(lineNumber);
        					buffer.put(UTF8.encode(line));
        					buffer.flip();
        					dc.send(buffer, serverAddress);
        					buffer.clear();
        				}
        				lineNumber++;
        			}
        			var start = System.currentTimeMillis(); // phase d'attente
					while(timeout  > (System.currentTimeMillis() - start)) {// tant qu'il reste du temps on stop le programme
					}
        		}

    		}catch(AsynchronousCloseException e) {
    			logger.info("AsynchronousException of SenderThreadRun");
    		}catch(IOException e) {
    			logger.severe("IOException of SenderThreadRun");
    		} finally {
    			logger.info("End of SenderThreadRun");
    		}
			// TODO : body of the sender thread

        }

        public void launch() throws IOException {
            Thread senderThread = Thread.ofPlatform().start(this::senderThreadRun);
			try {
				var buffer = ByteBuffer.allocate(BUFFER_SIZE);
				while(answersLog.missingAnswers()) {
					dc.receive(buffer);
					buffer.flip();
					if(buffer.remaining() < 8) {
						logger.info("package corrupted");
						continue;
					}
					var id  = (int) buffer.getLong();
					if(!answersLog.getAnswer(id)) {
						answersLog.setAnswer(id);
						upperCaseLines[id] = UTF8.decode(buffer).toString();
					}
					buffer.clear();
				}
				
			}catch(AsynchronousCloseException e) {
    			logger.info("AsynchronousException of launch");
    		}catch(IOException e) {
    			logger.severe("IOException of launch");
    		} finally {
    			logger.info("End of launch");
    		}
			
			senderThread.interrupt();
			Files.write(Paths.get(outFilename),Arrays.asList(upperCaseLines), UTF8,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.TRUNCATE_EXISTING);
        }

        public static void main(String[] args) throws IOException, InterruptedException {
            if (args.length !=5) {
                usage();
                return;
            }

            String inFilename = args[0];
            String outFilename = args[1];
            int timeout = Integer.valueOf(args[2]);
            String host=args[3];
            int port = Integer.valueOf(args[4]);
            InetSocketAddress serverAddress = new InetSocketAddress(host,port);

            //Read all lines of inFilename opened in UTF-8
            List<String> lines= Files.readAllLines(Paths.get(inFilename),UTF8);
            //Create client with the parameters and launch it
            ClientIdUpperCaseUDPBurst client = new ClientIdUpperCaseUDPBurst(lines,timeout,serverAddress,outFilename);
            client.launch();

        }

        private static class AnswersLog {
        	private final BitSet answers;
        	private final int nbLines;
        	private final ReentrantLock lock = new 	ReentrantLock();
        	
        	public AnswersLog(int nbLines) {
        		if(nbLines < 0) {
        			throw new IllegalArgumentException();
        		}
        		this.nbLines = nbLines;
        		this.answers = new BitSet(nbLines);
        	}
        	
        	public boolean missingAnswers() {
        		lock.lock();
        		try {
        			return answers.cardinality() != nbLines;
        		}finally {
        			lock.unlock();
        		}
        	}
        	
        	public void setAnswer(int value) {
        		lock.lock();
        		try {
        			answers.set(value);;
        		}finally {
        			lock.unlock();
        		}
        	}
        	
        	public boolean getAnswer(int value) {
        		lock.lock();
        		try {
        			return answers.get(value);
        		}finally {
        			lock.unlock();
        		}
        	}

        }
    }


