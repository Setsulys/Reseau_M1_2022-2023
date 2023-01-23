package fr.uge.ex2;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Optional;
import java.util.Scanner;
import java.util.logging.Logger;

public class ClientBetterUpperCaseUDP {
	private static final int MAX_PACKET_SIZE = 1024;

	private static Charset ASCII_CHARSET = StandardCharsets.US_ASCII; //Charset.forName("US-ASCII");
	
	/**
	 * Creates and returns an Optional containing a new ByteBuffer containing the encoded representation 
	 * of the String <code>msg</code> using the charset <code>charsetName</code> 
	 * in the following format:
	 * - the size (as a Big Indian int) of the charsetName encoded in ASCII<br/>
	 * - the bytes encoding this charsetName in ASCII<br/>
	 * - the bytes encoding the String msg in this charset.<br/>
	 * The returned ByteBuffer is in <strong>write mode</strong> (i.e. need to 
	 * be flipped before to be used).
	 * If the buffer is larger than MAX_PACKET_SIZE bytes, then returns Optional.empty.
	 *
	 * @param msg the String to encode
	 * @param charsetName the name of the Charset to encode the String msg
	 * @return an Optional containing a newly allocated ByteBuffer containing the representation of msg,
	 *         or an empty Optional if the buffer would be larger than 1024
	 */
	public static Optional<ByteBuffer> encodeMessage(String msg, String charsetName) {
		var buffer = ByteBuffer.allocate(MAX_PACKET_SIZE);
		//longeur du charset
		buffer.putInt(ASCII_CHARSET.encode(charsetName).remaining());
		//Nom du charset en ASCII
		buffer.put(ASCII_CHARSET.encode(charsetName));
		//Message encodé en le charset envoyé
		if(buffer.remaining() < Charset.forName(charsetName).encode(msg).remaining()) {
			return Optional.empty();
		}
		buffer.put(Charset.forName(charsetName).encode(msg));
		return Optional.of(buffer);
	}

	/**
	 * Creates and returns an Optional containing a String message represented by the ByteBuffer buffer,
	 * encoded in the following representation:
	 * - the size (as a Big Indian int) of a charsetName encoded in ASCII<br/>
	 * - the bytes encoding this charsetName in ASCII<br/>
	 * - the bytes encoding the message in this charset.<br/>
	 * The accepted ByteBuffer buffer must be in <strong>write mode</strong>
	 * (i.e. need to be flipped before to be used).
	 *
	 * @param buffer a ByteBuffer containing the representation of an encoded String message
	 * @return an Optional containing the String represented by buffer, or an empty Optional if the buffer cannot be decoded
	 */
	public static Optional<String> decodeMessage(ByteBuffer buffer) {
		//On flip pour lire le buffer
		buffer.flip();
		//On recupere la taille du buffer
		if(buffer.remaining() < 4) {
			return Optional.empty();
		}
		var longeurCharset = buffer.getInt();
		//On verifie que la variable est > 0 et quelle ne prend pas tout le buffer
		if (longeurCharset <= 0 ||  longeurCharset > buffer.remaining()) {
			return Optional.empty();
		}
		//On cree un nouveau buffer pour mettre le charset dedans
//		var buf = ByteBuffer.allocate(MAX_PACKET_SIZE);
//		for(var i= 0; i < longeurCharset;i++) {
//			buf.put(buffer.get());
//		}
		buffer.limit(longeurCharset);
		//on lit le nouveau buffer donc on le flip
		//buffer.flip();
		var chstr = ASCII_CHARSET.decode(buffer).toString();
		//On verifie que le charset donné est bien un charset existant
		if(!Charset.isSupported(chstr)) {
			return Optional.empty();
		}
		Charset cs = Charset.forName(chstr);
		buffer.clear();
		//On décode le message
		return Optional.of(cs.decode(buffer).toString());

	}

	public static void usage() {
		System.out.println("Usage : ClientBetterUpperCaseUDP host port charsetName");
	}

	public static void main(String[] args) throws IOException {
		// check and retrieve parameters
		if (args.length != 3) {
			usage();
			return;
		}
		var host = args[0];
		var port = Integer.valueOf(args[1]);
		var charsetName = args[2];

		var destination = new InetSocketAddress(host, port);
		// buffer to receive messages
		var buffer = ByteBuffer.allocateDirect(MAX_PACKET_SIZE);

		try(var scanner = new Scanner(System.in);
				var dc = DatagramChannel.open()){
			while (scanner.hasNextLine()) {
				var line = scanner.nextLine();
				
				var message = encodeMessage(line, charsetName);
				if (message.isEmpty()) {
					System.out.println("Line is too long to be sent using the protocol BetterUpperCase");
					continue;
				}
				var packet = message.get();
				packet.flip();
				dc.send(packet, destination);
				buffer.clear();
				dc.receive(buffer);
				
				decodeMessage(buffer).ifPresentOrElse(
						(str) -> System.out.println("Received: " + str), 
						() -> System.out.println("Received an invalid paquet"));
			}
		}
	}
}
