package tenet.protocol.datalink;

import org.knf.util.AssertLib;

import tenet.protocol.datalink.SEther.SimpleEthernetDatalink;
import tenet.protocol.network.arp.ARP;
import tenet.util.pattern.serviceclient.IClient;

public class L3iface extends SimpleEthernetDatalink {

	public L3iface(MediumAddress m_mac) {
		super(m_mac);
		// TODO Auto-generated constructor stub
	}
	
	@Override
	public void registryClient(IClient<Integer> client) {
		AssertLib.AssertFalse(
				(this.m_network_layers.containsKey(client.getUniqueID())),
				"invaild registry on datalink SEther", false);
		this.m_network_layers.put(client.getUniqueID(), client);
		client.attachTo(this);
		ARP arp = new ARP();
		m_node.registryClient(((IClient)arp));
		this.m_network_layers.put(arp.getUniqueID(), arp);
		arp.attachTo(this);
		arp.registryClient(client);
	}

}
