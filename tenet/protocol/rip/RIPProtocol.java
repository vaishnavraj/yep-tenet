package tenet.protocol.rip;

import java.util.List;

import tenet.core.Simulator;
import tenet.node.INode;
import tenet.node.L3Switch;
import tenet.protocol.IProtocol;
import tenet.protocol.interrupt.InterruptObject;
import tenet.protocol.interrupt.InterruptParam;
import tenet.protocol.network.ipv4.IPProtocol;
import tenet.protocol.network.ipv4.IPReceiveParam;
import tenet.protocol.network.ipv4.IPv4;
import tenet.protocol.network.ipv4.RouteEntry;
import tenet.protocol.transport.tcp.TCPSegment;
import tenet.util.pattern.serviceclient.IClient;
import tenet.util.pattern.serviceclient.IService;

public class RIPProtocol extends InterruptObject implements IProtocol,
		IClient<Integer> {

	public static final Integer protocolID = 0xFD;
	
	public L3Switch m_node;
	public IPv4 m_IP;
	
	@Override
	public void dump() {

	}

	@Override
	public String getIdentify() {
		return "RIP"+protocolID;
	}

	@Override
	public String getName() {
		return "RIP"+protocolID;
	}

	@Override
	public void attachTo(IService<?> service) {
		if(service instanceof IPProtocol) {
			m_IP = (IPv4) service;
			wait(IPProtocol.recPacketSignal, Double.NaN);
		}
		if(service instanceof L3Switch) {
			m_node = (L3Switch) service;
		}
		
	}

	@Override
	public void detachFrom(IService<?> service) {
		if(service == m_IP) {
			m_IP = null;
			this.resetInterrupt(IPProtocol.recPacketSignal);
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
	protected void interruptHandle(int signal, InterruptParam param) {
		if(signal==IPProtocol.recPacketSignal){
			
			if (param instanceof IPReceiveParam){
				IPReceiveParam pkt = (IPReceiveParam)param;
				Integer srcip = pkt.getSource();
				Integer destip = pkt.getDestination();
				byte[] data = pkt.getData();
				//System.out.println(IPv4.IPtoString(m_IP.getIntegerAddress())+" get RIP pkt from "+IPv4.IPtoString(srcip)+" "+Simulator.GetTime());
				m_node.getData(data, m_IP.getLinkNumber(m_IP.getIntegerAddress()),srcip,destip);
			}
		}

	}
	
	public void send(byte[] data){
		//System.out.println(IPv4.IPtoString(m_IP.getIntegerAddress())+" send RIP pkt");
		if (!m_IP.canSend()) return;
		//System.out.println("can! send RIP pkt");
		m_IP.sendPacket(data, 0xFD);
	}

}
