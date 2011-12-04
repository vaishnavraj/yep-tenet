package tenet.protocol.datalink;

import java.util.Arrays;

import tenet.protocol.interrupt.InterruptParam;
import tenet.util.ByteLib;

/**
 * 该类为一个帧的抽象结构，包含了一个帧所必须的几个域.
 * 该类用于Datalink层接口的参数传递，以及相对应的中断信号返回时的参数.
 * @see tenet.protocol.interrupt.InterruptParam
 * @author meilunsheng
 * @version 09.01
 *  
 */
public class FrameParamStruct extends InterruptParam {
	/**
	 * 该静态过程将一个byte串转换成一个FrameParamStruct结构.
	 * @param _data 需要被转换的byte串
	 * @return 转换后的结构
	 */
	public static FrameParamStruct fromBytes(byte[] _data) {
		return new FrameParamStruct(MediumAddress.fromBytes(Arrays.copyOfRange(
				_data, 0, 6)), MediumAddress.fromBytes(Arrays.copyOfRange(
				_data, 6, 12)), ByteLib.bytesToInt(
				Arrays.copyOfRange(_data, 12, 14), 0), Arrays.copyOfRange(
				_data, 14, _data.length));

	}

	/**
	 * 数据域
	 */
	public byte[] dataParam;
	/**
	 * 目标MAC
	 */
	public MediumAddress destinationParam;
	/**
	 * 源MAC
	 */
	public MediumAddress sourceParam;
	/**
	 * 该帧承载的上层协议的ethertype序号，设置不对应的值可能导致收发双方错误的中断被触发。
	 */
	public int typeParam;

	/**
	 * @param destinationParam 目标MAC，不可为空
	 * @param sourceParam 源MAC，不可为空
	 * @param typeParam ethertype，不可为空
	 * @param dataParam 数据段，不可为空
	 */
	public FrameParamStruct(MediumAddress destinationParam,
			MediumAddress sourceParam, int typeParam, byte[] dataParam) {
		super();
		this.destinationParam = destinationParam;
		this.sourceParam = sourceParam;
		this.typeParam = typeParam;
		this.dataParam = dataParam;
	}

	/**
	 * 将该对象转换成对应的比特串。
	 * @return 转换后的串
	 */
	public byte[] toBytes() {
		byte[] ret = new byte[14 + dataParam.length];
		System.arraycopy(destinationParam.toBytes(), 0, ret, 0, 6);
		System.arraycopy(sourceParam.toBytes(), 0, ret, 6, 6);
		ByteLib.bytesFromInt(ret, 12, typeParam);
		System.arraycopy(dataParam, 0, ret, 14, dataParam.length);
		return ret;
	}

	/**
	 * 覆盖原有的Object.toString()。输出该帧的信息，数据段部分将逐字节的输出ascii码.
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("DST=").append(destinationParam.toString()).append("\t");
		builder.append("SRC=").append(sourceParam.toString()).append("\t");
		builder.append("TYPE=").append(typeParam).append("\t");
		builder.append("DATA=");
		for (byte b : dataParam)
			builder.append(b).append(",");
		builder.append("EOF");
		return builder.toString();
	}
}
