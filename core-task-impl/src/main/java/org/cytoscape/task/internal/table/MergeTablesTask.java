package org.cytoscape.task.internal.table;

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

import static org.cytoscape.work.TunableValidator.ValidationState.OK;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.io.IOException;

import org.cytoscape.model.CyColumn;
import org.cytoscape.model.CyEdge;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;
import org.cytoscape.model.CyNetworkManager;
import org.cytoscape.model.CyNode;
import org.cytoscape.model.CyRow;
import org.cytoscape.model.CyTable;
import org.cytoscape.model.CyTableManager;
import org.cytoscape.model.subnetwork.CyRootNetwork;
import org.cytoscape.model.subnetwork.CyRootNetworkManager;
import org.cytoscape.work.AbstractTask;
import org.cytoscape.work.ProvidesTitle;
import org.cytoscape.work.TaskMonitor;
import org.cytoscape.work.Tunable;
import org.cytoscape.work.TunableValidator;
import org.cytoscape.work.TunableValidator.ValidationState;
import org.cytoscape.work.util.ListMultipleSelection;
import org.cytoscape.work.util.ListSingleSelection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class MergeTablesTask extends AbstractTask implements TunableValidator {
	
	enum TableType {
		NODE_ATTR("Node Table Columns", CyNode.class),
		EDGE_ATTR("Edge Table Columns", CyEdge.class),
		NETWORK_ATTR("Network Table Columns", CyNetwork.class);

		private final String name;
		private final Class<? extends CyIdentifiable> type;

		private TableType(final String name, Class<? extends CyIdentifiable> type) {
			this.name = name;
			this.type = type;
		}

		public Class<? extends CyIdentifiable> getType() {
			return this.type;
		}

		@Override
		public String toString() {
			return name;
		}
	};
	

	private static final Logger logger = LoggerFactory.getLogger(MapTableToNetworkTablesTask.class);
	private static final String NO_NETWORKS = "No Networks Found";
	public static final String NO_TABLES = "No Tables Found";
	
	public static final String NETWORK_COLLECTION = "To a network collection";
	public static final String NETWORK_SELECTION = "To selected networks only";
	public static final String UNASSIGNED_TABLE = "To an unassigned table";
	
	public static final String COPY_COLUMNS = "Copy Columns";
	public static final String LINK_COLUMNS = "Link To Columns";
	
	private CyRootNetworkManager rootNetworkManager;
	private CyNetworkManager networkManager;
	private CyTableManager tableMgr;
	private Map<String, CyNetwork> name2NetworkMap;
	private Map<String, CyRootNetwork> name2RootMap;
	private Map<String, String> source2targetColumnMap;
	
	public ListSingleSelection<CyTable> sourceTable;
	@Tunable(description="Source table to merge",gravity=0.1, groups={"Source Data Table"})
	
	public ListSingleSelection<CyTable> getSourceTable() {
		return sourceTable;
	}
	
	public void setSourceTable(ListSingleSelection<CyTable> table) {
		
		ListMultipleSelection<String> tempList = getColumns(table.getSelectedValue());
		if(!sourceMergeColumns.getPossibleValues().containsAll(tempList.getPossibleValues()) 
				|| sourceMergeColumns.getPossibleValues().size() != tempList.getPossibleValues().size())
		{
			sourceMergeColumns = tempList;
			sourceMergeKey = getColumnsWithNames(table.getSelectedValue());
			if(selectAllColumns)
				sourceMergeColumns.setSelectedValues(sourceMergeColumns.getPossibleValues());
			
		}
		
		List<Object> listOfGlobal = getPublicGlobalTables();
		if(listOfGlobal.contains(table.getSelectedValue()))
		{
			if( listOfGlobal.size()==1)
			{
				listOfGlobal.clear();
				listOfGlobal.add(NO_TABLES);
				targetMergeKey = new ListSingleSelection<String>(NO_TABLES);
				unassignedTable = new ListSingleSelection<Object>(listOfGlobal);
			}
			else
			{
				listOfGlobal.remove(table.getSelectedValue());
				unassignedTable = new ListSingleSelection<Object>(listOfGlobal);
				targetMergeKey = getColumnsWithNames((CyTable)unassignedTable.getSelectedValue());
			}
		}
		else
		{
			if( listOfGlobal.size()==1 && !(unassignedTable.getSelectedValue() instanceof CyTable) || 
					((listOfGlobal.size() != unassignedTable.getPossibleValues().size())	&& (listOfGlobal.size() > 0)))
			{
				unassignedTable = new ListSingleSelection<Object>(listOfGlobal);
				targetMergeKey = getColumnsWithNames((CyTable)unassignedTable.getSelectedValue());
			}
			
		}
		if(!isTableGlobal(table.getSelectedValue()))
		{
			if(mergeType.getPossibleValues().size() > 1)
				mergeType = new ListSingleSelection<String>(COPY_COLUMNS);
		}
		else
		{
			if(mergeType.getPossibleValues().size() < 2)
			{
				mergeType = new ListSingleSelection<String>(COPY_COLUMNS,LINK_COLUMNS);
				mergeType.setSelectedValue(COPY_COLUMNS);
			}
		}
		sourceTable = table;
	}
	
	public ListMultipleSelection<String> sourceMergeColumns;
	@Tunable(description="List of columns to merge",gravity=0.2, groups={"Source Data Table","Data Columns To Merge"},listenForChange={"SourceTable","SelectAllColumns"})
	public ListMultipleSelection<String> getSourceMergeColumns(){
		
		return sourceMergeColumns;
	}
	
	public void setSourceMergeColumns(ListMultipleSelection<String> columns){
		
		sourceMergeColumns = columns;
	}
	
	public boolean selectAllColumns = false;
	@Tunable(description="Select all columns",gravity=0.3, groups={"Source Data Table","Data Columns To Merge"},listenForChange={"SourceMergeColumns"})
	
	public boolean getSelectAllColumns (){		
		
		if(sourceMergeColumns.getPossibleValues().size() != sourceMergeColumns.getSelectedValues().size() && selectAllColumns)
		    selectAllColumns = false;
		
		if(sourceMergeColumns.getPossibleValues().size() == sourceMergeColumns.getSelectedValues().size() && !selectAllColumns)
		    selectAllColumns = true;
		
		return selectAllColumns;
	}
	
	public void setSelectAllColumns ( boolean selected){
		
		if(selected != selectAllColumns)
		{
			selectAllColumns = selected;
			if(selectAllColumns)
				sourceMergeColumns.setSelectedValues(sourceMergeColumns.getPossibleValues());
			else
				sourceMergeColumns.setSelectedValues(new ArrayList<String>());
		}
	}
	
	public ListSingleSelection<String> sourceMergeKey;
	@Tunable(description = "Key column to merge", groups={"Source Data Table"},gravity=0.4, listenForChange={"SourceTable"})
	public ListSingleSelection<String> getSourceMergeKey() {
		return sourceMergeKey;
	}

	public void setSourceMergeKey(ListSingleSelection<String> key) {
		this.sourceMergeKey = key;
	}
	
	@Tunable(description="Type of merge",gravity=0.5, groups={"Source Data Table"}, listenForChange={"SourceTable"})
	public ListSingleSelection<String> mergeType;

	
	public ListSingleSelection<String> whereMergeTable ;
	@Tunable(description="Where to merge the data table",gravity=1.0, groups={"Target Data Table"}, xorChildren=true)
	
	public ListSingleSelection<String> getWhereMergeTable() {
		return whereMergeTable;
	}

	public void setWhereMergeTable(ListSingleSelection<String> chooser) {
		this.whereMergeTable = chooser;
	}

	public ListSingleSelection<String> targetNetworkCollection;
	@Tunable(description = "Network collection", groups = {"Target Data Table","Select Network Collection"},gravity=2.0,  xorKey=NETWORK_COLLECTION)
	public ListSingleSelection<String> getTargetNetworkCollection() {
		return targetNetworkCollection;
	}

	public void setTargetNetworkCollection(ListSingleSelection<String> roots) {
		ListSingleSelection<String> tempList = getColumns(name2RootMap.get(targetNetworkCollection.getSelectedValue()),
				dataTypeTargetForNetworkCollection.getSelectedValue(), CyRootNetwork.SHARED_ATTRS);
		if(!targetKeyNetworkCollection.getPossibleValues().containsAll(tempList.getPossibleValues())
				|| targetKeyNetworkCollection.getPossibleValues().size() != tempList.getPossibleValues().size())
			targetKeyNetworkCollection = tempList;
	}

	public ListSingleSelection<String> targetKeyNetworkCollection;
	@Tunable(description = "Key column for network", groups = {"Target Data Table","Select Network Collection"},gravity=3.0, xorKey=NETWORK_COLLECTION, listenForChange = {
			"DataTypeTargetForNetworkCollection", "TargetNetworkCollection" })
	public ListSingleSelection<String> getTargetKeyNetworkCollection() {
		return targetKeyNetworkCollection;
	}

	public void setTargetKeyNetworkCollection(ListSingleSelection<String> colList) {
		this.targetKeyNetworkCollection = colList;
	}
	
	public ListSingleSelection<TableType> dataTypeTargetForNetworkCollection;

	@Tunable(description = "Merge data in", gravity=4.0, groups={"Target Data Table","Select Network Collection"}, xorKey=NETWORK_COLLECTION)
	public ListSingleSelection<TableType> getDataTypeTargetForNetworkCollection() {
		return dataTypeTargetForNetworkCollection;
	}

	public void setDataTypeTargetForNetworkCollection(ListSingleSelection<TableType> options) {
		ListSingleSelection<String> tempList = getColumns(name2RootMap.get(targetNetworkCollection.getSelectedValue()),
				dataTypeTargetForNetworkCollection.getSelectedValue(), CyRootNetwork.SHARED_ATTRS);
		if(!targetKeyNetworkCollection.getPossibleValues().containsAll(tempList.getPossibleValues()) 
				|| targetKeyNetworkCollection.getPossibleValues().size() != tempList.getPossibleValues().size())
			targetKeyNetworkCollection = tempList;
	}

	public ListMultipleSelection<String> targetNetworkList;
	@Tunable(description = "Network list", groups = {"Target Data Table","Select Networks"},gravity=5.0, xorKey=NETWORK_SELECTION, params = "displayState=uncollapsed")
	public ListMultipleSelection<String> getTargetNetworkList() {
		return targetNetworkList;
	}

	public void setTargetNetworkList(ListMultipleSelection<String> list) {
		this.targetNetworkList = list;
	}

	@Tunable(description = "Merge data in", gravity=6.0, groups={"Target Data Table","Select Networks"}, xorKey=NETWORK_SELECTION)
	public ListSingleSelection<TableType> dataTypeTargetForNetworkList;
	
	
	public ListSingleSelection<Object> unassignedTable;
	@Tunable(description = "Unassigned tables", groups = {"Target Data Table","Select Unassigned Table"},gravity=7.0,listenForChange={"SourceTable"}, xorKey=UNASSIGNED_TABLE)
	public ListSingleSelection<Object> getUnassignedTable() {
		return unassignedTable;
	}

	public void setUnassignedTable(ListSingleSelection<Object> tables) {
		if(tables.getSelectedValue() instanceof CyTable)
		{
			ListSingleSelection<String> tempList = getColumnsWithNames((CyTable)tables.getSelectedValue());
			if(!targetMergeKey.getPossibleValues().containsAll(tempList.getPossibleValues())
					|| targetMergeKey.getPossibleValues().size() != tempList.getPossibleValues().size())
			{
				targetMergeKey = tempList;
				targetMergeKey = getColumnsWithNames((CyTable)tables.getSelectedValue());
				
			}
		}
		this.unassignedTable = tables;
	}
	
	public ListSingleSelection<String> targetMergeKey;
	@Tunable(description = "Key column to merge", groups={"Target Data Table","Select Unassigned Table"},gravity=8.0, listenForChange={"UnassignedTable","SourceTable"})
	public ListSingleSelection<String> getTargetMergeKey() {
		return targetMergeKey;
	}

	public void setTargetMergeKey(ListSingleSelection<String> key) {
		this.targetMergeKey = key;
	}

	@ProvidesTitle
	public String getTitle() {
		return "Merge Data Table";
	}

	public MergeTablesTask( CyTableManager tableMgr,CyRootNetworkManager rootNetworkManager, CyNetworkManager networkManager) {
		init(tableMgr,rootNetworkManager, networkManager);
	}
	
	private final void init( CyTableManager tableMgr,CyRootNetworkManager rootNetworkManeger, CyNetworkManager networkManager) {
		this.rootNetworkManager = rootNetworkManeger;
		this.networkManager = networkManager;
		this.name2NetworkMap = new HashMap<String, CyNetwork>();
		this.name2RootMap = new HashMap<String, CyRootNetwork>();
		this.source2targetColumnMap = new HashMap<String, String>();
		this.tableMgr =  tableMgr;

		initTunable(tableMgr,networkManager);
	}

	private final void initTunable(CyTableManager tabelMgr,CyNetworkManager networkManager) {
		
		List<CyTable> listOfTables = new ArrayList<CyTable>();
		List<Object> listOfUTables = new ArrayList<Object>();
		for ( CyTable tempTable : tabelMgr.getGlobalTables()) 
		{
			if(tempTable.isPublic())
			{
				listOfTables.add(tempTable);
				listOfUTables.add(tempTable);
			}
		}
		
		if(networkManager.getNetworkSet().size()>0)
		{
			whereMergeTable = new ListSingleSelection<String>(NETWORK_COLLECTION,NETWORK_SELECTION,UNASSIGNED_TABLE);
			whereMergeTable.setSelectedValue(NETWORK_COLLECTION);
			final List<TableType> options = new ArrayList<TableType>();
			for (TableType type : TableType.values())
				options.add(type);
			dataTypeTargetForNetworkCollection = new ListSingleSelection<TableType>(options);
			dataTypeTargetForNetworkCollection.setSelectedValue(TableType.NODE_ATTR);
			dataTypeTargetForNetworkList = new ListSingleSelection<TableType>(options);
			dataTypeTargetForNetworkList.setSelectedValue(TableType.NODE_ATTR);
	
			for (CyNetwork net : networkManager.getNetworkSet()) {
				String netName = net.getRow(net).get(CyNetwork.NAME, String.class);
				name2NetworkMap.put(netName, net);
			}
			List<String> names = new ArrayList<String>();
			names.addAll(name2NetworkMap.keySet());
			if (names.isEmpty())
				targetNetworkList = new ListMultipleSelection<String>(NO_NETWORKS);
			else
				targetNetworkList = new ListMultipleSelection<String>(names);
	
			for (CyNetwork net : networkManager.getNetworkSet()) {
				final CyRootNetwork rootNet = rootNetworkManager.getRootNetwork(net);
				if (!name2RootMap.containsValue(rootNet))
					name2RootMap.put(rootNet.getRow(rootNet).get(CyRootNetwork.NAME, String.class), rootNet);
			}
			List<String> rootNames = new ArrayList<String>();
			rootNames.addAll(name2RootMap.keySet());
			targetNetworkCollection = new ListSingleSelection<String>(rootNames);
			if(!rootNames.isEmpty())
			{
				targetNetworkCollection.setSelectedValue(rootNames.get(0));
	
				targetKeyNetworkCollection = getColumns(name2RootMap.get(targetNetworkCollection.getSelectedValue()),
						dataTypeTargetForNetworkCollection.getSelectedValue(), CyRootNetwork.SHARED_ATTRS);
			}
			for ( CyNetwork network : networkManager.getNetworkSet()) 
			{
				listOfTables.add(network.getDefaultNodeTable());
				listOfTables.add(network.getDefaultEdgeTable());
			}
		}
		else
		{
			whereMergeTable = new ListSingleSelection<String>(UNASSIGNED_TABLE);
			whereMergeTable.setSelectedValue(UNASSIGNED_TABLE);
		}
		
		sourceTable = new ListSingleSelection<CyTable>(listOfTables);
		if(!isTableGlobal(sourceTable.getSelectedValue()))
			mergeType = new ListSingleSelection<String>(COPY_COLUMNS);
		else
		{
			mergeType = new ListSingleSelection<String>(COPY_COLUMNS,LINK_COLUMNS);
			mergeType.setSelectedValue(COPY_COLUMNS);
		}
		sourceMergeColumns = getColumns(sourceTable.getSelectedValue());
		sourceMergeKey = getColumnsWithNames(sourceTable.getSelectedValue());
		if(listOfUTables.size()>1)
		{
			if(listOfUTables.contains(sourceTable.getSelectedValue()))
				listOfUTables.remove(sourceTable.getSelectedValue());
			unassignedTable = new ListSingleSelection<Object>(listOfUTables);
			targetMergeKey = getColumnsWithNames((CyTable)unassignedTable.getSelectedValue());
		}
		else
		{
			listOfUTables.clear();
			listOfUTables.add(NO_TABLES);
			targetMergeKey = new ListSingleSelection<String>(NO_TABLES);
			unassignedTable = new ListSingleSelection<Object>(listOfUTables);
		}
	}

	public ListSingleSelection<String> getColumns(CyNetwork network, TableType tableType, String namespace) {
		CyTable selectedTable = getTable(network, tableType, CyRootNetwork.SHARED_ATTRS);

		String tempName;
		List<String> colNames = new ArrayList<String>();
		for (CyColumn col : selectedTable.getColumns())
		{
			tempName = col.getName();
			if(!tempName.matches(CyRootNetwork.SUID)  && !tempName.matches(CyRootNetwork.SELECTED))
				colNames.add(tempName);
		}

		ListSingleSelection<String> columns = new ListSingleSelection<String>(colNames);
		columns.setSelectedValue(CyRootNetwork.SHARED_NAME);
		return columns;
	}

	@Override
	public void run(TaskMonitor taskMonitor) throws Exception {
		
		//If we are here and there no networks loaded, we could only continue if the merge is on 
		// an unassigned table
		if(!whereMergeTable.getSelectedValue().matches(UNASSIGNED_TABLE))
		{
			if(name2RootMap.isEmpty())
				return;
		}

		if (!checkKeys()) {
			throw new IllegalArgumentException("Types of keys selected for tables are not matching.");
		}

		if(whereMergeTable.getSelectedValue().matches(NETWORK_COLLECTION))
			mapTableToDefaultAttrs(getDataTypeOptions());
		else if(whereMergeTable.getSelectedValue().matches(NETWORK_SELECTION))
			mapTableToLocalAttrs(getDataTypeOptions());
		else if(whereMergeTable.getSelectedValue().matches(UNASSIGNED_TABLE))
			mapTableToUnassignedTable();

	}
	
	private void mapTableToUnassignedTable() {
		
		if(!unassignedTable.getSelectedValue().toString().matches(NO_TABLES))
		{
			CyTable tableChosen = (CyTable)unassignedTable.getSelectedValue();
			if(!tableChosen.equals(sourceTable))
			{
				applyMapping(tableChosen);
			}
			
		}
		
	}

	private void mapTableToLocalAttrs(TableType tableType) {
		List<CyNetwork> networks = new ArrayList<CyNetwork>();

		if (targetNetworkList.getSelectedValues().isEmpty())
			return;

		if (!targetNetworkList.getSelectedValues().get(0).equals(NO_NETWORKS))
			for (String netName : targetNetworkList.getSelectedValues())
				networks.add(name2NetworkMap.get(netName));

		for (CyNetwork network : networks) {
			CyTable targetTable = getTable(network, tableType, CyNetwork.LOCAL_ATTRS);
			if (targetTable != null)
				applyMapping(targetTable);
		}
	}

	private void mapTableToDefaultAttrs(TableType tableType) {
		CyTable targetTable = getTable(name2RootMap.get(targetNetworkCollection.getSelectedValue()), tableType,
				CyRootNetwork.SHARED_DEFAULT_ATTRS);
		if (targetTable != null) {
			applyMapping(targetTable);
		}
	}

	private CyTable getTable(CyNetwork network, TableType tableType, String namespace) {
		if (tableType == TableType.NODE_ATTR)
			return network.getTable(CyNode.class, namespace);
		if (tableType == TableType.EDGE_ATTR)
			return network.getTable(CyEdge.class, namespace);
		if (tableType == TableType.NETWORK_ATTR)
			return network.getTable(CyNetwork.class, namespace);

		logger.warn("The selected table type is not valie. \nTable needs to be one of these types: "
				+ TableType.NODE_ATTR + ", " + TableType.EDGE_ATTR + ", " + TableType.NETWORK_ATTR + ".");
		return null;
	}

	private void applyMapping(CyTable targetTable) {
		ArrayList<CyColumn> columns = new ArrayList<CyColumn>();
		CyColumn tempCol;
		
		for(String colName :sourceMergeColumns.getSelectedValues())
		{
		    tempCol = sourceTable.getSelectedValue().getColumn(colName);
			if(tempCol != null)
				columns.add(tempCol);
		}
		copyColumns(sourceTable.getSelectedValue(),columns, targetTable,isNewColumnVirtual());
		if(!isNewColumnVirtual())
			copyRows(sourceTable.getSelectedValue(),columns, targetTable);
			

	}

	private CyColumn getJoinTargetColumn(CyTable targetTable) {
		String joinKeyName = CyNetwork.NAME;
		if(whereMergeTable.getSelectedValue().matches(NETWORK_COLLECTION))
			joinKeyName = targetKeyNetworkCollection.getSelectedValue();
		if(whereMergeTable.getSelectedValue().matches(UNASSIGNED_TABLE))
			joinKeyName = targetMergeKey.getSelectedValue();//targetTable.getPrimaryKey().getName();
		return targetTable.getColumn(joinKeyName);
	}

	private void copyRows(CyTable inputTable, List<CyColumn> sourceColumns, CyTable targetTable) {
		CyRow sourceRow;
		CyColumn targetKeyColumn = getJoinTargetColumn(targetTable);

		for (CyRow targetRow : targetTable.getAllRows()) {
			Object key = targetRow.get(targetKeyColumn.getName(), targetKeyColumn.getType());

			if(isMergeColumnKeyColumn())
			{
				if(!inputTable.rowExists(key))
					continue;
				
				sourceRow = inputTable.getRow(key);
			}
			else
			{
				if (inputTable.getMatchingRows(getMergeKeyColumn().getName(), key).isEmpty())
					continue;
	
				sourceRow = inputTable.getMatchingRows(getMergeKeyColumn().getName(), key).iterator().next();
			}

			if (sourceRow == null)
				continue;

			for (CyColumn col : sourceColumns) {
				
				if (col == getMergeKeyColumn())
					continue;
				

				String targetColName = source2targetColumnMap.get(col.getName());
				
				if (targetColName == null)
					continue;  // skip this column
				
				if (col.getType() == List.class)
					targetRow.set(targetColName, sourceRow.getList(col.getName(), col.getListElementType()));
				else
					targetRow.set(targetColName, sourceRow.get(col.getName(), col.getType()));

			}
		}

	}

	private void copyColumns(CyTable inputTable, List<CyColumn> sourceColumns,CyTable targetTable, boolean addVirtual) {

		for (CyColumn col : sourceColumns) {
			
			if (col == getMergeKeyColumn())
				continue;
			
			// This is a bad idea!  It prevents users from updating data in existing
			// columns, which is a common case
			// String targetColName = getUniqueColumnName(targetTable, col.getName());
			String targetColName = col.getName();
			
			if ( !isMergeColumnKeyColumn())
				addVirtual = false;
			
			if (targetTable.getColumn(targetColName) == null) {
				if(!addVirtual)
				{
					if (col.getType() == List.class)
						targetTable.createListColumn(targetColName, col.getListElementType(), col.isImmutable());
					else
						targetTable.createColumn(targetColName, col.getType(), col.isImmutable(), col.getDefaultValue());
				}
				else
				{
					targetTable.addVirtualColumn(targetColName, col.getName(), inputTable, getJoinTargetColumn(targetTable).getName(), false);
				}
			} else {
				CyColumn targetCol = targetTable.getColumn(targetColName);
				if ((targetCol.getType() != col.getType()) ||
				    (col.getType() == List.class && (targetCol.getListElementType() != col.getListElementType()))) {
					logger.error("Column '"+targetColName+"' has a different type in the target table -- skipping column");
					continue;
				}
			}

			source2targetColumnMap.put(col.getName(), targetColName);
		}
	}

	public boolean checkKeys() {

		Class<?> joinTargetColumnType = String.class;
		if(whereMergeTable.getSelectedValue().matches(NETWORK_COLLECTION))
		{
			joinTargetColumnType = getJoinTargetColumn(
					getTable(name2RootMap.get(targetNetworkCollection.getSelectedValue()), getDataTypeOptions(),
							CyNetwork.DEFAULT_ATTRS)).getType();
		}
		if(whereMergeTable.getSelectedValue().matches(UNASSIGNED_TABLE))
		{
			if(!unassignedTable.getSelectedValue().equals(NO_TABLES))
				joinTargetColumnType = getJoinTargetColumn((CyTable)unassignedTable.getSelectedValue()).getType();
		}
			
		if (getMergeKeyColumn().getType() != joinTargetColumnType)
			return false;
			

		return true;
	}
	
	private ListMultipleSelection<String> getColumns(CyTable selectedTable) {
		String tempName;
		List<String> colNames = new ArrayList<String>();
		for (CyColumn col : selectedTable.getColumns())
		{
			tempName = col.getName();
			if(!tempName.matches(CyRootNetwork.SHARED_NAME) && !tempName.matches(CyRootNetwork.NAME) 
					&& !tempName.matches(CyRootNetwork.SUID)  && !tempName.matches(CyRootNetwork.SELECTED))
				colNames.add(tempName);
		}

		ListMultipleSelection<String> columns = new ListMultipleSelection<String>(colNames);
		
		return columns;
	}
	
	private ListSingleSelection<String> getColumnsWithNames(CyTable selectedTable) {
		String tempName;
		
		List<String> colNames = new ArrayList<String>();
		for (CyColumn col : selectedTable.getColumns())
		{
			tempName = col.getName();
			if( !tempName.matches(CyRootNetwork.SUID) && !tempName.matches(CyRootNetwork.SELECTED))
				colNames.add(tempName);
		}

		ListSingleSelection<String> columns = new ListSingleSelection<String>(colNames);
		if( selectedTable.getColumn(CyRootNetwork.NAME) != null)
			columns.setSelectedValue(CyRootNetwork.NAME);
		else if(!selectedTable.getPrimaryKey().getName().matches(CyRootNetwork.SUID))
			columns.setSelectedValue(selectedTable.getPrimaryKey().getName());
		
		return columns;
	}
	
	private boolean isNewColumnVirtual ()
	{
		return (mergeType.getSelectedValue() == LINK_COLUMNS);
	}
	
	private boolean isMergeColumnKeyColumn ()
	{
		return sourceTable.getSelectedValue().getPrimaryKey() == getMergeKeyColumn();
	}
	
	private CyColumn getMergeKeyColumn ()
	{
		return sourceTable.getSelectedValue().getColumn(sourceMergeKey.getSelectedValue());
	}
	
	private List<Object> getPublicGlobalTables()
	{
		List<Object> listTables = new ArrayList<Object>();
		
		for ( CyTable tempTable : tableMgr.getGlobalTables()) 
		{
			if(tempTable.isPublic())
			{
				listTables.add(tempTable);
			}
		}
		
		return listTables;
	}
	
	private boolean isTableGlobal(CyTable table)
	{
		
		for ( CyTable tempTable : tableMgr.getGlobalTables()) 
		{
			if(tempTable.equals(table))
			{
				return true;
			}
		}
		
		return false;
	}

	private TableType getDataTypeOptions()
	{
		if(whereMergeTable.getSelectedValue().matches(NETWORK_COLLECTION))
			return dataTypeTargetForNetworkCollection.getSelectedValue();
		else
			return dataTypeTargetForNetworkList.getSelectedValue();
		
	}
	@Override
	public ValidationState getValidationState(Appendable errMsg) {
		if ( !isMergeColumnKeyColumn() && isNewColumnVirtual()) {
			try{
				mergeType.setSelectedValue(COPY_COLUMNS);
				errMsg.append("Source Key column needs to be the key column of source table to apply a soft merge.\n");
			} catch (IOException e) {
				e.printStackTrace();
				return ValidationState.INVALID;
			}
			return ValidationState.INVALID;
		}
		
		
		return OK;
	}
}
