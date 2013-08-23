package edu.mayo.mprc.utilities;

import edu.mayo.mprc.MprcException;
import org.ccil.cowan.tagsoup.Parser;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.Text;
import org.xml.sax.InputSource;

import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMResult;
import javax.xml.transform.sax.SAXSource;
import java.io.StringReader;
import java.util.Stack;

/**
 * Given a URL, parse the html at the URL for a FORM, and construct a
 */
public final class FormScraper {
	private FormScraper() {
	}

	/**
	 * Given an HTML Document as a string, remove any tags from it; text within those tags is preserved.
	 */
	public static String stripHtmlTags(final String s) {
		try {
			final StringBuilder sb = new StringBuilder();

			Node node = parseHTMLintoDOM(new InputSource(new StringReader(s)), null);
			final Stack<Node> stack = new Stack<Node>();
			stack.push(null);
			final char[] chars = new char[1];
			boolean visitChildren = true;
			while (node != null) {
				if (node instanceof Text) {
					final String txt = ((Text) node).getData();
					if (txt.isEmpty()) {
						continue;
					}
					if (sb.length() > 0) {
						sb.getChars(sb.length() - 1, sb.length(), chars, 0);
						if (!Character.isWhitespace(chars[0]) && !Character.isWhitespace(txt.charAt(0))) {
							sb.append(" ");
						}
					}
					sb.append(txt);
				}

				if (visitChildren && node.getFirstChild() != null) {
					stack.push(node);
					node = node.getFirstChild();
				} else if (node.getNextSibling() != null) {
					visitChildren = true;
					node = node.getNextSibling();
				} else {
					visitChildren = false;
					node = stack.pop();
				}

			}
			return sb.toString();
		} catch (Exception e) {
			throw new MprcException("Can't parse HTML from string", e);
		}
	}

	/**
	 * Parse the given HTML InputSource into a DOM Document, optionally specifying a systemId.
	 */
	private static Document parseHTMLintoDOM(final InputSource insource, final String systemIdForErrorMessages) {
		try {
			final Parser tagsoup = new Parser();

			if (systemIdForErrorMessages != null) {
				insource.setSystemId(systemIdForErrorMessages);
			}
			final Transformer xformer = TransformerFactory.newInstance().newTransformer();
			final DOMResult result = new DOMResult();
			xformer.transform(new SAXSource(tagsoup, insource), result);
			return (Document) result.getNode();
		} catch (Exception t) {
			throw new MprcException("Can't parse HTML from " + systemIdForErrorMessages, t);
		}
	}
}
