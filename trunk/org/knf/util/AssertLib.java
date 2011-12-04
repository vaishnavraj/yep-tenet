package org.knf.util;

import java.io.PrintStream;

public class AssertLib {
	public static boolean flag = false;
	protected static PrintStream out = null;

	public static void AssertFalse(boolean exp, String assertStatment) {
		AssertTrue(!exp, assertStatment, true);
	}

	public static void AssertFalse(boolean exp, String assertStatment,
			boolean forceExit) {
		AssertTrue(!exp, assertStatment, forceExit);
	}

	public static void AssertTrue(boolean exp, String assertStatment) {
		AssertTrue(exp, assertStatment, true);
	}

	public static void AssertTrue(boolean exp, String assertStatment,
			boolean forceExit) {
		if (!flag)
			return;
		if (exp)
			return;
		try {
			getOutput().println(assertStatment);
			getOutput().flush();
		} catch (Exception e) {
			System.err.println(assertStatment);
		}
		if (forceExit)
			System.exit(1);
		return;
	}

	protected static PrintStream getOutput() {
		if (out == null)
			out = System.err;
		return out;
	}

	public static void initialize(PrintStream out) {
		AssertLib.out = out;
	}

}
