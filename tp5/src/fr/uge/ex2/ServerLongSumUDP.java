package fr.uge.ex2;

import java.util.BitSet;
import java.util.HashMap;
import java.util.logging.Logger;

import fr.uge.ex1.ServerIdUpperCaseUDP;

import java.io.IOException;
import java.net.BindException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.AsynchronousCloseException;
import java.nio.channels.ClosedByInterruptException;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;

public class ServerLongSumUDP {

    private static final Logger logger = Logger.getLogger(ServerIdUpperCaseUDP.class.getName());
    private static final Charset UTF8 = StandardCharsets.UTF_8;
    private static final int BUFFER_SIZE = 1024;
    private final HashMap<Session, SumSession> map = new HashMap<>();    
    private final DatagramChannel dc;
    private final ByteBuffer buffer = ByteBuffer.allocateDirect(BUFFER_SIZE);

    public ServerLongSumUDP(int port) throws IOException {
        dc = DatagramChannel.open();
        dc.bind(new InetSocketAddress(port));
        logger.info("ServerBetterUpperCaseUDP started on port " + port);
    }

    public record Session(InetSocketAddress adress,long sessionId) {
        
    }
    
    public class SumSession {
        private int totalOp;
        private long sum;
        private final BitSet packets;
        
        public SumSession(long totalOp) {
            this.totalOp =(int)totalOp;
            this.sum =0;
            this.packets = new BitSet((int)totalOp);
            
        }
        
        public boolean isNotComplete() {
        	return packets.cardinality() != totalOp;
        }
        
        public void summing(long idPosOper,long opValue) {
        	if(!packets.get((int) idPosOper)) {
        		sum+= opValue;
        		packets.set((int)idPosOper);
        	}
        }
        
        public long sum() {
        	return sum;
        }
        
    }

    public void serve() throws IOException {
        try {
            while (!Thread.interrupted()) {
                // TODO
                
                buffer.clear();
                var sender = (InetSocketAddress) dc.receive(buffer);
                logger.info("Received " + buffer.position() + " byte from " + sender.toString());
                buffer.flip();
                if(buffer.remaining() < Byte.BYTES + 4*Long.BYTES) {
                	logger.warning("Malformed packet");
                	continue;
                }
                var bytelog = buffer.get();
                if(bytelog != 1) {
                	logger.info("Wrong packet received");
                	continue;
                }
                var SessionId = buffer.getLong();
                var idPosOper = buffer.getLong();
                var totalValue = buffer.getLong();
                var opValue = buffer.getLong();
                if(idPosOper < 0 || idPosOper > totalValue) {
                	logger.info("Wrong Operation");
                }
                if(totalValue < 1) {
                	logger.info("Impossible Operation");
                }
                var session = new Session(sender,SessionId);
                var sumSession = map.computeIfAbsent(session,v -> new SumSession(totalValue));
                sumSession.summing(idPosOper,opValue);
                buffer.clear();
                buffer.put((byte) 2);
                buffer.putLong(SessionId);
                buffer.putLong(idPosOper);
                buffer.flip();
                logger.info("acquitement");
                dc.send(buffer, sender);
                if(!sumSession.isNotComplete()) {
                    buffer.clear();
                	buffer.put((byte) 3);
                	buffer.putLong(SessionId);
                	buffer.putLong(sumSession.sum());
                	buffer.flip();
                	dc.send(buffer, sender); 
                	logger.info("sending ");
                }    
            }
        } catch(IOException e) {
            logger.severe("IOException");
        } finally {
            dc.close();
        }
    }

    public static void usage() {
        System.out.println("Usage : ServerIdUpperCaseUDP port");
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 1) {
            usage();
            return;
        }

        var port = Integer.parseInt(args[0]);

        if (!(port >= 1024) & port <= 65535) {
            logger.severe("The port number must be between 1024 and 65535");
            return;
        }

        try {
            new ServerLongSumUDP(port).serve();
        } catch (BindException e) {
            logger.severe("Server could not bind on " + port + "\nAnother server is probably running on this port.");
            return;
        }
    }
}