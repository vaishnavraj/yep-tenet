package tenet.protocol.datalink.sfdatalink;

import java.util.ArrayList;

import tenet.core.Simulator;
import tenet.node.l2switch.L2Switch;
import tenet.protocol.datalink.FrameParamStruct;
import tenet.protocol.datalink.IDataLinkLayer;
import tenet.protocol.datalink.MediumAddress;
import tenet.protocol.datalink.IDataLinkLayer.ReceiveParam;
import tenet.protocol.datalink.IDataLinkLayer.ReceiveStatus;
import tenet.protocol.datalink.SEther.SimpleEthernetDatalink;
import tenet.protocol.interrupt.InterruptObject;
import tenet.protocol.physics.Link;
import tenet.util.pattern.serviceclient.IClient;

/**
* 二层交换需要使用到的具有储存转发能力的IDatalinkLayer
* 一般来说，只有L2Switch会使用到StoreForwardingDatalink
* 同时，StoreForwardingDatalink绑定到的Node一般是L2Switch
*/
public class StoreForwardingDatalink extends SimpleEthernetDatalink {

	protected final static boolean debug=false;

    /**
     * 所要转发的帧的队列
     */
	public ArrayList<FrameParamStruct> m_send_queue;

	public StoreForwardingDatalink(MediumAddress m_mac) {
		super(m_mac);
		m_send_queue = new ArrayList<FrameParamStruct>();
	}

	/**
	* 当链路断开时进行的处理过程
	*/
	@Override
	protected void linkDown() {
		super.linkDown(); //调用父类过程，使得状态改变的信号达到所有相关对对象。
		//重置所有中断的等待状态，避免接收到不合理的中断导致逻辑错误
		this.resetInterrupt(Link.INT_LINK_READ_ERROR);
		this.resetInterrupt(Link.INT_LINK_READ_OK);
		this.resetInterrupt(INT_RECEIVE_LINKDOWN);
		this.resetInterrupt(INT_RECEIVE_COLLISION);
		this.resetInterrupt(Link.INT_LINK_SEND_ERROR);
		this.resetInterrupt(Link.INT_LINK_SEND_OK);
		this.resetInterrupt(INT_SEND_LINKDOWN);
		this.resetInterrupt(INT_SEND_COLLISION);
		this.m_send_queue.clear();//所有等待发送的帧都可以被丢弃
	}

	/**
	* 当链路变为连接状态时进行的处理
	*/
	@Override
	protected void linkUp() {
		super.linkUp();//调用父类过程，使得状态改变的信号达到所有相关对对象。
		this.waitReceiveSignal();//等待所有接受需要的中断信号
	}

	void sendnext(){
		if(m_transmit_client.size()==0 && !m_send_queue.isEmpty()){
			transmitFrame(m_send_queue.get(0));
			m_send_queue.remove(0);
			
		}
			
	}
	//下面的这些过程都是在SimpleEthernetDatalink.interruptHandler中调用的函数，具体对应的中断信号可以去原函数查看
	
	/**
	 * 处理接收帧出现错误的情况
	 */
	@Override
	protected void onReadError(FrameParamStruct param) {
		//drop the param
		//resetReceiveInterrupts(param.typeParam);
	}
	
	public static final MediumAddress BROADCAST_MA = MediumAddress.ZERO_MA;//new MediumAddress("FF:FF:FF:FF:FF:FF");
	private boolean isBroadcast(MediumAddress mac){
		if (mac.toString().equals(BROADCAST_MA.toString())) return true;
		return false;
	}
	/**
	 * 处理接收帧正确的情况
	 */
	@Override
	protected void onReadOK(FrameParamStruct param) {
		//resetReceiveInterrupts(param.typeParam);
		if (m_node instanceof L2Switch){
			L2Switch Switch = (L2Switch)m_node;
			Switch.learnMAC(param.sourceParam,this);
			if (!isBroadcast(param.destinationParam) && Switch.m_macport_map.containsKey(param.destinationParam)){
				SimpleEthernetDatalink des = Switch.m_macport_map.get(param.destinationParam);
				((StoreForwardingDatalink)des).m_send_queue.add(param);
				((StoreForwardingDatalink)des).sendnext();
			}else
				for (SimpleEthernetDatalink DataLink: Switch.m_datalinks.values()){
					((StoreForwardingDatalink)DataLink).m_send_queue.add(param);
					((StoreForwardingDatalink)DataLink).sendnext();
				}
		}
	}

	/**
	 * 处理接收帧出现校验错误的情况
	 */
	@Override
	protected void onReadOKwithCheckError(FrameParamStruct param) {
		//drop the param
		//resetReceiveInterrupts(param.typeParam);
	}

	/**
	 * 处理出现接收冲突的自中断的情况
	 */
	@Override
	protected void onReceiveRequireCollision(FrameParamStruct param) {
		//drop the param
	}

	/**
	 * 处理出现非接入状态接收的自中断的情况
	 */
	@Override
	protected void onReceiveRequireLinkDown(FrameParamStruct param) {
		//drop the param
		//resetReceiveInterrupts(param.typeParam);
	}

	/**
	 * 处理发送帧出现错误的情况
	 */
	@Override
	protected void onSendError(FrameParamStruct param) {
		//drop the param and sent next
		resetTransmitInterrupts(param.typeParam);
		sendnext();
	}

	/**
	 * 处理成功发送帧的情况
	 */
	@Override
	protected void onSendOK(FrameParamStruct param) {
		//sent next
		resetTransmitInterrupts(param.typeParam);
		sendnext();
	}

	/**
	 * 处理出现发送冲突的自中断的情况
	 */
	@Override
	protected void onTransmitRequireCollision(FrameParamStruct param) {
		//drop the param and sent next
		resetTransmitInterrupts(param.typeParam);
		sendnext();
	}

	/**
	 * 处理出现非接入状态发送的自中断的情况
	 */
	@Override
	protected void onTransmitRequireLinkDown(FrameParamStruct param) {
		//drop the param and sent next
		resetTransmitInterrupts(param.typeParam);
		sendnext();
	}
}
