package tenet.protocol.transport.tcp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import tenet.protocol.IProtocol;
import tenet.protocol.datalink.IDataLinkLayer;
import tenet.protocol.interrupt.InterruptObject;
import tenet.protocol.interrupt.InterruptParam;
import tenet.protocol.network.ipv4.IPProtocol;
import tenet.util.ByteLib;
import tenet.util.pattern.serviceclient.IClient;
import tenet.util.pattern.serviceclient.IService;

public class MyTCP extends InterruptObject implements TCPProtocol {

	private IPProtocol m_IP;
	private boolean m_state;
	
	private Map<Integer, IClient<Integer>> m_app_layers = new HashMap<Integer, IClient<Integer>>();
	
	public static final Integer protocolID = 0x06;
	
	@Override
	public void disable() {
		if (!m_state)
			return;
		this.m_state = false;
		resetInterrupt(IPProtocol.recPacketSignal);
		if(m_IP!=null)
		this.m_IP.disable();
	}

	@Override
	public void enable() {
		if (this.m_state)
			return;
		wait(IPProtocol.recPacketSignal, Double.NaN);
		this.m_state = true;
		if(m_IP!=null)
		this.m_IP.enable();
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
		return "MyTCPIdentify";
	}

	@Override
	public String getName() {
		return "MyTCP";
	}

	@Override
	public void registryClient(IClient<Integer> client) {
		m_app_layers.put(client.getUniqueID(),client);
		client.attachTo(this);
	}

	@Override
	public void unregistryClient(Integer id) {
		if (m_app_layers.containsKey(id)) m_app_layers.remove(id);
	}

	@Override
	public void unregistryClient(IClient<Integer> client) {
		if (m_app_layers.containsKey(client.getUniqueID())) m_app_layers.remove(client.getUniqueID());
		client.detachFrom(this);

	}

	@Override
	public void attachTo(IService<?> service) {
		if(service instanceof IPProtocol) {
			m_IP = (IPProtocol) service;
			wait(IPProtocol.recPacketSignal, Double.NaN);
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

	
	//there are some important variables for TCP find right socket
	public int nextHandle = 1;
	
	public Map<Integer, MySocket> handleSocket = new HashMap<Integer, MySocket>();
	public Map<Integer, Integer> portHandle = new HashMap<Integer, Integer>();
	
	@Override
	public int socket() {
		MySocket socket = new MySocket()
	}

	@Override
	public int bind(int handle, int port) {
		// TODO Auto-generated method stub
		return 0;
	}

	@Override
	public void listen(int handle) {
		// TODO Auto-generated method stub

	}

	@Override
	public void connect(int handle, int dstIP, int dstPort) {
		// TODO Auto-generated method stub

	}

	@Override
	public void close(int handle) {
		// TODO Auto-generated method stub

	}

	@Override
	public void accept(int handle) {
		// TODO Auto-generated method stub

	}

	@Override
	public void abort(int handle) {
		// TODO Auto-generated method stub

	}

	@Override
	public void send(int handle, byte[] data) {
		// TODO Auto-generated method stub

	}

	@Override
	public void receive(int handle) {
		// TODO Auto-generated method stub

	}

	@Override
	protected void interruptHandle(int signal, InterruptParam param) {
		// TODO Auto-generated method stub

	}

}
