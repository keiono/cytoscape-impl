package org.cytoscape.view.vizmap.internal.mappings;

import org.cytoscape.model.CyTable;
import org.cytoscape.view.model.VisualProperty;
import org.cytoscape.view.vizmap.VisualMappingFunction;
import org.cytoscape.view.vizmap.VisualMappingFunctionFactory;
import org.cytoscape.view.vizmap.mappings.ContinuousMapping;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ContinuousMappingFactory implements VisualMappingFunctionFactory {
	
	private static final Logger logger = LoggerFactory.getLogger(ContinuousMappingFactory.class);

	@Override
	public <K, V> VisualMappingFunction<K, V> createVisualMappingFunction(final String attributeName, 
			Class<K> attrValueType, final CyTable table, VisualProperty<V> vp) {
		
		logger.debug("Trying to create Continuous mapping.  Data Type is " + attrValueType);
		
		// Validate attribute type: Continuous Mapping is compatible with Numbers only.
		if(Number.class.isAssignableFrom(attrValueType) == false)
			throw new IllegalArgumentException("ContinuousMapping can be used for numerical attributes only.");
		
		return new ContinuousMappingImpl<K, V>(attributeName, attrValueType, table, vp);
	}
	
	@Override public String toString() {
		return ContinuousMapping.CONTINUOUS;
	}

	@Override
	public Class<?> getMappingFunctionType() {
		return ContinuousMapping.class;
	}

}
