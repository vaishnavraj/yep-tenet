package tenet.protocol.physics;

import tenet.protocol.interrupt.InterruptObject;
import tenet.protocol.interrupt.InterruptParam;

public class DataArrivalInterruptParam extends InterruptParam {
	public byte[] m_data;
	public InterruptObject m_receiver;

	public DataArrivalInterruptParam(InterruptObject m_receiver, byte[] m_data) {
		super();
		this.m_receiver = m_receiver;
		this.m_data = m_data;
	}
}
