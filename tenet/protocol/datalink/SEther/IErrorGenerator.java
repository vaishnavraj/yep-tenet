package tenet.protocol.datalink.SEther;

import tenet.protocol.datalink.FrameParamStruct;

public interface IErrorGenerator {
	public boolean check(FrameParamStruct frame);
}
