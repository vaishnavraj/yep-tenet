package tenet.protocol.physics;

import tenet.protocol.interrupt.InterruptObject;
import tenet.protocol.interrupt.InterruptParam;

public class DataSendedInterruptParam extends InterruptParam {
	public byte[] m_data;
	public InterruptObject m_sender;

	public DataSendedInterruptParam(InterruptObject m_sender, byte[] m_data) {
		super();
		this.m_sender = m_sender;
		this.m_data = m_data;
	}
}
