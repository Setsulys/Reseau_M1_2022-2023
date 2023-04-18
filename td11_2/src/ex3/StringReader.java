package ex3;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

public class StringReader implements Reader<String>{
	
	private enum State {
		DONE,WAITING,ERROR
	}
	
	private State state = State.WAITING;
	private static int BUFFSIZE = 1024;
	private final ByteBuffer msgBuffer = ByteBuffer.allocate(BUFFSIZE);
	private final ByteBuffer bufferSize = ByteBuffer.allocate(Integer.BYTES);
	private String message;
	@Override
	public ProcessStatus process(ByteBuffer bb) {
		// TODO Auto-generated method stub
		if(state == State.DONE || state == State.ERROR ) {
			throw new IllegalStateException();
		}
		bb.flip();
		try {
			while(bb.hasRemaining() && bufferSize.hasRemaining()) {
				bufferSize.put(bb.get());
			}
			if(bufferSize.remaining() != 0) {
				return ProcessStatus.REFILL;
			}
			int msgSize = bufferSize.flip().getInt();
			if(msgSize < 0 || msgSize > BUFFSIZE) {
				return ProcessStatus.ERROR;
			}
			while(bb.hasRemaining() && msgBuffer.position() < msgSize && msgBuffer.hasRemaining()) {
				msgBuffer.put(bb.get());
			}
			if(msgBuffer.position() < msgSize) {
				return ProcessStatus.REFILL;
			}
		}finally {
			bb.compact();
		}
		message = StandardCharsets.UTF_8.decode(msgBuffer.flip()).toString();
		state = State.DONE;
		return ProcessStatus.DONE;
	}

	@Override
	public String get() {
		if(state != State.DONE) {
			throw new IllegalStateException();
		}
		return message;
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		msgBuffer.clear();
		bufferSize.clear();
		state = State.WAITING;
	}

}
