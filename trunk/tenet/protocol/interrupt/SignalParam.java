package tenet.protocol.interrupt;

import tenet.util.pattern.IParam;

public class SignalParam implements IParam {
	protected int signal;

	public SignalParam(int signal) {
		this.signal = signal;
	}

	public int getSignal() {
		return this.signal;
	}
}
