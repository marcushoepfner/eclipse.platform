package org.eclipse.core.runtime;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

/**
 * Safe runnables represent blocks of code and associated exception
 * handlers.  They are typically used when a plug-in needs to call some
 * untrusted code (e.g., code contributed by another plug-in via an
 * extension).
* <p>
 * Clients may implement this interface.
 * </p>
 * 
 * @see Platform#run
 */
public interface ISafeRunnable {
/**
 * Handles an exception thrown by this runnable's <code>run</code>
 * method.  The processing done here should be specific to the
 * particular usecase for this runnable.  Generalized exception
 * processing (e.g., logging in the platform's log) is done by the
 * Platform's run mechanism.
 *
 * @param exception an exception which occurred during processing
 *		the body of this runnable (i.e., in <code>run()</code>)
 * @see Platform#run
 */
public void handleException(Throwable exception);
/**
 * Runs this runnable.  Any exceptions thrown from this method will
 * be passed to this runnable's <code>handleException</code>
 * method.
 *
 * @exception Exception if a problem occurred while running this method.
 *		The exception will be processed by <code>handleException</code>
 * @see Platform#run
 */
public void run() throws Exception;
}
