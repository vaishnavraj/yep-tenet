package tenet.protocol.transport.tcp;

import tenet.protocol.IProtocol;
import tenet.protocol.interrupt.InterruptParam;
import tenet.protocol.statecontrol.IStateSetable;
import tenet.util.pattern.serviceclient.IClient;

public interface TCPProtocol extends IStateSetable, IProtocol,
		IClient<Integer>{

	
	public static final int INT_RETURN=0x7000001;
	
	enum ReturnStatus {
		CONN_CLOSING,CONN_RESET,CONN_ALREADY_EXIST,CONN_REFUSED,OK
	}
	
	enum ReturnType{
		LISTEN,CONNECT,ABORT,SEND,RECEIVE,CLOSE
	}
	
	class ReturnParam extends InterruptParam {
		public ReturnType type;
		public int handle;
		public int status;
	}
	//The int means the handle
	public int listen(Integer port);
	public int connect(Integer dstIP,Integer dstPort,Integer srcPort);
	public int close();
	public int accept(int handle);
	public int abort();
	
	public int send(byte[] data);	
	public int receive();
}
