package tenet.util.pattern.cbc;

import tenet.util.pattern.IParam;

/**
 * CBC的抽象基类，
 * 当CBC被指定调用者（Caller）后，可以对调用者进行回调，但是并不切断回调链，如果需要切断回调链，需要使用{@link tenet.protocol.interrupt}
 * 基于Tenet特点，所有Command需要指定模拟器中的执行时间，但不保证一定按照执行时间执行（当执行时间小于模拟器时间时）。
 * 所以在通常情况下，约定调用时间不得先于模拟器时间。
 * @see tenet.protocol.interrupt
 * @see tenet.core.Simulator
 * @author meilunsheng
 * @version 09.01
 */
public abstract class Command implements Comparable<Command> {

	protected ICaller m_caller;
	protected String m_name;
	protected IReceiver m_recv;
	protected Double m_time;

	/**
	 * Command模式原型的构造函数
	 * @param m_time 调用时间
	 * @param m_recv 命令接受者
	 */
	protected Command(double m_time, IReceiver m_recv) {
		this(null, m_time, m_recv);
	}

	/**
	 * CBC模式构造函数
	 * @param caller 调用者
	 * @param m_time 调用时间
	 * @param m_recv 命令接受者
	 */
	protected Command(ICaller caller, double m_time, IReceiver m_recv) {
		super();
		this.m_time = m_time;
		this.m_recv = m_recv;
		assert this.m_recv != null : "There must be a receiver for any command. But "
				+ this.getClass().getName()
				+ " Instance:"
				+ this.toString()
				+ " have no receiver";
		assert this.m_time != null : "There must be an execute-time for any command. But "
				+ this.getClass().getName()
				+ " Instance:"
				+ this.toString()
				+ " has not.";
		this.m_caller = caller;
	}

	public final IParam _execute() {
		IParam result = this.execute();
		if (this.m_caller != null) {
			m_caller.onRecall(this, this.m_recv, result);
			return null;
		} else
			return result;
	}

	@Override
	public final int compareTo(Command obj) {
		return m_time.compareTo(obj.m_time);
	}

	public void dump() {
		System.out.println(this.getName());
	}

	/**
	 * 命令的实际调用
	 * @return 回调参数，相当于函数返回値，没有指定调用者时无效。
	 */
	public abstract IParam execute();

	/**
	 * 获得调用者
	 * @return 调用者
	 */
	public final ICaller getCaller() {
		return this.m_caller;
	}

	/**
	 * 获得执行时间
	 * @return 执行时间
	 */
	public final Double getExecuteTime() {
		return this.m_time;
	}

	public final String getName() {
		if (m_name != null)
			return m_name;
		else
			return this.getClass().getName();
	}

	public final IReceiver getReceiver() {
		return this.m_recv;
	}

}
