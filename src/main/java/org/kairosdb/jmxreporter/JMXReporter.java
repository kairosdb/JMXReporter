package org.kairosdb.jmxreporter;

import org.kairosdb.metrics4j.MetricSourceManager;
import org.kairosdb.metrics4j.collectors.MetricCollector;
import org.kairosdb.metrics4j.reporting.DoubleValue;
import org.kairosdb.metrics4j.reporting.LongValue;
import org.kairosdb.metrics4j.reporting.MetricReporter;
import org.kairosdb.metrics4j.reporting.MetricValue;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.management.*;
import javax.management.openmbean.CompositeData;
import javax.management.openmbean.CompositeType;
import javax.management.openmbean.OpenType;
import javax.management.openmbean.SimpleType;
import java.io.IOException;
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JMXReporter
{
	private static final Logger logger = LoggerFactory.getLogger(JMXReporter.class);

	public static void main(String[] args)
	{
		premain(null, null);

		try
		{
			Thread.sleep(10000);
		}
		catch (InterruptedException e)
		{
			e.printStackTrace();
		}
	}

	public static void premain(String args, Instrumentation instrumentation)
	{
		//This effectively loads metrics4j
		MetricSourceManager.getSource(Metrics.class);
	}
}
