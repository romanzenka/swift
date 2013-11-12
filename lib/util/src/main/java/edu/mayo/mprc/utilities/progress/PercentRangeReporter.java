package edu.mayo.mprc.utilities.progress;

/**
 * Reports percent of a task done within a given range.
 */
public final class PercentRangeReporter implements PercentProgressReporter {
	private final PercentProgressReporter reporter;
	private final float percentFrom;
	private final float percentTo;

	public PercentRangeReporter(final PercentProgressReporter reporter, final float percentFrom, final float percentTo) {
		this.reporter = reporter;
		this.percentFrom = percentFrom;
		this.percentTo = percentTo;
	}

	/**
	 * Get a percent range by splitting the current range into equally sized chunks and returning a chunk of a given numer.
	 *
	 * @param totalChunks How many chunks.
	 * @param chunkNumber Which chunk we want range for.
	 * @return a reporter going over the specified chunk
	 */
	public PercentRangeReporter getSubset(final int totalChunks, final int chunkNumber) {
		final float chunkPercent = (percentTo - percentFrom) / totalChunks;
		return new PercentRangeReporter(reporter, percentFrom + chunkPercent * chunkNumber, percentFrom + chunkPercent * (chunkNumber + 1));
	}

	@Override
	public void reportProgress(final float percent) {
		reporter.reportProgress(percentFrom + (percentTo - percentFrom) * percent);
	}
}
