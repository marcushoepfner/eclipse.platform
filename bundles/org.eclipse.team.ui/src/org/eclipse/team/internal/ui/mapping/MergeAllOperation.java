/*******************************************************************************
 * Copyright (c) 2006 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 * IBM Corporation - initial API and implementation
 *******************************************************************************/
package org.eclipse.team.internal.ui.mapping;

import java.lang.reflect.InvocationTargetException;

import org.eclipse.core.runtime.CoreException;
import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.team.core.mapping.IMergeContext;
import org.eclipse.team.core.mapping.ISynchronizationContext;
import org.eclipse.team.core.mapping.provider.SynchronizationContext;
import org.eclipse.team.ui.mapping.SynchronizationOperation;
import org.eclipse.team.ui.synchronize.ISynchronizePageConfiguration;
import org.eclipse.team.ui.synchronize.ModelMergeOperation;

public final class MergeAllOperation extends SynchronizationOperation {
	
	private final IMergeContext context;
	private final String jobName;

	public MergeAllOperation(String jobName, ISynchronizePageConfiguration configuration, Object[] elements, IMergeContext context) {
		super(configuration, elements);
		this.jobName = jobName;
		this.context = context;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.mapping.SynchronizationOperation#execute(org.eclipse.core.runtime.IProgressMonitor)
	 */
	public void execute(IProgressMonitor monitor) throws InvocationTargetException,
			InterruptedException {
		new ModelMergeOperation(getPart(), ((SynchronizationContext)context).getScopeManager()) {
			public boolean isPreviewRequested() {
				return false;
			}
			protected void initializeContext(IProgressMonitor monitor) throws CoreException {
				// Context is already initialized
			}
			protected ISynchronizationContext getContext() {
				return context;
			}
		}.run(monitor);
	}

	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.TeamOperation#canRunAsJob()
	 */
	protected boolean canRunAsJob() {
		return true;
	}
	
	/* (non-Javadoc)
	 * @see org.eclipse.team.ui.TeamOperation#getJobName()
	 */
	protected String getJobName() {;
		return jobName;
	}
}