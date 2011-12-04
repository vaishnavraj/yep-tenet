package tenet.util;

public class ByteLib {
	
	public static String byteToHex(byte value) {
		return Integer.toHexString(byteToUnsigned(value,0));
	}

	public static byte byteFromUnsigned(int value, int offset) {
		return (byte) ((value >> offset) & 0xFF);
	}

	/**
	 * Convert an int into its little-endian byte string representation.
	 * 
	 * @param array
	 *            the array in which to store the byte string.
	 * @param offset
	 *            the offset in the array where the string will start.
	 * @param value
	 *            the value to convert.
	 */
	public static void bytesFromInt(byte[] array, int offset, int value) {
		if (array.length > offset + 0)
			array[offset + 0] = (byte) ((value >> 0) & 0xFF);
		if (array.length > offset + 1)
			array[offset + 1] = (byte) ((value >> 8) & 0xFF);
		if (array.length > offset + 2)
			array[offset + 2] = (byte) ((value >> 16) & 0xFF);
		if (array.length > offset + 3)
			array[offset + 3] = (byte) ((value >> 24) & 0xFF);
	}

	/**
	 * Convert to an int from its little-endian byte string representation.
	 * 
	 * @param array
	 *            the array containing the byte string.
	 * @param offset
	 *            the offset of the byte string in the array.
	 * @return the corresponding int value.
	 */
	public static int bytesToInt(byte[] array, int offset) {
		return (int) ((array.length > offset + 0 ? (((int) array[offset + 0] & 0xFF) << 0)
				: 0)
				| (array.length > offset + 1 ? (((int) array[offset + 1] & 0xFF) << 8)
						: 0)
				| (array.length > offset + 2 ? (((int) array[offset + 2] & 0xFF) << 16)
						: 0) | (array.length > offset + 3 ? (((int) array[offset + 3] & 0xFF) << 24)
				: 0));
	}

	public static int byteToUnsigned(byte value, int offset) {
		return (int) ((int) value & 0xFF) << offset;
	}

	/**
	 * Returns the binary interval of original, starting at pos, masked by mask
	 * 
	 * @param value
	 *            the original value
	 * @param pos
	 *            the position
	 * @param mask
	 *            usually (1 << length) - 1
	 */
	public static int getBits(int value, int pos, int mask) {
		return (value >> pos) & mask;
	}

	/**
	 * Set the binary interval of original to value, starting at pos, masked by
	 * mask
	 * 
	 * @param original
	 *            original value
	 * @param value
	 *            new partial value
	 * @param pos
	 *            the position
	 * @param mask
	 *            usually (1 << length) - 1
	 * @return the new value
	 */
	public static int setBits(int original, int value, int pos, int mask) {
		return original & (~(mask << pos)) | (value << pos);
	}
}
