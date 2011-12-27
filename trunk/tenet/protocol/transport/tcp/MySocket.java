package tenet.protocol.transport.tcp;

import java.util.LinkedList;
import java.util.Random;

import tenet.core.Simulator;
import tenet.protocol.interrupt.InterruptObject;
import tenet.protocol.interrupt.InterruptParam;
import tenet.protocol.transport.tcp.TCPProtocol.ReturnStatus;
import tenet.protocol.transport.tcp.TCPProtocol.ReturnType;
import tenet.util.ByteLib;

public class MySocket extends InterruptObject {
	public int handle;
	public MyTCP m_tcp;
	
	public Integer src_ip;
	public int src_port;
	
	public Integer dest_ip;
	public int dest_port;
	
	public State CurrState = State.CLOSED;
	public State PrevState;
	public double RTO = 3.0;
	public double SRTT = -1;
	public double DRTT = -1;
	public int CWND;
	public int MSS;
	public int SSthresh;
	public double MSL = 30.0;
	
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
	//public int ExpBoff; use myRTO
	
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
	
	private void fork(TCPSegment recv, Integer srcip){
		MySocket newsocket = new MySocket(m_tcp.nextHandle, m_tcp);
		
		newsocket.src_ip = this.src_ip;
		newsocket.src_port = this.src_port;
		newsocket.dest_ip = srcip;
		newsocket.dest_port = recv.SourcePort;
		
		
		newsocket.Receive = this.Receive;
		this.Receive = 0;
		newsocket.SendBuffer = this.SendBuffer;
		this.SendBuffer = new  LinkedList<byte[]>();
		
		newsocket.forked(recv);
		
		m_tcp.handleSocket.put(m_tcp.nextHandle, newsocket);
		m_tcp.ReturnMsg(ReturnType.LISTEN, handle, ReturnStatus.OK, m_tcp.nextHandle, null);
		m_tcp.nextHandle++;
		
	}
	
	public void bind(int ip, int port) {
		src_ip = ip;
		src_port = port;
	}
	
	public void forked(TCPSegment recv){
		//System.out.println("forked");
		RCV_NXT = recv.SequenceNumber+1;
		IRS = recv.SequenceNumber;
		ISS = random.nextInt();
		seg = new TCPSegment(src_port, dest_port);
		seg.SequenceNumber = ISS;
		seg.AcknowledgmentNumber = RCV_NXT;
		seg.setSYN();
		seg.setACK();
		sendSEG(seg, dest_ip);
		SND_NXT = ISS+1;
		SND_UNA = ISS;
		changeState(State.SYN_RCVD);
	}
	
	
	private void changeState(State newState){
		System.out.println(handle+" :"+CurrState+"->"+newState);
		if (newState == State.CLOSED){
			//if (m_tcp.portHandle.get(src_port) == this) m_tcp.portHandle.remove(src_port);
			//m_tcp.handleSocket.remove(this.handle);
			dest_ip = null;
			SendBuffer.clear();
			ReceiveBuffer.clear();
			OORCV.clear();
			RexmtQueue.clear();
			this.resetInterrupt(REXMT);
			Receive = 0;
			Close = 0;
			m_tcp.ReturnMsg(ReturnType.CLOSE, handle, ReturnStatus.OK, 0, null);
		}
		if (newState == State.ESTABLISHED){
			while (SendBuffer.size()>0){
				seg = new TCPSegment(src_port, dest_port);
				seg.data = SendBuffer.poll();
				seg.SequenceNumber = SND_NXT;
				seg.AcknowledgmentNumber = RCV_NXT;
				seg.setACK();
				sendSEG(seg, dest_ip);
				SND_NXT += seg.getdataLength();
			}
			if (Close>0) this.close();
		}
		if (newState == State.LISTEN){
			dest_ip = null;
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
		switch (CurrState){
		case CLOSED:
			this.dest_ip = dstIP;
			this.dest_port = dstPort;
			ISS = random.nextInt();
			seg = new TCPSegment(src_port, dest_port);
			seg.SequenceNumber = ISS;
			seg.setSYN();
			sendSEG(seg, dest_ip);
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
			//TODO if (SND_NXT < SND_UNA + SND_WND){ //control
			while (SendBuffer.size()>0){
				seg = new TCPSegment(src_port, dest_port);
				seg.data = SendBuffer.poll();
				seg.SequenceNumber = SND_NXT;
				seg.AcknowledgmentNumber = RCV_NXT;
				seg.setACK();
				sendSEG(seg, dest_ip);
				SND_NXT += seg.getdataLength();
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
			//TODO MSL
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
				sendSEG(seg, dest_ip);
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
				sendSEG(seg, dest_ip);
				SND_NXT += seg.getdataLength();
			}
			seg = new TCPSegment(src_port, dest_port);
			seg.SequenceNumber = SND_NXT;
			seg.setFIN();
			sendSEG(seg, dest_ip);
			SND_NXT++;
			changeState(State.FIN_WAIT_1);
			break;
		case CLOSE_WAIT:
			while (SendBuffer.size()>0){
				seg = new TCPSegment(src_port, dest_port);
				seg.data = SendBuffer.poll();
				seg.SequenceNumber = SND_NXT;
				seg.AcknowledgmentNumber = RCV_NXT;
				seg.setACK();
				sendSEG(seg, dest_ip);
				SND_NXT += seg.getdataLength();
			}
			seg = new TCPSegment(src_port, dest_port);
			seg.SequenceNumber = SND_NXT;
			seg.setFIN();
			sendSEG(seg, dest_ip);
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
			sendSEG(seg, dest_ip);
		case LISTEN:
		case SYN_SENT:
			if (Receive>0) m_tcp.ReturnMsg(ReturnType.RECEIVE, handle, ReturnStatus.CONN_RESET, -1, null);
		case CLOSING:
		case TIME_WAIT:
		case LAST_ACK:
			m_tcp.ReturnMsg(ReturnType.ABORT, handle, ReturnStatus.OK, 0, null);
			changeState(State.CLOSED);
			break;
		}
	}
	
	//Segment Arrives
	public void segmentArrives(TCPSegment recv, Integer srcip){
		///*
		System.out.println(handle+" :recive from IP:"+srcip+" port:"+recv.SourcePort+" SEQ:"
				+recv.SequenceNumber+" ACKN:"+recv.AcknowledgmentNumber
				+" ACK:"+recv.getACK()+" RST:"+recv.getRST()+" SYN:"+recv.getSYN()+" FIN:"+recv.getFIN()
				+" DATA:"+recv.data);
		//*/
		switch (CurrState){
		case CLOSED:
			if (recv.getRST()) return;
			seg = new TCPSegment(src_port, recv.SourcePort);
			if (recv.getACK()){
				seg.SequenceNumber = recv.AcknowledgmentNumber;
				seg.setRST();
			}else{
				seg.SequenceNumber = recv.SequenceNumber + seg.getdataLength();
				seg.AcknowledgmentNumber = recv.SequenceNumber + seg.getdataLength();
				if (recv.getFIN() || recv.getSYN()) seg.AcknowledgmentNumber++;
				seg.setRST();
				seg.setACK();
			}
			sendSEG(seg, srcip);
			break;
		case LISTEN:
			if (!recv.getRST()){
				if (recv.getACK()){
					seg = new TCPSegment(src_port, recv.SourcePort);
					seg.SequenceNumber = recv.AcknowledgmentNumber;
					seg.setRST();
					sendSEG(seg, srcip);
				}else{
					if (recv.getSYN()){
						fork(recv, srcip);
					}
				}
			}
			break;
		case SYN_SENT:
			if (recv.getACK()){
				if (recv.AcknowledgmentNumber <= ISS || recv.AcknowledgmentNumber > SND_NXT
						|| !(recv.AcknowledgmentNumber >= SND_UNA && recv.AcknowledgmentNumber <= SND_NXT)){
					seg = new TCPSegment(src_port, dest_port);
					seg.SequenceNumber = recv.AcknowledgmentNumber;
					seg.setRST();
					sendSEG(seg, dest_ip);
				}else{
					if (recv.getRST()){
						m_tcp.ReturnMsg(ReturnType.CONNECT, handle, ReturnStatus.CONN_RESET, -1, null);
						changeState(State.CLOSED);
					}else{
						if (recv.getSYN()){
							RCV_NXT = recv.SequenceNumber + 1;
							IRS = recv.SequenceNumber;
							SND_UNA = recv.AcknowledgmentNumber;
							removeAckedSeg(SND_UNA);
							if (SND_UNA > ISS){
								seg = new TCPSegment(src_port, dest_port);
								seg.SequenceNumber = SND_NXT;
								seg.AcknowledgmentNumber = RCV_NXT;
								seg.setACK();
								sendSEG(seg, dest_ip);
								changeState(State.ESTABLISHED);
							}else{
								seg = new TCPSegment(src_port, dest_port);
								seg.SequenceNumber = ISS;
								seg.AcknowledgmentNumber = RCV_NXT;
								seg.setACK();
								sendSEG(seg, dest_ip);
								changeState(State.SYN_RCVD);
							}
						}
					}
				}
			}else{
				if (!recv.getRST() && recv.getSYN()){
					RCV_NXT = recv.SequenceNumber + 1;
					IRS = recv.SequenceNumber;
					seg = new TCPSegment(src_port, dest_port);
					seg.SequenceNumber = ISS;
					seg.AcknowledgmentNumber = RCV_NXT;
					seg.setACK();
					sendSEG(seg, dest_ip);
					changeState(State.SYN_RCVD);
				}
			}
			break;
		case SYN_RCVD:
			if (checkSeg(recv)){
				if(recv.getRST()){
					if (PrevState == State.LISTEN){
						RexmtQueue.clear();
						changeState(State.LISTEN);
					}else{
						m_tcp.ReturnMsg(ReturnType.CONNECT, handle, ReturnStatus.CONN_RESET, -1, null);
						changeState(State.CLOSED);
					}
				}else{
					if (recv.getSYN()){
						seg = new TCPSegment(src_port, dest_port);
						seg.SequenceNumber = SND_NXT;
						seg.setRST();
						sendSEG(seg, dest_ip);
						if (SendBuffer.size()>0)
							m_tcp.ReturnMsg(ReturnType.SEND, handle, ReturnStatus.CONN_RESET, -1, null);
						if (Receive>0)
							m_tcp.ReturnMsg(ReturnType.RECEIVE, handle, ReturnStatus.CONN_RESET, -1, null);
						changeState(State.CLOSED);
					}else{
						sr2(recv);
					}
				}
			}else{
				if (!recv.getRST()){
					seg = new TCPSegment(src_port, dest_port);
					seg.SequenceNumber = SND_NXT;
					seg.AcknowledgmentNumber = RCV_NXT;
					seg.setACK();
					sendSEG(seg, dest_ip);
				}
			}
			break;
		case ESTABLISHED:
		case FIN_WAIT_1:
		case FIN_WAIT_2:
		case CLOSE_WAIT:
			if (checkSeg(recv)){
				if(recv.getRST()){
					if (SendBuffer.size()>0)
						m_tcp.ReturnMsg(ReturnType.SEND, handle, ReturnStatus.CONN_RESET, -1, null);
					if (Receive>0)
						m_tcp.ReturnMsg(ReturnType.RECEIVE, handle, ReturnStatus.CONN_RESET, -1, null);
					changeState(State.CLOSED);
				}else{
					if (recv.getSYN()){
						seg = new TCPSegment(src_port, dest_port);
						seg.SequenceNumber = SND_NXT;
						seg.setRST();
						sendSEG(seg, dest_ip);
						if (SendBuffer.size()>0)
							m_tcp.ReturnMsg(ReturnType.SEND, handle, ReturnStatus.CONN_RESET, -1, null);
						if (Receive>0)
							m_tcp.ReturnMsg(ReturnType.RECEIVE, handle, ReturnStatus.CONN_RESET, -1, null);
						changeState(State.CLOSED);
					}else{
						switch (CurrState){
						case FIN_WAIT_1:
							fw1(recv);
							break;
						case FIN_WAIT_2:
							fwt1(recv);
							break;
						case CLOSE_WAIT:
							cw1(recv);
							break;
						case ESTABLISHED:
							es2(recv);
							break;
						}
					}
				}
			}else{
				if (!recv.getRST()){
					seg = new TCPSegment(src_port, dest_port);
					seg.SequenceNumber = SND_NXT;
					seg.AcknowledgmentNumber = RCV_NXT;
					seg.setACK();
					sendSEG(seg, dest_ip);
				}
			}
			break;
		case CLOSING:
			if (checkSeg(recv)){
				if(recv.getRST()){
					changeState(State.CLOSED);
				}else{
					if (recv.getSYN()){
						seg = new TCPSegment(src_port, dest_port);
						seg.SequenceNumber = SND_NXT;
						seg.setRST();
						sendSEG(seg, dest_ip);
						if (SendBuffer.size()>0)
							m_tcp.ReturnMsg(ReturnType.SEND, handle, ReturnStatus.CONN_RESET, -1, null);
						if (Receive>0)
							m_tcp.ReturnMsg(ReturnType.RECEIVE, handle, ReturnStatus.CONN_RESET, -1, null);
						changeState(State.CLOSED);
					}else{
						if (recv.getACK()){
							removeAckedSeg(recv.AcknowledgmentNumber);
							if (SND_UNA<recv.AcknowledgmentNumber && recv.AcknowledgmentNumber <=SND_NXT 
								&& FINacked(recv)){
								this.resetInterrupt(REXMT);
								this.resetInterrupt(TIMEWAIT);
								wait(TIMEWAIT, 2*MSL);
								changeState(State.TIME_WAIT);
							}
						}	
					}
				}
			}else{
				if (!recv.getRST()){
					seg = new TCPSegment(src_port, dest_port);
					seg.SequenceNumber = SND_NXT;
					seg.AcknowledgmentNumber = RCV_NXT;
					seg.setACK();
					sendSEG(seg, dest_ip);
				}
			}
			break;
		case TIME_WAIT:
			if (checkSeg(recv)){
				if(recv.getRST()){
					changeState(State.CLOSED);
				}else{
					if (recv.getSYN()){
						seg = new TCPSegment(src_port, dest_port);
						seg.SequenceNumber = SND_NXT;
						seg.setRST();
						sendSEG(seg, dest_ip);
						if (SendBuffer.size()>0)
							m_tcp.ReturnMsg(ReturnType.SEND, handle, ReturnStatus.CONN_RESET, -1, null);
						if (Receive>0)
							m_tcp.ReturnMsg(ReturnType.RECEIVE, handle, ReturnStatus.CONN_RESET, -1, null);
						changeState(State.CLOSED);
					}else{
						if (recv.getACK() 
								&& SND_UNA<recv.AcknowledgmentNumber && recv.AcknowledgmentNumber <=SND_NXT 
								&& recv.getFIN()){
							FINbit(recv);
							this.resetInterrupt(REXMT);
							this.resetInterrupt(TIMEWAIT);
							wait(TIMEWAIT, 2*MSL);
							
						}	
					}
				}
			}else{
				if (!recv.getRST()){
					seg = new TCPSegment(src_port, dest_port);
					seg.SequenceNumber = SND_NXT;
					seg.AcknowledgmentNumber = RCV_NXT;
					seg.setACK();
					sendSEG(seg, dest_ip);
				}
			}
			break;
		case LAST_ACK:
			if (checkSeg(recv)){
				if(recv.getRST()){
					changeState(State.CLOSED);
				}else{
					if (recv.getSYN()){
						seg = new TCPSegment(src_port, dest_port);
						seg.SequenceNumber = SND_NXT;
						seg.setRST();
						sendSEG(seg, dest_ip);
						if (SendBuffer.size()>0)
							m_tcp.ReturnMsg(ReturnType.SEND, handle, ReturnStatus.CONN_RESET, -1, null);
						if (Receive>0)
							m_tcp.ReturnMsg(ReturnType.RECEIVE, handle, ReturnStatus.CONN_RESET, -1, null);
						changeState(State.CLOSED);
					}else{
						if (recv.getACK()){
							removeAckedSeg(recv.AcknowledgmentNumber);
							if (SND_UNA<recv.AcknowledgmentNumber && recv.AcknowledgmentNumber <=SND_NXT 
								&& FINacked(recv)){
							changeState(State.CLOSED);
							}
						}	
					}
				}
			}else{
				if (!recv.getRST()){
					seg = new TCPSegment(src_port, dest_port);
					seg.SequenceNumber = SND_NXT;
					seg.AcknowledgmentNumber = RCV_NXT;
					seg.setACK();
					sendSEG(seg, dest_ip);
				}
			}
			break;
		}
	}
	
	private boolean checkSeg(TCPSegment recv){
		//TODO wnd control
		if (recv.getdataLength()==0){
			if (RCV_NXT <= recv.SequenceNumber) 
				return true;
			else return false;
		}else{
			if (RCV_NXT < recv.SequenceNumber || RCV_NXT < recv.SequenceNumber+recv.getdataLength())
				return true;
			else return false;
		}
		//if (recv.SequenceNumber != RCV_NXT) return false;
	}
	
	private boolean FINacked(TCPSegment recv){
		for (int i=0;i<RexmtQueue.size();i++){
			if (RexmtQueue.get(i).getFIN()) {
				//System.out.println("FIN_WAIT_1 "+RexmtQueue.get(i).SequenceNumber);
				return false;
			}
		}
		return true;
	}
	
	private void FINbit(TCPSegment recv){
		RCV_NXT = recv.SequenceNumber +1;
		seg = new TCPSegment(src_port, dest_port);
		seg.SequenceNumber = SND_NXT;
		seg.AcknowledgmentNumber = RCV_NXT;
		seg.setACK();
		sendSEG(seg, dest_ip);
		if (Receive>0)
			m_tcp.ReturnMsg(ReturnType.RECEIVE, handle, ReturnStatus.CONN_CLOSING, -1, null);
		
	}
	
	private void  WinUpdate(TCPSegment recv){
		//TODO control
	}
	
	private void receiveData(TCPSegment recv){
		if (recv.SequenceNumber == RCV_NXT){
			if (Receive>0 && recv.data!=null){
				m_tcp.ReturnMsg(ReturnType.RECEIVE, handle, ReturnStatus.OK, 0, recv.data);
				Receive--;
			}else 
				if (recv.data!=null) ReceiveBuffer.add(recv.data);
			//System.out.println("datalength "+recv.getdataLength());
			RCV_NXT = recv.SequenceNumber + recv.getdataLength();
			seg = new TCPSegment(src_port, dest_port);
			seg.SequenceNumber = SND_NXT;
			seg.AcknowledgmentNumber = RCV_NXT;
			seg.setACK();
			sendSEG(seg, dest_ip);
		}else{
			//TODO now i am lazy so i use go back N.... it should use stander TCP using OO RCV
		}
	}
	
	private void fw1(TCPSegment recv){
		if (!recv.getACK()){
			if (recv.getFIN()){
				FINbit(recv);
				changeState(State.CLOSING);
			}
			return;
		}
		//System.out.println("removeAckedSeg"+SND_UNA+" "+recv.AcknowledgmentNumber+" "+SND_NXT);
		if (SND_UNA<recv.AcknowledgmentNumber && recv.AcknowledgmentNumber <=SND_NXT){
			WinUpdate(recv);
			//SND_WND = Math.min(CWND, recv.WindowSize);
			SND_UNA = recv.AcknowledgmentNumber;
			removeAckedSeg(recv.AcknowledgmentNumber);
		}
		//fw2
		receiveData(recv);
		
		if (FINacked(recv)){
			if (recv.getFIN()){
				FINbit(recv);
				this.resetInterrupt(REXMT);
				this.resetInterrupt(TIMEWAIT);
				wait(TIMEWAIT, 2*MSL);
				changeState(State.TIME_WAIT);
			}else{
				changeState(State.FIN_WAIT_2);
			}
		}else{
			//System.out.println("FIN_WAIT_1");
			if (recv.getFIN()){
				FINbit(recv);
				changeState(State.CLOSING);
			}
		}
	}
	
	private void fwt1(TCPSegment recv){
		if (!recv.getACK()){
			if (recv.getFIN()){
				FINbit(recv);
				this.resetInterrupt(REXMT);
				this.resetInterrupt(TIMEWAIT);
				//System.out.println("TIMEWAIT"+2*MSL);
				wait(TIMEWAIT, 2*MSL);
				changeState(State.TIME_WAIT);
			}
			return;
		}
		if (SND_UNA<recv.AcknowledgmentNumber && recv.AcknowledgmentNumber <=SND_NXT){
			WinUpdate(recv);
			//SND_WND = Math.min(CWND, recv.WindowSize);
			SND_UNA = recv.AcknowledgmentNumber;
			removeAckedSeg(recv.AcknowledgmentNumber);
		}
		//fwt2
		receiveData(recv);
		if (recv.getFIN()){
			FINbit(recv);
			this.resetInterrupt(REXMT);
			this.resetInterrupt(TIMEWAIT);
			wait(TIMEWAIT, 2*MSL);
			changeState(State.TIME_WAIT);
		}
	}
	
	private void cw1(TCPSegment recv){
		if (recv.getACK() && SND_UNA<recv.AcknowledgmentNumber && recv.AcknowledgmentNumber <=SND_NXT){
			WinUpdate(recv);
			//SND_WND = Math.min(CWND, recv.WindowSize);
			SND_UNA = recv.AcknowledgmentNumber;
			myRTO = 1;
			removeAckedSeg(recv.AcknowledgmentNumber);
			if (recv.getFIN()) FINbit(recv);
		}
	}
	
	private void sr2(TCPSegment recv){
		if (recv.getACK()){
			if (SND_UNA<=recv.AcknowledgmentNumber && recv.AcknowledgmentNumber <= SND_NXT){
				removeAckedSeg(recv.AcknowledgmentNumber);
				changeState(State.ESTABLISHED);
			}else{
				if (recv.getFIN()){
					FINbit(recv);
					changeState(State.CLOSE_WAIT);
				}else{
					seg = new TCPSegment(src_port, dest_port);
					seg.SequenceNumber = SND_NXT;
					seg.setRST();
					sendSEG(seg, dest_ip);
					if (SendBuffer.size()>0)
						m_tcp.ReturnMsg(ReturnType.SEND, handle, ReturnStatus.CONN_RESET, -1, null);
					if (Receive>0)
						m_tcp.ReturnMsg(ReturnType.RECEIVE, handle, ReturnStatus.CONN_RESET, -1, null);
					changeState(State.CLOSED);
				}
			}
		}
	}
	
	private void es2(TCPSegment recv){
		if (!recv.getACK()) {
			if (recv.getFIN()){
				FINbit(recv);
				changeState(State.CLOSE_WAIT);
				seg = new TCPSegment(src_port, dest_port);
				seg.SequenceNumber = SND_NXT;
				seg.AcknowledgmentNumber = RCV_NXT;
				seg.setFIN();
				SND_NXT++;
				sendSEG(seg, dest_ip);
				changeState(State.LAST_ACK);
			}
			return;
		}
		if (SND_UNA<=recv.AcknowledgmentNumber && recv.AcknowledgmentNumber <= SND_NXT){
			WinUpdate(recv);
			//SND_WND = Math.min(CWND, recv.WindowSize);
			SND_UNA = recv.AcknowledgmentNumber;
			//myRTO = 1;
			removeAckedSeg(recv.AcknowledgmentNumber);
			es3(recv);
		}else{
			if (recv.AcknowledgmentNumber == SND_UNA){
				dACK++;
				switch (dACK){
				case 1:
					es3(recv);
					break;
				case 2:
					this.resetInterrupt(REXMT);
					rexmt();
					//TODO control
					es3(recv);
					break;
				default:
					es4(recv);
					break;
				}
			}
		}
	}
	
	private void es3(TCPSegment recv){
		receiveData(recv);
		if (recv.getFIN()){
			FINbit(recv);
			changeState(State.CLOSE_WAIT);
		}
	}
	
	private void es4(TCPSegment recv){
		while (SendBuffer.size()>0){
			seg = new TCPSegment(src_port, dest_port);
			seg.data = SendBuffer.poll();
			seg.SequenceNumber = SND_NXT;
			seg.AcknowledgmentNumber = RCV_NXT;
			seg.setACK();
			sendSEG(seg, dest_ip);
			SND_NXT += seg.getdataLength();
		}
	}
	
	public double transmitTime;
	public int myRTO;
	public int transmitCount;
	
	private void sendSEG(TCPSegment seg, Integer dest_ip) {
		//time retransmit
		if (!seg.getRST()) {
			if (RexmtQueue.size()==0){
				transmitTime = Simulator.GetTime();
				transmitCount = 0;
				myRTO = 1;
				wait(REXMT, RTO);
			}
			RexmtQueue.add(seg);
		}
		m_tcp.sendSeg(seg, src_ip, dest_ip);
		
	}
	
	//Rexmt Timeout
	private void rexmt(){
		//System.out.println(handle+" :rexmt"+Simulator.GetTime());
		this.resetInterrupt(REXMT);
		switch (CurrState){
		case ESTABLISHED:
			
			transmitCount++;
			//if (transmitCount >6) System.out.println("connecting seems lose");
			m_tcp.sendSeg(RexmtQueue.peek(), src_ip, dest_ip);
			if (myRTO <64) myRTO *= 2;
			wait(REXMT, myRTO*RTO);
			//TODO control
			break;
		case LISTEN:
		case SYN_SENT:
		case SYN_RCVD:
		case FIN_WAIT_1:
		case CLOSE_WAIT:
		case CLOSING:
		case LAST_ACK:
			transmitCount++;
			//if (transmitCount >6) System.out.println("connecting seems lose");
			m_tcp.sendSeg(RexmtQueue.peek(), src_ip, dest_ip);
			wait(REXMT, RTO);
			break;
		default:
			break;
		}
	}
	
	private void removeAckedSeg(int acknum){
		
		if (RexmtQueue.size()>0 && RexmtQueue.peek().SequenceNumber == acknum){
			if (transmitCount == 0){
				if (SRTT < 0){
					SRTT = (Simulator.GetTime()-transmitTime);
					DRTT = SRTT/2;
				}else{
					SRTT = 0.875*SRTT + 0.125*(Simulator.GetTime()-transmitTime);
					DRTT = 0.75*DRTT + 0.25*Math.abs(Simulator.GetTime()-transmitTime);
				}
				RTO = SRTT + 4*DRTT;
			}
		}
		this.resetInterrupt(REXMT);
		while (RexmtQueue.size()>0 && RexmtQueue.peek().SequenceNumber < acknum ) RexmtQueue.remove();
		
		if (RexmtQueue.size()>0) {
			//System.out.println("removeAckedSeg "+RexmtQueue.peek().SequenceNumber);
			transmitTime = Simulator.GetTime();
			transmitCount = 1;
			myRTO = 1;
			//wait(REXMT, RTO);
		}
	}
	
	
	@Override
	protected void interruptHandle(int signal, InterruptParam param) {
		switch (signal) {
		case REXMT:
			rexmt();
			break;
		case TIMEWAIT:
			changeState(State.CLOSED);
			break;
		}
		
	}
	
}
