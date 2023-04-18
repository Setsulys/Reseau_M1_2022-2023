package ex3;

import java.nio.ByteBuffer;

public class MessageReader implements Reader<Message>{

	private enum State{
		DONE,WAITING,ERROR
	}
	private Message msg;
	private StringReader reader = new StringReader();
	private State state = State.WAITING;
	
	@Override
	public ProcessStatus process(ByteBuffer bb) {
		// TODO Auto-generated method stub
		if(state == State.DONE || state == State.ERROR) {
			throw new IllegalStateException();
		}
		var readerState = reader.process(bb);
		if(readerState == ProcessStatus.DONE) {
			var login = reader.get();
			reader.reset();
			readerState = reader.process(bb);
			if(readerState == ProcessStatus.DONE) {
				var text = reader.get();
				msg = new Message(login,text);
			}
			else {
				return readerState;
			}
		}
		else {
			return readerState;
		}
		state =State.DONE;
		return ProcessStatus.DONE;
	}

	@Override
	public Message get() {
		if(state != State.DONE) {
			throw new IllegalStateException();
		}
		return msg;
	}

	@Override
	public void reset() {
		// TODO Auto-generated method stub
		reader.reset();
		state =State.WAITING;
	}

}
