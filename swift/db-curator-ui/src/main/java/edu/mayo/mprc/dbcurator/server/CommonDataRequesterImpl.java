package edu.mayo.mprc.dbcurator.server;

import edu.mayo.mprc.MprcException;
import edu.mayo.mprc.common.client.GWTServiceException;
import edu.mayo.mprc.common.server.SpringGwtServlet;
import edu.mayo.mprc.dbcurator.client.steppanels.CommonDataRequester;
import edu.mayo.mprc.dbcurator.client.steppanels.CurationStub;
import edu.mayo.mprc.dbcurator.client.steppanels.HeaderTransformStub;
import org.apache.log4j.Logger;

import javax.servlet.http.HttpSession;
import java.util.List;
import java.util.Map;

/**
 * @author Eric Winter
 */
public final class CommonDataRequesterImpl extends SpringGwtServlet implements CommonDataRequester {
	private static final long serialVersionUID = 20071220L;

	private static final Logger LOGGER = Logger.getLogger(CommonDataRequesterImpl.class);

	private transient CommonDataRequesterLogic logic;

	public CommonDataRequesterImpl() {
	}

	@Override
	public List<HeaderTransformStub> getHeaderTransformers() {
		return intializeLogic().getHeaderTransformers();
	}

	@Override
	public Map<String, String> getFTPDataSources() {
		return intializeLogic().getFTPDataSources();
	}

	@Override
	public Boolean isShortnameUnique(final String toCheck) {
		return intializeLogic().isShortnameUnique(toCheck);
	}

	/**
	 * Takes a CurationStub that we want to have updated with status from the server and performs that update returning
	 * the updated curation.  It is up to the client to use the returned stub and swap the old stub with the new stub
	 * and then perform any updating that may be required.
	 *
	 * @param toUpdate the stub that you want to have updated
	 * @return the updated stub
	 */
	@Override
	public CurationStub performUpdate(final CurationStub toUpdate) {
		return intializeLogic().performUpdate(toUpdate);
	}

	@Override
	public CurationStub lookForCuration() {
		return intializeLogic().lookForCuration();
	}

	@Override
	public CurationStub getCurationByID(final Integer id) {
		return intializeLogic().getCurationByID(id);
	}


	@Override
	public CurationStub copyCurationStub(final CurationStub toCopy) {
		return intializeLogic().copyCurationStub(toCopy);
	}

	/**
	 * This can be redefined if we want to cleanly separate from the gwt servlet machinery.
	 *
	 * @return {@link HttpSession} to to store session-related parameters
	 */
	protected HttpSession getHttpSession() {
		return getThreadLocalRequest().getSession();
	}

	/**
	 * Runs a curation on the server.  This will execute a curation.  If you want to get the status of a curation call getStatus()
	 *
	 * @param toRun the curation you want to have run
	 */
	@Override
	public CurationStub runCuration(final CurationStub toRun) throws GWTServiceException {
		try {
			return intializeLogic().runCuration(toRun);
		} catch (Exception e) {
			throw new GWTServiceException(MprcException.getDetailedMessage(e), "");
		}
	}

	@Override
	public String testPattern(final String pattern) {
		return intializeLogic().testPattern(pattern);
	}

	@Override
	public String[] getLines(final String sharedPath, final int startLineInclusive, final int numberOfLines, final String pattern) throws GWTServiceException {
		return intializeLogic().getLines(sharedPath, startLineInclusive, numberOfLines, pattern);
	}

	@Override
	public synchronized String[] getResults() throws GWTServiceException {
		return intializeLogic().getResults();
	}

	@Override
	public void setCancelMessage(final boolean cancelMessage) throws GWTServiceException {
		intializeLogic().setCancelMessage(cancelMessage);
	}

	public CommonDataRequesterLogic intializeLogic() {
		initializeFromRequest();
		return getLogic();
	}

	public CommonDataRequesterLogic getLogic() {
		return logic;
	}

	public void setLogic(final CommonDataRequesterLogic logic) {
		this.logic = logic;
	}

	private void initializeFromRequest() {
		logic.setAttributeStore(new AttributeStore() {
			@Override
			public Object getAttribute(final String name) {
				return getHttpSession().getAttribute(name);
			}

			@Override
			public void setAttribute(final String name, final Object value) {
				getHttpSession().setAttribute(name, value);
			}
		});
	}
}