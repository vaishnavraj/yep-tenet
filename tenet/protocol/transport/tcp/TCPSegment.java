package tenet.protocol.transport.tcp;

import java.util.Arrays;

import tenet.util.ByteLib;

public class TCPSegment {
	
	//4 bytes
	public int SourcePort;
	public int DestinationPort;
	
	//4 bytes
	public int SequenceNumber;
	
	//4byte
	public int AcknowledgmentNumber;
	
	//4 bytes
	public int SYN;
	public int ACK;
	public int RST;
	public int FIN;
	public int WindowSize;
	
	//2 bytes
	public int CheckSum;
	
	public byte[] data;
	
	public TCPSegment(int SourcePort, int DestinationPort){
		this.SourcePort = SourcePort;
		this.DestinationPort = DestinationPort;
		SYN = 0;
		ACK = 0;
		RST = 0;
		FIN = 0;
	}
	
	public TCPSegment(byte[] Segment){
		//TODO check Length OK and checksum
		SourcePort = ByteLib.bytesToInt(Arrays.copyOfRange(Segment, 0, 2), 0);
		DestinationPort = ByteLib.bytesToInt(Arrays.copyOfRange(Segment, 2, 4), 0);
		
		SequenceNumber = ByteLib.bytesToInt(Arrays.copyOfRange(Segment, 4, 8), 0);
		
		AcknowledgmentNumber = ByteLib.bytesToInt(Arrays.copyOfRange(Segment, 8, 12), 0);
		
		int control = ByteLib.bytesToInt(Arrays.copyOfRange(Segment, 12, 14), 0);
		FIN = control % 2;
		SYN = (control/2) %2;
		RST = (control/4) %2;
		ACK = (control/8) %2;
		
		WindowSize = ByteLib.bytesToInt(Arrays.copyOfRange(Segment, 14, 16), 0);
		
		CheckSum = ByteLib.bytesToInt(Arrays.copyOfRange(Segment, 16, 18), 0);
		if (Segment.length>18) data = Arrays.copyOfRange(Segment, 18, Segment.length);
		
	}
	
	public void setFIN(){
		FIN = 1;
	}
	public void setSYN(){
		SYN = 1;
	}
	public void setRST(){
		RST = 1;
	}
	public void setACK(){
		ACK = 1;
	}
	public boolean getFIN(){
		return (FIN == 1);
	}
	public boolean getSYN(){
		return (SYN == 1);
	}
	public boolean getRST(){
		return (RST == 1);
	}
	public boolean getACK(){
		return (ACK == 1);
	}
	
	public int getdataLength(){
		if (data!=null) return data.length;
		return 0;
	}
	
	public byte[] toBytes(){
		byte[] segment;
		if (data !=null) segment = new byte[data.length+18];
		else segment = new byte[18];
		
		byte[] SrcPort = new byte[2];
		ByteLib.bytesFromInt(SrcPort, 0, SourcePort);
		System.arraycopy(SrcPort, 0, segment, 0, 2);
		
		byte[] destPort = new byte[2];
		ByteLib.bytesFromInt(destPort, 0, DestinationPort);
		System.arraycopy(destPort, 0, segment, 2, 2);
		
		byte[] SEQ = new byte[4];
		ByteLib.bytesFromInt(SEQ, 0, SequenceNumber);
		System.arraycopy(SEQ, 0, segment, 4, 4);
		
		byte[] ACKN = new byte[4];
		ByteLib.bytesFromInt(ACKN, 0, AcknowledgmentNumber);
		System.arraycopy(ACKN, 0, segment, 8, 4);
		
		int control = FIN + SYN*2 + RST*4 + ACK*8;
		byte[] CTL = new byte[2];
		ByteLib.bytesFromInt(CTL, 0, control);
		System.arraycopy(CTL, 0, segment, 12, 2);
		
		byte[] WND = new byte[2];
		ByteLib.bytesFromInt(WND, 0, WindowSize);
		System.arraycopy(WND, 0, segment, 14, 2);
		
		//TODO calc check sum
		byte[] CheckSum = new byte[2];
		ByteLib.bytesFromInt(CheckSum, 0, this.CheckSum);
		System.arraycopy(CheckSum, 0, segment, 16, 2);
		
		if (data != null) System.arraycopy(data, 0, segment, 18, data.length);
		
		return segment;
	}
	
	
	
	
}

