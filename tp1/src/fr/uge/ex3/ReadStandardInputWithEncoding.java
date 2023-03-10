package fr.uge.ex3;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.channels.Channels;
import java.nio.channels.ReadableByteChannel;
import java.nio.charset.Charset;

public class ReadStandardInputWithEncoding {

	private static final int BUFFER_SIZE = 1024;

	private static void usage() {
		System.out.println("Usage: ReadStandardInputWithEncoding charset");
	}

	private static String stringFromStandardInput(Charset cs) throws IOException {
		ReadableByteChannel in = Channels.newChannel(System.in);
		var buffer = ByteBuffer.allocate(BUFFER_SIZE);
		var readable=0;
		CharBuffer cb=null;
		while(readable!=-1) {
//			while(buffer.hasRemaining() && readable!= -1) {
//				readable = in.read(buffer);
//			}
			readable = in.read(buffer);
			if(!buffer.hasRemaining() && readable!= -1) {
				buffer = ByteBuffer.allocate(buffer.capacity()*2);
			}
			buffer.flip();
			cb = cs.decode(buffer);
			buffer.clear();
//			var temp = ByteBuffer.allocate(buffer.capacity());
//			buffer.flip(); //On flip pour lire sur le buffer
//			temp.put(buffer); // On met le buffer dans une variable temporaire
//			buffer=ByteBuffer.allocate(buffer.capacity()*2); // On aggrandis la taille du buffer
//			temp.flip(); //On flip pour lire la variable temporaire
//			buffer.put(temp);	
		}
//		buffer.clear();
//		cb =cs.decode(buffer);
//		
		return cb.toString();
	}

	public static void main(String[] args) throws IOException {
		if (args.length != 1) {
			usage();
			return;
		}
		Charset cs = Charset.forName(args[0]);
		System.out.print(stringFromStandardInput(cs));
	}
}
