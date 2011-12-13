package tenet.protocol.transport.tcp;

import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;

import org.knf.tenet.test.util.TestSessionLayer;

import tenet.node.INode;
import tenet.node.MyNode;
import tenet.protocol.IProtocol;
import tenet.protocol.datalink.IDataLinkLayer;
import tenet.protocol.interrupt.InterruptObject;
import tenet.protocol.interrupt.InterruptParam;
import tenet.protocol.network.ipv4.IPProtocol;
import tenet.protocol.transport.tcp.TCPProtocol.ReturnStatus;
import tenet.protocol.transport.tcp.TCPProtocol.ReturnType;
import tenet.util.ByteLib;
import tenet.util.pattern.serviceclient.IClient;
import tenet.util.pattern.serviceclient.IService;

public class MyTCP extends InterruptObject implements TCPProtocol {

	private INode m_node;
	private IPProtocol m_IP;
	private boolean m_state;
	
	private LinkedList<IClient<Integer>> m_app_layers = new LinkedList<IClient<Integer>>();
	
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
		m_app_layers.add(client);
		client.attachTo(this);
	}

	@Override
	public void unregistryClient(Integer id) {
		
	}

	@Override
	public void unregistryClient(IClient<Integer> client) {
		m_app_layers.remove(client);
		client.detachFrom(this);

	}

	@Override
	public void attachTo(IService<?> service) {
		if(service instanceof IPProtocol) {
			m_IP = (IPProtocol) service;
			wait(IPProtocol.recPacketSignal, Double.NaN);
		}
		if(service instanceof INode) {
			m_node = (INode) service;
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
	public Map<Integer, MySocket> portHandle = new HashMap<Integer, MySocket>();
	public MySocket idleSocket = new MySocket(0, this);
	
	@Override
	public int socket() {
		MySocket socket = new MySocket(nextHandle,this);
		handleSocket.put(nextHandle, socket);
		nextHandle++;
		return nextHandle-1;
		
	}

	@Override
	public int bind(int handle, int port) {
		if (portHandle.containsKey(port)) return -1;
		portHandle.put(port, handleSocket.get(handle));
		handleSocket.get(port).bind(ByteLib.bytesToInt(m_node.getAddress(m_IP), 0), port);
		return 0;
	}

	@Override
	public void listen(int handle) {
		//check in the postHandle
		handleSocket.get(handle).Listen();

	}

	@Override
	public void connect(int handle, int dstIP, int dstPort) {
		//check in the postHandle
		handleSocket.get(handle).Connect(dstIP, dstPort);

	}

	@Override
	public void close(int handle) {
		handleSocket.get(handle).close();

	}

	@Override
	public void accept(int handle) {
		// TODO Auto-generated method stub

	}

	@Override
	public void abort(int handle) {
		handleSocket.get(handle).abort();

	}

	@Override
	public void send(int handle, byte[] data) {
		handleSocket.get(handle).send(data);

	}

	@Override
	public void receive(int handle) {
		handleSocket.get(handle).receive();

	}

	@Override
	protected void interruptHandle(int signal, InterruptParam param) {
		// TODO Auto-generated method stub

	}
	
	public void ReturnMsg(ReturnType type, int handle,	ReturnStatus state, int result, byte[] data) {
		ReturnParam param = new ReturnParam();
		param.type = type;
		param.handle = handle;
		param.status = state;
		param.result = result;
		param.data = data;
		for (IClient<Integer> client: m_app_layers )
			if (client instanceof TestSessionLayer && ((TestSessionLayer)client).handle == param.handle)
				((InterruptObject)client).delayInterrupt(INT_RETURN, param, 0);
	}
	
	public void sendSeg(TCPSegment seg, Integer src_ip, Integer dest_ip) {
		m_IP.sendPacket(seg.toBytes(), src_ip, dest_ip, this.getUniqueID());
	}


}
