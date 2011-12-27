package tenet.node;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import tenet.node.INode;
import tenet.protocol.interrupt.InterruptObject;
import tenet.protocol.interrupt.InterruptParam;
import tenet.protocol.network.ipv4.IPv4;
import tenet.protocol.network.ipv4.RouteEntry;
import tenet.protocol.statecontrol.IStateSetable;
import tenet.util.ByteLib;
import tenet.util.pattern.serviceclient.IClient;

public class MyNode extends InterruptObject implements INode {
	
	HashMap<IClient<?>,byte[]> addrmap=new HashMap<IClient<?>,byte[]>();
	HashSet<IClient<?>> clientset=new HashSet<IClient<?>>();
	boolean isEnabled=false;
	@Override
	public void registryClient(IClient<Object> client) {
		// TODO Auto-generated method stub
		clientset.add(client);
		client.attachTo(this);		
	}

	@Override
	public void unregistryClient(Object id) {
		// TODO Auto-generated method stub

	}

	@Override
	public void unregistryClient(IClient<Object> client) {
		// TODO Auto-generated method stub

	}

	@Override
	public void disable() {
		// TODO Auto-generated method stub
		if(!isEnabled) return;
		for(IClient<?> client:clientset)
			if(client instanceof IStateSetable)
				((IStateSetable)client).disable();
		isEnabled=false;
	}

	@Override
	public void enable() {
		if(isEnabled) return;
		for(IClient<?> client:clientset)
			if(client instanceof IStateSetable)
				((IStateSetable)client).enable();
		isEnabled=true;
	}

	@Override
	public boolean isEnable() {
		// TODO Auto-generated method stub
		return isEnabled;
	}

	@Override
	public void dump() {
		// TODO Auto-generated method stub

	}

	@Override
	public void setAddress(IClient<?> protocol, byte[] address) {
		// TODO Auto-generated method stub
		addrmap.put(protocol, address);
	}

	@Override
	public byte[] getAddress(IClient<?> protocol) {
		// TODO Auto-generated method stub
		return addrmap.get(protocol);
	}
	
	public void removeProtocal(IClient<?> protocol) {
		addrmap.remove(protocol);
		clientset.remove(protocol);
	}
	
	//the following is for being a IPv4 router
	protected List<IPv4> IPv4Links = new LinkedList<IPv4>();
	protected Map<Integer,IPv4> IPv4Address = new HashMap<Integer,IPv4>();
	
	public void registryIpv4(IPv4 ip){
		IPv4Links.add(ip);
		//System.out.println("registryIpv4 "+ip.getIntegerAddress());
		IPv4Address.put(ip.getIntegerAddress(), ip);
	}
	
	public int getIPv4Linknum(Integer ipaddr){
		//System.out.println("getIPv4Linknum "+ipaddr);
		if (!IPv4Address.containsKey(ipaddr)) return -1;
		IPv4 ip = IPv4Address.get(ipaddr);
		if (!IPv4Links.contains(ip)) return -1;
		return IPv4Links.indexOf(ip);
	}
	
	public int getIPv4Linknum(IPv4 ip){
		if (!IPv4Links.contains(ip)) return -1;
		return IPv4Links.indexOf(ip);
	}
	
	public IPv4 getIPv4ByAddress(Integer ipaddr){
		if (!IPv4Address.containsKey(ipaddr)) return null;
		IPv4 ip = IPv4Address.get(ipaddr);
		if (!IPv4Links.contains(ip)) return null;
		return ip;
	}
	
	public IPv4 getIPv4ByLinknum(int index){
		return IPv4Links.get(index);
	}
	
	public List<RouteEntry> RouteTable = new LinkedList<RouteEntry>();
	public RouteEntry DefaultRoute;
	@Override
	protected void interruptHandle(int signal, InterruptParam param) {
		// TODO Auto-generated method stub
		
	}
	

}
