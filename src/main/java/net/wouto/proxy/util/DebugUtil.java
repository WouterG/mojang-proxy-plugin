package net.wouto.proxy.util;

public class DebugUtil {

	/**
	 * @see #printStackTrace(Thread)
	 */
	public static void printStackTrace() {
		printStackTrace(Thread.currentThread());
	}

	/**
	 * Quickly print a stack trace without throwing any errors
	 */
	public static void printStackTrace(Thread t) {
		StackTraceElement[] st = t.getStackTrace();
		System.out.println("StackTracePrint: Application requested stacktrace print - This is not an error!");
		if (st.length <= 2) {
			System.err.println("\tFailed to write stacktrace! We ignore the first 2 elements but there aren't any others!");
			return;
		}
		for (int i = 2; i < st.length; i++) {
			StackTraceElement s = st[i];
			String line = s.isNativeMethod() ? "Native" : Integer.toString(s.getLineNumber());
			if (s.isNativeMethod()) {
				System.out.println("\t\tnative line number: " + Integer.toString(s.getLineNumber()));
			}
			System.out.println("\tat " + s.getClassName() + "." + s.getMethodName() + "(" + s.getFileName() + ":" + line + ")");
		}
	}

}
