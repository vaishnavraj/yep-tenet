package tenet.protocol.datalink.SEther;

import java.util.TreeMap;
import java.util.TreeSet;

import org.knf.util.AssertLib;

import tenet.core.Simulator;
import tenet.node.INode;
import tenet.protocol.datalink.FrameParamStruct;
import tenet.protocol.datalink.IDataLinkLayer;
import tenet.protocol.datalink.MediumAddress;
import tenet.protocol.interrupt.InterruptObject;
import tenet.protocol.interrupt.InterruptParam;
import tenet.protocol.physics.Link;
import tenet.util.pattern.IParam;
import tenet.util.pattern.cbc.Command;
import tenet.util.pattern.serviceclient.IClient;
import tenet.util.pattern.serviceclient.IRegistryableService;
import tenet.util.pattern.serviceclient.IService;

/**
 * 简易实现Ethernet的逻辑，上层协议的特征域为整数（即ethertype）
 * 其他的基于Ethernet的对数据链路的拓展均继承这个类
 * @author meilunsheng
 * @version 09.01
 */
public class SimpleEthernetDatalink extends InterruptObject implements
		IDataLinkLayer, IRegistryableService<Integer> {

	/**
	 * 发送帧的命令
	 * @author meilunsheng
	 * @version 09.01
	 */
	protected class TransmitFrame extends Command {

		byte[] m_data;

		public TransmitFrame(double m_time, SimpleEthernetDatalink m_recv,
				byte[] data) {
			super(m_time, m_recv);
			m_data = data;
		}

		@SuppressWarnings("unchecked")
		@Override
		public IParam execute() {
			((SimpleEthernetDatalink) m_recv).m_link.send(
					(IClient<MediumAddress>) this.m_recv, m_data);
			return null;
		}

	}

	/**
	 * 接收冲突的自中断，上层协议不可等待。
	 */
	protected static final int INT_RECEIVE_COLLISION = 0xF0000004;
	/**
	 * 非接入状态接收的自中断，上层协议不可等待。
	 */
	protected static final int INT_RECEIVE_LINKDOWN = 0xF0000002;
	/**
	 * 发送冲突的自中断，上层协议不可等待。
	 */
	protected static final int INT_SEND_COLLISION = 0xF0000003;
	/**
	 * 非接入状态发送的自中断，上层协议不可等待。
	 */
	protected static final int INT_SEND_LINKDOWN = 0xF0000001;

	/**
	 * Ethernet(v2)的MTU通常固定为1500
	 */
	protected int m_MTU=1500;
	/**
	 * 硬件延迟
	 */
	protected double m_delay = 0.001d;
	/**
	 * 底层连接必然为双绞线
	 */
	protected Link m_link;
	/**
	 * 接入状态
	 */
	protected boolean m_linkup = false;
	/**
	 * MAC地址
	 */
	protected MediumAddress m_mac;
	/**
	 * 线路错误发生器
	 */
	protected IErrorGenerator errgen=null;
	
	/**
	 * 上层协议
	 */
	protected TreeMap<Integer, IClient<Integer>> m_network_layers;

	/**
	 * 所在节点
	 */
	protected INode m_node;

	/**
	 * 接收状态的上层协议
	 */
	protected TreeSet<Integer> m_receive_client;

	protected boolean m_state = false;

	/**
	 * 发送状态的上层协议
	 */
	protected TreeSet<Integer> m_transmit_client;

	public SimpleEthernetDatalink(MediumAddress m_mac) {
		super();
		this.m_network_layers = new TreeMap<Integer, IClient<Integer>>();
		this.m_receive_client = new TreeSet<Integer>();
		this.m_transmit_client = new TreeSet<Integer>();
		this.m_mac = m_mac;
	}

	/**
	 * 绑定下层
	 * @param service 必须为 INode或者Link
	 */
	@Override
	public void attachTo(IService<?> service) {
		if (service instanceof INode)
			m_node = (INode) service;
		if (service instanceof Link)
			m_link = (Link) service;
	}

	public int getMTU() {
		return m_MTU;
	}

	public void setMTU(int m_MTU) {
		this.m_MTU = m_MTU;
	}

	public double getDelay() {
		return m_delay;
	}

	public void setDelay(double m_delay) {
		this.m_delay = m_delay;
	}

	@Override
	public void detachFrom(IService<?> service) {
		if (service instanceof INode)
			m_node = null;
		if (service instanceof Link)
			m_link = null;
	}

	/**
	 * 禁用
	 */
	@Override
	public void disable() {
		if (!m_state)
			return;
		this.m_state = false;
		this.interrupt(Link.INT_LINK_DOWN, null);
		resetInterrupt(Link.INT_LINK_DOWN);
		resetInterrupt(Link.INT_LINK_UP);
		if(m_link!=null)
		this.m_link.disable();
		// for(IClient<Integer> netl:m_network_layers.values())
		// if(netl instanceof InterruptObject)
		// ((InterruptObject)netl).interrupt(INT_INTERFACE_DOWN, null);
	}

	@Override
	public void dump() {

	}

	/**
	 * 使能
	 */
	@Override
	public void enable() {
		if (this.m_state)
			return;
		wait(Link.INT_LINK_UP, Double.NaN);
		this.m_state = true;
		if(m_link!=null)
		this.m_link.enable();

		// this.m_state=this.m_link.isEnable();
		// if(m_state)
		// for(IClient<Integer> netl:m_network_layers.values())
		// if(netl instanceof InterruptObject)
		// ((InterruptObject)netl).interrupt(INT_INTERFACE_UP, null);
	}

	private boolean errorGenerate(FrameParamStruct frame) {
		if(this.errgen==null) return false;
		return this.errgen.check(frame);
	}

	@Override
	public String getIdentify() {
		return this.getUniqueID().toString();
	}

	@Override
	public String getName() {
		return "SEther " + getIdentify();
	}

	@Override
	public MediumAddress getUniqueID() {
		return m_mac;
	}

	/**
	 * 中断处理
	 * @param signal 中断信号
	 * @param param 中断参数
	 */
	@Override
	protected void interruptHandle(int signal, InterruptParam param) {
		//IClient<Integer> client;
		switch (signal) {
		case Link.INT_LINK_UP:
			this.linkUp();
			break;
		case Link.INT_LINK_DOWN:
			this.linkDown();
			break;

		case Link.INT_LINK_READ_ERROR:
			onReadError((FrameParamStruct) param);
			break;
		case Link.INT_LINK_READ_OK:
			if (!errorGenerate((FrameParamStruct) param))
				onReadOK((FrameParamStruct) param);
			else
				onReadOKwithCheckError((FrameParamStruct) param);
			break;
		case INT_RECEIVE_LINKDOWN:
			onReceiveRequireLinkDown((FrameParamStruct) param);
			break;
		case INT_RECEIVE_COLLISION:
			onReceiveRequireCollision((FrameParamStruct) param);
			break;

		case Link.INT_LINK_SEND_ERROR:
			onSendError((FrameParamStruct) param);
			break;
		case Link.INT_LINK_SEND_OK:
			onSendOK((FrameParamStruct) param);
			break;
		case INT_SEND_COLLISION:
			onTransmitRequireCollision((FrameParamStruct) param);
			break;
		case INT_SEND_LINKDOWN:
			onTransmitRequireLinkDown((FrameParamStruct) param);
			break;
		}
	}

	@Override
	public boolean isEnable() {
		return this.m_state;
	}

	public boolean isLinkUp() {
		return m_linkup;
	}

	/**
	 * 转换到非接入状态
	 */
	protected void linkDown() {
		m_linkup = false;
		wait(Link.INT_LINK_UP, Double.NaN);
		resetInterrupt(Link.INT_LINK_DOWN);
		for (Object i : m_transmit_client.toArray())
			this.delayInterrupt(INT_SEND_LINKDOWN, new FrameParamStruct(
					MediumAddress.MAC_ALLONE, MediumAddress.MAC_ALLONE, (Integer) i,
					new byte[0]), m_delay);
		for (Object i : m_receive_client.toArray())
			this.delayInterrupt(INT_RECEIVE_LINKDOWN, new FrameParamStruct(
					MediumAddress.MAC_ALLONE, MediumAddress.MAC_ALLONE, (Integer) i,
					new byte[0]), m_delay);
		for (IClient<Integer> netl : m_network_layers.values())
			if (netl instanceof InterruptObject)
				((InterruptObject) netl).interrupt(INT_INTERFACE_DOWN, this.getUniqueID());

	}

	/**
	 * 转换到接入状态
	 */
	protected void linkUp() {
		m_linkup = true;
		wait(Link.INT_LINK_DOWN, Double.NaN);
		resetInterrupt(Link.INT_LINK_UP);
		for (IClient<Integer> netl : m_network_layers.values())
			if (netl instanceof InterruptObject)
				((InterruptObject) netl).interrupt(INT_INTERFACE_UP, this.getUniqueID());

	}

	/**
	 * 通知所有上层协议INT_FRAME_RECEIVE_READY
	 */
	protected void notifyReceiveReady() {
		for (IClient<Integer> clients : this.m_network_layers.values())
			((InterruptObject) clients).delayInterrupt(INT_FRAME_RECEIVE_READY,
					this.getUniqueID(), m_delay);
	}
	/**
	 * 通知所有上层协议INT_FRAME_TRANSMIT_READY
	 */
	protected void notifySendReady() {
		for (IClient<Integer> clients : this.m_network_layers.values())
			((InterruptObject) clients).delayInterrupt(INT_FRAME_TRANSMIT_READY,
					this.getUniqueID(), m_delay);
	}

	protected void onReadError(FrameParamStruct param) {
		if (!m_receive_client.contains(param.typeParam))
			return;
		resetReceiveInterrupts(param.typeParam);
		IClient<Integer> client = this.m_network_layers.get(param.typeParam);
		if (client == null)
			return;
		((InterruptObject) client).delayInterrupt(INT_FRAME_RECEIVE,
				new ReceiveParam(ReceiveStatus.receiveError,
						(InterruptObject) client, this, param), m_delay);
		notifyReceiveReady();
	}

	protected void onReadOK(FrameParamStruct param) {
		if(param.destinationParam==null||(!param.destinationParam.equals(this.m_mac)
				&&(!param.destinationParam.equals(MediumAddress.MAC_ALLONE)))) return;
		if (!m_receive_client.contains(param.typeParam))
			return;
		resetReceiveInterrupts(param.typeParam);
		IClient<Integer> client = this.m_network_layers.get(param.typeParam);
		if (client == null)
			return;
		((InterruptObject) client)
				.delayInterrupt(INT_FRAME_RECEIVE, new ReceiveParam(
						ReceiveStatus.receiveOK, (InterruptObject) client, this,
						(FrameParamStruct) param), m_delay);
		notifyReceiveReady();
	}

	protected void onReadOKwithCheckError(FrameParamStruct param) {
		if (!m_receive_client.contains(param.typeParam))
			return;
		resetReceiveInterrupts(param.typeParam);
		IClient<Integer> client = this.m_network_layers.get(param.typeParam);
		if (client == null)
			return;
		((InterruptObject) client).delayInterrupt(INT_FRAME_RECEIVE,
				new ReceiveParam(ReceiveStatus.frameCheckError,
						(InterruptObject) client, this, param), m_delay);
		notifyReceiveReady();
	}

	protected void onReceiveRequireCollision(FrameParamStruct param) {
		IClient<Integer> client = this.m_network_layers.get(param.typeParam);
		if (client == null)
			return;
		((InterruptObject) client).delayInterrupt(INT_FRAME_RECEIVE,
				new ReceiveParam(ReceiveStatus.receiveCollision,
						(InterruptObject) client, this, param), m_delay);
	}

	protected void onReceiveRequireLinkDown(FrameParamStruct param) {
		resetReceiveInterrupts(param.typeParam);
		IClient<Integer> client = this.m_network_layers.get(param.typeParam);
		if (client == null)
			return;
		((InterruptObject) client).delayInterrupt(INT_FRAME_RECEIVE,
				new ReceiveParam(ReceiveStatus.dataLinkOff,
						(InterruptObject) client, this, param), m_delay);

	}

	protected void onSendError(FrameParamStruct param) {
		if (!m_transmit_client.contains(param.typeParam))
			return;
		resetTransmitInterrupts(param.typeParam);
		IClient<Integer> client = this.m_network_layers.get(param.typeParam);
		if (client == null)
			return;
		((InterruptObject) client).delayInterrupt(INT_FRAME_TRANSMIT,
				new TransmitParam(TransmitStatus.transmitError,
						(InterruptObject) client, this, param), m_delay);
		notifySendReady();
	}

	protected void onSendOK(FrameParamStruct param) {
		if (!m_transmit_client.contains(param.typeParam))
			return;
		resetTransmitInterrupts(((FrameParamStruct) param).typeParam);
		IClient<Integer> client = this.m_network_layers.get(param.typeParam);
		if (client == null)
			return;
		((InterruptObject) client).delayInterrupt(INT_FRAME_TRANSMIT,
				new TransmitParam(TransmitStatus.transmitOK,
						(InterruptObject) client, this, param), m_delay);
		notifySendReady();
	}

	protected void onTransmitRequireCollision(FrameParamStruct param) {
		IClient<Integer> client = this.m_network_layers.get(param.typeParam);
		if (client == null)
			return;
		((InterruptObject) client).delayInterrupt(INT_FRAME_TRANSMIT,
				new TransmitParam(TransmitStatus.transmitCollision,
						(InterruptObject) client, this, param), m_delay);
	}

	protected void onTransmitRequireLinkDown(FrameParamStruct param) {
		resetTransmitInterrupts(((FrameParamStruct) param).typeParam);
		IClient<Integer> client = this.m_network_layers.get(param.typeParam);
		if (client == null)
			return;
		((InterruptObject) client).delayInterrupt(INT_FRAME_TRANSMIT,
				new TransmitParam(TransmitStatus.dataLinkOff,
						(InterruptObject) client, this, param), m_delay);
	}

	@Override
	public void receiveFrame(FrameParamStruct param) {
		waitReceiveSignal();
		if (m_receive_client.contains(param.typeParam))
		// if(m_receive_client.size()>1)
		{
			this.delayInterrupt(INT_RECEIVE_COLLISION, param, this.m_delay);
			return;
		}
		m_receive_client.add(param.typeParam);
		if (!this.isLinkUp()) {
			this.delayInterrupt(INT_RECEIVE_LINKDOWN, param, this.m_delay);
			return;
		}
		this.m_link.read(this);
	}

	/**
	 * 注册上层协议
	 * @param client
	 */
	@Override
	public void registryClient(IClient<Integer> client) {
		AssertLib.AssertFalse(
				(this.m_network_layers.containsKey(client.getUniqueID())),
				"invaild registry on datalink SEther", false);
		this.m_network_layers.put(client.getUniqueID(), client);
		client.attachTo(this);
	}

	/**
	 * 重置对于ethertype为typeParam的上层协议请求接收过程的中断等待状态
	 * @param typeParam 
	 */
	protected void resetReceiveInterrupts(int typeParam) {
		m_receive_client.remove(typeParam);
		if (!m_receive_client.isEmpty())
			return;
		this.resetInterrupt(Link.INT_LINK_READ_ERROR);
		this.resetInterrupt(Link.INT_LINK_READ_OK);
		this.resetInterrupt(INT_RECEIVE_LINKDOWN);
		this.resetInterrupt(INT_RECEIVE_COLLISION);
	}
	/**
	 * 重置对于ethertype为typeParam的上层协议请求发送过程的中断等待状态
	 * @param typeParam 
	 */
	protected void resetTransmitInterrupts(int typeParam) {
		m_transmit_client.remove(typeParam);
		if (!m_transmit_client.isEmpty())
			return;
		this.resetInterrupt(Link.INT_LINK_SEND_ERROR);
		this.resetInterrupt(Link.INT_LINK_SEND_OK);
		this.resetInterrupt(INT_SEND_LINKDOWN);
		this.resetInterrupt(INT_SEND_COLLISION);
	}

	@Override
	public void setUniqueID(MediumAddress id) {
		this.m_mac = id;
	}

	@Override
	public void transmitFrame(FrameParamStruct param) {
		waitTransmitSignal();
		// if(m_transmit_client.contains(param.typeParam))
		if (m_transmit_client.size() > 0) {
			this.delayInterrupt(INT_SEND_COLLISION, param, this.m_delay);
			return;
		}
		m_transmit_client.add(param.typeParam);
		if (!this.isLinkUp()) {
			this.delayInterrupt(INT_SEND_LINKDOWN, param, this.m_delay);
			return;
		}
		if (param.dataParam.length>this.m_MTU) {
			this.delayInterrupt(Link.INT_LINK_SEND_ERROR, param, this.m_delay);
			return;
		}
		Simulator.getInstance().schedule(
				new TransmitFrame(Simulator.getInstance().getTime() + m_delay,
						this, param.toBytes()));
	}

	@Override
	public void unregistryClient(IClient<Integer> client) {
		if (this.m_network_layers.get(client.getUniqueID()) == client) {
			this.m_network_layers.remove(client.getUniqueID());
			client.detachFrom(this);
		}
	}

	@Override
	public void unregistryClient(Integer id) {
		IClient<Integer> client = this.m_network_layers.get(id);
		if (client != null) {
			this.m_network_layers.remove(id);
			client.detachFrom(this);
		}
	}

	/**
	 * 设置接收过程可能遇到的中断信号
	 */
	protected void waitReceiveSignal() {
		this.wait(Link.INT_LINK_READ_ERROR, Double.NaN);
		this.wait(Link.INT_LINK_READ_OK, Double.NaN);
		this.wait(INT_RECEIVE_LINKDOWN, Double.NaN);
		this.wait(INT_RECEIVE_COLLISION, Double.NaN);
	}
	/**
	 * 设置发送过程可能遇到的中断信号
	 */
	protected void waitTransmitSignal() {
		this.wait(Link.INT_LINK_SEND_ERROR, Double.NaN);
		this.wait(Link.INT_LINK_SEND_OK, Double.NaN);
		this.wait(INT_SEND_LINKDOWN, Double.NaN);
		this.wait(INT_SEND_COLLISION, Double.NaN);
	}

}
