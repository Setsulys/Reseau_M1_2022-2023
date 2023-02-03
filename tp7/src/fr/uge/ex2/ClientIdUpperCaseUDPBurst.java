package fr.uge.ex2;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Logger;

import fr.uge.ex1.ClientIdUpperCaseUDPOneByOne.State;

import static java.nio.file.StandardOpenOption.*;

public class ClientIdUpperCaseUDPBurst {

    private static Logger logger = Logger.getLogger(ClientIdUpperCaseUDPBurst.class.getName());
    private static final Charset UTF8 = Charset.forName("UTF8");
    private static final int BUFFER_SIZE = 1024;

    private enum State {
        SENDING, RECEIVING, FINISHED
    };

    private final List<String> lines;
    private final List<String> upperCaseLines = new ArrayList<>();
    private final long timeout;
    private final InetSocketAddress serverAddress;
    private final DatagramChannel dc;
    private final Selector selector;
    private final SelectionKey uniqueKey;
    private final int startTime;
    private ByteBuffer receiveBuffer ;
    private ByteBuffer senddingBuffer;    
    private final AnswerLog ans;

    // TODO add new fields

    private State state;

    private static void usage() {
        System.out.println("Usage : ClientIdUpperCaseUDPOneByOne in-filename out-filename timeout host port ");
    }
    
    private ClientIdUpperCaseUDPBurst(List<String> lines, long timeout, InetSocketAddress serverAddress,
            DatagramChannel dc, Selector selector, SelectionKey uniqueKey){
        this.lines = lines;
        this.timeout = timeout;
        this.serverAddress = serverAddress;
        this.dc = dc;
        this.selector = selector;
        this.uniqueKey = uniqueKey;
        this.state = State.SENDING;
        this.receiveBuffer= ByteBuffer.allocate(BUFFER_SIZE);
        this.senddingBuffer = ByteBuffer.allocate(BUFFER_SIZE);
        this.ans = new AnswerLog(lines.size());
    }

    public static ClientIdUpperCaseUDPBurst create(String inFilename, long timeout,
            InetSocketAddress serverAddress) throws IOException {
        Objects.requireNonNull(inFilename);
        Objects.requireNonNull(serverAddress);
        Objects.checkIndex(timeout, Long.MAX_VALUE);
        
        // Read all lines of inFilename opened in UTF-8
        var lines = Files.readAllLines(Path.of(inFilename), UTF8);
        var dc = DatagramChannel.open();
        dc.configureBlocking(false);
        dc.bind(null);
        var selector = Selector.open();
        var uniqueKey = dc.register(selector, SelectionKey.OP_WRITE);
        return new ClientIdUpperCaseUDPBurst(lines, timeout, serverAddress, dc, selector, uniqueKey);
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

        // Create client with the parameters and launch it
        var upperCaseLines = create(inFilename, timeout, server).launch();
        
        Files.write(Path.of(outFilename), upperCaseLines, UTF8, CREATE, WRITE, TRUNCATE_EXISTING);
    }

    private List<String> launch() throws IOException, InterruptedException {
        try {
            while (!isFinished()) {
                try {
                    selector.select(this::treatKey, updateInterestOps());
                } catch (UncheckedIOException tunneled) {
                    throw tunneled.getCause();
                }
            }
            return upperCaseLines;
        } finally {
            dc.close();
        }
    }

    private void treatKey(SelectionKey key) {
        try {
            if (key.isValid() && key.isWritable()) {
                doWrite();
            }
            if (key.isValid() && key.isReadable()) {
                doRead();
            }
        } catch (IOException ioe) {
            throw new UncheckedIOException(ioe);
        }
    }

    /**
     * Updates the interestOps on key based on state of the context
     *
     * @return the timeout for the next select (0 means no timeout)
     */

    private long updateInterestOps() {
        // TODO
    	switch(state) {
    	case SENDING: 
    		uniqueKey.interestOps(SelectionKey.OP_WRITE);
    		state = State.RECEIVING;
    	case RECEIVING:
    		var tm = (timeout + startTime) - System.currentTimeMillis();
    		if(tm <= 0) {
    			state = State.SENDING;
    			uniqueKey.interestOps(SelectionKey.OP_WRITE);
    			return 0;
    		}
    		uniqueKey.interestOps(SelectionKey.OP_READ); 
    		return tm;
    	default: 
    		return 0;
    	}
    }

    private boolean isFinished() {
        return state == State.FINISHED;
    }

    /**
     * Performs the receptions of packets
     *
     * @throws IOException
     */

    private void doRead() throws IOException {
        // TODO
    	receiveBuffer.clear();
    	dc.receive(receiveBuffer);
    	receiveBuffer.flip();
    	if(receiveBuffer.remaining() < Long.BYTES) {
    		logger.info("Malformed Packet");
    		return;
    	}
    	var id = (int) receiveBuffer.getLong();
    	if(!ans.missingAnswer()) {
    		ans.setAnswer(id);
    		upperCaseLines.add(UTF8.decode(receiveBuffer).toString());
    	}
    }

    /**
     * Tries to send the packets
     *
     * @throws IOException
     */

    private void doWrite() throws IOException {
        // TODO
    }
    
    
    class AnswerLog {
    	private final BitSet answers;
    	private final int nblines;
    	private final ReentrantLock lock = new ReentrantLock();
    	
    	public AnswerLog(int nblines) {
    		if(nblines < 0 ) {
    			throw new IllegalArgumentException();
    		}
    		this.nblines = nblines;
    		this.answers = new BitSet(nblines);
    	}
    	
    	public boolean missingAnswer() {
    		lock.lock();
    		try {
    			return answers.cardinality() != nblines;
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