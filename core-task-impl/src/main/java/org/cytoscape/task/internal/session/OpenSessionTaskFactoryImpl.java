/*
 File: OpenSessionTaskFactory.java

 Copyright (c) 2006, 2010, The Cytoscape Consortium (www.cytoscape.org)

 This library is free software; you can redistribute it and/or modify it
 under the terms of the GNU Lesser General Public License as published
 by the Free Software Foundation; either version 2.1 of the License, or
 any later version.

 This library is distributed in the hope that it will be useful, but
 WITHOUT ANY WARRANTY, WITHOUT EVEN THE IMPLIED WARRANTY OF
 MERCHANTABILITY OR FITNESS FOR A PARTICULAR PURPOSE.  The software and
 documentation provided hereunder is on an "as is" basis, and the
 Institute for Systems Biology and the Whitehead Institute
 have no obligations to provide maintenance, support,
 updates, enhancements or modifications.  In no event shall the
 Institute for Systems Biology and the Whitehead Institute
 be liable to any party for direct, indirect, special,
 incidental or consequential damages, including lost profits, arising
 out of the use of this software and its documentation, even if the
 Institute for Systems Biology and the Whitehead Institute
 have been advised of the possibility of such damage.  See
 the GNU Lesser General Public License for more details.

 You should have received a copy of the GNU Lesser General Public License
 along with this library; if not, write to the Free Software Foundation,
 Inc., 59 Temple Place, Suite 330, Boston, MA 02111-1307 USA.
 */
package org.cytoscape.task.internal.session;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.io.read.CySessionReaderManager;
import org.cytoscape.io.util.RecentlyOpenedTracker;
import org.cytoscape.session.CySession;
import org.cytoscape.session.CySessionManager;
import org.cytoscape.task.session.OpenSessionTaskFactory;
import org.cytoscape.work.AbstractTaskFactory;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;
import org.cytoscape.work.TunableSetter;

public class OpenSessionTaskFactoryImpl extends AbstractTaskFactory implements OpenSessionTaskFactory {

	private CySessionManager mgr;
	private CySessionReaderManager rmgr;

	private final CyApplicationManager appManager;
	private final RecentlyOpenedTracker tracker;

	private final SynchronousTaskManager<?> syncTaskManager;
	private final TunableSetter tunableSetter; 
	
	private OpenSessionTask task;

	public OpenSessionTaskFactoryImpl(CySessionManager mgr, final CySessionReaderManager rmgr,
			final CyApplicationManager appManager, final RecentlyOpenedTracker tracker,
			final SynchronousTaskManager<?> syncTaskManager, final TunableSetter tunableSetter) {
		this.mgr = mgr;
		this.rmgr = rmgr;
		this.appManager = appManager;
		this.tracker = tracker;
		this.syncTaskManager = syncTaskManager;
		this.tunableSetter = tunableSetter;
	}

	public TaskIterator createTaskIterator() {
		task = new OpenSessionTask(mgr, rmgr, appManager, tracker);
		return new TaskIterator(2, task);
	}

	@Override
	public TaskIterator createTaskIterator(File file) {
		final Map<String, Object> m = new HashMap<String, Object>();
		m.put("file", file);

		return tunableSetter.createTaskIterator(this.createTaskIterator(), m); 
	}

}