package tenet.protocol.statecontrol;

import org.knf.util.AssertLib;

import tenet.util.pattern.IParam;
import tenet.util.pattern.cbc.Command;
import tenet.util.pattern.cbc.IReceiver;

/**
 * Ê¹ÄÜÃüÁî
 * @author meilunsheng
 * @version 09.01
 */
public class EnableCommand extends Command {

	public EnableCommand(double time, IReceiver recv) {
		super(time, recv);
		AssertLib.AssertTrue(recv instanceof IStateSetable,
				"Only IStateSetable can be the receiver of EnableCommand");
	}

	@Override
	public IParam execute() {
		((IStateSetable) (this.getReceiver())).enable();
		return null;
	}

}
