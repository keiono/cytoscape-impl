package org.cytoscape.task.internal.utils;

/*
 * #%L
 * Cytoscape Core Task Impl (core-task-impl)
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
import java.util.List;

import org.cytoscape.application.CyApplicationManager;
import org.cytoscape.command.util.EdgeList;
import org.cytoscape.command.util.NodeList;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNode;
import org.cytoscape.work.Tunable;

public class NodeAndEdgeTunable {
	CyApplicationManager appMgr;
	
	@Tunable(description="Network", context="nogui")
	public CyNetwork network = null;

	public NodeList nodeList = null;
	@Tunable(description="List of nodes", context="nogui")
	public NodeList getnodeList() {
		if (network == null)
			network = appMgr.getCurrentNetwork();
		if (nodeList == null) 
			nodeList = new NodeList(network);
		else
			nodeList.setNetwork(network);
		return nodeList;
	}
  public void setnodeList(NodeList setValue) {}

	public EdgeList edgeList = null;
	@Tunable(description="List of edges", context="nogui")
	public EdgeList getedgeList() {
		if (network == null)
			network = appMgr.getCurrentNetwork();
		if (edgeList == null) 
			edgeList = new EdgeList(network);
		else
			edgeList.setNetwork(network);
		return edgeList;
	}
  public void setedgeList(EdgeList setValue) {}

	public NodeAndEdgeTunable(CyApplicationManager appMgr) {
		this.appMgr = appMgr;
	}

	public CyNetwork getNetwork() { 
		if (network == null)
			network = appMgr.getCurrentNetwork();
		return network; 
	}

	public List<CyNode> getNodeList() {
		return getNodeList(true);
	}

	public List<CyNode> getNodeList(boolean useAllForDefault) {
		if (nodeList == null || nodeList.getValue() == null) {
			if (useAllForDefault)
				return getNetwork().getNodeList();
			else
				return null;
		}
		return nodeList.getValue();
	}

	public List<CyEdge> getEdgeList() {
		return getEdgeList(true);
	}

	public List<CyEdge> getEdgeList(boolean useAllForDefault) {
		if (edgeList == null || edgeList.getValue() == null) {
			if (useAllForDefault)
				return getNetwork().getEdgeList();
			else
				return null;
		}
		return edgeList.getValue();
	}
}
