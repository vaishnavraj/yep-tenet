package tenet.protocol.network.ipv4;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;

import tenet.core.Simulator;
import tenet.node.INode;
import tenet.node.MyNode;
import tenet.protocol.datalink.FrameParamStruct;
import tenet.protocol.datalink.IDataLinkLayer;
import tenet.protocol.datalink.MediumAddress;
import tenet.protocol.interrupt.InterruptObject;
import tenet.protocol.interrupt.InterruptParam;
import tenet.protocol.network.arp.ARP;
import tenet.protocol.physics.Link;
import tenet.protocol.rip.RIPProtocol;
import tenet.util.ByteLib;
import tenet.util.pattern.IParam;
import tenet.util.pattern.cbc.Command;
import tenet.util.pattern.cbc.IReceiver;
import tenet.util.pattern.serviceclient.IClient;
import tenet.util.pattern.serviceclient.IService;

public class IPv4 extends InterruptObject implements IPProtocol {

	public class Send extends Command {

		FrameParamStruct m_frame;
		IPv4 m_obj;

		protected Send(double m_time, IReceiver m_recv, FrameParamStruct frame,
				IPv4 obj) {
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
	

	MyNode m_node;
	
	IDataLinkLayer m_dllayer;
	
	public ARP m_arp;
	
	protected IClient<Integer> m_transport_layers;
	
	protected RIPProtocol m_rip;
	
	byte[] m_paddr = new byte[4];
	
	int mask; 

	double m_delay = 0.001;

	LinkedList<FrameParamStruct> m_receive_link = new LinkedList<FrameParamStruct>();

	LinkedList<FrameParamStruct> m_send_link = new LinkedList<FrameParamStruct>();
	
	public boolean m_state = false;
	
	public boolean m_linkup = false;
	

	public IPv4(String ip, Integer mask){
		try{
		String[] v=ip.split("\\.");
		if (v.length!=4) System.out.println("seems wrong in IPv4 format");
		for(int i=0;i<v.length;++i)
			m_paddr[3-i]=ByteLib.byteFromUnsigned(Integer.valueOf(v[i]),0);
		//System.out.println(getIntegerAddress());
		this.mask = mask;
		}catch(Exception e){
			//System.out.println(e.getMessage());
			e.printStackTrace();
		}
	}
	public IPv4(int ip, int mask){
		ByteLib.bytesFromInt(m_paddr, 0, ip);
		this.mask = mask;
	}
	
	
	public Integer getIntegerAddress(){
		return ByteLib.bytesToInt(m_paddr, 0);
	}
	public int getMask(){
		return mask;
	}
	
	public static Integer transToInteger(byte[] m_paddr){
		return ByteLib.bytesToInt(m_paddr, 0);
	}
	
	public static byte[] transToBytes(Integer addr){
		byte[] m_paddr = new byte[4];
		ByteLib.bytesFromInt(m_paddr, 0, addr);
		return m_paddr;
	}
	
	public static String IPtoString(int ip) {
		byte[] b=new byte[4];
		ByteLib.bytesFromInt(b, 0, ip);
		byte temp;
		temp = b[0];
		b[0] = b[3];
		b[3] = temp;
		temp = b[1];
		b[1] = b[2];
		b[2] = temp;
		return byteToString(b,false);
	}
	
	private static String byteToString(byte[] content,boolean hex) {
		StringBuilder sb=new StringBuilder();
		for(int i=0;i<content.length;++i) {
			sb.append(i==0?"":".");
			if(hex)sb.append(ByteLib.byteToHex(content[i]));
			sb.append(""+ByteLib.byteToUnsigned(content[i],0));
		}
		return sb.toString();
	}
	
	@Override
	public void disable() {
		if (!m_state)
			return;
		this.m_state = false;
		this.interrupt(IDataLinkLayer.INT_INTERFACE_DOWN, null);
		resetInterrupt(IDataLinkLayer.INT_INTERFACE_DOWN);
		resetInterrupt(IDataLinkLayer.INT_INTERFACE_UP);
		if(m_dllayer!=null)
		this.m_dllayer.disable();
		
	}

	@Override
	public void enable() {
		if (this.m_state)
			return;
		wait(IDataLinkLayer.INT_INTERFACE_UP, Double.NaN);
		this.m_state = true;
		if(m_dllayer!=null)
		this.m_dllayer.enable();

	}

	@Override
	public boolean isEnable() {
		return m_state;
	}

	@Override
	public void dump() {

	}

	@Override
	public String getIdentify() {
		StringBuilder sb=new StringBuilder();
		for(int i=0;i<this.m_paddr.length;++i) {
			if(i!=0)sb.append(":");
			sb.append(m_paddr[i]);			
		}
		sb.append("@");
		sb.append(getName());
		sb.append("#");
		sb.append(this.m_dllayer.getIdentify());
		return sb.toString();
	}

	@Override
	public String getName() {
		return "IPv4";
	}

	@Override
	public void attachTo(IService<?> service) {
		if(service instanceof IDataLinkLayer) {
			m_dllayer = (IDataLinkLayer) service;
			wait(IDataLinkLayer.INT_INTERFACE_UP, Double.NaN);
		}
		if(service instanceof MyNode) {
			m_node = (MyNode)service;
			((MyNode)service).setAddress(this, m_paddr);
			m_node.registryIpv4(this);
		}
		if(service instanceof ARP) {
			this.m_arp=(ARP) service;
		}

	}

	@Override
	public void detachFrom(IService<?> service) {
		if(service == m_dllayer) {
			m_dllayer = null;
			this.resetInterrupt(IDataLinkLayer.INT_INTERFACE_UP);
		}
		if(service == m_node) {
			m_node = null;
			((MyNode)service).removeProtocal(this);
		}
		if(service == m_arp) {
			this.m_arp =null;
		}
		
	}

	@Override
	public Integer getUniqueID() {
		return protocolID;
	}

	@Override
	public void setUniqueID(Integer id) {
	}

	@Override
	public void registryClient(IClient<Integer> client) {
		if (client instanceof RIPProtocol)
			m_rip = (RIPProtocol)client;
		else
			m_transport_layers = client;
		client.attachTo(this);
	}

	@Override
	public void unregistryClient(Integer id) {
		if (m_transport_layers.getUniqueID().equals(id)){
			m_transport_layers.detachFrom(this);
			m_transport_layers = null;
		}

	}

	@Override
	public void unregistryClient(IClient<Integer> client) {
		if (m_transport_layers == client) m_transport_layers = null;
		client.detachFrom(this);

	}


	@Override
	public int totalLink() {
		// TODO Auto-generated method stub
		// For dynamic routing
		return 0;
	}

	@Override
	public void clearRouteTable() {
		m_node.RouteTable.clear();
	}

	@Override
	public void clearDynamicRoutingTable() {
		// TODO Auto-generated method stub
		// For dynamic routing
	}

	@Override
	public void clearStaticRoutingTable() {
		// TODO Auto-generated method stub
		// For dynamic routing
	}

	@Override
	public void addRoute(Integer destIPAddr, Integer mask, Integer linkNumber,
			Integer nextNodeIP, Integer metric) {
		if (linkNumber == null) System.out.println("-.-R linkNumber wrong");
		m_node.RouteTable.add(new RouteEntry(destIPAddr, mask, linkNumber, nextNodeIP, metric, 0));
		Collections.sort(m_node.RouteTable);
	}

	@Override
	public void addDefaultRoute(Integer linkNumber, Integer nextRouterIP) {
		P("addDefaultRoute "+IPtoString(nextRouterIP));
		if (linkNumber == null) System.out.println("-.-D linkNumber wrong");
		m_node.DefaultRoute = new RouteEntry(0, 0, linkNumber, nextRouterIP, 0, 0);
	}

	@Override
	public Integer getLinkNumber(Integer ipaddr) {
		if (m_node!= null){
			int linknum = m_node.getIPv4Linknum(ipaddr);
			//System.out.println("getLinkNumber "+ipaddr+" "+linknum);
			if (linknum<0) return null;
			return linknum;
		}
		return null;
	}

	@Override
	public void deleteDefaultRoute() {
		m_node.DefaultRoute = null;
	}

	@Override
	public void setMTU(int mtu) {
		// TODO Auto-generated method stub
		//Do What?
	}
	
	protected void interruptHandle(int signal, InterruptParam param) {
		switch (signal) {
		case IDataLinkLayer.INT_INTERFACE_UP:
			this.linkUp();
			Receive();
			break;
		case IDataLinkLayer.INT_INTERFACE_DOWN:
			this.linkDown();
			this.resetInterrupt(IDataLinkLayer.INT_FRAME_RECEIVE);
			m_receive_link.clear();
			m_send_link.clear();
			break;
		case IDataLinkLayer.INT_FRAME_TRANSMIT:
			IDataLinkLayer.TransmitStatus tstatus = ((IDataLinkLayer.TransmitParam) param).status;
			switch (tstatus) {
			case transmitOK:
				break;
			case transmitCollision:
				m_send_link
						.addLast(((IDataLinkLayer.TransmitParam) param).frame);
				wait(IDataLinkLayer.INT_FRAME_TRANSMIT_READY, Double.NaN);
				break;
			case transmitError:
			case dataLinkOff:
				P("transmitError or dataLinkOff");
				break;	
			}
			break;
		case IDataLinkLayer.INT_FRAME_RECEIVE:
			IDataLinkLayer.ReceiveStatus rstatus = ((IDataLinkLayer.ReceiveParam) param).status;
			switch (rstatus) {
			case receiveOK:
				//System.out.println("receiveOK pkt");
				IPv4Packet packet = new IPv4Packet(((IDataLinkLayer.ReceiveParam) param).frame.dataParam);
				handleReceiveOK(packet, false);
				break;
			case receiveCollision:
				break;
			case receiveError:
			case dataLinkOff:
				break;	
			}
			Receive();
			break;
		case IDataLinkLayer.INT_FRAME_TRANSMIT_READY:
			if(!this.m_send_link.isEmpty())
			Simulator.getInstance().schedule(
					new Send(Simulator.getInstance().getTime(),
							(IReceiver) this.m_dllayer, this.m_send_link
									.pollFirst(), this));
			break;			
		}
	}
	
	private List<FragmentBuffer> receiveBuffer = new LinkedList<FragmentBuffer>();
	public void handleReceiveOK(IPv4Packet Packet, boolean newPkt){
		Integer destIPAddr = Packet.destIPAddr;
		P("handleReceiveOK "+(destIPAddr.intValue() == this.getIntegerAddress().intValue())+" portocal id:"+ByteLib.byteToUnsigned(Packet.protocol,0));
		
		if (destIPAddr.intValue() == this.getIntegerAddress().intValue()||(destIPAddr.intValue() == broadcastIP.intValue())){
			if (m_transport_layers!=null && Packet.getProtocal()== m_transport_layers.getUniqueID() ){
				FragmentBuffer bufferID = new FragmentBuffer(Packet.srcIPAddr, Packet.destIPAddr, Packet.getProtocal(), Packet.getIndentifier());
				//Reassembly
				//TODO check timeout and flush the buffer
				//System.out.println(IPtoString(this.getIntegerAddress())+" Reassembly");
				if (!receiveBuffer.contains(bufferID)) {
					receiveBuffer.add(bufferID);
					//System.out.println("new bufferID");
				}
				int index = receiveBuffer.indexOf(bufferID);
				if (index < 0) System.out.println("seems error in Reassembly");
				byte[] data = receiveBuffer.get(index).arrive(Packet);
				if (data != null){
					receiveBuffer.remove(index);
					IPReceiveParam param = new IPReceiveParam(
							IPReceiveParam.ReceiveType.OK,
							Packet.destIPAddr,
							Packet.srcIPAddr,
							this.getUniqueID(),
							data);
					((InterruptObject) m_transport_layers).delayInterrupt(recPacketSignal, param, 0);
				}
			}
			if (m_rip!=null && Packet.getProtocal()== RIPProtocol.protocolID ){
				IPReceiveParam param = new IPReceiveParam(
						IPReceiveParam.ReceiveType.OK,
						Packet.destIPAddr,
						Packet.srcIPAddr,
						this.getUniqueID(),
						Packet.data);
				m_rip.delayInterrupt(recPacketSignal, param, 0);
			}
			return;
		}
		
		IPv4 sendIPv4 = getRoute(destIPAddr);
		//P(""+getRoutenextIPAddr);
		if (sendIPv4 == null){
			//System.out.println("can't find route rule (sendIPv4)");
			return;
		}
		if (getRoutenextIPAddr == null) {
			//System.out.println("can't find route rule (getRoutenextIPAddr)");
			return;
		}
		if (sendIPv4 != this) {
			Packet.TTL--;
			if (Packet.TTL == 0) return;
			//TODO recalc checksum
		}else 
			if (!newPkt) return;
		//P(""+(sendIPv4==null));
		if (sendIPv4.inSubnet(destIPAddr)) getRoutenextIPAddr = destIPAddr;
		sendIPv4.sendPacketLocal(Packet, getRoutenextIPAddr);
	}
	
	public void sendPacketLocal(IPv4Packet Packet, Integer nextIPAddr){
		P("!"+IPtoString(this.getIntegerAddress())+" send frame to "+IPtoString(nextIPAddr));
		boolean flag=false;
		if (nextIPAddr.intValue() == broadcastIP.intValue()) flag = true;
		MediumAddress dest_mac=null;
		if(!flag) {			
			dest_mac=getAddr(nextIPAddr);
			if(dest_mac==null) {
				//System.out.println("no arp entry");
				//return;
				dest_mac=MediumAddress.MAC_ALLONE;
			}
		}
		else
			dest_mac=MediumAddress.MAC_ALLONE;
		//System.out.println("sendPacketLocal RIP pkt");
		List<IPv4Packet> packetsList = Packet.fragment(m_dllayer.getMTU());
		for (int i=0;i<packetsList.size();i++){
			IPv4Packet packet = packetsList.get(i);
			FrameParamStruct frame=new FrameParamStruct(dest_mac, this.m_dllayer.getUniqueID(), this.getUniqueID(), packet.toBytes());
			Simulator.Schedule(new Send(Simulator.GetTime(),(IReceiver) this.m_dllayer,frame,this));

		}
	}
	
	public MediumAddress getAddr(Integer targetAddr) {
		// TODO Auto-generated method stub
		byte[] target_addr =new byte[4];
		ByteLib.bytesFromInt(target_addr, 0, targetAddr);
		byte[] ret=m_arp.getHardwareAddress(getUniqueID(), target_addr);
		if(ret==null) return null;
		return MediumAddress.fromBytes(ret);
	}
	
	private void Receive(){
		//receive
		FrameParamStruct frame=new FrameParamStruct(MediumAddress.MAC_ZERO, this.m_dllayer.getUniqueID(), this.getUniqueID(), new byte[0]);
		this.wait(IDataLinkLayer.INT_FRAME_RECEIVE, Double.NaN);
		this.m_dllayer.receiveFrame(frame);
	}
	
	public boolean isLinkup(){
		return m_linkup;
	}
	
	protected void linkDown() {
		m_linkup = false;
		wait(IDataLinkLayer.INT_INTERFACE_UP, Double.NaN);
		resetInterrupt(IDataLinkLayer.INT_INTERFACE_DOWN);
	}

	protected void linkUp() {
		m_linkup = true;
		wait(IDataLinkLayer.INT_INTERFACE_DOWN, Double.NaN);
		resetInterrupt(IDataLinkLayer.INT_INTERFACE_UP);
	}
	
	public boolean inSubnet(Integer destIPAddr){
		if (mask == 0) return true;
		Integer thisAddr = this.getIntegerAddress();
		if (destIPAddr.intValue() == broadcastIP.intValue()) return true;
		int a = (int) (((int) (thisAddr.intValue() >>> 24 ) & 0xFF) << 0)
				| (((int) (thisAddr.intValue() >>> 16 ) & 0xFF) << 8)
				| (((int) (thisAddr.intValue() >>> 8 ) & 0xFF) << 16)
				| (((int) (thisAddr.intValue()) & 0xFF) << 24);
		int b = (int) (((int) (destIPAddr.intValue() >>> 24 ) & 0xFF) << 0)
				| (((int) (destIPAddr.intValue() >>> 16 ) & 0xFF) << 8)
				| (((int) (destIPAddr.intValue() >>> 8 ) & 0xFF) << 16)
				| (((int) (destIPAddr.intValue()) & 0xFF) << 24);
		//P("IPinSubnet "+thisAddr+" "+destIPAddr+" "+a+" "+b+" "+(a >>> (32-mask))+" "+(b >>> (32-mask)));
		if ((thisAddr.intValue() >>> (32-mask))==(destIPAddr.intValue() >>> (32-mask))) return true;
		//if ((thisAddr.intValue() >>> (32-mask))==(destIPAddr.intValue() >>> (32-mask))) return true;
		return false;
	}
	
	private Integer getRoutenextIPAddr;
	
	private IPv4 getRoute(Integer destIPAddr){
		IPv4 sendIPv4 = null;
		P(""+IPtoString(this.getIntegerAddress())+" ToGetRoute: "+IPtoString(destIPAddr));
		getRoutenextIPAddr = destIPAddr;
		if (inSubnet(destIPAddr)) 
			sendIPv4 = this;
		else {
			for (int i = 0; i<m_node.RouteTable.size();i++){
				P("check RouteTable Entry "+i);
				if (m_node.RouteTable.get(i).suitable(destIPAddr)){
					sendIPv4 = m_node.getIPv4ByLinknum(m_node.RouteTable.get(i).sendLinknum);
					getRoutenextIPAddr = m_node.RouteTable.get(i).nextAddress;
					break;
				}
			}
		}
		if (sendIPv4 == null && m_node.DefaultRoute != null) {
			P("DefaultRoute");
			//System.out.println(m_node.DefaultRoute.sendLinknum);	
			sendIPv4 = m_node.getIPv4ByLinknum(m_node.DefaultRoute.sendLinknum);
			getRoutenextIPAddr = m_node.DefaultRoute.nextAddress;
		}
		destIPAddr = getRoutenextIPAddr;
		P("getRouteResult: sendIPv4->"+sendIPv4+";getRoutenextIPAddr->"+IPtoString(getRoutenextIPAddr));
		return sendIPv4;
	}
	
	@Override
	public boolean canSend(Integer destIPAddr) {
		if (!m_state || !m_linkup) return false;
		return (getRoute(destIPAddr)!=null);
	}

	int nowID = 0;
	@Override
	public void sendPacket(byte[] data, Integer srcIPAddr, Integer destIPAddr,
			Integer clientProtocolId) {
		P("send "+IPtoString(srcIPAddr)+" "+IPtoString(destIPAddr)+" "+clientProtocolId);
		IPv4Packet packet = new IPv4Packet(nowID++ ,maxTimeToLive, clientProtocolId, srcIPAddr, destIPAddr, data);
		this.handleReceiveOK(packet, true);
	}

	@Override
	public boolean canSend() {
		return (m_state&&m_linkup);
	}

	@Override
	public void sendPacket(byte[] data,	Integer clientProtocolId) {
		IPv4Packet packet = new IPv4Packet(nowID++ ,maxTimeToLive, clientProtocolId, this.getIntegerAddress(), broadcastIP, data);
		this.sendPacketLocal(packet, broadcastIP);

	}
	
	private static void P(String dump){
		//System.out.println(dump);
	}

}

class FragmentBuffer {
	public Integer source;
	public Integer destination;
	public int protocol;
	public int identification;
	public int totalLength;
	public int receivedLenth;
	private List<IPv4Packet> receivedPkt;
	class PacketComparator implements Comparator<IPv4Packet>{
		public int compare(IPv4Packet p1, IPv4Packet p2){
			if (p1.getFragmentOffset() < p2.getFragmentOffset()) return -1;
			if (p1.getFragmentOffset() > p2.getFragmentOffset()) return 1;
			return 0;
		}
	}
	
	public FragmentBuffer(Integer source, Integer destination , int protocol, int identification){
		this.source = source;
		this.destination = destination;
		this.protocol = protocol;
		this.identification = identification;
		totalLength = 0;
		receivedLenth = 0;
		receivedPkt = new LinkedList<IPv4Packet>();
	}
	
	public byte[] arrive(IPv4Packet pkt){
		//System.out.println("arrive MF:"+pkt.MF+" Offset:"+pkt.getFragmentOffset()+" Indentifier:"+pkt.getIndentifier()+" "+totalLength+" "+receivedLenth);
		if (pkt.MF == 0){
			totalLength = pkt.getFragmentOffset()*8 + pkt.data.length;
			//System.out.println("totalLength to "+totalLength);
		}
		receivedPkt.add(pkt);
		receivedLenth += pkt.data.length;
		if (totalLength == receivedLenth){
			Collections.sort(receivedPkt, new PacketComparator());
			byte[] OKData = new byte[totalLength];
			for (int i=0;i<receivedPkt.size();i++){
				//System.out.println("arraycopy "+receivedPkt.get(i).getFragmentOffset()*8+" "+receivedPkt.get(i).data.length);
				System.arraycopy(receivedPkt.get(i).data, 0, OKData, receivedPkt.get(i).getFragmentOffset()*8, receivedPkt.get(i).data.length);
			}
			return OKData;
		}else return null;
	}
	
	public boolean equals(Object obuffer){
		FragmentBuffer buffer = (FragmentBuffer)obuffer;
		//System.out.println("equals?");
		
		if (source.intValue() == buffer.source.intValue() && destination.intValue() == buffer.destination.intValue() && protocol == buffer.protocol && identification == buffer.identification)
			return true;
		//System.out.println("not equals");
		return false;
	}
}

class IPv4Packet {

	//head 18 bytes
	//2 bytes
	public byte[] totalLength = new byte[2]; 

	//4 bytes
	public byte[] indentifier = new byte[2];
	public int MF;
	public byte[] fragmentOffset  = new byte[2];
	//4 bytes
	public byte TTL;
	public byte protocol;
	public byte[] headerChecksum = new byte[2];

	//4 bytes
	public Integer srcIPAddr;
	//4 bytes
	public Integer destIPAddr;

	public byte[] data;
	
	public IPv4Packet(int ID, int TTL, int protocol, Integer srcIPAddr, Integer destIPAddr, byte[] data){
		this(ID, 0, 0, TTL, protocol, srcIPAddr, destIPAddr, data);
	}
		
	
	public IPv4Packet(byte[] packet){
		//TODO check Length OK and checksum
		totalLength = Arrays.copyOfRange(packet, 0, 2);
		indentifier = Arrays.copyOfRange(packet, 2, 4);
		fragmentOffset = Arrays.copyOfRange(packet, 4, 6);
		MF = (ByteLib.byteToUnsigned(fragmentOffset[1],0)/32)%2;
		fragmentOffset[1] = ByteLib.byteFromUnsigned(fragmentOffset[1]%32,0);
		TTL = packet[6];
		protocol = packet[7];
		headerChecksum = Arrays.copyOfRange(packet, 8, 10);
		byte[] srcIP = Arrays.copyOfRange(packet, 10, 14);
		byte[] destIP = Arrays.copyOfRange(packet, 14, 18);
		srcIPAddr = ByteLib.bytesToInt(srcIP, 0);
		destIPAddr = ByteLib.bytesToInt(destIP, 0);
		
		int length = ByteLib.bytesToInt(totalLength, 0);
		if (length != packet.length) System.out.println("seems error at IPv4Packet");
		data = Arrays.copyOfRange(packet, 18, length);
		//System.out.println(data[0]+"~"+data[1]+"~"+data[2]);
	}
	
	public IPv4Packet(int ID, int MF, int fragmentOffset, int TTL, int protocol, Integer srcIPAddr, Integer destIPAddr, byte[] data){
		ByteLib.bytesFromInt(indentifier, 0, ID);
		this.MF = MF;
		ByteLib.bytesFromInt(this.fragmentOffset, 0, fragmentOffset);
		//System.out.println("IPv4Packet:" +this.getFragmentOffset());
		this.TTL = ByteLib.byteFromUnsigned(TTL,0);
		this.protocol = ByteLib.byteFromUnsigned(protocol,0);
		//System.out.println("portocalid: "+protocol);
		this.srcIPAddr = srcIPAddr;
		this.destIPAddr = destIPAddr;
		this.data = data;
		
		ByteLib.bytesFromInt(this.totalLength, 0, data.length + 18);
	}
	
	public int getFragmentOffset(){
		return ByteLib.bytesToInt(fragmentOffset, 0);
	}
	
	public int getProtocal(){
		return ByteLib.byteToUnsigned(protocol, 0);
	}
	
	public int getIndentifier(){
		return ByteLib.bytesToInt(indentifier, 0);
	}
	
	public List<IPv4Packet> fragment(int mtu){
		List<IPv4Packet> packetList = new LinkedList<IPv4Packet>();
		if (data.length+18 <= mtu) {
			packetList.add(this);
			return packetList;
		}

		if (data.length+18> mtu){
			int NFB = (mtu-18)/8;
			int myIndentifier = ByteLib.bytesToInt(indentifier, 0);
			int newFragmentOffset = ByteLib.bytesToInt(fragmentOffset,0);
			IPv4Packet newPacket = new IPv4Packet(
										myIndentifier,
										1, 
										newFragmentOffset,
										TTL,
										ByteLib.byteToUnsigned(protocol, 0),
										srcIPAddr,
										destIPAddr,
										Arrays.copyOfRange(data, 0, NFB*8));
			packetList.add(newPacket);
			IPv4Packet nextPacket = new IPv4Packet(
										myIndentifier,
										MF, 
										newFragmentOffset+NFB,
										TTL,
										ByteLib.byteToUnsigned(protocol, 0),
										srcIPAddr,
										destIPAddr,
										Arrays.copyOfRange(data, NFB*8, data.length));
			packetList.addAll(nextPacket.fragment(mtu));
		}
		return packetList;
		
	}
	
	public byte[] toBytes(){
		int length = data.length + 18;
		byte[] packet = new byte[length];
		
		System.arraycopy(totalLength, 0, packet, 0, 2);
		System.arraycopy(indentifier, 0, packet, 2, 2);
		fragmentOffset[1] = ByteLib.byteFromUnsigned((fragmentOffset[1]%32 + MF*32),0);
		System.arraycopy(fragmentOffset, 0, packet, 4, 2);
		packet[6] = TTL;
		packet[7] = protocol;
		//TODO calc this.headerChecksum =
		System.arraycopy(headerChecksum, 0, packet, 8, 2);
		
		byte[] srcIP = new byte[4];
		byte[] destIP = new byte[4];
		ByteLib.bytesFromInt(srcIP, 0, srcIPAddr);
		ByteLib.bytesFromInt(destIP, 0, destIPAddr);
		System.arraycopy(srcIP, 0, packet, 10, 4);
		System.arraycopy(destIP, 0, packet, 14, 4);	
		System.arraycopy(data, 0, packet, 18, data.length);	
		return packet;
	}
	

}

