package edu.mayo.mprc.swift.webservice;

import edu.mayo.mprc.heme.HemeReport;
import edu.mayo.mprc.heme.HemeReportEntry;
import org.springframework.web.servlet.View;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

/**
 * @author Roman Zenka
 */
public final class HemeReportView implements View {
	@Override
	public String getContentType() {
		return "text/tab-separated-values";
	}

	@Override
	public void render(Map<String, ?> model, HttpServletRequest request, HttpServletResponse response) throws Exception {
		HemeReport report = (HemeReport) model.get("report");
		ServletOutputStream outputStream = response.getOutputStream();
		outputStream.print("Accession Number\tDescription\tTotal spectra\tMass Delta\n");
		for (HemeReportEntry entry : report.getEntries()) {
			outputStream.print(entry.getProteinAccNum());
			outputStream.print('\t');
			outputStream.print(entry.getProteinDescription());
			outputStream.print('\t');
			outputStream.print(entry.getTotalSpectra());
			outputStream.print('\t');
			outputStream.print(entry.getMassDelta());
			outputStream.print('\n');
		}
	}
}
