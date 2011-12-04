package tenet.protocol.statecontrol;

/**
 * 可使能接口。表示实现类可以被使能（启用）和禁用，并能查询状态。
 * @author meilunsheng
 * @version 09.01
 */
public interface IStateSetable {
	public void disable();

	public void enable();

	public boolean isEnable();
}
