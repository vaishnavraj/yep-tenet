package tenet.protocol.transport.tcp;

import java.util.LinkedList;
import java.util.Random;

import tenet.protocol.interrupt.InterruptObject;
import tenet.protocol.interrupt.InterruptParam;
import tenet.protocol.transport.tcp.TCPProtocol.ReturnStatus;
import tenet.protocol.transport.tcp.TCPProtocol.ReturnType;

public class MySocket extends InterruptObject {
	public int handle;
	public MyTCP m_tcp;
	
	public Integer src_ip;
	public int src_port;
	
	public Integer dest_ip;
	public int dest_port;
	
	public State CurrState = State.CLOSED;
	public State PrevState;
	public double RTO = 2.0;
	public double RTT;
	public double SRTT;
	public int CWND;
	public int MSS;
	public int SSthresh;
	public int MSL;
	
	public int SND_UNA;
	public int SND_NXT;
	public int SND_WND;
	public int ISS;
	
	public int RCV_NXT;
	public int RCV_WND;
	public int IRS;
	
	
	
	public enum State {
		CLOSED, LISTEN, SYN_SENT, SYN_RCVD, ESTABLISHED, FIN_WAIT_1, FIN_WAIT_2,
		CLOSING, CLOSE_WAIT, LAST_ACK, TIME_WAIT
	}
	
	protected final static int REXMT = 0x00000001;
	protected final static int TIMEWAIT = 0x00000002;
	protected final static int USERTIME = 0x00000003;
	
	public int dACK;
	public int ExpBoff;
	
	//buffer
	public LinkedList<byte[]> SendBuffer = new  LinkedList<byte[]>();
	public LinkedList<byte[]> ReceiveBuffer = new  LinkedList<byte[]>();
	public LinkedList<byte[]> OORCV = new  LinkedList<byte[]>();
	public LinkedList<TCPSegment> RexmtQueue = new  LinkedList<TCPSegment>();
	public int Receive = 0;
	public int Close = 0;
	
	Random random = new Random();
	TCPSegment seg;
	
	public MySocket(int handle, MyTCP m_tcp){
		this.handle = handle;
		this.m_tcp = m_tcp;
		
	}
	
	public void bind(int ip, int port) {
		src_ip = ip;
		src_port = port;
	}
	
	
	private void changeState(State newState){
		if (newState == State.CLOSED){
			if (m_tcp.portHandle.get(src_port) == this) m_tcp.portHandle.remove(src_port);
			m_tcp.handleSocket.remove(this.handle);
		}
		PrevState = CurrState;
		CurrState = newState;
	}
	
	//Passive Open
	public void Listen(){
		switch (CurrState){
		case CLOSED:
			changeState(State.LISTEN);
			m_tcp.ReturnMsg(ReturnType.LISTEN, handle, ReturnStatus.OK, 0, null);
			break;
		case LISTEN:
			//TODO
			System.out.println("TODO");
			break;
		default:
			m_tcp.ReturnMsg(ReturnType.LISTEN, handle, ReturnStatus.CONN_ALREADY_EXIST, -1, null);
			break;			
		}
	}
	
	//Active Open
	public void Connect(int dstIP, int dstPort){
		this.dest_ip = dstIP;
		this.dest_port = dstPort;
		switch (CurrState){
		case CLOSED:
			ISS = random.nextInt();
			seg = new TCPSegment(src_port, dest_port);
			seg.SequenceNumber = ISS;
			seg.setSYN();
			sendSEG(seg);
			SND_UNA = ISS;
			SND_NXT = ISS+1;
			changeState(State.SYN_SENT);
			break;
		case LISTEN:
			//TODO
			System.out.println("TODO");
			break;
		default:
			m_tcp.ReturnMsg(ReturnType.CONNECT, handle, ReturnStatus.CONN_ALREADY_EXIST, -1, null);
			break;		
		}
	}

	//Send
	public void send(byte[] data){
		switch (CurrState){
		case CLOSED:
			m_tcp.ReturnMsg(ReturnType.SEND, handle, ReturnStatus.CONN_NOT_EXIST, -1, null);
			break;	
		case LISTEN:
			//TODO
			System.out.println("TODO");
			break;
		case SYN_SENT:
		case SYN_RCVD:
			SendBuffer.add(data);
			break;
		case ESTABLISHED:
		case CLOSE_WAIT:
			SendBuffer.add(data);
			m_tcp.ReturnMsg(ReturnType.SEND, handle, ReturnStatus.OK, 0, null);
			//if (SND_NXT < SND_UNA + SND_WND){
			while (SendBuffer.size()>0){
				seg = new TCPSegment(src_port, dest_port);
				seg.data = SendBuffer.poll();
				seg.SequenceNumber = SND_NXT;
				seg.AcknowledgmentNumber = RCV_NXT;
				seg.setACK();
				sendSEG(seg);
				SND_NXT += seg.data.length;
			}
			break;
		case FIN_WAIT_1:
		case FIN_WAIT_2:
		case CLOSING:
		case TIME_WAIT:
		case LAST_ACK:
		default:
			m_tcp.ReturnMsg(ReturnType.SEND, handle, ReturnStatus.CONN_CLOSING, -1, null);
			break;
		}
	}
	
	//Receive
	public void receive(){
		switch (CurrState){
		case CLOSED:
			m_tcp.ReturnMsg(ReturnType.RECEIVE, handle, ReturnStatus.CONN_NOT_EXIST, -1, null);
			break;
		case LISTEN:
		case SYN_SENT:
		case SYN_RCVD:
			Receive++;
			break;
		case ESTABLISHED:
		case FIN_WAIT_1:
		case FIN_WAIT_2:
		case CLOSE_WAIT:
			if (ReceiveBuffer.size()>0){
				m_tcp.ReturnMsg(ReturnType.RECEIVE, handle, ReturnStatus.OK, 0, ReceiveBuffer.poll());
			}else{
				Receive++;
			}
			break;
		case CLOSING:
		case TIME_WAIT:
		case LAST_ACK:
		default:
			m_tcp.ReturnMsg(ReturnType.RECEIVE, handle, ReturnStatus.CONN_CLOSING, -1, null);
			break;
		}
	}
	
	//Close
	public void close(){
		switch (CurrState){
		case CLOSED:
			m_tcp.ReturnMsg(ReturnType.CLOSE, handle, ReturnStatus.CONN_NOT_EXIST, -1, null);
			break;
		case LISTEN:
		case SYN_SENT:
			if (Receive>0) m_tcp.ReturnMsg(ReturnType.RECEIVE, handle, ReturnStatus.CONN_CLOSING, -1, null);	
			changeState(State.CLOSED);
			break;
		case SYN_RCVD:
			if (SendBuffer.size()>0){
				Close++;
			}else{
				seg = new TCPSegment(src_port, dest_port);
				seg.SequenceNumber = SND_NXT;
				seg.setFIN();
				sendSEG(seg);
				changeState(State.FIN_WAIT_1);
			}
			break;
		case ESTABLISHED:
			while (SendBuffer.size()>0){
				seg = new TCPSegment(src_port, dest_port);
				seg.data = SendBuffer.poll();
				seg.SequenceNumber = SND_NXT;
				seg.AcknowledgmentNumber = RCV_NXT;
				seg.setACK();
				sendSEG(seg);
				SND_NXT += seg.data.length;
			}
			seg = new TCPSegment(src_port, dest_port);
			seg.SequenceNumber = SND_NXT;
			seg.setFIN();
			sendSEG(seg);
			changeState(State.FIN_WAIT_1);
			break;
		case CLOSE_WAIT:
			while (SendBuffer.size()>0){
				seg = new TCPSegment(src_port, dest_port);
				seg.data = SendBuffer.poll();
				seg.SequenceNumber = SND_NXT;
				seg.AcknowledgmentNumber = RCV_NXT;
				seg.setACK();
				sendSEG(seg);
				SND_NXT += seg.data.length;
			}
			seg = new TCPSegment(src_port, dest_port);
			seg.SequenceNumber = SND_NXT;
			seg.setFIN();
			sendSEG(seg);
			changeState(State.LAST_ACK);
			break;
		default:
			m_tcp.ReturnMsg(ReturnType.CLOSE, handle, ReturnStatus.CONN_CLOSING, -1, null);
			break;
		}
	}
	
	//Abort
	public void abort(){
		switch (CurrState){
		case CLOSED:
			m_tcp.ReturnMsg(ReturnType.ABORT, handle, ReturnStatus.CONN_NOT_EXIST, -1, null);
			break;
		case SYN_RCVD:
		case ESTABLISHED:
		case FIN_WAIT_1:
		case FIN_WAIT_2:
		case CLOSE_WAIT:
			seg = new TCPSegment(src_port, dest_port);
			seg.SequenceNumber = SND_NXT;
			seg.setRST();
			sendSEG(seg);
		case LISTEN:
		case SYN_SENT:
			if (Receive>0) m_tcp.ReturnMsg(ReturnType.RECEIVE, handle, ReturnStatus.CONN_CLOSING, -1, null);
		case CLOSING:
		case TIME_WAIT:
		case LAST_ACK:
			m_tcp.ReturnMsg(ReturnType.ABORT, handle, ReturnStatus.OK, 0, null);
			changeState(State.CLOSED);
			break;
		}
	}
	
	//Segment Arrives
	public void segmentArrives(){
		
	}
	
	private void sendSEG(TCPSegment seg) {
		//time retransmit
		if (!seg.getRST()) {
			RexmtQueue.add(seg);
			wait(REXMT, RTO);
		}
		m_tcp.sendSeg(seg, src_ip, dest_ip);
		
	}
	
	//Rexmt Timeout
	private void rexmtTimeout(){
		switch (CurrState){
		case ESTABLISHED:
			//TODO
		case LISTEN:
		case SYN_SENT:
		case SYN_RCVD:
		case FIN_WAIT_1:
		case CLOSE_WAIT:
		case CLOSING:
		case LAST_ACK:
			m_tcp.sendSeg(RexmtQueue.peek(), src_ip, dest_ip);
			//calcRTO();
			wait(REXMT, RTO);
			break;
		default:
		
		}
	}
	

	@Override
	protected void interruptHandle(int signal, InterruptParam param) {
		switch (signal) {
		case REXMT:
			this.resetInterrupt(REXMT);
			rexmtTimeout();
			break;
		}
		
	}
	
}
