package org.cytoscape.filter.internal.composite;

import java.awt.dnd.DropTarget;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

import javax.swing.BorderFactory;
import javax.swing.ComboBoxModel;
import javax.swing.GroupLayout;
import javax.swing.GroupLayout.Alignment;
import javax.swing.GroupLayout.Group;
import javax.swing.GroupLayout.ParallelGroup;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.border.Border;

import org.cytoscape.filter.internal.view.DragHandler;
import org.cytoscape.filter.internal.view.DynamicComboBoxModel;
import org.cytoscape.filter.internal.view.FilterPanel;
import org.cytoscape.filter.internal.view.FilterPanelController;
import org.cytoscape.filter.internal.view.IconManager;
import org.cytoscape.filter.internal.view.TransformerElementViewModel;
import org.cytoscape.filter.internal.view.ViewUtil;
import org.cytoscape.filter.model.CompositeFilter;
import org.cytoscape.filter.model.CompositeFilter.Type;
import org.cytoscape.filter.model.Filter;
import org.cytoscape.model.CyIdentifiable;
import org.cytoscape.model.CyNetwork;

@SuppressWarnings("serial")
public class CompositeFilterPanel extends JPanel {
	
	private static final Border NO_BORDER = BorderFactory.createEmptyBorder();

	private Map<Filter<CyNetwork, CyIdentifiable>, TransformerElementViewModel<FilterPanel>> viewModels;
	private GroupLayout layout;
	private int depth;
	private JComboBox combiningMethodComboBox;
	private FilterPanelController filterPanelController;
	private final JButton addButton;
	private FilterPanel parent;
	private CompositeFilter<CyNetwork, CyIdentifiable> model;
	private final IconManager iconManager;
	private JComponent separator; 
	
	public CompositeFilterPanel(FilterPanel parent, FilterPanelController filterPanelController, 
			final CompositeFilter<CyNetwork, CyIdentifiable> model, int depth, IconManager iconManager) {
		this(parent, filterPanelController, new Controller(), model, depth, iconManager);
	}
	
	public CompositeFilterPanel(FilterPanel parent, FilterPanelController filterPanelController, 
			final Controller controller, final CompositeFilter<CyNetwork, CyIdentifiable> model, int depth,
			IconManager iconManager) {
		this.parent = parent;
		this.filterPanelController = filterPanelController;
		this.depth = depth;
		this.model = model;
		this.iconManager = iconManager;
		
		separator = new CompositeSeparator();
		new DropTarget(separator, new DragHandler<FilterPanel>(separator, filterPanelController, parent, null));

		ViewUtil.configureFilterView(this);
		
		viewModels = new WeakHashMap<Filter<CyNetwork,CyIdentifiable>, TransformerElementViewModel<FilterPanel>>();
		layout = new GroupLayout(this);
		setLayout(layout);
		updateBorder();
		
		combiningMethodComboBox = new JComboBox(controller.getCombiningMethodComboBoxModel());
		combiningMethodComboBox.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				controller.handleCombiningMethodSelected(combiningMethodComboBox, model);
			}
		});
		
		addButton = createAddConditionButton();
		
		for (int i = 0; i < model.getLength(); i++) {
			Filter<CyNetwork, CyIdentifiable> filter = model.get(i);
			JComponent component = filterPanelController.createView(parent, filter, depth + 1);
			TransformerElementViewModel<FilterPanel> viewModel = new TransformerElementViewModel<FilterPanel>(component, filterPanelController, parent, iconManager);
			viewModels.put(filter, viewModel);
		}
	}
	
	private void updateBorder() {
		if (depth > 0) {
			setBorder(ViewUtil.COMPOSITE_PANEL_BORDER);
		} else {
			setBorder(NO_BORDER);
		}
	}

	public void updateLayout() {
		removeAll();

		final ParallelGroup checkBoxGroup = layout.createParallelGroup(Alignment.LEADING);
		final ParallelGroup viewGroup = layout.createParallelGroup(Alignment.LEADING);
		
		final Group columns = layout.createParallelGroup(Alignment.LEADING, true);
		final Group rows = layout.createSequentialGroup();
		
		if (depth > 0 || model.getLength() > 1) {
			columns.addComponent(combiningMethodComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
			rows.addComponent(combiningMethodComboBox, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE);
		}
		
		columns.addGroup(layout.createSequentialGroup()
				.addGap(4)
				.addGroup(checkBoxGroup)
				.addGap(4)
				.addGroup(viewGroup));
		
		int separatorHeight = 5;
		viewGroup.addComponent(separator);
		rows.addComponent(separator, separatorHeight, separatorHeight, separatorHeight);
		
		for (int i = 0; i < model.getLength(); i++) {
			final TransformerElementViewModel<FilterPanel> viewModel = getViewModel(model.get(i));
			if (viewModel.view instanceof CompositeFilterPanel) {
				CompositeFilterPanel panel = (CompositeFilterPanel) viewModel.view;
				panel.updateLayout();
			}

			checkBoxGroup.addComponent(viewModel.handle, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.DEFAULT_SIZE);
			viewGroup.addComponent(viewModel.view, 0, GroupLayout.PREFERRED_SIZE, Short.MAX_VALUE)
					 .addComponent(viewModel.separator);
			
			rows.addGroup(layout.createParallelGroup(Alignment.LEADING)
								.addGroup(layout.createSequentialGroup()
										.addGap(ViewUtil.INTERNAL_VERTICAL_PADDING)
										.addComponent(viewModel.handle, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE, GroupLayout.PREFERRED_SIZE))
								.addComponent(viewModel.view, GroupLayout.PREFERRED_SIZE, GroupLayout.DEFAULT_SIZE, GroupLayout.PREFERRED_SIZE));
			rows.addComponent(viewModel.separator, separatorHeight, separatorHeight, separatorHeight);
		}
		
		columns.addComponent(addButton);
		rows.addGap(ViewUtil.INTERNAL_VERTICAL_PADDING).addComponent(addButton);
		
		layout.setHorizontalGroup(columns);
		layout.setVerticalGroup(rows);
	}
	
	JButton createAddConditionButton() {
		final JButton button = new JButton(IconManager.ICON_PLUS);
		button.setFont(iconManager.getIconFont(12.0f));
		button.setToolTipText("Add new condition...");
		button.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent event) {
				JPopupMenu menu = filterPanelController.createAddConditionMenu(CompositeFilterPanel.this, parent);
				menu.show(button, 0, button.getHeight());
			}
		});
		return button;
	}

	public void addViewModel(int index, Filter<CyNetwork, CyIdentifiable> filter, TransformerElementViewModel<FilterPanel> viewModel) {
		model.insert(index, filter);
		viewModels.put(filter, viewModel);
	}


	public void addViewModel(Filter<CyNetwork, CyIdentifiable> filter, TransformerElementViewModel<FilterPanel> viewModel) {
		model.append(filter);
		viewModels.put(filter, viewModel);
	}

	public void addFilter(Filter<CyNetwork, CyIdentifiable> filter) {
		JComponent component = filterPanelController.createView(parent, filter, depth + 1);
		final TransformerElementViewModel<FilterPanel> viewModel = new TransformerElementViewModel<FilterPanel>(component, filterPanelController, parent, iconManager);
		addViewModel(filter, viewModel);
	}

	public TransformerElementViewModel<FilterPanel> getViewModel(Filter<CyNetwork, CyIdentifiable> filter) {
		return viewModels.get(filter);
	}

	public CompositeFilter<CyNetwork, CyIdentifiable> getModel() {
		return model;
	}
	
	public int getDepth() {
		return depth;
	}
	
	public void setDepth(int depth) {
		this.depth = depth;
		updateBorder();
		for (TransformerElementViewModel<FilterPanel> viewModel : viewModels.values()) {
			if (viewModel.view instanceof CompositeFilterPanel) {
				CompositeFilterPanel panel = (CompositeFilterPanel) viewModel.view;
				panel.setDepth(depth + 1);
			}
		}
	}

	public void removeFilter(int index, boolean unregister) {
		Filter<CyNetwork, CyIdentifiable> filter = model.remove(index);
		TransformerElementViewModel<FilterPanel> model = viewModels.remove(filter);
		if (model == null || model.view == null) {
			return;
		}
		
		if (!unregister) {
			return;
		}
		
		filterPanelController.unregisterView(model.view);
		if (model.view instanceof CompositeFilterPanel) {
			((CompositeFilterPanel) model.view).removeAllFilters();
		}
	}

	void removeAllFilters() {
		while (model.getLength() > 0) {
			removeFilter(0, true);
		}
	}
	
	public Collection<TransformerElementViewModel<FilterPanel>> getViewModels() {
		return viewModels.values();
	}
	
	public static class Controller implements CompositeFilterController {
		private static List<CombiningMethodElement> combiningMethods = createCombiningMethods();

		private static List<CombiningMethodElement> createCombiningMethods() {
			ArrayList<CombiningMethodElement> methods = new ArrayList<CombiningMethodElement>();
			methods.add(new CombiningMethodElement("Match all (AND)", CompositeFilter.Type.ALL));
			methods.add(new CombiningMethodElement("Match any (OR)", CompositeFilter.Type.ANY));
			return methods;
		}

		public void handleCombiningMethodSelected(JComboBox comboBox, CompositeFilter<CyNetwork, CyIdentifiable> model) {
			CombiningMethodElement selected = (CombiningMethodElement) comboBox.getSelectedItem();
			model.setType(selected.combiningMethod);
		}

		public ComboBoxModel getCombiningMethodComboBoxModel() {
			return new DynamicComboBoxModel<CombiningMethodElement>(combiningMethods);
		}
	}
	
	public JComponent getSeparator() {
		return separator;
	}
	
	static class CombiningMethodElement {
		public final String name;
		public final CompositeFilter.Type combiningMethod;
		
		public CombiningMethodElement(String name, Type combiningMethod) {
			this.name = name;
			this.combiningMethod = combiningMethod;
		}
		
		@Override
		public String toString() {
			return name;
		}
	}
}