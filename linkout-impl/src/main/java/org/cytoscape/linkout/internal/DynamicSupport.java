
/*
 Copyright (c) 2006, 2007, The Cytoscape Consortium (www.cytoscape.org)

 The Cytoscape Consortium is:
 - Institute for Systems Biology
 - University of California San Diego
 - Memorial Sloan-Kettering Cancer Center
 - Institut Pasteur
 - Agilent Technologies

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

package org.cytoscape.linkout.internal;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.swing.JMenuItem;

import org.cytoscape.application.swing.CyMenuItem;
import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.util.swing.OpenBrowser;
import org.cytoscape.work.SynchronousTaskManager;
import org.cytoscape.work.TaskIterator;


 class DynamicSupport {

	private static final String EXTERNAL_LINK_ATTR = "Linkout.ExternalLinks";

	private final SynchronousTaskManager synTaskManager;

	private Map<String,String> menuTitleURLMap = new HashMap<String,String>();
	private CyIdentifiable[] tableEntries;

	protected final OpenBrowser browser;

	public DynamicSupport(OpenBrowser browser, SynchronousTaskManager synTaskManager) {
		this.browser = browser;
		this.synTaskManager = synTaskManager;
	}

	protected synchronized void setURLs(CyNetwork network, CyIdentifiable... entries) {
		menuTitleURLMap.clear();
		if ( entries == null || network == null) {
			menuTitleURLMap.clear();
			return;
		}

		tableEntries = entries; 

		for ( CyIdentifiable entry : tableEntries )
			generateExternalLinks(network.getRow(entry), menuTitleURLMap);

	}

	private void generateExternalLinks(CyRow row, Map<String,String> urlMap) {
		//System.out.println("looking for external links for CyRow: " + row.get("name", String.class));
		CyColumn column = row.getTable().getColumn(EXTERNAL_LINK_ATTR); 
		if (column != null) {
			Class<?> attrType = column.getType();
			// Single title=url pair
			if (attrType == String.class) { 
				//System.out.println(" it's a String");
				String linkAttr = row.get(EXTERNAL_LINK_ATTR,String.class);
				addExternalLink(linkAttr, urlMap);
				// List of title=url pairs 
			} else if (attrType == List.class) { 
				//System.out.println(" it's a List");
				List<String> attrList = row.getList(EXTERNAL_LINK_ATTR, String.class);
				for (String linkAttr : (List<String>) attrList) {
					addExternalLink(linkAttr, urlMap);
				}
			}
		}
	}

	private void addExternalLink(String linkAttr, Map<String,String> urlMap) {
		if (linkAttr == null) {
			System.out.println("link attr is null");
			return;
		}

		String[] pair = linkAttr.split("=", 2);

		if (pair.length != 2) {
			System.out.println("Didn't tokenize on equals" + linkAttr);
			return;
		}

		if (!pair[1].startsWith("http")) {
			System.out.println("not a url: " + pair[1]);
			return;
		}

		//System.out.println("EXTERNAL LINK putting menu: " + pair[0] + "    and link: " + pair[1]);

		urlMap.put(removeMarker(pair[0]),pair[1]);
	}

	private String removeMarker(String s) {
		if ( s.startsWith(LinkOut.NODEMARKER) )
			return s.substring(LinkOut.NODEMARKER.length());
		else if ( s.startsWith(LinkOut.EDGEMARKER) )
			return s.substring(LinkOut.EDGEMARKER.length());
		else 
			return s;
	}

	public void createSubMenus(CyMenuItem menuItem, final CyNetwork network, CyIdentifiable... entries) {

		setURLs(network, entries);
		
		for(final String menuTitle: menuTitleURLMap.keySet()){
			JMenuItem subMenu = new JMenuItem(menuTitle);
			subMenu.addActionListener(new ActionListener() {

				@Override
				public void actionPerformed(ActionEvent e) {
					String url = "none found"; 
					synchronized (this) {
						url = menuTitleURLMap.get( menuTitle );	
					}
					synTaskManager.execute(new TaskIterator(new LinkoutTask(url, browser, network, tableEntries )));
				}
			});
			menuItem.getMenuItem().add(subMenu);
		}
	}
}
