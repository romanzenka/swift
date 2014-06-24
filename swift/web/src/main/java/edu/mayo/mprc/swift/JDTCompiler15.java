package edu.mayo.mprc.swift;

import org.apache.tools.ant.taskdefs.Javac;
import org.eclipse.jdt.core.JDTCompilerAdapter;

/**
 * This is to tweak GWT to support JSP properly.
 *
 * @author Roman Zenka
 */
public class JDTCompiler15 extends JDTCompilerAdapter {
	@Override
	public void setJavac(Javac attributes) {
		if (attributes.getTarget() == null) {
			attributes.setTarget("1.6");
		}
		if (attributes.getSource() == null) {
			attributes.setSource("1.6");
		}
		super.setJavac(attributes);
	}
}
