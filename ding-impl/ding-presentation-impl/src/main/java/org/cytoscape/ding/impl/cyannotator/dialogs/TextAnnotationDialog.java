package org.cytoscape.ding.impl.cyannotator.dialogs;

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


import java.awt.geom.Point2D;

import javax.swing.JDialog;
import javax.swing.JFrame;

import org.cytoscape.ding.impl.DGraphView;
import org.cytoscape.ding.impl.cyannotator.CyAnnotator;
import org.cytoscape.ding.impl.cyannotator.annotations.TextAnnotationImpl;

public class TextAnnotationDialog extends JDialog {

	private final DGraphView view;
	private final CyAnnotator cyAnnotator;
	private final Point2D startingLocation;
	private final boolean create;
	private final TextAnnotationImpl mAnnotation;
	private TextAnnotationPanel textAnnotation1;
	private TextAnnotationImpl preview;

	public TextAnnotationDialog(DGraphView view, Point2D start) {
		this.view = view;
		this.cyAnnotator =  view.getCyAnnotator();
		this.startingLocation = start;
		this.mAnnotation = new TextAnnotationImpl(cyAnnotator, view);
		create = true;

		initComponents();	
	}	

	public TextAnnotationDialog(TextAnnotationImpl mAnnotation) {
		this.mAnnotation=mAnnotation;
		this.cyAnnotator = mAnnotation.getCyAnnotator();
		this.view = cyAnnotator.getView();
		this.create = false;
		this.startingLocation = null;

		initComponents();
	}

		
	private void initComponents() {
		int TEXT_HEIGHT = 220;
		int TEXT_WIDTH = 500;
		int PREVIEW_WIDTH = 500;
		int PREVIEW_HEIGHT = 200;

		// Create the preview panel
		preview = new TextAnnotationImpl(cyAnnotator, view);
		preview.setUsedForPreviews(true);
		preview.getComponent().setSize(PREVIEW_WIDTH-10, PREVIEW_HEIGHT-10);
		PreviewPanel previewPanel = new PreviewPanel(preview, PREVIEW_WIDTH, PREVIEW_HEIGHT);

		textAnnotation1 = new TextAnnotationPanel(mAnnotation, previewPanel, TEXT_WIDTH, TEXT_HEIGHT);

		applyButton = new javax.swing.JButton();
		cancelButton = new javax.swing.JButton();

		if (create)
			setTitle("Create Text Annotation");
		else 
			setTitle("Modify Text Annotation");

		setResizable(false);
		getContentPane().setLayout(null);

		getContentPane().add(textAnnotation1);
		textAnnotation1.setBounds(0, 0, TEXT_WIDTH, TEXT_HEIGHT);

		getContentPane().add(previewPanel);
		previewPanel.setBounds(0, TEXT_HEIGHT+5, PREVIEW_WIDTH, PREVIEW_HEIGHT);

		int y = TEXT_HEIGHT+PREVIEW_HEIGHT+20;

		applyButton.setText("OK");
		applyButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				applyButtonActionPerformed(evt);
			}
		});
		getContentPane().add(applyButton);
		applyButton.setBounds(290, y, applyButton.getPreferredSize().width, 23);

		cancelButton.setText("Cancel");
		cancelButton.addActionListener(new java.awt.event.ActionListener() {
			public void actionPerformed(java.awt.event.ActionEvent evt) {
				cancelButtonActionPerformed(evt);
			}
		});

		getContentPane().add(cancelButton);
		cancelButton.setBounds(370, y, cancelButton.getPreferredSize().width, 23);

		pack();
		setSize(TEXT_WIDTH+10, 510);
		setDefaultCloseOperation(JFrame.DISPOSE_ON_CLOSE);
		setModalityType(DEFAULT_MODALITY_TYPE);
	}
			 
	private void applyButtonActionPerformed(java.awt.event.ActionEvent evt) {
		dispose();

		//Apply
		mAnnotation.setFont(textAnnotation1.getNewFont());
		mAnnotation.setTextColor(textAnnotation1.getTextColor());
		mAnnotation.setText(textAnnotation1.getText());

		if (!create) {
			mAnnotation.update(); 
			return;
		}

		//Apply
		mAnnotation.addComponent(null);
		mAnnotation.getComponent().setLocation((int)startingLocation.getX(), (int)startingLocation.getY());

		// We need to have bounds or it won't render
		mAnnotation.getComponent().setBounds(mAnnotation.getComponent().getBounds());

		mAnnotation.update();
		mAnnotation.contentChanged();

		// Update the canvas
		view.getCanvas(DGraphView.Canvas.FOREGROUND_CANVAS).repaint();
	}

	private void cancelButtonActionPerformed(java.awt.event.ActionEvent evt) {
		//Cancel
		dispose();
	}
		
	private javax.swing.JButton applyButton;
	private javax.swing.JButton cancelButton;
}


