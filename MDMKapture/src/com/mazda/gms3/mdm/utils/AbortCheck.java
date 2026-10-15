package com.mazda.gms3.mdm.utils;

/**
 * LETS A SHARED UTILITY ASK ITS CALLER WHETHER THE SCHEDULE HAS BEEN ABORTED.
 *
 * The abort is cooperative - Thread.stop() was removed in Java 20, so a worker can only be stopped
 * by checking a flag and returning. That works inside the worker, but the loops that do the bulk of
 * the file work live in UTILITY classes (XcopyUtil.copyFilesToServer walks every image of a
 * document), and a utility has no schedule id and no business owning one.
 *
 * Seen live on SI Channel schedule 4330: the abort was detected correctly, but only AFTER the image
 * copy had finished. With three images that was instant; a document carrying hundreds would have
 * kept copying long after the user pressed Abort.
 *
 * So the worker hands the utility this one-method check, and the utility breaks out of its own
 * loops when it answers true. The utility stays free of any schedule or DAO knowledge, and a
 * utility given no check simply never stops early - which is the behaviour of every caller that has
 * nothing to abort.
 */
public interface AbortCheck {

	/**
	 * @return true when the schedule being processed has been aborted from the screen
	 */
	boolean isAborted();

}
