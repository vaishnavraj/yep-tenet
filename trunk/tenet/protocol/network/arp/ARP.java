package tenet.protocol.network.arp;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.TreeMap;

import org.knf.tenet.test.util.TestNetworkLayer;
import org.knf.tenet.test.util.TestNetworkLayer.Send;

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
			// TODO Auto-generated method stub
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
			// TODO Auto-generated method stub
			m_obj.wait(IDataLinkLayer.INT_FRAME_RECEIVE, Double.NaN);
			((IDataLinkLayer) m_recv).receiveFrame(m_frame);
			return null;
		}
	}
	
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
		this.wait(IDataLinkLayer.INT_FRAME_RECEIVE, Double.NaN);
	}
	

	
	public void send(int HTYPE, byte[] hardwareAddr, int PTYPE, byte[] protocolAddr) {
		//The basic function that you can send a arp package to the others.
		//SEther's HTYPE is 1, IPv4's PTYPE is 0x0800 as default
		//Some different PTYPE will appear in the verification
		MediumAddress desMac = MediumAddress.fromBytes(hardwareAddr);	
		ArpFrame arpframe = new ArpFrame();
		arpframe.send(desMac, protocolAddr, datalink.getUniqueID(), m_node.getAddress(m_network_layers) );
		FrameParamStruct frame = new FrameParamStruct(BROADCAST_MA, datalink.getUniqueID(), this.getUniqueID(), arpframe.toByte());
		Simulator.Schedule(new Send(Simulator.GetTime(),(IReceiver) this.datalink,frame,this));
	}
	
	public static final MediumAddress BROADCAST_MA = MediumAddress.ZERO_MA;//new MediumAddress("FF:FF:FF:FF:FF:FF");
	
	
	public byte[] getHardwareAddress(int PTYPE, byte[] protocolAddr) {
		//The mapping you have maintained will be used by some other protocol.
		//The higher layer will use it to get the mac.
		if (ArpTable.containsKey(ByteLib.bytesToInt(protocolAddr,0))){
			return ArpTable.get(ByteLib.bytesToInt(protocolAddr,0)).toBytes();
		}else{
			ArpFrame arpframe = new ArpFrame();
			//System.out.println(protocolAddr);
			//System.out.println(m_network_layers);
			arpframe.request(protocolAddr,datalink.getUniqueID(),m_node.getAddress(m_network_layers));
			FrameParamStruct frame = new FrameParamStruct(BROADCAST_MA, datalink.getUniqueID(), this.getUniqueID(), arpframe.toByte());
			Simulator.Schedule(new Send(Simulator.GetTime()+m_delay,(IReceiver) this.datalink,frame,this));
			//receive
			frame=new FrameParamStruct(MediumAddress.ZERO_MA, this.datalink.getUniqueID(), this.getUniqueID(), new byte[0]);
			this.wait(IDataLinkLayer.INT_FRAME_RECEIVE, Double.NaN);
			this.datalink.receiveFrame(frame);
			return null;
		}

	}

	@Override
	protected void interruptHandle(int signal, InterruptParam param) {
		// This is the most important function in ARP.
		// Ths signals may be caused by the IDataLinkLayer and yourself.
		// IDataLinkLayer.INT_FRAME_RECEIVE & IDataLinkLayer.INT_FRAME_TRANSMIT are the important signals. 
		switch (signal) {
		case IDataLinkLayer.INT_FRAME_RECEIVE:
			if (((ReceiveParam)param).status == ReceiveStatus.receiveOK){
				FrameParamStruct frame = ((ReceiveParam)param).frame;
				if (frame.typeParam == this.getUniqueID()){
					ArpFrame arpframe = new ArpFrame(frame.dataParam);
					if (arpframe.OPER[1]==0x01){
						//send the arpframe
						send(ByteLib.bytesToInt(arpframe.HTYPE,0), arpframe.SenderHardwareAddress.toBytes(), ByteLib.bytesToInt(arpframe.PTYPE,0), arpframe.SenderProtocolAddress);
					}
					if (arpframe.OPER[1]==0x02){
						//do with the collision and update the arp table 
						if (ArpTable.containsKey(ByteLib.bytesToInt(arpframe.SenderProtocolAddress,0))){
							//TODO may cause mac collision
						}else{
							ArpTable.put(ByteLib.bytesToInt(arpframe.SenderProtocolAddress,0), arpframe.SenderHardwareAddress);
						}
					}
				}
			}
			break;
		case IDataLinkLayer.INT_FRAME_TRANSMIT:
			break;
		}
	}

	@Override
	public void dump() {
		// TODO This is used for the debugging. You may use it to export something iteratively.	

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
		// TODO You will attachTo a lower layer in the Protocol Stack. 
		if (service instanceof SimpleEthernetDatalink){
			datalink = (SimpleEthernetDatalink)service;
			wait(IDataLinkLayer.INT_INTERFACE_UP, Double.NaN);
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
		// TODO return the Protocol ID.
		return UniqueID;
	}

	@Override
	public void setUniqueID(Integer id) {
		// TODO Set the Protocol ID.
		UniqueID = id;
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
		PLEN = 4;
		HTYPE = new byte[2];
		PTYPE = new byte[2];
		OPER = new byte[2];
		HTYPE[0] = 0x00;
		HTYPE[1] = 0x01;
		PTYPE[0] = 0x08;
		PTYPE[1] = 0x00;
	}
	
	public ArpFrame(byte[] framedata){
		HLEN = 6;
		PLEN = 4;
		HTYPE = new byte[2];
		PTYPE = new byte[2];
		HTYPE[0] = 0x00;
		HTYPE[1] = 0x01;
		PTYPE[0] = 0x08;
		PTYPE[1] = 0x00;
		TargetHardwareAddress = MediumAddress.fromBytes(Arrays.copyOfRange(framedata, 8, 14));
		TargetProtocolAddress = Arrays.copyOfRange(framedata, 14, 18);
		SenderHardwareAddress = MediumAddress.fromBytes(Arrays.copyOfRange(framedata, 18, 24));
		SenderProtocolAddress = Arrays.copyOfRange(framedata, 24, 28);
		OPER[0]=framedata[6];
		OPER[1]=framedata[7];
	}
	
	public void request(byte[] targetIP , MediumAddress senderMac , byte[] senderIP){
		TargetProtocolAddress = targetIP;
		TargetHardwareAddress = MediumAddress.ZERO_MA;
		SenderHardwareAddress = senderMac;
		SenderProtocolAddress = senderIP;
		//System.out.println(senderIP);
		OPER[0]=0x00;
		OPER[1]=0x01;
	}
	
	public void send(MediumAddress targetMac, byte[] targetIP, MediumAddress senderMac, byte[] senderIP ){
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
		System.arraycopy(SenderProtocolAddress, 0, ret, 14, SenderProtocolAddress.length);
		System.arraycopy(TargetHardwareAddress.toBytes(), 0, ret, 18, 6);
		System.arraycopy(TargetProtocolAddress, 0, ret, 24, TargetProtocolAddress.length);
		return ret;
	}
	

}