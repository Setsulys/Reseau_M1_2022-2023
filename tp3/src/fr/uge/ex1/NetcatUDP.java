package fr.uge.ex1;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;

public class NetcatUDP {
    public static final int BUFFER_SIZE = 1024;

    private static void usage() {
        System.out.println("Usage : NetcatUDP host port charset");
    }

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            usage();
            return;
        }

        var server = new InetSocketAddress(args[0], Integer.parseInt(args[1]));
        var cs = Charset.forName(args[2]);
        var buffer = ByteBuffer.allocate(BUFFER_SIZE);
       
        try (var scanner = new Scanner(System.in);) {
            DatagramChannel dc = DatagramChannel.open();
     		dc.bind(null);
            while (scanner.hasNextLine()) {
            	//lecture de la ligne et envoi du paquet
                var line = scanner.nextLine();
            	buffer.put(cs.encode(line));
            	buffer.flip();
        		dc.send(buffer, server);
        		
        		//Réception du paquet et lecture du paquet
        		//On reinitialise la zone de travail pour recevoir le paquet 
        		buffer.clear();
        		//puis on recoit
            	var sender = (InetSocketAddress) dc.receive(buffer);
            	buffer.flip();
                System.out.println("Received " + buffer.remaining() + " bytes from " + sender);
                System.out.println("String : " + cs.decode(buffer).toString());
                
                //on réinitialise la zone de travail pour le message suivant
                buffer.clear();
            }
            buffer.clear();
        }
    }
}


//Comme recieve est bloquant, si un paquet est perdu, le protocole va attendre dans le vide
