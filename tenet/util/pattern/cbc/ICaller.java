package tenet.util.pattern.cbc;

import tenet.util.pattern.IParam;

public interface ICaller {
	public void onRecall(Command cmd, IReceiver recv, IParam result);
}
