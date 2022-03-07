package org.kairosdb.jmxreporter;

import org.kairosdb.metrics4j.collectors.LongCollector;

public interface Metrics
{
	LongCollector metrics();
}
