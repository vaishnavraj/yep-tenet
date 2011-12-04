package tenet.anticheat;

public interface ICallWatcher {
	public void eventRecord(Object obj, StackTraceElement[] stackTraceElements);
}
