package com.mazda.gms3.dmt.utils;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.mazda.gms3.dmt.logging.LogManager;
import com.mazda.gms3.dmt.logging.Logger;

/**
 * REPLACEMENT FOR Thread.stop().
 *
 * Every job in this application runs in its own named thread, and the Abort buttons used to
 * kill that thread with Thread.stop(). From Java 20 onwards Thread.stop() only throws
 * UnsupportedOperationException, so an abort changed the status in the database while the
 * job itself kept running to the end.
 *
 * abort() now only FLAGS the thread. The job thread notices the flag at its next checkpoint
 * - checkpoint() is called every time a database connection is requested, which a running job
 * does continuously - and ends itself by throwing AbortSignal. AbortSignal is an Error, not an
 * Exception, for the same reason the ThreadDeath that Thread.stop() raised was one: the job
 * code is full of catch(Exception) blocks, and the signal has to pass through them while still
 * running every finally block (connections, streams).
 */
public class ThreadAbortUtil {

	private static Logger logger = LogManager.getLogger(ThreadAbortUtil.class);

	/** Names of the job threads that have been asked to stop. */
	private static final Set<String> abortRequested = Collections.synchronizedSet(new HashSet<String>());

	/** Raised inside the job thread itself when it finds that it has been asked to stop. */
	public static class AbortSignal extends Error {
		private static final long serialVersionUID = 1L;

		public AbortSignal(String message) {
			// no stack trace - this is a normal, requested end of the job
			super(message, null, false, false);
		}
	}

	/**
	 * Ask the given thread to stop.
	 *
	 * A thread asking for ITSELF to be stopped (the job calls this as its very last step) has
	 * nothing left to do, so nothing is flagged and the thread simply runs out.
	 */
	public static void abort(Thread thread) {
		if (null == thread || null == thread.getName()) {
			return;
		}
		if (thread == Thread.currentThread()) {
			logger.info("abort :: job thread {" + thread.getName() + "} has finished.");
			return;
		}
		abortRequested.add(thread.getName());
		logger.info("abort :: abort requested for job thread {" + thread.getName()
				+ "}. It stops at its next checkpoint.");
	}

	/**
	 * Called from the job code. Ends the CURRENT thread if an abort was requested for it.
	 */
	public static void checkpoint() {
		if (abortRequested.isEmpty()) {
			return;
		}
		String name = Thread.currentThread().getName();
		if (abortRequested.remove(name)) {
			logger.info("checkpoint :: job thread {" + name + "} stopped on abort request.");
			throw new AbortSignal("Job thread " + name + " aborted on request");
		}
	}
}
