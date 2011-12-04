package tenet.node.l2switch;

import java.util.Collection;
import java.util.HashMap;
import java.util.PriorityQueue;

import tenet.core.Simulator;
import tenet.node.INode;
import tenet.protocol.datalink.FrameParamStruct;
import tenet.protocol.datalink.IDataLinkLayer;
import tenet.protocol.datalink.MediumAddress;
import tenet.protocol.datalink.SEther.SimpleEthernetDatalink;
import tenet.protocol.interrupt.InterruptObject;
import tenet.protocol.interrupt.InterruptParam;
import tenet.util.pattern.serviceclient.IClient;

public class L2Switch extends InterruptObject implements INode {

	public class MACWithTimestamp implements Comparable<MACWithTimestamp> {

		public Double m_time;
		public MediumAddress m_mac;

		public MACWithTimestamp(Double m_time, MediumAddress m_mac) {
			super();
			this.m_time = m_time;
			this.m_mac = m_mac;
		}

		@Override
		public int compareTo(MACWithTimestamp arg0) {
			return m_time.compareTo(arg0.m_time);
		}

	}

	protected final static int TIMER = 0x00000001;

	/**
	 * 存储二层交换机上的所有SimpleEthernetDatalink的信息
	 */
	public HashMap<MediumAddress, SimpleEthernetDatalink> m_datalinks;

	/**
	 * 存储转发目的地所对应的出口,即MAC表
	 */
	public HashMap<MediumAddress, SimpleEthernetDatalink> m_macport_map;
	
	public PriorityQueue<MACWithTimestamp> m_timeout;
	protected boolean m_state = false;

	public L2Switch() {
		super();
		wait(TIMER, 1.0); // 通过超时等待，每秒触发一次计时器
		m_datalinks = new HashMap<MediumAddress, SimpleEthernetDatalink>();
		m_macport_map = new HashMap<MediumAddress, SimpleEthernetDatalink>();
		m_timeout = new PriorityQueue<MACWithTimestamp>();
	}

	@Override
	public void disable() {
		if (!m_state)
			return;
		for (IDataLinkLayer iface : m_datalinks.values())
			iface.disable();
		m_state = false;
	}

	@Override
	public void dump() {

	}

	@Override
	public void enable() {
		if (m_state)
			return;
		for (IDataLinkLayer iface : m_datalinks.values())
			iface.enable();
		m_state = true;
	}

	/**
	 * 获得mac地址所对应的二层交换机上的SimpleEthernetDatalink
	 */
	public SimpleEthernetDatalink getDatalink(MediumAddress mac) {
		return m_datalinks.get(mac);
	}

	/**
	 * 获得二层交换机上的所有SimpleEthernetDatalink 
	 */
	public Collection<SimpleEthernetDatalink> getDatalinks() {
		return m_datalinks.values();
	}

	/**
	 * 从mac表中获得mac地址所对应的端口上的SimpleEthernetDatalink
	 */
	public SimpleEthernetDatalink getTransmitDatalink(MediumAddress mac) {
		return m_macport_map.get(mac);
	}

	@Override
	protected void interruptHandle(int signal, InterruptParam param) {
		switch (signal) {
		case TIMER:
			// TODO 重新开始计时器读时
			this.resetInterrupt(TIMER);
			wait(TIMER, 1.0);
			// TODO 检查是否有需要清除的mac记录
			//I am lazy...
			
		}
	}
	

	@Override
	public boolean isEnable() {
		return m_state;
	}

	public void learnMAC(MediumAddress mac, SimpleEthernetDatalink iface) {
	    // TODO 更新MAC表，将MAC地址与所对应的SimpleEthernetDatalink放入表中
		if (m_macport_map.containsKey(mac) && m_macport_map.get(mac)!=iface ) m_macport_map.remove(mac);
		m_macport_map.put(mac, iface);
	}

	@Override
	public void registryClient(IClient client) {
		// TODO 将一个IDatalinkLayer安置到二层交换机上
		if (client instanceof SimpleEthernetDatalink){
			m_datalinks.put((MediumAddress)client.getUniqueID(), (SimpleEthernetDatalink)client);
			client.attachTo(this);
		}
		
	}

	@Override
	public void unregistryClient(IClient<Object> client) {
		if (m_datalinks.containsKey((MediumAddress)client.getUniqueID())){
				m_datalinks.remove((MediumAddress)client.getUniqueID());
				client.detachFrom(this);
		}
	}

	@Override
	public void unregistryClient(Object id) {
		if (id instanceof MediumAddress){
			IClient<MediumAddress> client = this.m_datalinks.get(id);
			m_datalinks.remove(id);
			client.detachFrom(this);
		}
	}
	
	@Override
	public void setAddress(IClient<?> protocol, byte[] address){
		
	}
	
	@Override
	public byte[] getAddress(IClient<?> protocol){
		return null;
	}
	
}
