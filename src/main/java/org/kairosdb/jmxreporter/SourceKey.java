package org.kairosdb.jmxreporter;

import java.util.Map;

public class SourceKey
{
	private final String m_className;
	private final String m_methodName;
	private final Map<String, String> m_tags;


	public SourceKey(String className, String methodName, Map<String, String> tags)
	{
		m_className = className;
		m_methodName = methodName;
		m_tags = tags;
	}

	public String getClassName()
	{
		return m_className;
	}

	public String getMethodName()
	{
		return m_methodName;
	}

	public Map<String, String> getTags()
	{
		return m_tags;
	}
}
