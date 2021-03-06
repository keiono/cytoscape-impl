package org.cytoscape.ding;

/*
 * #%L
 * Cytoscape Ding View/Presentation Impl (ding-presentation-impl)
 * $Id:$
 * $HeadURL:$
 * %%
 * Copyright (C) 2006 - 2013 The Cytoscape Consortium
 * %%
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU Lesser General Public License as 
 * published by the Free Software Foundation, either version 2.1 of the 
 * License, or (at your option) any later version.
 * 
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Lesser Public License for more details.
 * 
 * You should have received a copy of the GNU General Lesser Public 
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/lgpl-2.1.html>.
 * #L%
 */

import static org.cytoscape.work.ServiceProperties.*;

import java.net.URL;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import org.cytoscape.application.CyApplicationConfiguration;
import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.application.NetworkViewRenderer;
import org.cytoscape.application.swing.CyAction;
import org.cytoscape.application.swing.CyEdgeViewContextMenuFactory;
import org.cytoscape.application.swing.CyNetworkViewContextMenuFactory;
import org.cytoscape.application.swing.CyNodeViewContextMenuFactory;
import org.cytoscape.ding.action.GraphicsDetailAction;
import org.cytoscape.ding.customgraphics.CustomGraphicsManager;
import org.cytoscape.ding.customgraphics.CustomGraphicsTranslator;
import org.cytoscape.ding.customgraphics.NullCustomGraphicsFactory;
import org.cytoscape.ding.customgraphics.bitmap.URLImageCustomGraphicsFactory;
import org.cytoscape.ding.customgraphics.vector.GradientRoundRectangleFactory;
import org.cytoscape.ding.customgraphics.vector.GradientOvalFactory;
import org.cytoscape.ding.customgraphicsmgr.internal.CustomGraphicsManagerImpl;
import org.cytoscape.ding.customgraphicsmgr.internal.action.CustomGraphicsManagerAction;
import org.cytoscape.ding.customgraphicsmgr.internal.ui.CustomGraphicsBrowser;
import org.cytoscape.ding.dependency.CustomGraphicsSizeDependencyFactory;
import org.cytoscape.ding.dependency.EdgeColorDependencyFactory;
import org.cytoscape.ding.dependency.NodeSizeDependencyFactory;
import org.cytoscape.ding.impl.AddEdgeNodeViewTaskFactoryImpl;
import org.cytoscape.ding.impl.BendFactoryImpl;
import org.cytoscape.ding.impl.DingGraphLOD;
import org.cytoscape.ding.impl.DingGraphLODAll;
import org.cytoscape.ding.impl.DingNavigationRenderingEngineFactory;
import org.cytoscape.ding.impl.DingRenderer;
import org.cytoscape.ding.impl.DingRenderingEngineFactory;
import org.cytoscape.ding.impl.DingViewModelFactory;
import org.cytoscape.ding.impl.DingVisualStyleRenderingEngineFactory;
import org.cytoscape.ding.impl.HandleFactoryImpl;
import org.cytoscape.ding.impl.NVLTFActionSupport;
import org.cytoscape.ding.impl.ViewTaskFactoryListener;
//
// Annotation api
//
import org.cytoscape.view.presentation.annotations.Annotation;
import org.cytoscape.view.presentation.annotations.AnnotationFactory;
import org.cytoscape.view.presentation.annotations.AnnotationManager;
import org.cytoscape.view.presentation.annotations.ArrowAnnotation;
import org.cytoscape.view.presentation.annotations.BoundedTextAnnotation;
import org.cytoscape.view.presentation.annotations.ImageAnnotation;
import org.cytoscape.view.presentation.annotations.ShapeAnnotation;
import org.cytoscape.view.presentation.annotations.TextAnnotation;

// Annotation creation
import org.cytoscape.ding.impl.cyannotator.AnnotationFactoryManager;
import org.cytoscape.ding.impl.cyannotator.AnnotationManagerImpl;
import org.cytoscape.ding.impl.cyannotator.create.ArrowAnnotationFactory;
import org.cytoscape.ding.impl.cyannotator.create.ImageAnnotationFactory;
import org.cytoscape.ding.impl.cyannotator.create.ShapeAnnotationFactory;
import org.cytoscape.ding.impl.cyannotator.create.BoundedTextAnnotationFactory;
import org.cytoscape.ding.impl.cyannotator.create.TextAnnotationFactory;
import org.cytoscape.ding.impl.cyannotator.create.GroupAnnotationFactory;

// Annotation edits and changes
import org.cytoscape.ding.impl.cyannotator.tasks.AddAnnotationTaskFactory;
import org.cytoscape.ding.impl.cyannotator.tasks.AddArrowTaskFactory;
import org.cytoscape.ding.impl.cyannotator.tasks.ChangeAnnotationCanvasTaskFactory;
import org.cytoscape.ding.impl.cyannotator.tasks.EditAnnotationTaskFactory;
import org.cytoscape.ding.impl.cyannotator.tasks.GroupAnnotationsTaskFactory;
import org.cytoscape.ding.impl.cyannotator.tasks.LayerAnnotationTaskFactory;
import org.cytoscape.ding.impl.cyannotator.tasks.MoveAnnotationTaskFactory;
import org.cytoscape.ding.impl.cyannotator.tasks.RemoveAnnotationTaskFactory;
import org.cytoscape.ding.impl.cyannotator.tasks.ResizeAnnotationTaskFactory;
import org.cytoscape.ding.impl.cyannotator.tasks.SelectAnnotationTaskFactory;
import org.cytoscape.ding.impl.cyannotator.tasks.UngroupAnnotationsTaskFactory;

import org.cytoscape.ding.impl.editor.EdgeBendEditor;
import org.cytoscape.ding.impl.editor.EdgeBendValueEditor;
import org.cytoscape.ding.impl.editor.ObjectPositionEditor;
import org.cytoscape.event.CyEventHelper;
import org.cytoscape.model.CyNetworkFactory;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNetworkTableManager;
import org.cytoscape.model.CyTableFactory;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.property.CyProperty;
import org.cytoscape.property.PropertyUpdatedListener;
import org.cytoscape.service.util.AbstractCyActivator;
import org.cytoscape.service.util.CyServiceRegistrar;
import org.cytoscape.spacial.SpacialIndex2DFactory;
import org.cytoscape.spacial.internal.rtree.RTreeFactory;
import org.cytoscape.task.EdgeViewTaskFactory;
import org.cytoscape.task.NetworkTaskFactory;
import org.cytoscape.task.NetworkViewLocationTaskFactory;
import org.cytoscape.task.NetworkViewTaskFactory;
import org.cytoscape.task.NodeViewTaskFactory;
import org.cytoscape.view.model.CyNetworkViewFactory;
import org.cytoscape.view.model.CyNetworkViewManager;
import org.cytoscape.view.model.VisualLexicon;
import org.cytoscape.view.presentation.RenderingEngineManager;
import org.cytoscape.view.presentation.customgraphics.CyCustomGraphicsFactory;
import org.cytoscape.view.presentation.property.values.BendFactory;
import org.cytoscape.view.presentation.property.values.HandleFactory;
import org.cytoscape.view.vizmap.VisualMappingManager;
import org.cytoscape.view.vizmap.VisualPropertyDependencyFactory;
import org.cytoscape.view.vizmap.gui.editor.ContinuousMappingCellRendererFactory;
import org.cytoscape.view.vizmap.gui.editor.ValueEditor;
import org.cytoscape.view.vizmap.gui.editor.VisualPropertyEditor;
import org.cytoscape.view.vizmap.mappings.ValueTranslator;
import org.cytoscape.work.TaskFactory;
import org.cytoscape.work.swing.DialogTaskManager;
import org.cytoscape.work.undo.UndoSupport;
import org.osgi.framework.BundleContext;
//
// Annotation api
//
// Annotation creation
// Annotation edits and changes


public class CyActivator extends AbstractCyActivator {
	public CyActivator() {
		super();
	}

	@Override
	public void start(BundleContext bc) {
		startSpacial(bc); 
		startCustomGraphicsMgr(bc);
		startPresentationImpl(bc);
	}


	private void startPresentationImpl(BundleContext bc) {

		VisualMappingManager vmmServiceRef = getService(bc, VisualMappingManager.class);
		CyServiceRegistrar cyServiceRegistrarServiceRef = getService(bc, CyServiceRegistrar.class);
		CyApplicationManager applicationManagerServiceRef = getService(bc, CyApplicationManager.class);
		CustomGraphicsManager customGraphicsManagerServiceRef = getService(bc, CustomGraphicsManager.class);
		RenderingEngineManager renderingEngineManagerServiceRef = getService(bc, RenderingEngineManager.class);
		CyRootNetworkManager cyRootNetworkFactoryServiceRef = getService(bc, CyRootNetworkManager.class);
		UndoSupport undoSupportServiceRef = getService(bc, UndoSupport.class);
		CyTableFactory cyDataTableFactoryServiceRef = getService(bc, CyTableFactory.class);
		SpacialIndex2DFactory spacialIndex2DFactoryServiceRef = getService(bc, SpacialIndex2DFactory.class);
		DialogTaskManager dialogTaskManager = getService(bc, DialogTaskManager.class);
		CyServiceRegistrar cyServiceRegistrarRef = getService(bc, CyServiceRegistrar.class);
		CyNetworkManager cyNetworkManagerServiceRef = getService(bc, CyNetworkManager.class);
		CyEventHelper cyEventHelperServiceRef = getService(bc, CyEventHelper.class);
		CyProperty cyPropertyServiceRef = getService(bc, CyProperty.class, "(cyPropertyName=cytoscape3.props)");
		CyNetworkTableManager cyNetworkTableManagerServiceRef = getService(bc, CyNetworkTableManager.class);
		CyNetworkViewManager cyNetworkViewManagerServiceRef = getService(bc, CyNetworkViewManager.class);

		CyNetworkFactory cyNetworkFactory = getService(bc, CyNetworkFactory.class);

		DVisualLexicon dVisualLexicon = new DVisualLexicon(customGraphicsManagerServiceRef);

		NVLTFActionSupport nvltfActionSupport = 
		    new NVLTFActionSupport(applicationManagerServiceRef,cyNetworkViewManagerServiceRef,
		                           dialogTaskManager,cyServiceRegistrarRef);
		ViewTaskFactoryListener vtfListener = new ViewTaskFactoryListener(nvltfActionSupport);

		AnnotationFactoryManager annotationFactoryManager = new AnnotationFactoryManager();
		AnnotationManager annotationManager = new AnnotationManagerImpl(cyNetworkViewManagerServiceRef);

		DingGraphLOD dingGraphLOD = new DingGraphLOD(cyPropertyServiceRef, applicationManagerServiceRef);
		registerService(bc, dingGraphLOD, PropertyUpdatedListener.class, new Properties());
		
		DingGraphLODAll dingGraphLODAll = new DingGraphLODAll();
		
		HandleFactory handleFactory = new HandleFactoryImpl();
		registerService(bc, handleFactory, HandleFactory.class, new Properties());
		
		DingRenderingEngineFactory dingRenderingEngineFactory = new DingRenderingEngineFactory(
				cyDataTableFactoryServiceRef, cyRootNetworkFactoryServiceRef, undoSupportServiceRef,
				spacialIndex2DFactoryServiceRef, dVisualLexicon, dialogTaskManager,
				cyServiceRegistrarRef, cyNetworkTableManagerServiceRef, cyEventHelperServiceRef,
				vtfListener, annotationFactoryManager, dingGraphLOD, vmmServiceRef,cyNetworkViewManagerServiceRef, handleFactory);
		DingNavigationRenderingEngineFactory dingNavigationRenderingEngineFactory = new DingNavigationRenderingEngineFactory(
				cyServiceRegistrarServiceRef, dVisualLexicon, renderingEngineManagerServiceRef,
				applicationManagerServiceRef);
		DingRenderingEngineFactory dingVisualStyleRenderingEngineFactory = new DingVisualStyleRenderingEngineFactory(
				cyDataTableFactoryServiceRef, cyRootNetworkFactoryServiceRef, undoSupportServiceRef,
				spacialIndex2DFactoryServiceRef, dVisualLexicon, dialogTaskManager,
				cyServiceRegistrarRef, cyNetworkTableManagerServiceRef, cyEventHelperServiceRef,
				vtfListener, annotationFactoryManager, dingGraphLOD, vmmServiceRef,cyNetworkViewManagerServiceRef, handleFactory);
		AddEdgeNodeViewTaskFactoryImpl addEdgeNodeViewTaskFactory = new AddEdgeNodeViewTaskFactoryImpl(vmmServiceRef, cyEventHelperServiceRef);

		ContinuousMappingCellRendererFactory continuousMappingCellRendererFactory = getService(bc, ContinuousMappingCellRendererFactory.class);

		// Object Position Editor
		ObjectPositionValueEditor objectPositionValueEditor = new ObjectPositionValueEditor();
		ObjectPositionEditor objectPositionEditor = new ObjectPositionEditor(objectPositionValueEditor, continuousMappingCellRendererFactory);

		DingViewModelFactory dingNetworkViewFactory = new DingViewModelFactory(cyDataTableFactoryServiceRef,
				cyRootNetworkFactoryServiceRef, undoSupportServiceRef, spacialIndex2DFactoryServiceRef, dVisualLexicon,
				dialogTaskManager, cyServiceRegistrarRef, cyNetworkTableManagerServiceRef,
				cyEventHelperServiceRef, vtfListener, annotationFactoryManager, dingGraphLOD, vmmServiceRef, cyNetworkViewManagerServiceRef, handleFactory);

		DingRenderer renderer = DingRenderer.getInstance();
		renderer.registerNetworkViewFactory(dingNetworkViewFactory);
		renderer.registerRenderingEngineFactory(NetworkViewRenderer.DEFAULT_CONTEXT, dingRenderingEngineFactory);
		renderer.registerRenderingEngineFactory(NetworkViewRenderer.BIRDS_EYE_CONTEXT, dingNavigationRenderingEngineFactory);
		renderer.registerRenderingEngineFactory(NetworkViewRenderer.VISUAL_STYLE_PREVIEW_CONTEXT, dingVisualStyleRenderingEngineFactory);
		registerService(bc, renderer, NetworkViewRenderer.class, new Properties());
		
		// Edge Bend editor
		EdgeBendValueEditor edgeBendValueEditor = new EdgeBendValueEditor(cyNetworkFactory, dingNetworkViewFactory,
				dingRenderingEngineFactory);
		EdgeBendEditor edgeBendEditor = new EdgeBendEditor(edgeBendValueEditor, continuousMappingCellRendererFactory);

		
		Properties dingRenderingEngineFactoryProps = new Properties();
		dingRenderingEngineFactoryProps.setProperty(ID, "ding");
		registerAllServices(bc, dingRenderingEngineFactory, dingRenderingEngineFactoryProps);

		Properties dingNavigationRenderingEngineFactoryProps = new Properties();
		dingNavigationRenderingEngineFactoryProps.setProperty(ID, "dingNavigation");
		registerAllServices(bc, dingNavigationRenderingEngineFactory, dingNavigationRenderingEngineFactoryProps);

		Properties addEdgeNodeViewTaskFactoryProps = new Properties();
		addEdgeNodeViewTaskFactoryProps.setProperty(PREFERRED_ACTION, "Edge");
		addEdgeNodeViewTaskFactoryProps.setProperty(PREFERRED_MENU, NODE_ADD_MENU);
		addEdgeNodeViewTaskFactoryProps.setProperty(TITLE, "Edge");
		addEdgeNodeViewTaskFactoryProps.setProperty(MENU_GRAVITY, "0.1");
		registerService(bc, addEdgeNodeViewTaskFactory, NodeViewTaskFactory.class, addEdgeNodeViewTaskFactoryProps);

		Properties dVisualLexiconProps = new Properties();
		dVisualLexiconProps.setProperty(ID, "ding");
		registerService(bc, dVisualLexicon, VisualLexicon.class, dVisualLexiconProps);

		final Properties positionEditorProp = new Properties();
		positionEditorProp.setProperty(ID, "objectPositionValueEditor");
		registerService(bc, objectPositionValueEditor, ValueEditor.class, positionEditorProp);

		final Properties objectPositionEditorProp = new Properties();
		objectPositionEditorProp.setProperty(ID, "objectPositionEditor");
		registerService(bc, objectPositionEditor, VisualPropertyEditor.class, objectPositionEditorProp);

		registerAllServices(bc, edgeBendValueEditor, new Properties());
		registerService(bc, edgeBendEditor, VisualPropertyEditor.class, new Properties());

		Properties dingNetworkViewFactoryServiceProps = new Properties();
		registerService(bc, dingNetworkViewFactory, CyNetworkViewFactory.class, dingNetworkViewFactoryServiceProps);

		// Annotations
		registerServiceListener(bc, annotationFactoryManager, "addAnnotationFactory", "removeAnnotationFactory",
		                        AnnotationFactory.class);
		registerService(bc, annotationManager, AnnotationManager.class, new Properties());

		// Arrow
		AnnotationFactory arrowAnnotationFactory = new ArrowAnnotationFactory();
		Properties arrowFactory = new Properties();
		arrowFactory.setProperty("type","ArrowAnnotation.class");
		registerService(bc, arrowAnnotationFactory, AnnotationFactory.class, arrowFactory);

		AddArrowTaskFactory addArrowTaskFactory = new AddArrowTaskFactory(arrowAnnotationFactory);
		Properties addArrowTaskFactoryProps = new Properties();
		addArrowTaskFactoryProps.setProperty(PREFERRED_ACTION, "NEW");
		addArrowTaskFactoryProps.setProperty(PREFERRED_MENU, NETWORK_ADD_MENU);
		addArrowTaskFactoryProps.setProperty(MENU_GRAVITY, "1.2");
		addArrowTaskFactoryProps.setProperty(TITLE, "Arrow Annotation");
		registerService(bc, addArrowTaskFactory, NetworkViewLocationTaskFactory.class, addArrowTaskFactoryProps);

		// Image annotation
		AnnotationFactory imageAnnotationFactory = new ImageAnnotationFactory(customGraphicsManagerServiceRef);
		Properties imageFactory = new Properties();
		imageFactory.setProperty("type","ImageAnnotation.class");
		registerService(bc, imageAnnotationFactory, AnnotationFactory.class, imageFactory);

		AddAnnotationTaskFactory addImageTaskFactory = new AddAnnotationTaskFactory(imageAnnotationFactory);
		Properties addImageTaskFactoryProps = new Properties();
		addImageTaskFactoryProps.setProperty(PREFERRED_ACTION, "NEW");
		addImageTaskFactoryProps.setProperty(PREFERRED_MENU, NETWORK_ADD_MENU);
		addImageTaskFactoryProps.setProperty(MENU_GRAVITY, "1.3");
		addImageTaskFactoryProps.setProperty(TITLE, "Image Annotation");
		registerService(bc, addImageTaskFactory, NetworkViewLocationTaskFactory.class, addImageTaskFactoryProps);

		// Shape annotation
		AnnotationFactory shapeAnnotationFactory = new ShapeAnnotationFactory();
		Properties shapeFactory = new Properties();
		shapeFactory.setProperty("type","ShapeAnnotation.class");
		registerService(bc, shapeAnnotationFactory, AnnotationFactory.class, shapeFactory);

		AddAnnotationTaskFactory addShapeTaskFactory = new AddAnnotationTaskFactory(shapeAnnotationFactory);
		Properties addShapeTaskFactoryProps = new Properties();
		addShapeTaskFactoryProps.setProperty(PREFERRED_ACTION, "NEW");
		addShapeTaskFactoryProps.setProperty(PREFERRED_MENU, NETWORK_ADD_MENU);
		addShapeTaskFactoryProps.setProperty(MENU_GRAVITY, "1.4");
		addShapeTaskFactoryProps.setProperty(TITLE, "Shape Annotation");
		registerService(bc, addShapeTaskFactory, NetworkViewLocationTaskFactory.class, addShapeTaskFactoryProps);

		// Text annotation
		AnnotationFactory textAnnotationFactory = new TextAnnotationFactory();
		Properties textFactory = new Properties();
		textFactory.setProperty("type","TextAnnotation.class");
		registerService(bc, textAnnotationFactory, AnnotationFactory.class, textFactory);

		AddAnnotationTaskFactory addTextTaskFactory = new AddAnnotationTaskFactory(textAnnotationFactory);
		Properties addTextTaskFactoryProps = new Properties();
		addTextTaskFactoryProps.setProperty(PREFERRED_ACTION, "NEW");
		addTextTaskFactoryProps.setProperty(MENU_GRAVITY, "1.5");
		addTextTaskFactoryProps.setProperty(PREFERRED_MENU, NETWORK_ADD_MENU);
		addTextTaskFactoryProps.setProperty(TITLE, "Text Annotation");
		registerService(bc, addTextTaskFactory, NetworkViewLocationTaskFactory.class, addTextTaskFactoryProps);

		// Bounded Text annotation
		AnnotationFactory boundedAnnotationFactory = new BoundedTextAnnotationFactory();
		Properties boundedFactory = new Properties();
		boundedFactory.setProperty("type","BoundedTextAnnotation.class");
		registerService(bc, boundedAnnotationFactory, AnnotationFactory.class, boundedFactory);

		AddAnnotationTaskFactory addBoundedTextTaskFactory = 
			new AddAnnotationTaskFactory(boundedAnnotationFactory);
		Properties addBoundedTextTaskFactoryProps = new Properties();
		addBoundedTextTaskFactoryProps.setProperty(PREFERRED_ACTION, "NEW");
		addBoundedTextTaskFactoryProps.setProperty(MENU_GRAVITY, "1.6");
		addBoundedTextTaskFactoryProps.setProperty(PREFERRED_MENU, NETWORK_ADD_MENU);
		addBoundedTextTaskFactoryProps.setProperty(TITLE, "Bounded Text Annotation");
		registerService(bc, addBoundedTextTaskFactory, NetworkViewLocationTaskFactory.class, 
		                addBoundedTextTaskFactoryProps);

		AnnotationFactory groupAnnotationFactory = new GroupAnnotationFactory();
		Properties groupFactory = new Properties();
		groupFactory.setProperty("type","GroupAnnotation.class");
		registerService(bc, groupAnnotationFactory, AnnotationFactory.class, groupFactory);

		// Annotation edit
		EditAnnotationTaskFactory editAnnotationTaskFactory = new EditAnnotationTaskFactory();
		Properties editAnnotationTaskFactoryProps = new Properties();
		editAnnotationTaskFactoryProps.setProperty(PREFERRED_ACTION, "NEW");
		editAnnotationTaskFactoryProps.setProperty(MENU_GRAVITY, "2.0");
		editAnnotationTaskFactoryProps.setProperty(PREFERRED_MENU, NETWORK_EDIT_MENU);
		editAnnotationTaskFactoryProps.setProperty(TITLE, "Modify Annotation");
		registerService(bc, editAnnotationTaskFactory, NetworkViewLocationTaskFactory.class, 
		                editAnnotationTaskFactoryProps);

		MoveAnnotationTaskFactory moveAnnotationTaskFactory = new MoveAnnotationTaskFactory();
		Properties moveAnnotationTaskFactoryProps = new Properties();
		moveAnnotationTaskFactoryProps.setProperty(PREFERRED_ACTION, "NEW");
		moveAnnotationTaskFactoryProps.setProperty(MENU_GRAVITY, "2.1");
		moveAnnotationTaskFactoryProps.setProperty(PREFERRED_MENU, NETWORK_EDIT_MENU);
		moveAnnotationTaskFactoryProps.setProperty(TITLE, "Move Annotation");
		registerService(bc, moveAnnotationTaskFactory, NetworkViewLocationTaskFactory.class, 
		                moveAnnotationTaskFactoryProps);


		LayerAnnotationTaskFactory moveTFAnnotationTaskFactory = new LayerAnnotationTaskFactory(-10000);
		Properties moveTFAnnotationTaskFactoryProps = new Properties();
		moveTFAnnotationTaskFactoryProps.setProperty(PREFERRED_ACTION, "NEW");
		moveTFAnnotationTaskFactoryProps.setProperty(MENU_GRAVITY, "3.1");
		moveTFAnnotationTaskFactoryProps.setProperty(PREFERRED_MENU, NETWORK_EDIT_MENU+".Reorder Annotations[2.2]");
		moveTFAnnotationTaskFactoryProps.setProperty(TITLE, "Move Annotation To Front");
		registerService(bc, moveTFAnnotationTaskFactory, NetworkViewLocationTaskFactory.class, 
		                moveTFAnnotationTaskFactoryProps);

		LayerAnnotationTaskFactory moveFAnnotationTaskFactory = new LayerAnnotationTaskFactory(-1);
		Properties moveFAnnotationTaskFactoryProps = new Properties();
		moveFAnnotationTaskFactoryProps.setProperty(PREFERRED_ACTION, "NEW");
		moveFAnnotationTaskFactoryProps.setProperty(MENU_GRAVITY, "3.2");
		moveFAnnotationTaskFactoryProps.setProperty(PREFERRED_MENU, NETWORK_EDIT_MENU+".Reorder Annotations[2.2]");
		moveFAnnotationTaskFactoryProps.setProperty(TITLE, "Move Annotation Forwards");
		registerService(bc, moveFAnnotationTaskFactory, NetworkViewLocationTaskFactory.class, 
		                moveFAnnotationTaskFactoryProps);

		LayerAnnotationTaskFactory moveBAnnotationTaskFactory = new LayerAnnotationTaskFactory(1);
		Properties moveBAnnotationTaskFactoryProps = new Properties();
		moveBAnnotationTaskFactoryProps.setProperty(PREFERRED_ACTION, "NEW");
		moveBAnnotationTaskFactoryProps.setProperty(MENU_GRAVITY, "3.3");
		moveBAnnotationTaskFactoryProps.setProperty(PREFERRED_MENU, NETWORK_EDIT_MENU+".Reorder Annotations[2.2]");
		moveBAnnotationTaskFactoryProps.setProperty(TITLE, "Move Annotation Backwards");
		registerService(bc, moveBAnnotationTaskFactory, NetworkViewLocationTaskFactory.class, 
		                moveBAnnotationTaskFactoryProps);

		LayerAnnotationTaskFactory moveTBAnnotationTaskFactory = new LayerAnnotationTaskFactory(10000);
		Properties moveTBAnnotationTaskFactoryProps = new Properties();
		moveTBAnnotationTaskFactoryProps.setProperty(PREFERRED_ACTION, "NEW");
		moveTBAnnotationTaskFactoryProps.setProperty(MENU_GRAVITY, "3.4");
		moveTBAnnotationTaskFactoryProps.setProperty(PREFERRED_MENU, NETWORK_EDIT_MENU+".Reorder Annotations[2.2]");
		moveTBAnnotationTaskFactoryProps.setProperty(TITLE, "Move Annotation To Back");
		moveTBAnnotationTaskFactoryProps.setProperty(INSERT_SEPARATOR_AFTER, "true");
		registerService(bc, moveTBAnnotationTaskFactory, NetworkViewLocationTaskFactory.class, 
		                moveTBAnnotationTaskFactoryProps);

		ChangeAnnotationCanvasTaskFactory pullAnnotationTaskFactory = new ChangeAnnotationCanvasTaskFactory(Annotation.FOREGROUND);
		Properties pullAnnotationTaskFactoryProps = new Properties();
		pullAnnotationTaskFactoryProps.setProperty(PREFERRED_ACTION, "NEW");
		pullAnnotationTaskFactoryProps.setProperty(MENU_GRAVITY, "3.5");
		pullAnnotationTaskFactoryProps.setProperty(PREFERRED_MENU, NETWORK_EDIT_MENU+".Reorder Annotations[2.2]");
		pullAnnotationTaskFactoryProps.setProperty(TITLE, "Pull Annotation to Foreground Canvas");
		registerService(bc, pullAnnotationTaskFactory, NetworkViewLocationTaskFactory.class, 
		                pullAnnotationTaskFactoryProps);

		ChangeAnnotationCanvasTaskFactory pushAnnotationTaskFactory = new ChangeAnnotationCanvasTaskFactory(Annotation.BACKGROUND);
		Properties pushAnnotationTaskFactoryProps = new Properties();
		pushAnnotationTaskFactoryProps.setProperty(PREFERRED_ACTION, "NEW");
		pushAnnotationTaskFactoryProps.setProperty(MENU_GRAVITY, "3.6");
		pushAnnotationTaskFactoryProps.setProperty(PREFERRED_MENU, NETWORK_EDIT_MENU+".Reorder Annotations[2.2]");
		pushAnnotationTaskFactoryProps.setProperty(TITLE, "Push Annotation to Background Canvas");
		registerService(bc, pushAnnotationTaskFactory, NetworkViewLocationTaskFactory.class, 
		                pushAnnotationTaskFactoryProps);

		ResizeAnnotationTaskFactory resizeAnnotationTaskFactory = new ResizeAnnotationTaskFactory();
		Properties resizeAnnotationTaskFactoryProps = new Properties();
		resizeAnnotationTaskFactoryProps.setProperty(PREFERRED_ACTION, "NEW");
		resizeAnnotationTaskFactoryProps.setProperty(MENU_GRAVITY, "2.3");
		resizeAnnotationTaskFactoryProps.setProperty(PREFERRED_MENU, NETWORK_EDIT_MENU);
		resizeAnnotationTaskFactoryProps.setProperty(TITLE, "Resize Annotation");
		registerService(bc, resizeAnnotationTaskFactory, NetworkViewLocationTaskFactory.class, 
		                resizeAnnotationTaskFactoryProps);

		// Annotation delete
		RemoveAnnotationTaskFactory removeAnnotationTaskFactory = new RemoveAnnotationTaskFactory();
		Properties removeAnnotationTaskFactoryProps = new Properties();
		removeAnnotationTaskFactoryProps.setProperty(PREFERRED_ACTION, "NEW");
		removeAnnotationTaskFactoryProps.setProperty(MENU_GRAVITY, "1.1");
		removeAnnotationTaskFactoryProps.setProperty(PREFERRED_MENU, NETWORK_DELETE_MENU);
		removeAnnotationTaskFactoryProps.setProperty(TITLE, "Annotation");
		registerService(bc, removeAnnotationTaskFactory, NetworkViewLocationTaskFactory.class, 
		                removeAnnotationTaskFactoryProps);

		// Annotation select
		SelectAnnotationTaskFactory selectAnnotationTaskFactory = new SelectAnnotationTaskFactory();
		Properties selectAnnotationTaskFactoryProps = new Properties();
		selectAnnotationTaskFactoryProps.setProperty(PREFERRED_ACTION, "NEW");
		selectAnnotationTaskFactoryProps.setProperty(MENU_GRAVITY, "1.1");
		selectAnnotationTaskFactoryProps.setProperty(PREFERRED_MENU, NETWORK_SELECT_MENU);
		selectAnnotationTaskFactoryProps.setProperty(TITLE, "Select/Unselect Annotation");
		registerService(bc, selectAnnotationTaskFactory, NetworkViewLocationTaskFactory.class, 
		                selectAnnotationTaskFactoryProps);

		// Annotation group
		GroupAnnotationsTaskFactory groupAnnotationTaskFactory = new GroupAnnotationsTaskFactory();
		Properties groupAnnotationTaskFactoryProps = new Properties();
		groupAnnotationTaskFactoryProps.setProperty(PREFERRED_ACTION, "NEW");
		groupAnnotationTaskFactoryProps.setProperty(MENU_GRAVITY, "100");
		groupAnnotationTaskFactoryProps.setProperty(INSERT_SEPARATOR_BEFORE, "true");
		groupAnnotationTaskFactoryProps.setProperty(IN_MENU_BAR, "false");
		groupAnnotationTaskFactoryProps.setProperty(PREFERRED_MENU, NETWORK_GROUP_MENU);
		groupAnnotationTaskFactoryProps.setProperty(TITLE, "Group Annotations");
		registerService(bc, groupAnnotationTaskFactory, NetworkViewTaskFactory.class, 
		                groupAnnotationTaskFactoryProps);

		// Annotation ungroup
		UngroupAnnotationsTaskFactory ungroupAnnotationTaskFactory = new UngroupAnnotationsTaskFactory();
		Properties ungroupAnnotationTaskFactoryProps = new Properties();
		ungroupAnnotationTaskFactoryProps.setProperty(PREFERRED_ACTION, "NEW");
		ungroupAnnotationTaskFactoryProps.setProperty(MENU_GRAVITY, "100");
		ungroupAnnotationTaskFactoryProps.setProperty(INSERT_SEPARATOR_BEFORE, "true");
		ungroupAnnotationTaskFactoryProps.setProperty(PREFERRED_MENU, NETWORK_GROUP_MENU);
		ungroupAnnotationTaskFactoryProps.setProperty(TITLE, "Ungroup Annotations");
		registerService(bc, ungroupAnnotationTaskFactory, NetworkViewLocationTaskFactory.class, 
		                ungroupAnnotationTaskFactoryProps);

		// Set mouse drag selection modes
		SelectModeTaskFactory selectNodesOnly = new SelectModeTaskFactory("Nodes only", applicationManagerServiceRef);
		Properties selectNodesOnlyProps = new Properties();
		selectNodesOnlyProps.setProperty(PREFERRED_ACTION, "New");
		selectNodesOnlyProps.setProperty(MENU_GRAVITY, "0.5");
		selectNodesOnlyProps.setProperty(PREFERRED_MENU, "Select.Mouse Drag Selects");
		selectNodesOnlyProps.setProperty(TITLE, "Nodes Only");
		registerService(bc, selectNodesOnly, TaskFactory.class, selectNodesOnlyProps);
		
		SelectModeTaskFactory selectEdgesOnly = new SelectModeTaskFactory("Edges only", applicationManagerServiceRef);
		Properties selectEdgesOnlyProps = new Properties();
		selectEdgesOnlyProps.setProperty(PREFERRED_ACTION, "New");
		selectEdgesOnlyProps.setProperty(MENU_GRAVITY, "0.6");
		selectEdgesOnlyProps.setProperty(PREFERRED_MENU, "Select.Mouse Drag Selects");
		selectEdgesOnlyProps.setProperty(TITLE, "Edges Only");
		registerService(bc, selectEdgesOnly, TaskFactory.class, selectEdgesOnlyProps);

		SelectModeTaskFactory selectNodesAndEdges = new SelectModeTaskFactory("Nodes and Edges", applicationManagerServiceRef);
		Properties selectNodesEdgesProps = new Properties();
		selectNodesEdgesProps.setProperty(PREFERRED_ACTION, "New");
		selectNodesEdgesProps.setProperty(MENU_GRAVITY, "0.7");
		selectNodesEdgesProps.setProperty(PREFERRED_MENU, "Select.Mouse Drag Selects");
		selectNodesEdgesProps.setProperty(TITLE, "Nodes and Edges");
		registerService(bc, selectNodesAndEdges, TaskFactory.class, selectNodesEdgesProps);
		
		//
		ShowGraphicsDetailsTaskFactory showGraphicsDetailsTaskFactory = new ShowGraphicsDetailsTaskFactory(applicationManagerServiceRef,dingGraphLOD, dingGraphLODAll);
		Properties showGraphicsDetailsTaskFactoryProps = new Properties();
		showGraphicsDetailsTaskFactoryProps.setProperty(MENU_GRAVITY, "11.0");
		showGraphicsDetailsTaskFactoryProps.setProperty(ENABLE_FOR,"networkAndView");
		showGraphicsDetailsTaskFactoryProps.setProperty(TITLE, "Show/Hide Graphics Details");
		showGraphicsDetailsTaskFactoryProps.setProperty(IN_NETWORK_PANEL_CONTEXT_MENU,"true");		
		registerService(bc, showGraphicsDetailsTaskFactory, NetworkTaskFactory.class, showGraphicsDetailsTaskFactoryProps);

		final String vtfFilter = String.format("(| (!(%s=*)) (%s=true))", IN_CONTEXT_MENU, IN_CONTEXT_MENU); // if IN_CONTEXT_MENU is not specified, default to true
		registerServiceListener(bc, vtfListener, "addNodeViewTaskFactory", "removeNodeViewTaskFactory",
				NodeViewTaskFactory.class, vtfFilter);
		registerServiceListener(bc, vtfListener, "addEdgeViewTaskFactory", "removeEdgeViewTaskFactory",
				EdgeViewTaskFactory.class, vtfFilter);
		registerServiceListener(bc, vtfListener, "addNetworkViewTaskFactory", "removeNetworkViewTaskFactory",
				NetworkViewTaskFactory.class, vtfFilter);
		registerServiceListener(bc, vtfListener, "addNetworkViewLocationTaskFactory",
				"removeNetworkViewLocationTaskFactory", NetworkViewLocationTaskFactory.class);
		registerServiceListener(bc, vtfListener, "addCyEdgeViewContextMenuFactory",
				"removeCyEdgeViewContextMenuFactory", CyEdgeViewContextMenuFactory.class);
		registerServiceListener(bc, vtfListener, "addCyNodeViewContextMenuFactory",
				"removeCyNodeViewContextMenuFactory", CyNodeViewContextMenuFactory.class);
		registerServiceListener(bc, vtfListener, "addCyNetworkViewContextMenuFactory",
				"removeCyNetworkViewContextMenuFactory", CyNetworkViewContextMenuFactory.class);

		registerServiceListener(bc, annotationFactoryManager, "addAnnotationFactory", "removeAnnotationFactory",
				AnnotationFactory.class);

		GraphicsDetailAction graphicsDetailAction = new GraphicsDetailAction(applicationManagerServiceRef,
				cyNetworkViewManagerServiceRef, dingGraphLOD, dingGraphLODAll);
		registerAllServices(bc, graphicsDetailAction, new Properties());

		BendFactory bendFactory = new BendFactoryImpl();
		registerService(bc, bendFactory, BendFactory.class, new Properties());

		// Register the factory
		dVisualLexicon.addBendFactory(bendFactory, new HashMap());
		
		// Translator for Passthrough
		final CustomGraphicsTranslator cgTranslator = new CustomGraphicsTranslator(customGraphicsManagerServiceRef);
		registerService(bc, cgTranslator, ValueTranslator.class, new Properties());
		
		// Factories for Visual Property Dependency
		final NodeSizeDependencyFactory nodeSizeDependencyFactory = new NodeSizeDependencyFactory(dVisualLexicon);
		registerService(bc, nodeSizeDependencyFactory, VisualPropertyDependencyFactory.class, new Properties());

		final EdgeColorDependencyFactory edgeColorDependencyFactory = new EdgeColorDependencyFactory(dVisualLexicon);
		registerService(bc, edgeColorDependencyFactory, VisualPropertyDependencyFactory.class, new Properties());

		final CustomGraphicsSizeDependencyFactory customGraphicsSizeDependencyFactory = new CustomGraphicsSizeDependencyFactory(dVisualLexicon);
		registerService(bc, customGraphicsSizeDependencyFactory, VisualPropertyDependencyFactory.class, new Properties());

	}

	private void startCustomGraphicsMgr(BundleContext bc) {

		DialogTaskManager dialogTaskManagerServiceRef = getService(bc, DialogTaskManager.class);
		CyProperty coreCyPropertyServiceRef = getService(bc, CyProperty.class, "(cyPropertyName=cytoscape3.props)");
		CyApplicationManager cyApplicationManagerServiceRef = getService(bc, CyApplicationManager.class);
		CyApplicationConfiguration cyApplicationConfigurationServiceRef = getService(bc,
				CyApplicationConfiguration.class);
		CyEventHelper eventHelperServiceRef = getService(bc, CyEventHelper.class);

		VisualMappingManager vmmServiceRef = getService(bc, VisualMappingManager.class);
		
		CustomGraphicsManagerImpl customGraphicsManager = new CustomGraphicsManagerImpl(coreCyPropertyServiceRef,
				dialogTaskManagerServiceRef, cyApplicationConfigurationServiceRef, eventHelperServiceRef,
				vmmServiceRef, cyApplicationManagerServiceRef, getdefaultImageURLs(bc));
		CustomGraphicsBrowser browser = new CustomGraphicsBrowser(customGraphicsManager);
		registerAllServices(bc, browser, new Properties());

		CustomGraphicsManagerAction customGraphicsManagerAction = new CustomGraphicsManagerAction(
				customGraphicsManager, cyApplicationManagerServiceRef, browser);

		registerAllServices(bc, customGraphicsManager, new Properties());
		registerService(bc, customGraphicsManagerAction, CyAction.class, new Properties());

		// Create and register our built-in factories.
		// TODO:  When the CustomGraphicsFactory service stuff is set up, just
		// register these as services
		NullCustomGraphicsFactory nullFactory = new NullCustomGraphicsFactory(customGraphicsManager);
		customGraphicsManager.addCustomGraphicsFactory(nullFactory, new Properties());

		URLImageCustomGraphicsFactory imageFactory = new URLImageCustomGraphicsFactory(customGraphicsManager);
		customGraphicsManager.addCustomGraphicsFactory(imageFactory, new Properties());

		GradientOvalFactory ovalFactory = new GradientOvalFactory(customGraphicsManager);
		customGraphicsManager.addCustomGraphicsFactory(ovalFactory, new Properties());

		GradientRoundRectangleFactory rectangleFactory = 
		     new GradientRoundRectangleFactory(customGraphicsManager);
		customGraphicsManager.addCustomGraphicsFactory(rectangleFactory, new Properties());

		// Now register our service listener so that app writers can
		// provide their own CustomGraphics factory methods
		registerServiceListener(bc, customGraphicsManager, 
		                        "addCustomGraphicsFactory", "removeCustomGraphicsFactory", 
		                        CyCustomGraphicsFactory.class);
	}
	
	/**
	 * Get list of default images from resource.
	 * 
	 * @param bc
	 * @return Set of default image files in the bundle
	 */
	private Set<URL> getdefaultImageURLs(BundleContext bc) {
		Enumeration<URL> e = bc.getBundle().findEntries("images/sampleCustomGraphics", "*.png", true);
		final Set<URL> defaultImageUrls = new HashSet<URL>();
		while (e.hasMoreElements())
			defaultImageUrls.add(e.nextElement());
		
		return defaultImageUrls;
	}

	private void startSpacial(BundleContext bc) {
		RTreeFactory rtreeFactory = new RTreeFactory();
		registerService(bc,rtreeFactory,SpacialIndex2DFactory.class, new Properties());
	}
}
