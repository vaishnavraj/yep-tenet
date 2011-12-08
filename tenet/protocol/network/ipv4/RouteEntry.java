package tenet.protocol.network.ipv4;

public class RouteEntry implements Comparable<RouteEntry> {
	Integer address;
	int mask;
	int sendLinknum;
	Integer nextAddress;
	int metric;
	
	public RouteEntry(Integer address, int mask, int sendLinknum ,Integer nextAddress, int metric){
		this.address = address;
		this.mask = mask;
		this.sendLinknum = sendLinknum;
		this.nextAddress = nextAddress;
		this.metric = metric;
	}
	
	public boolean suitable(Integer destIPAddr){
		//System.out.println("mask :"+mask+" "+address+" "+destIPAddr);
		//System.out.println("mask :"+mask+" "+IPv4.IPtoString(address)+" "+IPv4.IPtoString(destIPAddr));
		if (mask == 0 )return true;
		int a = (int) (((int) (address.intValue() >>> 24 ) & 0xFF) << 0)
		| (((int) (address.intValue() >>> 16 ) & 0xFF) << 8)
		| (((int) (address.intValue() >>> 8 ) & 0xFF) << 16)
		| (((int) (address.intValue()) & 0xFF) << 24);
int b = (int) (((int) (destIPAddr.intValue() >>> 24 ) & 0xFF) << 0)
		| (((int) (destIPAddr.intValue() >>> 16 ) & 0xFF) << 8)
		| (((int) (destIPAddr.intValue() >>> 8 ) & 0xFF) << 16)
		| (((int) (destIPAddr.intValue()) & 0xFF) << 24);
		//System.out.println("RouteEntryinSubnet "+address+" "+destIPAddr+" "+a+" "+b+" "+(a >>> (32-mask))+" "+(b >>> (32-mask)));
		if ((a >>> (32-mask))==(b >>> (32-mask))) return true;
		return false;
	}
	
	public int compareTo(RouteEntry r){
		if (metric < r.metric) return -1;
		if (metric == r.metric) return 0;
		return 1;
	}
	
}