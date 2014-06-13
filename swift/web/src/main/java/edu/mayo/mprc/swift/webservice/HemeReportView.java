package edu.mayo.mprc.swift.webservice;

import com.google.common.primitives.Ints;
import edu.mayo.mprc.heme.HemeReport;
import edu.mayo.mprc.heme.HemeReportEntry;
import edu.mayo.mprc.heme.ProteinId;
import org.springframework.web.servlet.View;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * @author Roman Zenka
 */
public final class HemeReportView implements View {
	@Override
	public String getContentType() {
		return "text/html";
	}

	@Override
	public void render(final Map<String, ?> model, final HttpServletRequest request, final HttpServletResponse response) throws Exception {
		final SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd");
		final HemeReport report = (HemeReport) model.get("report");
		final ServletOutputStream outputStream = response.getOutputStream();
		outputStream.print("<html><head><title>");
		outputStream.print("Report for patient " + report.getName() + " from " + format.format(report.getDate()));
		outputStream.print("</title>");
		outputStream.print("<meta name=\"viewport\" content=\"width=device-width, initial-scale=1.0\">\n" +
				"<link href=\"/common/bootstrap/css/bootstrap.min.css\" rel=\"stylesheet\" media=\"screen\">");
		outputStream.print("</head><body><div class=\"container\">");
		outputStream.print("<h1>" + report.getName() + "</h1>");
		outputStream.print("<h2>" + format.format(report.getDate()) + "</h2>");
		outputStream.print("Listing proteins with mass delta of " + report.getMass() + " specified with tolerance of " + report.getMassTolerance());
		outputStream.print("<hr/>");

		outputStream.print("<h3>Matching proteins</h3>");
		outputStream.print("<p>These protein groups contain at least one protein that matches the requested intact mass within given tolerance.</p>");
		outputStream.print("<p>If there are multiple proteins listed in a cell, it is because we could not distinguish between them based on the peptide evidence itself.</p>");
		reportEntries(outputStream, report.getWithinRange(), report);

		outputStream.print("<h3>Related proteins, not matching</h3>");
		outputStream.print("<p>These are proteins of interest whose mass delta did not match the query.</p>");
		reportEntries(outputStream, report.getHaveMassDelta(), report);

		outputStream.print("<h3>Contaminants</h3>");
		outputStream.print("<p>These proteins are unrelated or not targetted, but were detected in the sample.</p>");
		reportEntries(outputStream, report.getAllOthers(), report);

		outputStream.print("</div></body></html>");
	}

	private void reportEntries(final ServletOutputStream outputStream, final List<HemeReportEntry> list, final HemeReport report) throws IOException {
		Collections.sort(list, new HemeReportEntryComparator());
		outputStream.print("<table class=\"table table-condensed\"><tr><th>Spectrum count</th><th>Match</th><th>Protein accession</th><th>Protein description</th><th>Mass &Delta;</th></tr>");
		for (final HemeReportEntry entry : list) {
			final int ids = entry.getProteinIds().size();
			boolean first = true;
			Collections.sort(entry.getProteinIds(), new ProteinIdComparator());
			for (final ProteinId id : entry.getProteinIds()) {
				outputStream.print("<tr>");
				if (first) {
					outputStream.print("<td rowspan=\"" + ids + "\">" + entry.getTotalSpectra() + "</td>");
					first = false;
				}
				if (report.isMatch(id)) {
					outputStream.print("<td>&#10004;</td>");
				} else {
					outputStream.print("<td>&nbsp;</td>");
				}
				outputStream.print("<td>" + id.getAccNum() + "</td>");
				outputStream.print("<td>" + clearDeltaMass(id.getDescription()) + "</td>");
				outputStream.print("<td>" + (id.getMassDelta() == null ? "" : id.getMassDelta()) + "</td>");
				outputStream.print("</tr>");
			}
		}
		outputStream.print("</table>");
	}

	private String clearDeltaMass(final String description) {
		return description.replaceAll(",?\\s*#DeltaMass:[^#]*#", "");
	}

	private static class HemeReportEntryComparator implements Comparator<HemeReportEntry> {
		@Override
		public int compare(final HemeReportEntry o1, final HemeReportEntry o2) {
			return Ints.compare(o2.getTotalSpectra(), o1.getTotalSpectra());
		}
	}

	private static class ProteinIdComparator implements Comparator<ProteinId> {
		@Override
		public int compare(final ProteinId o1, final ProteinId o2) {
			return o1.getAccNum().compareTo(o2.getAccNum());
		}
	}
}
