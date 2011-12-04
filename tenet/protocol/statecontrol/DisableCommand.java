package tenet.protocol.statecontrol;

import org.knf.util.AssertLib;

import tenet.util.pattern.IParam;
import tenet.util.pattern.cbc.Command;
import tenet.util.pattern.cbc.IReceiver;

/**
 * ½ûÓÃÃüÁî
 * @author meilunsheng
 * @version 09.01
 */
public class DisableCommand extends Command {

	public DisableCommand(double time, IReceiver recv) {
		super(time, recv);
		AssertLib.AssertTrue(recv instanceof IStateSetable,
				"Only IStateSetable can be the receiver of DisableCommand");
	}

	@Override
	public IParam execute() {
		((IStateSetable) (this.getReceiver())).disable();
		return null;
	}

}
