package tenet.protocol.interrupt;

import java.util.TreeMap;

import tenet.core.Simulator;
import tenet.util.pattern.IParam;
import tenet.util.pattern.cbc.Command;
import tenet.util.pattern.cbc.ICaller;
import tenet.util.pattern.cbc.IReceiver;

/**
 * 可中断对象.<br>
 * 这个抽象基类描述了该对象可以被发送中断，从而实现异步回调。<br>
 * Tenet 11'是一个单线程模拟多线程同步阻塞过程的模拟器。由于单线程程序不能直接模拟同步阻塞，因此采用了异步回调来模拟阻塞过程。<br>
 * 而中断模式是解决异步回调的两种模式之一，需要遵循以下约定：
 * <ul>
 * <li>任何一个调用者可以根据需求对特定的信号进行等待，但是用于等待的函数{@link #wait(int, Double)}必须先于阻塞函数调用语句，或者保证中断信号在阻塞函数执行完成时不会被发送</li>
 * </ul>
 * 如果是模拟同步阻塞的过程，需要另外遵守以下规定：
 * <ul>
 * <li>任何一个需要模拟同步阻塞的函数，必须在模拟逻辑上向调用者发送合理的中断，即使调用者并不在这个信号上等待</li>
 * <li>不在一个过程中连续调用模拟同步阻塞的函数</li>
 * </ul>
 * 中断模式不切断调用链，对于回调链，当使用{@link #interrupt(int, InterruptParam)}时，不切断；当使用{@link #delayInterrupt(int, InterruptParam, double)}时切断。
 * 如果需要切断调用链，则必须在过程中使用{@link tenet.util.pattern.cbc}提供的CBC模式，或者当异步回调函数所在类本身也是个interruptObject时，对自身使用{@link #delayInterrupt(int, InterruptParam, double)}.
 * @author meilunsheng
 * @version 09.01
 */
public abstract class InterruptObject implements ICaller, IReceiver {

	protected class DelayInterrupt extends Command {

		InterruptParam m_param;
		int m_signal;

		protected DelayInterrupt(double m_time, IReceiver m_recv, int signal,
				InterruptParam param) {
			super(m_time, m_recv);
			this.m_signal = signal;
			this.m_param = param;
		}

		@Override
		public IParam execute() {
			((InterruptObject) this.m_recv).interrupt(m_signal, m_param);
			return null;
		}

	}

	protected class TimeoutCommand extends Command {

		private int m_signal;

		protected TimeoutCommand(ICaller caller, double m_time,
				IReceiver m_recv, int signal) {
			super(caller, m_time, m_recv);
			m_signal = signal;
		}

		@Override
		public IParam execute() {
			return new SignalParam(m_signal);
		}

	}

	public TreeMap<Integer, TimeoutCommand> m_timeout;

	public InterruptObject() {
		super();
		this.m_timeout = new TreeMap<Integer, TimeoutCommand>();
	}

	/**
	 * 延迟触发中断，可以切断回调链
	 * @param signal 中断信号
	 * @param param 中断参数
	 * @param delay 延迟时间
	 */
	public void delayInterrupt(int signal, InterruptParam param, double delay) {
		if (this.m_timeout.containsKey(signal)) {
			Simulator.getInstance().schedule(
					new DelayInterrupt(
							Simulator.getInstance().getTime() + delay, this,
							signal, param));
		}
	}

	/**
	 * 判断是否已经在等待某个中断信号
	 * @param signal 中断信号
	 * @return <b>null</b>没有等待<br>
	 * 否则为该信号等待超时的Command
	 */
	public TimeoutCommand hasWaiter(int signal) {
		return m_timeout.get(signal);
	}
	
	/**
	 * 触发中断，不切断回调链
	 * @param signal 中断信号
	 * @param param 中断参数
	 */
	public void interrupt(int signal, InterruptParam param) {
		if (this.m_timeout.containsKey(signal)) {
			// this.m_timeout.remove(signal);
			if (TimeoutCommand.class.isInstance(param))
				this.m_timeout.remove(new Integer(signal));
			this.interruptHandle(signal, param);
		}
	}

	
	/**
	 * 中断信号处理过程
	 * @param signal
	 * @param param
	 */
	protected abstract void interruptHandle(int signal, InterruptParam param);

	@Override
	public void onRecall(Command cmd, IReceiver recv, IParam result) {
		if (cmd instanceof TimeoutCommand) {
			int signal = ((SignalParam) result).getSignal();
			if (this.m_timeout.get(signal) != cmd)
				return;
			// this.m_timeout.remove(new Integer(signal));
			interrupt(signal, new TimeoutInterrupt());
		}
	}

	/**
	 * 指定的中断信号等待位复位
	 * @param signal 中断信号
	 */
	public void resetInterrupt(int signal) {
		this.m_timeout.remove(signal);
	}

	/**
	 * 等待指定的中断信号触发，或超时
	 * @param signal 中断信号
	 * @param timeout 超时时间，当为Double.NaN时，为永不超时。
	 * @return 该信号等待超时的TimeoutCommand
	 */
	public TimeoutCommand wait(int signal, Double timeout) {
		if (timeout!=null&&!timeout.isNaN()) {
			TimeoutCommand tc = new TimeoutCommand(this, Simulator
					.getInstance().getTime() + timeout, null, signal);
			this.m_timeout.put(signal, tc);
			Simulator.getInstance().schedule(tc);
			return tc;
		} else {
			TimeoutCommand tc = new TimeoutCommand(this, Double.NaN, null,
					signal);
			this.m_timeout.put(signal, tc);
			return tc;
		}
	}
}
