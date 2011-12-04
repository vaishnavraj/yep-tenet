package tenet.protocol.transport;

import tenet.protocol.IProtocol;
import tenet.util.pattern.serviceclient.IClient;

public interface ITransportLayer extends IProtocol, IClient<Integer> {

}
