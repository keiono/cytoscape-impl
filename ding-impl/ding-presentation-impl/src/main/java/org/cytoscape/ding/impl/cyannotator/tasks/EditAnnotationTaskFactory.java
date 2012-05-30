package org.cytoscape.ding.impl.cyannotator.tasks; 


import java.awt.geom.Point2D;
import java.awt.datatransfer.Transferable;

import org.cytoscape.task.NetworkViewLocationTaskFactory;
import org.cytoscape.view.model.CyNetworkView;
import org.cytoscape.work.TaskIterator;

import org.cytoscape.ding.impl.DGraphView;
import org.cytoscape.ding.impl.cyannotator.CyAnnotator;
import org.cytoscape.ding.impl.cyannotator.api.Annotation;


public class EditAnnotationTaskFactory implements NetworkViewLocationTaskFactory {
	private CyAnnotator cyAnnotator;
	private Annotation annotation;
	
	@Override
	public TaskIterator createTaskIterator(CyNetworkView networkView, Point2D javaPt, Point2D xformPt) {
		this.cyAnnotator = ((DGraphView)networkView).getCyAnnotator();
		annotation = cyAnnotator.getAnnotation(javaPt);
		return new TaskIterator(new EditAnnotationTask(networkView, annotation, javaPt));

	}

	@Override
	public boolean isReady(CyNetworkView networkView, Point2D javaPt, Point2D xformPt) {
		this.cyAnnotator = ((DGraphView)networkView).getCyAnnotator();
		annotation = cyAnnotator.getAnnotation(javaPt);
		if (annotation != null)
			return true;
		return false;
	}
}
