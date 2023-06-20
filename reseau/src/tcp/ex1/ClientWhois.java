package tcp.ex1;

import java.io.IOException;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SocketChannel;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.logging.Logger;

public class ClientWhois {
    private static final Logger logger = Logger.getLogger(ClientWhois.class.getName());

    private final InetSocketAddress serverAddress;
    private final SocketChannel socketChannel;

    public ClientWhois(InetSocketAddress serverAddress) throws IOException {
        this.serverAddress = Objects.requireNonNull(serverAddress);
        this.socketChannel = SocketChannel.open(serverAddress);
    }

    public List<InetSocketAddress> performRequest(int max) throws IOException {
        // TODO
    	List<InetSocketAddress> lst = new ArrayList<InetSocketAddress>();
    	
    	ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES);
    	buffer.putInt(max);
    	buffer.flip();
    	socketChannel.write(buffer);
    	var size = max *(2*Integer.BYTES + 17 * Byte.BYTES);
    	buffer = ByteBuffer.allocate(size);
    	var read = socketChannel.read(buffer);
    	if(read == -1) {
    		return null;
    	}
    	buffer.flip();
    	int nb = buffer.getInt();
    	byte[] ipv;
    	while(buffer.hasRemaining()) {
    		var ip = buffer.get();
    		var oldlimit = buffer.limit();
    		if(ip == 4) {
    			buffer.limit(buffer.position()+4);
    			ipv = new byte[4];
    			buffer.get(ipv);
    			
    		}
    		else{
    			buffer.limit(buffer.position()+16);
    			ipv = new byte[16];
    			buffer.get(ipv);
    			
    		}
    		buffer.limit(oldlimit);
    		var host = buffer.getInt();
    		InetSocketAddress address = new InetSocketAddress(InetAddress.getByAddress(ipv),host);
    		lst.add(address);
    	}
        return lst;
    }
    

    public void launch() throws IOException {
        try (Scanner scanner = new Scanner(System.in)) {
            System.out.println("How many addresses maximum do you want to get ?");
            while (scanner.hasNextInt()) {
                var max = scanner.nextInt();
                if (max < 0) {
                    System.out.println("max must be positive");
                    continue;
                }
                var answer = performRequest(max);
                if (answer == null) {
                    System.out.println("Problem with request, exiting program");
                    return;
                }
                System.out.println(answer);
                System.out.println("How many addresses maximum do you want to get ?");
            }
        }
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 2) {
            System.out.println("usage: java fr.uge.ex1.ClientWhois host port");
            return;
        }
        var serverAddress = new InetSocketAddress(args[0], Integer.valueOf(args[1]));
        new ClientWhois(serverAddress).launch();
    }
}