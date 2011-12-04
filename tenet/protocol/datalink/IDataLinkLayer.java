package tenet.protocol.datalink;

import tenet.protocol.IProtocol;
import tenet.protocol.interrupt.InterruptObject;
import tenet.protocol.interrupt.InterruptParam;
import tenet.protocol.statecontrol.IStateSetable;
import tenet.util.pattern.serviceclient.IClient;

/**
 * Datalink层的标准接口.<br>
 * 该接口可使能.<br>
 * 该接口为作为IClient的特征为{@link tenet.protocol.datalink.MediumAddress}，特征域即为该接口的实例的MAC地址.<br>
 * @see tenet.protocol.statecontrol.IStateSetable
 * @see tenet.util.pattern.serviceclient
 * @see tenet.protocol.datalink.MediumAddress
 * @author meilunsheng
 * @version 09.01
 */
public interface IDataLinkLayer extends IStateSetable, IProtocol,
		IClient<MediumAddress> {

	/**
	 * 接受过程的中断信号参数.<br>
	 * 必定伴随 {@link IDataLinkLayer#INT_FRAME_RECEIVE}中断返回
	 * @see IDataLinkLayer#INT_FRAME_RECEIVE
	 * @author meilunsheng
	 * @version 09.01
	 */
	public class ReceiveParam extends InterruptParam {
		/**
		 * 调用发起者（即被中断者）
		 */
		public InterruptObject caller;
		/**
		 * 中断发起者（即对应的Datalink实例）
		 */
		public IDataLinkLayer datalink;
		/**
		 * 返回参数.对应不同的{@link #status}具有不同的含义.
		 * @see tenet.protocol.datalink.IDataLinkLayer.ReceiveStatus
		 * @see #status
		 */
		public FrameParamStruct frame;
		/**
		 * 接受过程的完成状态.
		 * @see tenet.protocol.datalink.IDataLinkLayer.ReceiveStatus
		 */
		public ReceiveStatus status;

		public ReceiveParam(ReceiveStatus status, InterruptObject caller,
				IDataLinkLayer datalink, FrameParamStruct frame) {
			super();
			this.status = status;
			this.caller = caller;
			this.datalink = datalink;
			this.frame = frame;
		}
	}

	/**
	 * 接收函数中断返回状态的枚举.
	 * @author meilunsheng
	 * @version 09.01
	 */
	public enum ReceiveStatus {
		/**
		 * datalink层处于非连入状态时，调用了{@link tenet.protocol.datalink.IDataLinkLayer#receiveFrame(FrameParamStruct)}将会导致调用者接收到这个中断.<br>
		 * 中断的frame参数将为调用{@link tenet.protocol.datalink.IDataLinkLayer#receiveFrame(FrameParamStruct)}时的param参数.
		 */
		dataLinkOff, 
		/**
		 * 接收到对应ethertype的帧，但是该帧存在校验错误。<br>
		 * 中断的frame参数将为实际接收的frame
		 */
		frameCheckError, /**
		 * 当同一ethertype已经有调用{@link tenet.protocol.datalink.IDataLinkLayer#receiveFrame(FrameParamStruct)}，并依然处在接收过程中时，后一次的{@link tenet.protocol.datalink.IDataLinkLayer#receiveFrame(FrameParamStruct)}调用将立刻返回该中断.<br>
		 * 中断的frame参数将为后一次调用{@link tenet.protocol.datalink.IDataLinkLayer#receiveFrame(FrameParamStruct)}时的param参数.
		 */
		receiveCollision, /**
		 * 在调用receiveFrame后由于任何原因导致datalink不在处于可以进行接收帧的状态时，均触发这个中断.<br>
		 * 需要注意的是，如果由于datalink不再处于连入状态，导致无法接收，仅触发状态为{@link #receiveCollision}中断，而不会触发状态为{@link #dataLinkOff}.<br>
		 * 中断的frame参数将为调用{@link tenet.protocol.datalink.IDataLinkLayer#receiveFrame(FrameParamStruct)}时的param参数.
		 */
		receiveError, /**
		 * 接收到对应ethertype的帧，且帧校验正确。<br>
		 * 中断的frame参数将为实际接收的frame
		 */
		receiveOK
	}

	/**
	 * 传输过程的中断信号参数.<br>
	 * 必定伴随{@link IDataLinkLayer#INT_FRAME_TRANSMIT} 中断返回
	 * @see IDataLinkLayer#INT_FRAME_TRANSMIT
	 * @author meilunsheng
	 * @version 09.01
	 */
	public class TransmitParam extends InterruptParam {
		/**
		 * 调用发起者（即被中断者）
		 */
		public InterruptObject caller;
		/**
		 * 中断发起者（即对应的Datalink实例）
		 */
		public IDataLinkLayer datalink;
		/**
		 * 返回参数.对应不同的{@link #status}具有不同的含义.
		 * @see tenet.protocol.datalink.IDataLinkLayer.TransmitStatus
		 * @see #status
		 */
		public FrameParamStruct frame;
		/**
		 * 传输过程的完成状态.
		 * @see tenet.protocol.datalink.IDataLinkLayer.TransmitStatus
		 */
		public TransmitStatus status;

		public TransmitParam(TransmitStatus status, InterruptObject caller,
				IDataLinkLayer datalink, FrameParamStruct frame) {
			super();
			this.status = status;
			this.caller = caller;
			this.datalink = datalink;
			this.frame = frame;
		}
	}
	/**
	 * 传输函数中断返回状态的枚举.
	 * @author meilunsheng
	 * @version 09.01
	 */
	public enum TransmitStatus {
		/**
		 * datalink层处于非连入状态时，调用了{@link tenet.protocol.datalink.IDataLinkLayer#transmitFrame(FrameParamStruct)}将会导致调用者接收到这个中断.<br>
		 * 中断的frame参数将为调用transmitFrame{@link tenet.protocol.datalink.IDataLinkLayer#transmitFrame(FrameParamStruct)}时的param参数.
		 */
		dataLinkOff, 
		/**
		 * 当同一datalink已经有调用{@link tenet.protocol.datalink.IDataLinkLayer#transmitFrame(FrameParamStruct)}，并依然处在接收过程中时，后一次的{@link tenet.protocol.datalink.IDataLinkLayer#transmitFrame(FrameParamStruct)}调用将立刻返回该中断.<br>
		 * 中断的frame参数将为后一次调用{@link tenet.protocol.datalink.IDataLinkLayer#transmitFrame(FrameParamStruct)}时的param参数.
		 */
		transmitCollision, 
		/**
		 * 在调用transmitFrame后由于任何原因导致datalink不在处于可以进行传输帧的状态或者传输的数据大于对应的datalink层的MTU时，均触发这个中断.<br>
		 * 需要注意的是，如果由于datalink不再处于连入状态，导致无法传输，仅触发状态为{@link #transmitCollision}中断，而不会触发状态为{@link #dataLinkOff}.<br>
		 * 中断的frame参数将为调用{@link tenet.protocol.datalink.IDataLinkLayer#transmitFrame(FrameParamStruct)}时的param参数.
		 */
		transmitError, 
		/**
		 * 正常完成传输函数的执行
		 * 中断的frame参数将为调用{@link tenet.protocol.datalink.IDataLinkLayer#transmitFrame(FrameParamStruct)}时的param参数.
		 */
		transmitOK
	}

	/**
	 * 可等待中断信号。当该datalink被调用{@link #receiveFrame(FrameParamStruct)}后，当这个过程完成时调用者被该信号中断，参数为一个{@link tenet.protocol.datalink.IDataLinkLayer.ReceiveParam}
	 * @see tenet.protocol.datalink.IDataLinkLayer.ReceiveParam
	 */
	public final static int INT_FRAME_RECEIVE = 0x80000200;
	/**
	 * 可等待中断信号。当该datalink被调用{@link #receiveFrame(FrameParamStruct)}后，当这个过程完成时所有上层协议均会被该信号中断，参数为该datalink的MAC
	 */
	public final static int INT_FRAME_RECEIVE_READY = 0x80000600;
	/**
	 * 可等待中断信号。当该datalink被调用{@link #transmitFrame(FrameParamStruct)}后，当这个过程完成时调用者被该信号中断，参数为一个{@link tenet.protocol.datalink.IDataLinkLayer.TransmitParam}
	 * @see tenet.protocol.datalink.IDataLinkLayer.TransmitParam
	 */
	public final static int INT_FRAME_TRANSMIT = 0x80000100;
	/**
	 * 可等待中断信号。当该datalink被调用{@link #transmitFrame(FrameParamStruct)}后，当这个过程完成时所有上层协议均会被该信号中断，参数为该datalink的MAC
	 */
	public final static int INT_FRAME_TRANSMIT_READY = 0x80000500;
	/**
	 * 可等待中断信号。当datalink处于接入状态时，自身被禁用或者物理介质连接的datalink被禁用，或者物理介质被禁用，则会导致设备转换为非接入状态，同时向所有上层协议发送该中断信号。参数为该datalink的MAC
	 */
	public final static int INT_INTERFACE_DOWN = 0x80000300;
	/**
	 * 可等待中断信号。当datalink处于非接入状态时，自身、物理介质连接的datalink以及物理介质均被使能，则会导致设备转换为接入状态，同时向所有上层协议发送该中断信号。参数为该datalink的MAC
	 */
	public final static int INT_INTERFACE_UP = 0x80000400;

	/**
	 * 判断设备是否处于接入状态
	 * @return <b>true</b> 接入状态； <b>false</b> 非接入状态
	 */
	public boolean isLinkUp();

	/**
	 * 请求接收一个帧。
	 * @param param 接收参数<br>
	 * 这里要求param的ethertype通常必须为协议对应的ethertype，其他参数对接收没有实际作用。但该参数在非正常接收的情况下可以通过中断完整返回。
	 * @see FrameParamStruct
	 * @see ReceiveParam
	 * @see ReceiveStatus
	 * @see #INT_FRAME_RECEIVE
	 * @see #INT_FRAME_RECEIVE_READY
	 */
	public void receiveFrame(FrameParamStruct param);

	/**
	 * 请求发送一个帧.
	 * @param param 发送参数
	 * @see FrameParamStruct
	 * @see TransmitParam
	 * @see TransmitStatus
	 * @see #INT_FRAME_TRANSMIT
	 * @see #INT_FRAME_TRANSMIT_READY
	 */
	public void transmitFrame(FrameParamStruct param);
	
	/**
	 * 获得MTU
	 * @return MTU
	 */
	public int getMTU();
}
