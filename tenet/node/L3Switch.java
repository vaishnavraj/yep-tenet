package tenet.node;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import tenet.core.Simulator;
import tenet.protocol.IProtocol;
import tenet.protocol.interrupt.InterruptObject;
import tenet.protocol.interrupt.InterruptParam;
import tenet.protocol.network.ipv4.IPProtocol;
import tenet.protocol.network.ipv4.IPv4;
import tenet.protocol.network.ipv4.RouteEntry;
import tenet.protocol.rip.RIPProtocol;
import tenet.protocol.statecontrol.IStateSetable;
import tenet.util.ByteLib;
import tenet.util.pattern.serviceclient.IClient;

public class L3Switch extends MyNode {
	
	
	@Override
	public void registryClient(IClient<Object> client) {
		clientset.add(client);
		client.attachTo(this);
		if ((IProtocol)client instanceof IPProtocol){
			RIPProtocol rip = new RIPProtocol();
			RIPTable.add(rip);
			this.registryClient((IClient)rip);
			IProtocol ip = (IProtocol)client;
			((IPProtocol)ip).registryClient(rip);
			
		}
	}

		
	//for dynamic routing
	protected final static int TIMER = 0x00000001;
	
	public List<RIPProtocol> RIPTable = new LinkedList<RIPProtocol>();
	
	public L3Switch(){
		Random random = new Random();
		double firstWait = Math.abs(random.nextInt())%30;
		wait(TIMER, firstWait);
	}
	
	public void getData(byte[] data, int linknum, Integer srcip, Integer destip){
		List<RouteEntry> getRoute = (new RIPSegment(data)).getRouteTable(linknum);
		for (int i=0;i<getRoute.size();i++){
			RouteEntry nre = getRoute.get(i);
			//System.out.println(get RIP pkt form "+IPv4.IPtoString(srcip));
			if (IPv4Links.get(linknum).getIntegerAddress().equals(nre.nextAddress)) continue;
			boolean isnew = false;
			for (int r=0;r<RouteTable.size();r++){
				RouteEntry re = RouteTable.get(r);
				if (re.suitable(nre.address, nre.mask)){
					isnew = true;
					re.update(nre.metric, srcip, linknum);
				}
			}
			if (!isnew) {
				nre.nextAddress = srcip;
				//System.out.println(IPv4.IPtoString(nre.address)+" "+isnew+" "+IPv4.IPtoString(nre.nextAddress));
				RouteTable.add(nre);
			}
		}
		Collections.sort(RouteTable);
	}
	
	@Override
	protected void interruptHandle(int signal, InterruptParam param) {
		switch (signal) {
		case TIMER:
			//System.out.println("TIMER to send RIP pkt");
			this.resetInterrupt(TIMER);
			wait(TIMER, 30.0);
			//signal all rip to send pkt
			for (int i=0;i<RouteTable.size();){
				if (RouteTable.get(i).type == 1 && Simulator.GetTime()-RouteTable.get(i).time >100){
					RouteEntry re = RouteTable.get(i);
					System.out.println("remove "+re.type+" "+re.time+" "+IPv4.IPtoString(re.address)+" "+IPv4.IPtoString(re.nextAddress));
					RouteTable.remove(i);
					
				}else i++;
			}
			if (RouteTable.size()>0){
				RIPSegment newSeg = new RIPSegment(RouteTable);
				for (int i=0;i<RIPTable.size();i++) RIPTable.get(i).send(newSeg.toBytes());
			}
		}

	}

}

class RIPSegment {
	
	//2 bytes
	public int EntryNum;

	//2 bytes
	public int CheckSum;
	
	public byte[] data;
	
	public RIPSegment(List<RouteEntry> RouteTable){
		this.EntryNum = RouteTable.size();
		CheckSum = 0;
		
		data = new byte[EntryNum*16];
		for (int i=0;i<RouteTable.size();i++){
			//System.out.println("RIP entry");
			RouteEntry re = RouteTable.get(i);
			byte[] entry = new byte[4];
			
			ByteLib.bytesFromInt(entry, 0, re.address);
			System.arraycopy(entry, 0, data, i*16, 4);
			
			ByteLib.bytesFromInt(entry, 0, re.mask);
			System.arraycopy(entry, 0, data, i*16+4, 4);
			
			ByteLib.bytesFromInt(entry, 0, re.nextAddress);
			System.arraycopy(entry, 0, data, i*16+8, 4);
			
			ByteLib.bytesFromInt(entry, 0, re.metric);
			System.arraycopy(entry, 0, data, i*16+12, 4);
		}
	}
	
	public RIPSegment(byte[] Segment){
		//TODO check Length OK and checksum
		EntryNum = ByteLib.bytesToInt(Arrays.copyOfRange(Segment, 0, 2), 0);
		CheckSum = ByteLib.bytesToInt(Arrays.copyOfRange(Segment, 2, 4), 0);
		
		if (Segment.length!=(EntryNum*16+4)) System.out.println("error in RIPSeg");
		data = Arrays.copyOfRange(Segment, 4, Segment.length);
		
	}
	
	public List<RouteEntry> getRouteTable(int linknum){
		List<RouteEntry> RouteTable = new LinkedList<RouteEntry>();
		for (int i=0;i<EntryNum;i++){
			int ip = ByteLib.bytesToInt(Arrays.copyOfRange(data, i*16+0, i*16+4), 0);
			int mask = ByteLib.bytesToInt(Arrays.copyOfRange(data, i*16+4, i*16+8), 0);
			int nextip = ByteLib.bytesToInt(Arrays.copyOfRange(data, i*16+8, i*16+12), 0);
			int metric = ByteLib.bytesToInt(Arrays.copyOfRange(data, i*16+12, i*16+16), 0);
			RouteTable.add(new RouteEntry(ip, mask, linknum, nextip, metric, 1));
		}
		
		return RouteTable;
	}
	
	
	public byte[] toBytes(){
		byte[] segment;
		segment = new byte[data.length+4];
		
		byte[] EntryNum = new byte[2];
		ByteLib.bytesFromInt(EntryNum, 0, this.EntryNum);
		System.arraycopy(EntryNum, 0, segment, 0, 2);
		//TODO calc check sum
		byte[] CheckSum = new byte[2];
		ByteLib.bytesFromInt(CheckSum, 0, this.CheckSum);
		System.arraycopy(CheckSum, 0, segment, 2, 2);
		
		System.arraycopy(data, 0, segment, 4, data.length);
		
		return segment;
	}
	
}
