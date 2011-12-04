package tenet.util.pattern.serviceclient;

public interface IRegistryableService<ClientIdentity> extends
		IService<ClientIdentity> {
	public void registryClient(IClient<ClientIdentity> client);

	public void unregistryClient(ClientIdentity id);

	public void unregistryClient(IClient<ClientIdentity> client);
}
