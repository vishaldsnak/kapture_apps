package com.mazda.gms3.cdrom.bean;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.mazda.gms3.mdm.logging.Logger;
import com.mazda.gms3.mdm.utils.ApplicationProperties;

/**
 * WHERE ONE CD JOB SPENT ITS TIME (2026-10-30) - logged as a summary just before the job ends.
 * Also used by RMI Export and MNAO Data Export (start(jobName, title, contextLine)) for the same summary.
 *
 * Two kinds of numbers:
 *   - PHASES: wall clock of the job thread, one step after another. A phase started inside another
 *     (the folder copy inside the first WD document) is taken OUT of the outer one, so the phases add
 *     up to the job time. Each phase also shows how much of it went into file / DB operations made on
 *     the job thread itself.
 *   - OPERATIONS: every single file or DB call, timed where it is made (count, total, average, max,
 *     bytes). Copies run on several threads at once, so their totals can be more than the wall clock.
 *
 * One instance per job, reachable from the job thread and the copy threads it starts
 * (InheritableThreadLocal) - parallel CD jobs never mix their numbers. No job = every call does nothing.
 */
public final class CDRomJobTimings {

	public static final String FILE = "FILE";
	public static final String DB = "DB";
	public static final String MIXED = "MIXED";

	private static final InheritableThreadLocal<CDRomJobTimings> CURRENT = new InheritableThreadLocal<CDRomJobTimings>();

	private static class Stat {
		String category;
		long calls;
		long ms;
		long bytes;
		long maxMs;
		/** phases only: file / DB operations made on the job thread inside the phase */
		long fileMs;
		long dbMs;
	}

	private static class Frame {
		String name;
		String category;
		long resumedAt;
		long accumulated;
		long fileMs;
		long dbMs;
	}

	private final String jobName;
	/** "CD JOB" for CD Creation; RMI Export / MNAO Data Export pass their own (2026-10-30, same summary) */
	private final String title;
	/** where the job reads / writes - null = the CD line built from the cdrom.* keys */
	private final String contextLine;
	private final Thread jobThread;
	private final long started = System.currentTimeMillis();
	private final Map<String, Stat> phases = new LinkedHashMap<String, Stat>();
	private final Map<String, Stat> operations = new LinkedHashMap<String, Stat>();
	private final Deque<Frame> stack = new ArrayDeque<Frame>();
	private boolean summaryLogged = false;

	private CDRomJobTimings(String jobName, String title, String contextLine) {
		this.jobName = jobName;
		this.title = title;
		this.contextLine = contextLine;
		this.jobThread = Thread.currentThread();
	}

	/** Starts the timings of the CD job running on this thread. */
	public static CDRomJobTimings start(String jobName) {
		return start(jobName, "CD JOB", null);
	}

	/** Starts the timings of any other job running on this thread (RMI Export, MNAO Data Export). */
	public static CDRomJobTimings start(String jobName, String title, String contextLine) {
		CDRomJobTimings timings = new CDRomJobTimings(jobName, title, contextLine);
		CURRENT.set(timings);
		return timings;
	}

	public static void clear() {
		CURRENT.remove();
	}

	/** Start of a phase of the job thread - always pair with end() in a finally. */
	public static void begin(String phase, String category) {
		CDRomJobTimings t = CURRENT.get();
		if (null == t || Thread.currentThread() != t.jobThread) {
			return;
		}
		synchronized (t) {
			long now = System.currentTimeMillis();
			Frame outer = t.stack.peek();
			if (null != outer) {
				outer.accumulated += now - outer.resumedAt;
			}
			Frame f = new Frame();
			f.name = phase;
			f.category = category;
			f.resumedAt = now;
			t.stack.push(f);
		}
	}

	public static void end() {
		CDRomJobTimings t = CURRENT.get();
		if (null == t || Thread.currentThread() != t.jobThread) {
			return;
		}
		synchronized (t) {
			Frame f = t.stack.poll();
			if (null == f) {
				return;
			}
			long now = System.currentTimeMillis();
			long ms = f.accumulated + now - f.resumedAt;
			Stat s = stat(t.phases, f.name, f.category);
			s.calls++;
			s.ms += ms;
			s.maxMs = Math.max(s.maxMs, ms);
			s.fileMs += f.fileMs;
			s.dbMs += f.dbMs;
			Frame outer = t.stack.peek();
			if (null != outer) {
				outer.resumedAt = now;
			}
		}
	}

	/** One file or DB call (any thread of the job). bytes: 0 when not known. */
	public static void op(String operation, String category, long ms, long bytes) {
		CDRomJobTimings t = CURRENT.get();
		if (null == t) {
			return;
		}
		synchronized (t) {
			Stat s = stat(t.operations, operation, category);
			s.calls++;
			s.ms += ms;
			s.bytes += Math.max(0L, bytes);
			s.maxMs = Math.max(s.maxMs, ms);
			Frame f = t.stack.peek();
			if (null != f && Thread.currentThread() == t.jobThread) {
				if (FILE.equals(category)) {
					f.fileMs += ms;
				} else if (DB.equals(category)) {
					f.dbMs += ms;
				}
			}
		}
	}

	private static Stat stat(Map<String, Stat> map, String name, String category) {
		Stat s = map.get(name);
		if (null == s) {
			s = new Stat();
			s.category = category;
			map.put(name, s);
		}
		return s;
	}

	/** Writes the summary once (later calls do nothing). */
	public synchronized void logSummary(Logger logger) {
		if (summaryLogged) {
			return;
		}
		summaryLogged = true;
		long total = System.currentTimeMillis() - started;
		List<String> out = new ArrayList<String>();
		out.add("=================== " + title + " TIMING SUMMARY :: " + jobName + " ===================");
		out.add("Job time (wall clock): " + duration(total) + " (" + total + " ms)");
		if (null != contextLine) {
			out.add(contextLine);
		} else {
			out.add("CD folder / zip location: " + ApplicationProperties.getProperty("cdrom.schedule.path")
					+ " | content read from: " + ApplicationProperties.getProperty("cdrom.server.path.name")
					+ " | copy threads per job: " + ApplicationProperties.getProperty("cdrom.copy.parallel.threads"));
		}
		out.add("--- PHASES (wall clock of the job, they do not overlap; 'file' / 'DB' = operations inside the phase on the job thread) ---");
		out.add(String.format("  %-62s %5s %12s %7s %12s %12s %12s", "phase", "type", "time", "% job", "file ops", "DB ops", "rest"));
		long phaseSum = 0L;
		long fileSum = 0L;
		long dbSum = 0L;
		for (Map.Entry<String, Stat> e : phases.entrySet()) {
			Stat s = e.getValue();
			phaseSum += s.ms;
			long fileMs = FILE.equals(s.category) ? s.ms : s.fileMs;
			long dbMs = DB.equals(s.category) ? s.ms : s.dbMs;
			fileSum += fileMs;
			dbSum += dbMs;
			out.add(String.format("  %-62s %5s %12s %6.1f%% %12s %12s %12s", cut(e.getKey(), 62), s.category, duration(s.ms),
					pct(s.ms, total), duration(fileMs), duration(dbMs), duration(Math.max(0L, s.ms - fileMs - dbMs))));
		}
		long other = Math.max(0L, total - phaseSum);
		out.add(String.format("  %-62s %5s %12s %6.1f%%", "Other (status updates, between steps)", "", duration(other), pct(other, total)));
		out.add(String.format("  JOB TIME SPLIT: file operations %s (%.1f%%) | DB %s (%.1f%%) | everything else %s (%.1f%%)",
				duration(fileSum), pct(fileSum, total), duration(dbSum), pct(dbSum, total),
				duration(Math.max(0L, total - fileSum - dbSum)), pct(Math.max(0L, total - fileSum - dbSum), total)));
		out.add("--- OPERATIONS (every call timed; copies run on several threads, so totals can exceed the wall clock) ---");
		out.add(String.format("  %-58s %5s %8s %12s %10s %10s %10s %10s", "operation", "type", "calls", "total", "avg ms", "max ms",
				"MB", "avg KB"));
		for (String category : new String[] { FILE, DB, MIXED }) {
			for (Map.Entry<String, Stat> e : operations.entrySet()) {
				Stat s = e.getValue();
				if (!category.equals(s.category)) {
					continue;
				}
				out.add(String.format("  %-58s %5s %8d %12s %10.1f %10d %10.1f %10s", cut(e.getKey(), 58), s.category, s.calls,
						duration(s.ms), s.calls == 0 ? 0.0 : (double) s.ms / s.calls, s.maxMs, s.bytes / 1048576.0,
						s.bytes == 0 || s.calls == 0 ? "-" : String.format("%.1f", s.bytes / 1024.0 / s.calls)));
			}
		}
		out.add("--- FILE OPERATIONS, READ AS PER-FILE COST ---");
		for (Map.Entry<String, Stat> e : operations.entrySet()) {
			Stat s = e.getValue();
			if (!FILE.equals(s.category) || s.calls < 10) {
				continue;
			}
			double avgMs = (double) s.ms / s.calls;
			String line = String.format("  %s: %d calls, %.1f ms each on average", e.getKey(), s.calls, avgMs);
			if (s.bytes > 0) {
				double avgKb = s.bytes / 1024.0 / s.calls;
				double mbPerSec = s.ms == 0 ? 0.0 : (s.bytes / 1048576.0) / (s.ms / 1000.0);
				line += String.format(" for %.1f KB each = %.3f MB/s per call", avgKb, mbPerSec);
			}
			out.add(line);
		}
		out.add("=================== END " + title + " TIMING SUMMARY :: " + jobName + " ===================");
		for (String line : out) {
			logger.info(line);
		}
	}

	private static double pct(long part, long total) {
		return total <= 0 ? 0.0 : part * 100.0 / total;
	}

	private static String cut(String s, int max) {
		return s.length() <= max ? s : s.substring(0, max);
	}

	/** 1 h 02 min 03 s / 4 min 05 s / 12.3 s */
	static String duration(long ms) {
		if (ms < 60000L) {
			return String.format("%.1f s", ms / 1000.0);
		}
		long s = ms / 1000L;
		if (s < 3600L) {
			return String.format("%d min %02d s", s / 60, s % 60);
		}
		return String.format("%d h %02d min %02d s", s / 3600, (s % 3600) / 60, s % 60);
	}
}
