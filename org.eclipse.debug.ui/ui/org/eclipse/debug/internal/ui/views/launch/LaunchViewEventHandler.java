package org.eclipse.debug.internal.ui.views.launch;

/*
 * (c) Copyright IBM Corp. 2000, 2001.
 * All Rights Reserved.
 */

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.eclipse.debug.core.DebugEvent;
import org.eclipse.debug.core.DebugException;
import org.eclipse.debug.core.DebugPlugin;
import org.eclipse.debug.core.ILaunch;
import org.eclipse.debug.core.ILaunchManager;
import org.eclipse.debug.core.ILaunchesListener;
import org.eclipse.debug.core.model.IDebugTarget;
import org.eclipse.debug.core.model.IProcess;
import org.eclipse.debug.core.model.IStackFrame;
import org.eclipse.debug.core.model.IThread;
import org.eclipse.debug.internal.ui.DebugUIPlugin;
import org.eclipse.debug.internal.ui.InstructionPointerManager;
import org.eclipse.debug.internal.ui.views.AbstractDebugEventHandler;
import org.eclipse.jface.viewers.IStructuredSelection;
import org.eclipse.jface.viewers.ITreeContentProvider;

/**
 * Handles debug events, updating the launch view and viewer.
 */
public class LaunchViewEventHandler extends AbstractDebugEventHandler implements ILaunchesListener {
	/**
	 * The timer used to time step and evaluation events. The timer allows
	 * the UI to not refresh during fast evaluations and steps.
	 */
	private ThreadTimer fThreadTimer= new ThreadTimer();
	
	/**
	 * Constructs an event handler for the given launch view.
	 * 
	 * @param view launch view
	 */
	public LaunchViewEventHandler(LaunchView view) {
		super(view);
		DebugPlugin plugin= DebugPlugin.getDefault();
		plugin.getLaunchManager().addLaunchListener(this);
	}
	

	/**
	 * @see AbstractDebugEventHandler#doHandleDebugEvents(DebugEvent[])
	 */
	protected void doHandleDebugEvents(DebugEvent[] events) {
		fThreadTimer.handleDebugEvents(events);
		Object suspendee = null;
		for (int i = 0; i < events.length; i++) {
			DebugEvent event = events[i];
			Object source= event.getSource();
			if (!(source instanceof IStackFrame || source instanceof IThread || source instanceof IDebugTarget || source instanceof IProcess)) {
				// the launch view is not interested in any other types of elements
				return;
			}
			switch (event.getKind()) {
				case DebugEvent.CREATE :
					insert(source);
					if (source instanceof IDebugTarget) {
						ILaunch launch= ((IDebugTarget)source).getLaunch();
						getLaunchView().autoExpand(launch, true, true);
					}
					break;
				case DebugEvent.TERMINATE :
					removeInstructionPointerAnnotations(source);
					if (source instanceof IThread) {
						clearSourceSelection((IThread)source);
						fThreadTimer.getTimedOutThreads().remove(source);
						remove(source);
					} else {
						clearSourceSelection(null);
						Object parent = ((ITreeContentProvider)getTreeViewer().getContentProvider()).getParent(source);
						refresh(parent);
					}
					break;
				case DebugEvent.RESUME :
					doHandleResumeEvent(event, source);
					break;
				case DebugEvent.SUSPEND :
					if (suspendee == null || !suspendee.equals(source)) {
						doHandleSuspendEvent(source, event);
						suspendee = source;
					}
					break;
				case DebugEvent.CHANGE :
					if (source instanceof IStackFrame) {
						IStackFrame lastFrame= getLaunchView().getStackFrame();
						if (source.equals(lastFrame)) {
							getLaunchView().setStackFrame(null);
						}
					}
					if (event.getDetail() == DebugEvent.STATE) {
						labelChanged(source);
					} else {
						//structural change
						refresh(source);
					}
					break;
			}
		}
	}
	
	/**
	 * Handles the given resume event with the given source.
	 */
	protected void doHandleResumeEvent(DebugEvent event, Object source) {
		if (!event.isEvaluation()) {
			removeInstructionPointerAnnotations(source);
		}
		if (event.isEvaluation() || event.isStepStart()) {
			// Do not update for step starts and evaluation
			// starts immediately. Instead, start the timer.
			IThread thread= getThread(source);
			if (thread != null) {
				fThreadTimer.startTimer(thread);
			}
			return;
		}	
		clearSourceSelection(null);
		refresh(source);
		if (source instanceof IThread) {
			selectAndReveal(source);
			return;
		}	
		labelChanged(source);
	}
	
	/**
	 * Updates the stack frame icons for a running thread.
	 * This is useful for the case where a thread is resumed
	 * temporarily  but the view should keep the stack frame 
	 * visible (for example, step start or evaluation start).
	 */
	protected void updateRunningThread(IThread thread) {
		labelChanged(thread);
		getLaunchViewer().updateStackFrameIcons(thread);
		getLaunchView().clearSourceSelection();
	}

	protected void doHandleSuspendEvent(Object element, DebugEvent event) {
		if (event.isEvaluation() || (event.getDetail() & DebugEvent.STEP_END) != 0) {
			IThread thread= getThread(element);
			if (thread != null) {
				fThreadTimer.stopTimer((IThread)element);
			}
			
			boolean wasTimedOut= fThreadTimer.getTimedOutThreads().remove(thread);
			if (event.isEvaluation() && ((event.getDetail() & DebugEvent.EVALUATION_IMPLICIT) != 0)) {
				if (thread != null && wasTimedOut) {
					// Refresh the thread label when a timed out evaluation finishes.
					// This is necessary because the timeout updates
					// the label when it occurs
					refresh(thread);
				}
				// Don't refresh fully for evaluation completion.
				return;
			}
		}
		if (element instanceof IThread) {
			doHandleSuspendThreadEvent((IThread)element, event);
			return;
		}
		refresh(element);
	}
	
	/**
	 * Remove all instruction pointer annotations associated with the specified element.	 */
	protected void removeInstructionPointerAnnotations(Object element) {
		if (element instanceof IThread) {
			InstructionPointerManager.getDefault().removeAnnotations((IThread)element);
		} else if (element instanceof IDebugTarget) {
			InstructionPointerManager.getDefault().removeAnnotations((IDebugTarget)element);			
		}
	}
	
	/**
	 * Updates the given thread for the given suspend event.
	 */
	protected void doHandleSuspendThreadEvent(IThread thread, DebugEvent event) {
		// if the thread has already resumed, do nothing
		if (!thread.isSuspended()) {
			return;
		}
		// Auto-expand the thread. Only select the thread if this wasn't the end
		// of an evaluation
		boolean evaluationEvent = event.isEvaluation();
		getLaunchView().autoExpand(thread, true, !evaluationEvent);
	}
	
	/**
	 * De-registers this event handler from the debug model.
	 */
	public void dispose() {
		super.dispose();
		fThreadTimer.stop();
		DebugPlugin plugin= DebugPlugin.getDefault();
		plugin.getLaunchManager().removeLaunchListener(this);
	}

	/**
	 * Clear the selection in the editor - must be called in UI thread
	 */
	private void clearSourceSelection(IThread thread) {
		if (getViewer() != null) {
			if (thread != null) {
				IStructuredSelection selection= (IStructuredSelection)getLaunchViewer().getSelection();
				Object element= selection.getFirstElement();
				if (element instanceof IStackFrame) {
					IStackFrame stackFrame = (IStackFrame) element;
					if (!stackFrame.getThread().equals(thread)) {
						//do not clear the source selection
						//a thread has terminated that is not the
						//parent of the currently selected stack frame
						return;
					}
				} else {
					return;
				}
			}
		
			getLaunchView().clearSourceSelection();
		}
	}

	/**
	 * Returns this event handler's launch viewer
	 * 
	 * @return launch viewer
	 */
	protected LaunchViewer getLaunchViewer() {
		return (LaunchViewer)getViewer();
	}
	
	/**
	 * Returns this event handler's launch view
	 * 
	 * @return launch view
	 */
	protected LaunchView getLaunchView() {
		return (LaunchView)getView();
	}		
	
	private IThread getThread(Object element) {
		IThread thread = null;
		if (element instanceof IThread) {
			thread = (IThread) element;
		} else if (element instanceof IStackFrame) {
			thread = ((IStackFrame)element).getThread();
		}
		return thread;
	}
	
	class ThreadTimer {
		
		private Thread fThread;
		/**
		 * The time allotted before a thread will be updated
		 */
		private long TIMEOUT= 500;
		private boolean fStopped= false;
		private Object fLock= new Object();
		
		/**
		 * Maps threads that are currently performing being timed
		 * to the allowed time by which they must finish. If this
		 * limit expires before the timer is stopped, the thread will
		 * be refreshed.
		 */
		HashMap fStopTimes= new HashMap();
		/**
		 * Collection of threads whose timers have expired.
		 */
		HashSet fTimedOutThreads= new HashSet();
		
		public Set getTimedOutThreads() {
			return fTimedOutThreads;
		}
		
		/**
		 * Handle debug events dispatched from launch view event handler.
		 * If there are no running targets, stop this timer.
		 */
		public void handleDebugEvents(DebugEvent[] events) {
			if (fStopped) {
				return;
			}
			DebugEvent event;
			for (int i= 0, numEvents= events.length; i < numEvents; i++) {
				event= events[i];
				if (event.getKind() == DebugEvent.TERMINATE && event.getSource() instanceof IDebugTarget) {
					ILaunch[] launches= DebugPlugin.getDefault().getLaunchManager().getLaunches();
					// If there are no more active DebugTargets, stop the thread.
					for (int j= 0; j < launches.length; j++) {
						IDebugTarget[] targets= launches[j].getDebugTargets();
						for (int k = 0; k < targets.length; k++) {
							IDebugTarget target = targets[k];
							if (target != null && !target.isDisconnected() && !target.isTerminated()) {
								return;
							}
						}
					}
					// To get here, there must be no running DebugTargets
					stop();
					return;
				}
			}
		}
			
		public void startTimer(IThread thread) {
			synchronized (fLock) {
				fStopTimes.put(thread, new Long(System.currentTimeMillis() + TIMEOUT));
				if (fThread == null) {
					startThread();
				}
			}
		}
		
		public void stop() {
			synchronized (fLock) {
				fStopped= true;
				fThread= null;
				fStopTimes.clear();
			}
		}
		
		public void stopTimer(IThread thread) {
			synchronized (fLock) {
				fStopTimes.remove(thread);
			}
		}
		
		private void startThread() {
			fThread= new Thread(new Runnable() {
				public void run() {
					fStopped= false;
					while (!fStopped) {
						checkTimers();
					}
					
				}
			}, "Thread timer"); //$NON-NLS-1$
			fThread.start();
		}
		
		private void checkTimers() {
			long timeToWait= TIMEOUT;
			Map.Entry[] entries;
			synchronized (fLock) {
				entries= (Map.Entry[])fStopTimes.entrySet().toArray(new Map.Entry[0]);
			}
			long stopTime, currentTime= System.currentTimeMillis();
			Long entryValue;
			Map.Entry entry= null;
			for (int i= 0, numEntries= entries.length; i < numEntries; i++) {
				entry= entries[i];
				entryValue= (Long)entry.getValue();
				if (entryValue == null) {
					continue;
				}
				stopTime= ((Long)entryValue).longValue();
				if (stopTime <= currentTime) {
					// The timer has expired for this thread.
					// Refresh the UI to show that the thread
					// is performing a long evaluation
					final IThread thread= (IThread)entry.getKey();
					fStopTimes.remove(thread);	
					getView().asyncExec(new Runnable() {
						public void run() {
							fTimedOutThreads.add(thread);
							updateRunningThread(thread);
						}
					});
				} else {
					timeToWait= Math.min(timeToWait, stopTime - currentTime);
				}
			}
			try {
				Thread.sleep(timeToWait);
			} catch (InterruptedException e) {
			}
		}
	}
	
	/**
	 * @see org.eclipse.debug.core.ILaunchesListener#launchesAdded(org.eclipse.debug.core.ILaunch)
	 */
	public void launchesAdded(final ILaunch[] launches) {
		Runnable r= new Runnable() {
			public void run() {
				if (isAvailable()) {
					if (launches.length > 1) {
						refresh();
					} else {
						insert(launches[0]);
					}
					for (int i = 0; i < launches.length; i++) {
						if (launches[i].hasChildren()) {
							getLaunchView().autoExpand(launches[i], false, i == (launches.length - 1));
						}
					}					

				}
			}
		};

		getView().syncExec(r);		
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchesListener#launchesChanged(org.eclipse.debug.core.ILaunch)
	 */
	public void launchesChanged(final ILaunch[] launches) {
		Runnable r= new Runnable() {
			public void run() {
				if (isAvailable()) {	
					if (launches.length > 1) {
						refresh();
					} else {
						refresh(launches[0]);
					}
					for (int i = 0; i < launches.length; i++) {
						if (launches[i].hasChildren()) {
							getLaunchView().autoExpand(launches[i], false, i == (launches.length - 1));
						}						
					}
				}
			}
		};

		getView().syncExec(r);				
	}

	/**
	 * @see org.eclipse.debug.core.ILaunchesListener#launchesRemoved(org.eclipse.debug.core.ILaunch)
	 */
	public void launchesRemoved(final ILaunch[] launches) {
		Runnable r= new Runnable() {
			public void run() {
				if (isAvailable()) {
					if (launches.length > 1) {
						refresh();
					} else {
						remove(launches[0]);
					}
					ILaunchManager lm= DebugPlugin.getDefault().getLaunchManager();
					IDebugTarget[] targets= lm.getDebugTargets();
					if (targets.length > 0) {
						IDebugTarget target= targets[targets.length - 1];
						try {
							IThread[] threads= target.getThreads();
							for (int i=0; i < threads.length; i++) {
								if (threads[i].isSuspended()) {
									getLaunchView().autoExpand(threads[i], false, true);
									return;
								}
							}						
						} catch (DebugException de) {
							DebugUIPlugin.log(de);
						}
						
						getLaunchView().autoExpand(target.getLaunch(), false, true);
					}
				}
			}
		};

		getView().asyncExec(r);		
	}

}
