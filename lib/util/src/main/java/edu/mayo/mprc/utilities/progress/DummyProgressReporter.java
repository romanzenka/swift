package edu.mayo.mprc.utilities.progress;

/**
 * @author Roman Zenka
 */
public final class DummyProgressReporter implements PercentProgressReporter {
	@Override
	public void reportProgress(float percent) {
		// Do nothing
	}
}
