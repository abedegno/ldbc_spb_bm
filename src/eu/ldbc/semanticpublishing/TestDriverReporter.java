package eu.ldbc.semanticpublishing;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import eu.ldbc.semanticpublishing.statistics.Statistics;

/**
 * This class is used to produce a result summary for the benchmark. The thread is scheduled to start at a fixed
 * rate of one second. Results are printed to console and log file.
 */
public class TestDriverReporter extends Thread {
	private final AtomicLong totalQueryExecutions;
	private final AtomicLong totalCompletedQueryMixRuns;
	private final AtomicBoolean benchmarkState;
	private final AtomicBoolean keepAlive;
	private final AtomicBoolean benchmarkResultIsValid;
	private final AtomicBoolean maxUpdateRateReached;
	private final String queryPoolsDefinitions;
	private final double maxUpdateRateThresholdOps;
	private double minUpdateRateThresholdOps;	
	private double updateRateReachTimePercent;
	private boolean verbose;
	private long seconds;
	private long runPeriodSeconds;
	private int minUpdateRatePassesCount;
	private int aggregationAgentsCount;
	private int editorialAgentsCount;
	private int initializedCount;
	
	private final static Logger LOGGER = LoggerFactory.getLogger(TestDriverReporter.class.getName());
	
	public TestDriverReporter(AtomicLong totalQueryExecutions, AtomicLong totalCompletedQueryMixRuns, AtomicBoolean benchmarkState, AtomicBoolean keepAlive, AtomicBoolean benchmarkResultIsValid, double updateQueryRateFirstReachTimePercent, double minUpdateQueriesRateThresholdOps, double maxUpdateRateThresholdOps, AtomicBoolean maxUpdateRateReached, int editorialAgentsCount, int aggregationAgentsCount, long runPeriodSeconds, /*long benchmarkByQueryMixRuns, long benchmarkByQueryRuns, */String queryPoolsDefinitons, boolean verbose) {
		this.totalQueryExecutions = totalQueryExecutions;
		this.totalCompletedQueryMixRuns = totalCompletedQueryMixRuns;
		this.benchmarkState = benchmarkState;
		this.keepAlive = keepAlive;
		this.benchmarkResultIsValid = benchmarkResultIsValid;
		this.updateRateReachTimePercent = updateQueryRateFirstReachTimePercent;
		this.seconds = 0;
		this.runPeriodSeconds = runPeriodSeconds;
		this.verbose = verbose;
		this.aggregationAgentsCount = aggregationAgentsCount;
		this.editorialAgentsCount = editorialAgentsCount;
		this.minUpdateRateThresholdOps = minUpdateQueriesRateThresholdOps;
		this.minUpdateRatePassesCount = 0;
		this.maxUpdateRateThresholdOps = maxUpdateRateThresholdOps;
		this.maxUpdateRateReached = maxUpdateRateReached;
		this.initializedCount = 0;
		this.queryPoolsDefinitions = queryPoolsDefinitons;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Runnable#run()
	 * 
	 * Will be run by a ScheduledThreadPoolExecutor.scheduleAtFixedRate()
	 */
	@Override
	public void run() {
		try {
			long timeCorreciton = 0;
			long startTime = System.currentTimeMillis();
			while (benchmarkState.get() || keepAlive.get()) {
				Thread.sleep(Math.abs(1000 - timeCorreciton));
				seconds = (long) ((System.currentTimeMillis() - startTime) / 1000);
				timeCorreciton = collectAndShowResults(/*(benchmarkByQueryRuns == 0) && (benchmarkByQueryMixRuns == 0)*/);
			}
		} catch (Throwable t) {
			System.out.println("BenchmarkProcessObserver :: encountered a problem : " + t.getMessage());
			t.printStackTrace();
		}
	}
	
	/**
	 * Displays to console and writes to log file a result summary of the benchmark.
	 * Editorial and Aggregation operations per second.
	 */
	private long collectAndShowResults(/*boolean secondsOrExecutions*/) {
		long time = System.currentTimeMillis();		
		StringBuilder sb = new StringBuilder();
		
		long insertOpsCount = Statistics.insertCreativeWorksQueryStatistics.getRunsCount();
		long updateOpsCount = Statistics.updateCreativeWorksQueryStatistics.getRunsCount();
		long deleteOpsCount = Statistics.deleteCreativeWorksQueryStatistics.getRunsCount();
		long totalAggregateOpsCount = Statistics.totalAggregateQueryStatistics.getRunsCount();
		
		long failedInsertOpsCount = Statistics.insertCreativeWorksQueryStatistics.getFailuresCount();
		long failedUpdateOpsCount = Statistics.updateCreativeWorksQueryStatistics.getFailuresCount();
		long failedDeleteOpsCount = Statistics.deleteCreativeWorksQueryStatistics.getFailuresCount();
		long failedTotalAggregateOpsCount = Statistics.totalAggregateQueryStatistics.getFailuresCount();
		
		sb.append("\n");
		
		sb.append("\nSeconds : " + seconds);
		if (!queryPoolsDefinitions.isEmpty()) {
			sb.append(" (completed query mixes : " + totalCompletedQueryMixRuns.get() + ")");
		} else {
			sb.append(" (completed query runs : " + totalQueryExecutions.get() + ")");
		}

		sb.append("\n");
		sb.append("\tEditorial:\n");
		sb.append(String.format("\t\t%s agents\n\n", editorialAgentsCount));
		if (verbose) {
			
			sb.append(String.format("\t\t%-5d inserts (avg : %-7d ms, min : %-7d ms, max : %-7d ms)\n", insertOpsCount ,Statistics.insertCreativeWorksQueryStatistics.getAvgExecutionTimeMs(), Statistics.insertCreativeWorksQueryStatistics.getMinExecutionTimeMs(), Statistics.insertCreativeWorksQueryStatistics.getMaxExecutionTimeMs()));
			sb.append(String.format("\t\t%-5d updates (avg : %-7d ms, min : %-7d ms, max : %-7d ms)\n", updateOpsCount ,Statistics.updateCreativeWorksQueryStatistics.getAvgExecutionTimeMs(), Statistics.updateCreativeWorksQueryStatistics.getMinExecutionTimeMs(), Statistics.updateCreativeWorksQueryStatistics.getMaxExecutionTimeMs()));
			sb.append(String.format("\t\t%-5d deletes (avg : %-7d ms, min : %-7d ms, max : %-7d ms)\n", deleteOpsCount ,Statistics.deleteCreativeWorksQueryStatistics.getAvgExecutionTimeMs(), Statistics.deleteCreativeWorksQueryStatistics.getMinExecutionTimeMs(), Statistics.deleteCreativeWorksQueryStatistics.getMaxExecutionTimeMs()));
			sb.append("\n");
			sb.append(String.format("\t\t%d operations (%d CW Inserts (%d errors), %d CW Updates (%d errors), %d CW Deletions (%d errors))\n", ( insertOpsCount + updateOpsCount + deleteOpsCount ),
																	  																			 insertOpsCount, failedInsertOpsCount,
																	  																			 updateOpsCount, failedUpdateOpsCount,
																	  																			 deleteOpsCount, failedDeleteOpsCount) );
		} else {
			sb.append(String.format("\t\t%d operations (%d CW Inserts, %d CW Updates, %d CW Deletions)\n", ( insertOpsCount + updateOpsCount + deleteOpsCount ),
																											 insertOpsCount, 
																											 updateOpsCount,
																											 deleteOpsCount) );
		}

		//time correction is not needed for update operations, as they are performed by separate agents and are not counting/parsing results
		double averageOperationsPerSecond = (double)(insertOpsCount + updateOpsCount + deleteOpsCount) / (double)seconds;
		
		//keep track of update rate ops
		updateInternalStatus(averageOperationsPerSecond);
		
		sb.append(String.format("\t\t%.4f average operations per second\n", averageOperationsPerSecond));

		sb.append("\n");
		sb.append("\tAggregation:\n");
		sb.append(String.format("\t\t%s agents\n\n", aggregationAgentsCount));
		if (verbose) {
			for (int i = 0; i < Statistics.AGGREGATE_QUERIES_COUNT; i++) {
				sb.append(String.format("\t\t%-5d Q%-2d  queries (avg : %-7d ms, min : %-7d ms, max : %-7d ms, %d errors)\n", Statistics.aggregateQueriesArray[i].getRunsCount(), 
																											   				  (i + 1),
																											   				  Statistics.aggregateQueriesArray[i].getAvgExecutionTimeMs(),
																											   				  Statistics.aggregateQueriesArray[i].getMinExecutionTimeMs(), 
																											   				  Statistics.aggregateQueriesArray[i].getMaxExecutionTimeMs(), 
																											   				  Statistics.aggregateQueriesArray[i].getFailuresCount()));
			}
			
			sb.append(String.format("\n\t\t%d total retrieval queries (%d timed-out)\n", totalAggregateOpsCount, failedTotalAggregateOpsCount));
		} else {
			for (int i = 0; i < Statistics.AGGREGATE_QUERIES_COUNT; i++) {
				sb.append(String.format("\t\t%-5d Q%-2d  queries\n", Statistics.aggregateQueriesArray[i].getRunsCount(), (i + 1)));
			}
			
			sb.append(String.format("\n\t\t%d total retrieval queries\n", totalAggregateOpsCount));
		}
		
		//considering a time correction caused by result parsing for each aggregate query by each of aggregate agents, that time is subtracted when calculating the total average		
		double averageQueriesPerSecond = (double)totalAggregateOpsCount / ((double)seconds - (double)(Statistics.timeCorrectionsMS.get() / 1000));
		if ((double)(Statistics.timeCorrectionsMS.get() / 1000) >= (double)seconds) {
			averageQueriesPerSecond = (double)totalAggregateOpsCount / ((double)seconds);
		}
		sb.append(String.format("\t\t%.4f average queries per second\n", averageQueriesPerSecond));		
				
		//in case using minUpdateRateThresholdOps option, display a message that benchmark is not 
		if (minUpdateRateThresholdOps > 0.0) {
			String message = "";
			if (!benchmarkResultIsValid.get()) {
				if ((seconds <= (int)(runPeriodSeconds * updateRateReachTimePercent)) && minUpdateRatePassesCount <= 1) {
					message = String.format("Waiting for update operations rate (current rate : %.1f ops) to reach minimum threshold of %.1f ops in %d second(s)", averageOperationsPerSecond, minUpdateRateThresholdOps, ((int)(runPeriodSeconds * updateRateReachTimePercent) - seconds));
					LOGGER.info(message);
					System.out.println(message);
				} else {
					message = String.format("Warning : Update operations rate has not reached or has dropped below minimum threshold of %.1f ops, benchmark results are not valid!", minUpdateRateThresholdOps);
					LOGGER.warn(message);
					System.out.println(message);
					System.exit(0);
				}				
				return (System.currentTimeMillis() - time);
			}
		}
		
		LOGGER.info(sb.toString());
		System.out.println(sb.toString());	
		
		return (System.currentTimeMillis() - time);		
	}	
	
	private void updateInternalStatus(double averageOperationsPerSecond) {
		
		//using maxUpdateRate threshold to control the update rate of editorial agents
		if (maxUpdateRateThresholdOps > 0.0) {
		//if maxUpdateRateOps is set to zero, it is disabled
			if (averageOperationsPerSecond > maxUpdateRateThresholdOps) {
				maxUpdateRateReached.set(true);
			} else {
				maxUpdateRateReached.set(false);
			}
		}
		
		if (minUpdateRateThresholdOps <= 0.0 && initializedCount >= 0) {
			//skip setting same values for AtomicBoolean variable : benchmarkResultIsValid, as it is read from other
			if (initializedCount > 0) {
				return;
			}
			//default value for minUpdateRateThresholdOps is 0.0 (or if explicitly set in properties file to 0.0), then 
			//disable the UpdateQueriesRateThreshold feature and consider results from benchmark always as valid 
			benchmarkResultIsValid.set(true);
			initializedCount++;
			return;
		}
		
		//the time frame during which update rate should be reached (and kept during the whole benchmark run)
		if (seconds < (runPeriodSeconds * updateRateReachTimePercent)) {
			String message = "";
			//initial reaching of the threshold
			if ((averageOperationsPerSecond >= minUpdateRateThresholdOps) && (minUpdateRatePassesCount == 0)) {
				message = String.format("Threshold %.1f ops (current update operations rate value : %.1f) has been reached reached at second %d ", minUpdateRateThresholdOps, averageOperationsPerSecond, seconds);
				minUpdateRatePassesCount++;
				benchmarkResultIsValid.set(true);
				LOGGER.info(message);
				System.out.println(message);
			}
			
			//averageOperationsPerSecond are dropping below the threshold - in which case the benchmark result is considered invalid
			if ((averageOperationsPerSecond < minUpdateRateThresholdOps) && (minUpdateRatePassesCount == 1)) {
				message = String.format("Warning : Current update operations rate : %.1f ops has dropped below minimum threshold %.1f at second : %d", averageOperationsPerSecond, minUpdateRateThresholdOps, seconds);
				minUpdateRatePassesCount++;
				benchmarkResultIsValid.set(false);
				LOGGER.warn(message);
				System.out.println(message);
			}
		//rest of the benchmark run time
		} else {
			//minUpdateRatePassedCount should be equal to 1 if threshold was reached during the first time frame, and hasn't dropped after reaching it
			if (minUpdateRatePassesCount != 1) {
				benchmarkResultIsValid.set(false);
				return;
			}
			
			if (averageOperationsPerSecond < minUpdateRateThresholdOps) {
				benchmarkResultIsValid.set(false);
			}
		}
	}
}
