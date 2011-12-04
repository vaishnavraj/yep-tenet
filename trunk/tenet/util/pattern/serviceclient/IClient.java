package tenet.util.pattern.serviceclient;

public interface IClient<UID> {
	public void attachTo(IService<?> service);

	public void detachFrom(IService<?> service);

	public UID getUniqueID();

	public void setUniqueID(UID id);
}
