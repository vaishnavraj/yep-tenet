package tenet.protocol.physics;

import java.util.HashMap;
import java.util.HashSet;

import org.knf.util.AssertLib;

import tenet.core.Simulator;
import tenet.protocol.IProtocol;
import tenet.protocol.datalink.FrameParamStruct;
import tenet.protocol.datalink.MediumAddress;
import tenet.protocol.interrupt.InterruptObject;
import tenet.protocol.interrupt.InterruptParam;
import tenet.protocol.statecontrol.IStateSetable;
import tenet.util.pattern.IParam;
import tenet.util.pattern.cbc.Command;
import tenet.util.pattern.cbc.IReceiver;
import tenet.util.pattern.serviceclient.IClient;
import tenet.util.pattern.serviceclient.IRegistryableService;

/**
 * RJ45µÄÄ£ÄâÀà
 * @author meilunsheng
 * @version 09.01
 */
public class Link extends InterruptObject implements IProtocol,
		IRegistryableService<MediumAddress>, IStateSetable, IReceiver {

	protected class Send extends Command {

		protected byte[] m_data;
		protected Link m_link;
		protected IClient<MediumAddress> m_receiver;
		protected IClient<MediumAddress> m_sender;

		protected Send(double m_time, IReceiver m_recv, Link link,
				IClient<MediumAddress> sender, IClient<MediumAddress> receiver,
				byte[] data) {
			super(m_time, m_recv);
			m_sender = sender;
			m_receiver = receiver;
			m_data = data;
			m_link = link;
		}

		@Override
		public IParam execute() {
			// m_link.m_busy.remove(m_sender);
			m_link.delayInterrupt(INT_LINK_READ, new DataArrivalInterruptParam(
					(InterruptObject) m_receiver, m_data), 0.0);
			m_link.delayInterrupt(INT_LINK_SEND, new DataSendedInterruptParam(
					(InterruptObject) m_sender, m_data), 0.0);
			return null;
		}

	}

	public static final int INT_LINK_DOWN = 0x80000000;
	public static final int INT_LINK_READ = 0x80000002;
	public static final int INT_LINK_READ_ERROR = 0x80000005;
	public static final int INT_LINK_READ_OK = 0x80000004;
	public static final int INT_LINK_SEND = 0x80000003;

	public static final int INT_LINK_SEND_ERROR = 0x80000007;
	public static final int INT_LINK_SEND_OK = 0x80000006;

	public static final int INT_LINK_UP = 0x80000001;
	protected int m_bandwidth = 1024;

	protected HashMap<IClient<MediumAddress>, Send> m_busy;
	protected double m_delay = 0.01d;

	protected HashSet<IClient<MediumAddress>> m_interfaces;

	private boolean m_state = false;

	public Link() {
		super();
		m_interfaces = new HashSet<IClient<MediumAddress>>();
		m_busy = new HashMap<IClient<MediumAddress>, Send>();
		this.wait(INT_LINK_DOWN, Double.NaN);
	}

	public Link(double delay, int bandwidth) {
		this();
		this.setDelay(delay);
		this.setBandwidth(bandwidth);
	}

	protected double calculateSendCost(byte[] data) {
		return (double) data.length / (double) (this.getBandwidth());
	}

	@Override
	public void disable() {
		if (!m_state)
			return;
		m_state = false;
		for (IClient<MediumAddress> iface : m_interfaces)
			((InterruptObject) iface).delayInterrupt(INT_LINK_DOWN, null, 0.0);
		interrupt(INT_LINK_DOWN, null);
	}

	@Override
	public void dump() {

	}

	@Override
	public void enable() {
		if (m_state)
			return;
		m_state = true;
		if (m_interfaces.size() == 2) {
			for (IClient<MediumAddress> iface : m_interfaces)
				m_state = m_state && ((IStateSetable) iface).isEnable();
		}
		if (m_state)
			for (IClient<MediumAddress> iface : m_interfaces)
				((InterruptObject) iface).delayInterrupt(INT_LINK_UP, null,
						this.m_delay);
	}

	public int getBandwidth() {
		return m_bandwidth;
	}

	public double getDelay() {
		return m_delay;
	}

	@Override
	public String getIdentify() {
		return "";
	}

	@Override
	public String getName() {
		return "LINK";
	}

	@Override
	protected void interruptHandle(int signal, InterruptParam param) {
		switch (signal) {
		case INT_LINK_READ:
			AssertLib.AssertTrue(param instanceof DataArrivalInterruptParam, "",
					true);
			((InterruptObject) ((DataArrivalInterruptParam) param).m_receiver)
					.delayInterrupt(
							INT_LINK_READ_OK,
							FrameParamStruct
									.fromBytes(((DataArrivalInterruptParam) param).m_data),
							m_delay);
			// this.resetinterrupt(INT_LINK_READ);
			break;
		case INT_LINK_SEND:
			((DataSendedInterruptParam) param).m_sender
					.delayInterrupt(
							INT_LINK_SEND_OK,
							FrameParamStruct
									.fromBytes(((DataSendedInterruptParam) param).m_data),
							m_delay);
			this.m_busy.remove(((DataSendedInterruptParam) param).m_sender);
			// this.resetinterrupt(INT_LINK_SEND);
			break;
		case INT_LINK_DOWN:
			for (IClient<MediumAddress> iface : this.m_busy.keySet())
				((InterruptObject) iface).delayInterrupt(INT_LINK_SEND_ERROR,
						FrameParamStruct.fromBytes(m_busy.get(iface).m_data),
						m_delay);
			this.m_busy.clear();
			this.resetInterrupt(INT_LINK_READ);
			this.resetInterrupt(INT_LINK_SEND);
			break;
		}
	}

	@Override
	public boolean isEnable() {
		return m_state;
	}

	public void read(IClient<MediumAddress> caller) {
		wait(INT_LINK_READ, Double.NaN);
	}

	@Override
	public void registryClient(IClient<MediumAddress> client) {
		AssertLib.AssertTrue(client instanceof InterruptObject,
				"client of Link must be a InterruptObject", true);
		if (m_interfaces.size() < 2) {
			m_interfaces.add((IClient<MediumAddress>) client);
			client.attachTo(this);
		}
	}

	public void send(IClient<MediumAddress> caller, byte[] data) {
		if (this.m_busy.containsKey(caller)) {
			((InterruptObject) caller).interrupt(INT_LINK_SEND_ERROR,
					FrameParamStruct.fromBytes(data));
			return;
		}
		for (IClient<MediumAddress> target : m_interfaces)
			if (target != caller) {
				Send send = new Send(Simulator.getInstance().getTime()
						+ calculateSendCost(data), null, this, caller, target,
						data);
				Simulator.getInstance().schedule(send);
				this.m_busy.put(caller, send);
				wait(INT_LINK_SEND, Double.NaN);
				wait(INT_LINK_READ, Double.NaN);
			}
	}

	public void setBandwidth(int bandwidth) {
		this.m_bandwidth = bandwidth;
	}

	public void setDelay(double delay) {
		this.m_delay = delay;
	}

	@Override
	public void unregistryClient(IClient<MediumAddress> client) {
		disable();
		m_interfaces.remove(client);
		client.detachFrom(this);
	}

	@Override
	public void unregistryClient(MediumAddress id) {
		AssertLib.AssertFalse(true, "[@tenet.protocol.physics]invaild call",
				true);
	}

}
