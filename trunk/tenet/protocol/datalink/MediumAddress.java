package tenet.protocol.datalink;

import java.util.Arrays;

import tenet.protocol.interrupt.InterruptParam;
import tenet.util.ByteLib;

/**
 * MAC类，可以作为中断信号的参数。采用的书写方式为aa:bb:cc:dd:ee:ff形式。
 * @see tenet.protocol.interrupt.InterruptParam
 * @author meilunsheng
 * @version 09.01
 */
public class MediumAddress extends InterruptParam {
	/**
	 * 广播MAC地址.
	 */
	public static final MediumAddress MAC_ALLONE = new MediumAddress(
			"FF:FF:FF:FF:FF:FF");
	/**
	 * 0 MAC.可作为MAC地址未知的含义。
	 */
	public static final MediumAddress MAC_ZERO = new MediumAddress(
			"00:00:00:00:00:00");

	/**
	 * 从一个6位的比特串生成一个MAC类。注意任意一个byte都将按物理形式转换成unsigned byte
	 * @param addr
	 * @return 对应的MAC类
	 */
	public static MediumAddress fromBytes(byte[] addr) {
		int[] t = new int[6];
		for (int i = 0; i < 6; ++i)
			t[i] = ByteLib.byteToUnsigned(addr[i], 0);
		return new MediumAddress(t);
	}

	/**
	 * 从一个6个元素的整数数组生成一个MAC类。每个整数仅最低8bits有效
	 * @param addr
	 * @return 对应的MAC类
	 */
	public static MediumAddress fromBytes(int[] addr) {
		return new MediumAddress(addr);
	}

	/**
	 * 从字符串生成MAC类
	 * @param addr
	 * @return 对应的MAC类
	 */
	public static MediumAddress fromString(String addr) {
		return new MediumAddress(addr);
	}

	protected int[] address;

	public MediumAddress(int[] address) {
//		AssertLib
//				.AssertTrue(address != null && address.length == 6,
//						"MAC must be a 6 bytes array.[@MediumAddress.<constructor>(byte[])]");
		this.address = address.clone();
	}

	public MediumAddress(String address) {
		parse(address);
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MediumAddress other = (MediumAddress) obj;
		if (!Arrays.equals(address, other.address))
			return false;
		return true;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + Arrays.hashCode(address);
		return result;
	}

	protected void parse(String addr) {
//		boolean check = false;
//		if (addr != null) {
//			String[] parts = addr.split("\\-");
//			if (parts.length == 6)
//				for (String part : parts)
//					check = check && part.matches("[0-9a-f][0-9a-f]");
//			else
//				check = false;
//		} else
//			check = false;
//		AssertLib.AssertTrue(check, "MAC format mistake.");
		String[] ad = addr.split(":");
		this.address = new int[6];
		for (int i = 0; i < 6; ++i)
			address[i] = Integer.valueOf(ad[i], 16);
	}

	/**
	 *  将该MAC类的地址转换为6字节byte串
	 * @return byte串
	 */
	public byte[] toBytes() {
		byte[] ret = new byte[6];
		for (int i = 0; i < 6; ++i)
			ret[i] = ByteLib.byteFromUnsigned(address[i], 0);
		return ret;
	}

	/**
	 * 将该MAC类的地址转换成字符串
	 * @return 符合格式标准的字符串
	 */
	public String toString() {
		StringBuilder builder = new StringBuilder();
		for (int i = 0; i < 6; ++i)
			builder.append(i == 0 ? "" : ":").append(
					Integer.toHexString(this.address[i]));
		return builder.toString();
	}
}
