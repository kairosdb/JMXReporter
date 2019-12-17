package org.kairosdb.jmxreporter;

import javax.management.DynamicMBean;

public interface TestMXBean
{
	int getIntCount();
	long getLongCount();
	float getFloatValue();
	double getDoubleValue();
}
