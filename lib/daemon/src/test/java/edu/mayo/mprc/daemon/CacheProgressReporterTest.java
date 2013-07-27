package edu.mayo.mprc.daemon;

import edu.mayo.mprc.utilities.progress.ProgressInfo;
import edu.mayo.mprc.utilities.progress.ProgressReporter;
import org.mockito.InOrder;
import org.testng.annotations.Test;

import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;

public final class CacheProgressReporterTest {

	@Test
	public static void shouldDistributeCalls() {
		// Make sure the calls are passed to a reporter that is already established
		final ProgressReporter reporter1 = mock(ProgressReporter.class);
		final ProgressInfo progressInfo = mock(ProgressInfo.class);

		final CacheProgressReporter reporter = new CacheProgressReporter();

		reporter.addProgressReporter(reporter1);

		reporter.reportStart("localhost");
		reporter.reportProgress(progressInfo);
		reporter.reportSuccess();

		final InOrder order1 = inOrder(reporter1);
		order1.verify(reporter1).reportStart("localhost");
		order1.verify(reporter1).reportProgress(progressInfo);
		order1.verify(reporter1).reportSuccess();

		// Make sure the calls are also passed to a reporter that comes along later
		final ProgressReporter reporter2 = mock(ProgressReporter.class);
		reporter.addProgressReporter(reporter2);

		final InOrder order2 = inOrder(reporter2);
		order2.verify(reporter2).reportStart("localhost");
		order2.verify(reporter2).reportProgress(progressInfo);
		order2.verify(reporter2).reportSuccess();
	}
}
