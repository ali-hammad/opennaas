package org.opennaas.extensions.sampleresource.model;

import java.util.ArrayList;
import java.util.List;

import org.opennaas.core.resources.IModel;

/**
 * 
 * @author Elisabeth Rigol
 * 
 */
public class SampleModel implements IModel {

	/**
	 * 
	 */
	private static final long	serialVersionUID	= -4002472167559948067L;

	@Override
	public List<String> getChildren() {
		return new ArrayList<String>();
	}

}
