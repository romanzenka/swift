package edu.mayo.mprc.swift.ui.client.widgets;

import com.google.gwt.user.client.DOM;
import com.google.gwt.user.client.Element;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.WindowCloseListener;
import com.google.gwt.user.client.ui.ComplexPanel;
import com.google.gwt.user.client.ui.Widget;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

/**
 * A GWT Panel that can clone existing chunks of DOM and decorate them with widgets.
 * First finds an existing chunk of DOM by id and pulls it out of the document.
 * Then, as {@link #insertClone(String) requested}, clones that DOM chunk,
 * appends it as a child of it's parent, and then allows widgets to be
 * {@link #append(String, String, Widget)}  appended} to or {@link #replace(String, String, Widget)}  replaced} as (sub) elements
 * of it.
 * <p/>
 * This acts a lot like RootPanel, in that it is a root for GWT widgets and it cleans itself up on window close.
 * <p/>
 * For example, if GWT host document has:
 * <p/>
 * <PRE>
 * &lt;table&gt;
 * &lt;tr id="domChunkId1"&gt;&lt;td id="subElement1"&gt;&lt;/td&gt;&lt;td id="subElement2"&gt;&lt;bold&gt;junk&lt;/bold&gt;&lt;/td&gt;&lt;/tr&gt;
 * &lt;/table&gt;
 * </PRE>
 * <p/>
 * <p/>
 * <p/>
 * the code:
 * <p/>
 * <PRE>
 * ExistingDOMPanel pan = new ExistingDOMPanel("domChunkId1");
 * pan.append("clone1", "subElement1", new HTML("<bold>Bob</bold>");
 * pan.replace("clone2", "subElement2", new PushButton("Blammo!"));
 * </PRE>
 * <p/>
 * will result in :
 * <p/>
 * <PRE>
 * &lt;table&gt;
 * &lt;tr id="domChunkId1-clone1"&gt;&lt;td id="subElement1-clone1"&gt;&lt;bold&gt;Bob&lt;/bold&gt;&lt;/td&gt;&lt;td id="subElement2-clone1"&gt;&lt;bold&gt;junk&lt;/bold&gt;&lt;/td&gt;&lt;/tr&gt;
 * &lt;tr id="domChunkId1-clone2"&gt;&lt;td id="subElement1-clone2"&gt;&lt;/td&gt;&lt;td id="subElement2-clone2"&gt;GWT PushButton(Blammo!)&lt;/td&gt;&lt;/tr&gt;
 * &lt;/table&gt;
 * </PRE>
 */
public final class ExistingDOMPanel extends ComplexPanel {
	private static Map<String, ExistingDOMPanel> panels = new HashMap<String, ExistingDOMPanel>();
	private Map<String, Element> clones = new HashMap<String, Element>();

	private Element root;  // the element to be clone.
	private Element parent; // the element to append to.
	private Element sibling; // where to insert.

	/**
	 * Find a chunk of DOM with id domChunkId, and removes it from
	 * the document in preparation for cloning it.
	 *
	 * @param domChunkId Id on an existing DOM chunk
	 */
	public ExistingDOMPanel(final String domChunkId) {
		super();
		if (panels.containsKey(domChunkId)) {
			throw new ExistingDOMPanelException("Already created ExistingDOMPanel from " + domChunkId
					+ "; use get() instead.");
		}
		root = DOM.getElementById(domChunkId);
		if (root == null) {
			throw new ExistingDOMPanelException("Can't find " + domChunkId);
		}
		parent = DOM.getParent(root);
		if (parent == null) {
			throw new ExistingDOMPanelException("Can't find parent of " + domChunkId);
		}
		sibling = DOM.getNextSibling(root);
		DOM.removeChild(parent, root);

		if (panels.isEmpty()) {
			hookWindowClosing();
		}
		panels.put(domChunkId, this);
		onAttach();
	}

	/**
	 * Gets an already created ExistingDOMPanel given it's chunk id.
	 */
	public static ExistingDOMPanel get(final String domChunkId) {
		return panels.get(domChunkId);
	}

	/**
	 * Inserts a new Clone of the DOMChunk captured in this ExistingDOMPanel,
	 * if one doesn't already exist with the given cloneId.
	 *
	 * @param cloneId id which is appended to all existing ids in the DOM chunk.
	 * @return the clone with the given cloneId.
	 */
	public Element insertClone(final String cloneId) {
		return insertClone(cloneId, -1);
	}

	/**
	 * Inserts a new Clone of the DOMChunk captured in this ExistingDOMPanel,
	 * if one doesn't already exist with the given cloneId.
	 *
	 * @param cloneId id which is appended to all existing ids in the DOM chunk.
	 * @param visible 1 - make the element visible, 0 - make the element invisible, -1 do not modify visibility
	 * @return the clone with the given cloneId.
	 */
	private Element insertClone(final String cloneId, final int visible) {
		final Element clone;
		if (clones.containsKey(cloneId)) {
			clone = clones.get(cloneId);
		} else {
			clone = clone(root);
			clones.put(cloneId, clone);
			suffixIds(clone, cloneId);
			if (visible == 0 || visible == 1) {
				DOM.setStyleAttribute(clone, "display", visible == 1 ? "block" : "none");
			}
			DOM.insertBefore(parent, clone, sibling);
		}
		return clone;
	}

	/**
	 * Append a given Widget to one of the clones managed by this ExistingDOMPanel.
	 *
	 * @param cloneId      manipulate (or create) the DOM Chunk clone with the given Id
	 * @param subElementId the (unsuffixed) id of the sub-DOM element to append to.
	 * @param w            the Widget to append.
	 * @param visible      true if the cloned content should be initially visible
	 */
	public Element append(final String cloneId, final String subElementId, final Widget w, final boolean visible) {
		return append(cloneId, subElementId, w, visible ? 1 : 0);
	}

	/**
	 * Append a given Widget to one of the clones managed by this ExistingDOMPanel.
	 *
	 * @param cloneId      manipulate (or create) the DOM Chunk clone with the given Id
	 * @param subElementId the (unsuffixed) id of the sub-DOM element to append to.
	 * @param w            the Widget to append.
	 */
	public Element append(final String cloneId, final String subElementId, final Widget w) {
		return append(cloneId, subElementId, w, -1);
	}

	/**
	 * Append a given Widget to one of the clones managed by this ExistingDOMPanel.
	 *
	 * @param cloneId      manipulate (or create) the DOM Chunk clone with the given Id
	 * @param subElementId the (unsuffixed) id of the sub-DOM element to append to.
	 * @param w            the Widget to append.
	 * @param visible      0 - invisible, 1 - visible, -1 do not modify visibility
	 */
	private Element append(final String cloneId, final String subElementId, final Widget w, final int visible) {
		final Element clone = insertClone(cloneId, visible);
		final String id = subElementId + "-" + cloneId;
		final Element e = findContainedElementById(clone, id);
		if (e == null) {
			throw new ExistingDOMPanelException("Can't find sub element " + id);
		}
		add(w, e);
		return clone;
	}

	/**
	 * Like {@link #append(String, String, Widget)} but replaces any existing, contained DOM.
	 * <p/>
	 * TODO this needs to check if any of the contained DOM is actually a widget, and remove it.
	 */

	public void replace(final String cloneId, final String subElementId, final Widget w) {
		final Element clone = insertClone(cloneId);
		final String id = subElementId + "-" + cloneId;
		final Element e = findContainedElementById(clone, id);
		for (int i = 0; i < DOM.getChildCount(e); ++i) {
			DOM.removeChild(e, DOM.getChild(e, i));
		}
		append(cloneId, subElementId, w);
	}

	private Element findContainedElementById(final Element e, final String id) {
		final String eid = DOM.getElementAttribute(e, "id");
		if (eid != null && eid.equals(id)) {
			return e;
		}
		for (int i = 0; i < DOM.getChildCount(e); ++i) {
			final Element ee = findContainedElementById(DOM.getChild(e, i), id);
			if (ee != null) {
				return ee;
			}
		}
		return null;
	}

	private void suffixIds(final Element e, final String cloneId) {
		String id = DOM.getElementAttribute(e, "id");
		if ((id != null) && (!id.equals(""))) {
			id += "-" + cloneId;
			DOM.setElementAttribute(e, "id", id);
		}
		for (int i = 0; i < DOM.getChildCount(e); ++i) {
			suffixIds(DOM.getChild(e, i), cloneId);
		}
	}

	@Override
	public Element getElement() {
		return parent;
	}

	public Element getElement(final String cloneId, final String subElementId) {
		final Element clone = insertClone(cloneId);
		return findContainedElementById(clone, subElementId + "-" + cloneId);
	}

	private native Element clone(Element e) /*-{
		return e.cloneNode(true);
	}-*/;

	private static void hookWindowClosing() {
		// Catch the window closing event.
		Window.addWindowCloseListener(new WindowCloseListener() {
			@Override
			public void onWindowClosed() {
				// When the window is closing, detach all root panels. This will cause
				// all of their children's event listeners to be unhooked, which will
				// avoid potential memory leaks.
				for (Iterator it = panels.values().iterator(); it.hasNext(); ) {
					final ExistingDOMPanel gwt = (ExistingDOMPanel) it.next();
					if (gwt.isAttached()) {
						gwt.onDetach();
					}
				}
			}

			@Override
			public String onWindowClosing() {
				return null;
			}
		});
	}


	public static class ExistingDOMPanelException extends RuntimeException {
		private static final long serialVersionUID = 20071220L;

		public ExistingDOMPanelException() {
			super();
		}

		public ExistingDOMPanelException(final String s) {
			super(s);
		}

		public ExistingDOMPanelException(final String s, final Throwable throwable) {
			super(s, throwable);
		}

		public ExistingDOMPanelException(final Throwable throwable) {
			super(throwable);
		}
	}

}
