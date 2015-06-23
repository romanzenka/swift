package edu.mayo.mprc.swift.webservice;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import javax.activation.MimetypesFileTypeMap;

/**
 * @author Roman Zenka
 */

@Configuration
public class SwiftFileTypeMap extends MimetypesFileTypeMap {
	public SwiftFileTypeMap() {
		addMimeTypes("image/png png PNG");
		addMimeTypes("image/jpeg jpg jpeg JPG JPEG");
		addMimeTypes("application/vnd.ms-excel xls");
		addMimeTypes("text/html htm html");
		addMimeTypes("text/css css");
	}

	@Bean(name = "defaultTypeMap")
	public SwiftFileTypeMap getDefaultTypeMap() {
		return new SwiftFileTypeMap();
	}
}
