package tenet.node;

import tenet.protocol.statecontrol.IStateSetable;
import tenet.util.pattern.cbc.IReceiver;
import tenet.util.pattern.serviceclient.IClient;
import tenet.util.pattern.serviceclient.IRegistryableService;

/**
 * 节点接口，所有节点必须直接或间接的实现该接口，所有协议（物理介质以外）都能够注册到节点上
 * @author meilunsheng
 * @version 09.01
 */
public interface INode extends IRegistryableService<Object>, IStateSetable, IReceiver {
	public void dump();
	public void setAddress(IClient<?> protocol, byte[] address);
	public byte[] getAddress(IClient<?> protocol);
}
