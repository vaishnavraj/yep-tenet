package tenet.protocol;

/**
 * 协议接口，所有协议都需要直接或间接的实现这个接口
 * @author meilunsheng
 * @version 09.01
 */
public interface IProtocol {
	/**
	 * 
	 */
	public void dump();

	/**
	 * 得到协议的标识
	 * @return 协议的标识
	 */
	public String getIdentify();

	/**
	 * 返回对象的名称
	 * @return 对象名称
	 */
	public String getName();
}
