package edu.mayo.mprc.daemon.worker;

/**
 * Marker interface for workers that do not want to utilize automatic logging facilities.
 * <p/>
 * HACK: This is kind of a hack for caches that should not log themselves - should only provide log from the
 * worker whose execution they cache. A proper solution would log everything but allow the cache task
 * to have sub-tasks.
 */
public interface NoLoggingWorker extends Worker {
}
