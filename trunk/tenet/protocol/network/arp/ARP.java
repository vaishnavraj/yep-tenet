package tenet.protocol.network.arp;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.TreeMap;

import tenet.core.Simulator;
import tenet.node.INode;
import tenet.protocol.datalink.IDataLinkLayer;
import tenet.protocol.datalink.IDataLinkLayer.*;
import tenet.protocol.IProtocol;
import tenet.protocol.datalink.MediumAddress;
import tenet.protocol.interrupt.InterruptObject;
import tenet.protocol.interrupt.InterruptParam;
import tenet.protocol.datalink.FrameParamStruct;
import tenet.protocol.network.INetworkLayer;
import tenet.protocol.datalink.SEther.SimpleEthernetDatalink;
import tenet.util.ByteLib;
import tenet.util.pattern.IParam;
import tenet.util.pattern.cbc.Command;
import tenet.util.pattern.cbc.IReceiver;
import tenet.util.pattern.serviceclient.IClient;
import tenet.util.pattern.serviceclient.IRegistryableService;
import tenet.util.pattern.serviceclient.IService;


//The MAC conflict should throw the 0xE0000500 to the upper layer.
public class ARP extends InterruptObject implements INetworkLayer,
		IRegistryableService<Integer> {

	public class Send extends Command {

		FrameParamStruct m_frame;
		ARP m_obj;

		protected Send(double m_time, IReceiver m_recv, FrameParamStruct frame,
				ARP obj) {
			super(m_time, m_recv);
			m_frame = frame;
			m_obj = obj;
		}

		@Override
		public IParam execute() {
			m_obj.wait(IDataLinkLayer.INT_FRAME_TRANSMIT, Double.NaN);
			((IDataLinkLayer) m_recv).transmitFrame(m_frame);
			return null;
		}
	}

	public class Receive extends Command {

		FrameParamStruct m_frame;
		ARP m_obj;

		protected Receive(double m_time, IReceiver m_recv, FrameParamStruct frame,
				ARP obj) {
			super(m_time, m_recv);
			m_frame = frame;
			m_obj = obj;
		}

		@Override
		public IParam execute() {
			m_obj.wait(IDataLinkLayer.INT_FRAME_RECEIVE, Double.NaN);
			((IDataLinkLayer) m_recv).receiveFrame(m_frame);
			return null;
		}
	}
	
	static boolean initBroadcast =false;
	
	SimpleEthernetDatalink  datalink;
	INode m_node;
	
	Integer UniqueID = 0x0806;
	double m_delay = 0.001;
	
	public LinkedList<FrameParamStruct> m_receive_link = new LinkedList<FrameParamStruct>();
	public LinkedList<FrameParamStruct> m_send_link = new LinkedList<FrameParamStruct>();
	
	protected IClient<Integer> m_network_layers;
	
	public TreeMap<Integer,MediumAddress> ArpTable;
	
	public ARP() {
		//The initialization
		ArpTable = new TreeMap<Integer,MediumAddress>();
		//this.wait(IDataLinkLayer.INT_FRAME_RECEIVE, Double.NaN);
	}
	

	
	public void send(MediumAddress dest, int HTYPE, byte[] hardwareAddr, int PTYPE, byte[] protocolAddr) {
		//The basic function that you can send a arp package to the others.
		//SEther's HTYPE is 1, IPv4's PTYPE is 0x0800 as default
		//Some different PTYPE will appear in the verification
		MediumAddress desMac = MediumAddress.fromBytes(hardwareAddr);	
		ArpFrame arpframe = new ArpFrame();
		arpframe.send(PTYPE, desMac, protocolAddr, datalink.getUniqueID(), m_node.getAddress(m_network_layers) );
		FrameParamStruct frame = new FrameParamStruct(dest, datalink.getUniqueID(), this.getUniqueID(), arpframe.toByte());
		Simulator.Schedule(new Send(Simulator.GetTime()+m_delay,(IReceiver) this.datalink,frame,this));
	}
	
	public static final MediumAddress BROADCAST_MA = MediumAddress.MAC_ALLONE;//new MediumAddress("FF:FF:FF:FF:FF:FF");
	
	
	public byte[] getHardwareAddress(int PTYPE, byte[] protocolAddr) {
		//The mapping you have maintained will be used by some other protocol.
		//The higher layer will use it to get the mac.
		if (ArpTable.containsKey(ByteLib.bytesToInt(protocolAddr,0))){
			return ArpTable.get(ByteLib.bytesToInt(protocolAddr,0)).toBytes();
		}else{
			ArpFrame arpframe = new ArpFrame();
			//System.out.println(protocolAddr);
			//System.out.println(m_network_layers);
			arpframe.request(PTYPE, protocolAddr,datalink.getUniqueID(),m_node.getAddress(m_network_layers));
			FrameParamStruct frame = new FrameParamStruct(BROADCAST_MA, datalink.getUniqueID(), this.getUniqueID(), arpframe.toByte());
			Simulator.Schedule(new Send(Simulator.GetTime()+m_delay,(IReceiver) this.datalink,frame,this));
			
			return null;
		}

	}

	@Override
	protected void interruptHandle(int signal, InterruptParam param) {
		// This is the most important function in ARP.
		// Ths signals may be caused by the IDataLinkLayer and yourself.
		// IDataLinkLayer.INT_FRAME_RECEIVE & IDataLinkLayer.INT_FRAME_TRANSMIT are the important signals. 
		switch (signal) {
		case IDataLinkLayer.INT_INTERFACE_UP:
			wait(IDataLinkLayer.INT_INTERFACE_DOWN, Double.NaN);
			this.resetInterrupt(IDataLinkLayer.INT_INTERFACE_UP);
			Receive();
			if (initBroadcast){
				if (m_network_layers != null){
					//System.out.println("broadcast");
					ArpFrame arpframe = new ArpFrame();
					arpframe.request(m_network_layers.getUniqueID(), m_node.getAddress(m_network_layers),datalink.getUniqueID(),m_node.getAddress(m_network_layers));
					FrameParamStruct frame = new FrameParamStruct(BROADCAST_MA, datalink.getUniqueID(), this.getUniqueID(), arpframe.toByte());
					Simulator.Schedule(new Send(Simulator.GetTime(),(IReceiver) this.datalink,frame,this));
				}
			}
			break;
		case IDataLinkLayer.INT_INTERFACE_DOWN:
			wait(IDataLinkLayer.INT_INTERFACE_UP, Double.NaN);
			this.resetInterrupt(IDataLinkLayer.INT_INTERFACE_DOWN);
			this.resetInterrupt(IDataLinkLayer.INT_FRAME_RECEIVE);
			//m_receive_link.clear();
			//m_send_link.clear();
			break;
		case IDataLinkLayer.INT_FRAME_RECEIVE:
			//System.out.println("got "+((ReceiveParam)param).status);
			if (((ReceiveParam)param).status == ReceiveStatus.receiveOK){
				FrameParamStruct frame = ((ReceiveParam)param).frame;
				if (frame.typeParam == this.getUniqueID()){
					//System.out.println(frame.dataParam.length);
					ArpFrame arpframe = new ArpFrame(frame.dataParam);
					//System.out.println("got arp "+arpframe.SenderHardwareAddress+" "+arpframe.TargetProtocolAddress.length+" "+m_node.getAddress(m_network_layers).length);
					if (arpframe.SenderHardwareAddress.equals(datalink.getUniqueID()) && !issame(arpframe.SenderProtocolAddress, m_node.getAddress(m_network_layers))){
						// mac collision
						if (m_network_layers instanceof InterruptObject){
							((InterruptObject)m_network_layers).delayInterrupt(0xE0000500, null, 0.0);
						}
					}
					
					if (ArpTable.containsKey(ByteLib.bytesToInt(arpframe.SenderProtocolAddress,0))){
						ArpTable.put(ByteLib.bytesToInt(arpframe.SenderProtocolAddress,0), arpframe.SenderHardwareAddress);
					}
					if (issame(arpframe.TargetProtocolAddress,m_node.getAddress(m_network_layers))){
						ArpTable.put(ByteLib.bytesToInt(arpframe.SenderProtocolAddress,0), arpframe.SenderHardwareAddress);
						if (arpframe.OPER[1]==0x01){
						//send the arpframe
						//System.out.println("send arp"+arpframe.SenderHardwareAddress);
						send(arpframe.SenderHardwareAddress, ByteLib.bytesToInt(arpframe.HTYPE,0), arpframe.SenderHardwareAddress.toBytes(), ByteLib.bytesToInt(arpframe.PTYPE,0), arpframe.SenderProtocolAddress);
						}
					}
					/*
					if (arpframe.OPER[1]==0x02){
						//do with the collision and update the arp table 
						if (ArpTable.containsKey(ByteLib.bytesToInt(arpframe.SenderProtocolAddress,0))){
							
							if (m_network_layers instanceof InterruptObject){
								((InterruptObject)m_network_layers).delayInterrupt(0xE0000500, null, 0.0);
							}
							
						}else{
							ArpTable.put(ByteLib.bytesToInt(arpframe.SenderProtocolAddress,0), arpframe.SenderHardwareAddress);
						}
					}
					*/
				}
			}
			Receive();
			break;
		case IDataLinkLayer.INT_FRAME_TRANSMIT:
			IDataLinkLayer.TransmitStatus tstatus = ((IDataLinkLayer.TransmitParam) param).status;
			switch (tstatus) {
			case transmitCollision:
				m_send_link
						.addLast(((IDataLinkLayer.TransmitParam) param).frame);
				wait(IDataLinkLayer.INT_FRAME_TRANSMIT_READY, Double.NaN);
				break;
			}
			break;
		case IDataLinkLayer.INT_FRAME_TRANSMIT_READY:
			if(!this.m_send_link.isEmpty())
			Simulator.getInstance().schedule(
					new Send(Simulator.getInstance().getTime(),
							(IReceiver) this.datalink, this.m_send_link
									.pollFirst(), this));
			break;
		}
	}

	private boolean issame(byte[] a, byte[] b){
		if (a==null || b==null ||(a.length!=b.length)) return false;
		int length = a.length;
		for (int i=0;i<length;i++){
			//System.out.print(a[i]+" "+b[i]+":");
			if (a[i]!=b[i]) return false;
		}
		//System.out.println();
		return true;
	}
	@Override
	public void dump() {
		// This is used for the debugging. You may use it to export something iteratively.	

	}

	@Override
	public String getIdentify() {
		return null;
	}

	@Override
	public String getName() {
		return null;		
	}

	@Override
	public void registryClient(IClient<Integer> client) {
		//A higher layer client may transfer it.
		m_network_layers = client;
		client.attachTo(this);
	}

	@Override
	public void unregistryClient(IClient<Integer> client) {
		if (m_network_layers == client) m_network_layers = null;
		client.detachFrom(this);
	}

	@Override
	public void unregistryClient(Integer id) {
		if (m_network_layers.getUniqueID().equals(id)){
			m_network_layers.detachFrom(this);
			m_network_layers = null;
		}
	}

	
	@Override
	public void attachTo(IService service) {
		if (service instanceof SimpleEthernetDatalink){
			datalink = (SimpleEthernetDatalink)service;
			wait(IDataLinkLayer.INT_INTERFACE_UP, Double.NaN);
			//Receive();
		}
		if (service instanceof INode)
			m_node = (INode) service;
	}

	@Override
	public void detachFrom(IService service) {
		if (service instanceof SimpleEthernetDatalink)
			datalink = null;
		if (service instanceof INode)
			m_node = null;
	}
	
	@Override
	public Integer getUniqueID() {
		// return the Protocol ID.
		return UniqueID;
	}

	@Override
	public void setUniqueID(Integer id) {
		// Set the Protocol ID.
		UniqueID = id;
	}
	
	private void Receive(){
		//receive
		FrameParamStruct frame=new FrameParamStruct(MediumAddress.MAC_ZERO, this.datalink.getUniqueID(), this.getUniqueID(), new byte[0]);
		this.wait(IDataLinkLayer.INT_FRAME_RECEIVE, Double.NaN);
		this.datalink.receiveFrame(frame);
	}


}

class ArpFrame {
	// the ARP frame data struct. 
	
	/**
	 * Ô´MAC
	 */
	public MediumAddress SenderHardwareAddress;
	/**
	 * Ä¿±êMAC
	 */
	public MediumAddress TargetHardwareAddress;
	
	public byte[] SenderProtocolAddress;
	
	public byte[]  TargetProtocolAddress;
	
	public byte[] OPER;
	
	public byte HLEN,PLEN;
	
	public byte[] HTYPE;
	
	public byte[] PTYPE;
	
	public ArpFrame(){
		HLEN = 6;
		HTYPE = new byte[2];
		PTYPE = new byte[2];
		OPER = new byte[2];
		HTYPE[0] = 0x00;
		HTYPE[1] = 0x01;
	}
	
	public ArpFrame(byte[] framedata){
		//TODO check Length OK and checksum
		HLEN = 6;
		PLEN = framedata[5];
		HTYPE = new byte[2];
		PTYPE = new byte[2];
		OPER = new byte[2];
		HTYPE[0] = 0x00;
		HTYPE[1] = 0x01;
		PTYPE = Arrays.copyOfRange(framedata, 2, 4);
		SenderHardwareAddress = MediumAddress.fromBytes(Arrays.copyOfRange(framedata, 8, 14));
		SenderProtocolAddress = Arrays.copyOfRange(framedata, 14, 14+PLEN);
		TargetHardwareAddress = MediumAddress.fromBytes(Arrays.copyOfRange(framedata, 18, 24));
		TargetProtocolAddress = Arrays.copyOfRange(framedata, 24, 24+PLEN);
		OPER[0]=framedata[6];
		OPER[1]=framedata[7];
	}
	
	public void request(int ptype, byte[] targetIP , MediumAddress senderMac , byte[] senderIP){
		PLEN = (byte)(senderIP.length & 0xFF);
		ByteLib.bytesFromInt(PTYPE, 0, ptype);
		TargetProtocolAddress = targetIP;
		TargetHardwareAddress = MediumAddress.MAC_ZERO;
		SenderHardwareAddress = senderMac;
		SenderProtocolAddress = senderIP;
		//System.out.println(senderIP);
		OPER[0]=0x00;
		OPER[1]=0x01;
	}
	
	public void send(int ptype, MediumAddress targetMac, byte[] targetIP, MediumAddress senderMac, byte[] senderIP ){
		PLEN = (byte)(senderIP.length & 0xFF);
		ByteLib.bytesFromInt(PTYPE, 0, ptype);
		TargetProtocolAddress = targetIP;
		TargetHardwareAddress = targetMac;
		SenderHardwareAddress = senderMac;
		SenderProtocolAddress = senderIP;
		OPER[0]=0x00;
		OPER[1]=0x02;
	}
	
	public byte[] toByte(){
		byte[] ret = new byte[28];
		ret[0]=HTYPE[0];
		ret[1]=HTYPE[1];
		ret[2]=PTYPE[0];
		ret[3]=PTYPE[1];
		ret[4]=HLEN;
		ret[5]=PLEN;
		ret[6]=OPER[0];
		ret[7]=OPER[1];
		System.arraycopy(SenderHardwareAddress.toBytes(), 0, ret, 8, 6);
		System.arraycopy(SenderProtocolAddress, 0, ret, 14, PLEN);
		System.arraycopy(TargetHardwareAddress.toBytes(), 0, ret, 18, 6);
		System.arraycopy(TargetProtocolAddress, 0, ret, 24, PLEN);
		return ret;
	}
	

}