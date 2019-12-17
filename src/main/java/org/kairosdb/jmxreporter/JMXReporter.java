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
import java.lang.instrument.Instrumentation;
import java.lang.management.ManagementFactory;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JMXReporter implements NotificationListener
{
	private static final Logger logger = LoggerFactory.getLogger(JMXReporter.class);

	private final MBeanServer m_server;
	private final Map<ObjectName, List<SourceKey>> m_sourceKeyMap = new HashMap<>();

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
		MBeanServer server = ManagementFactory.getPlatformMBeanServer();

		JMXReporter reporter = new JMXReporter(server);

		//Register all current MBeans
		reporter.loadExistingMBeans();


		//Add notification for future MBeans
		try
		{
			reporter.addMBeanNotification();
		}
		catch (InstanceNotFoundException e)
		{
			e.printStackTrace();
		}
	}

	public void close() throws ListenerNotFoundException, InstanceNotFoundException
	{
		m_server.removeNotificationListener(MBeanServerDelegate.DELEGATE_NAME, this);
	}

	public JMXReporter(MBeanServer server)
	{
		m_server = server;
	}

	/*package*/ void loadExistingMBeans()
	{
		for (ObjectInstance queryMBean : m_server.queryMBeans(null, null))
		{
			registerMBean(queryMBean.getObjectName());
		}
	}

	/*package*/ void addMBeanNotification() throws InstanceNotFoundException
	{
		m_server.addNotificationListener(MBeanServerDelegate.DELEGATE_NAME, this, null, null);
	}

	private void registerMBean(ObjectName beanName)
	{
		List<SourceKey> sourceKeys = new ArrayList<>();
		try
		{
			for (MBeanAttributeInfo attribute : m_server.getMBeanInfo(beanName).getAttributes())
			{
				if (attribute.isReadable() && !attribute.isWritable())
				{
					String type = attribute.getType();
					String className = beanName.getDomain()+"."+beanName.getKeyProperty("type");
					String methodName = attribute.getName();
					Map<String, String> tags = beanName.getKeyPropertyList();
					//System.out.println(attribute.getName());
					//System.out.println(attribute.getDescription());
					if (type.equals("int"))
					{
						MetricSourceManager.addSource(className, methodName,
								tags, null, new IntAttributeSource(beanName, attribute.getName()));
						sourceKeys.add(new SourceKey(className, methodName, tags));
					}
					else if (type.equals("long"))
					{
						MetricSourceManager.addSource(className, methodName,
								tags, null, new LongAttributeSource(beanName, attribute.getName()));
						sourceKeys.add(new SourceKey(className, methodName, tags));
					}
					else if (type.equals("float"))
					{
						MetricSourceManager.addSource(className, methodName,
								tags, null, new FloatAttributeSource(beanName, attribute.getName()));
						sourceKeys.add(new SourceKey(className, methodName, tags));
					}
					else if (type.equals("double"))
					{
						MetricSourceManager.addSource(className, methodName,
								tags, null, new DoubleAttributeSource(beanName, attribute.getName()));
						sourceKeys.add(new SourceKey(className, methodName, tags));
					}
					else if (type.equals("javax.management.openmbean.CompositeData"))
					{
						MetricSourceManager.addSource(className, methodName,
								tags, null, new CompositeAttributeSource(beanName, attribute.getName()));
						sourceKeys.add(new SourceKey(className, methodName, tags));
					}
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}

		if (sourceKeys.size() != 0)
			m_sourceKeyMap.put(beanName, sourceKeys);
	}

	private void unregisterMBean(ObjectName beanName)
	{
		try
		{
			List<SourceKey> sourceKeys = m_sourceKeyMap.get(beanName);

			if (sourceKeys != null)
			{
				for (SourceKey sourceKey : sourceKeys)
				{
					MetricSourceManager.removeSource(sourceKey.getClassName(), sourceKey.getMethodName(),
							sourceKey.getTags());
				}
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}

	@Override
	public void handleNotification(Notification notification, Object handback)
	{
		if (!(notification instanceof MBeanServerNotification)) {
			System.out.println("Ignored notification of class " + notification.getClass().getName());
			return;
		}
		MBeanServerNotification mbsn = (MBeanServerNotification) notification;
		String what = "";
		ObjectName beanName = mbsn.getMBeanName();
		if (notification.getType().equals(MBeanServerNotification.REGISTRATION_NOTIFICATION))
		{
			registerMBean(beanName);
			what = "MBean registered";
		}
		else if (notification.getType().equals(MBeanServerNotification.UNREGISTRATION_NOTIFICATION))
		{
			unregisterMBean(beanName);
			what = "MBean unregistered";
		}

		logger.debug("Received MBean Server notification: {}: {}", what, beanName);
	}

	private abstract class AttributeSource implements MetricCollector
	{
		protected final ObjectName m_objectName;
		protected final String m_attribute;

		private AttributeSource(ObjectName objectName, String attribute)
		{
			m_objectName = objectName;
			m_attribute = attribute;
		}

		protected abstract MetricValue getValue();

		@Override
		public void reportMetric(MetricReporter metricReporter)
		{
			metricReporter.put("value", getValue());
		}
	}


	private class IntAttributeSource extends AttributeSource
	{
		private IntAttributeSource(ObjectName objectName, String attribute)
		{
			super(objectName, attribute);
		}

		@Override
		public MetricValue getValue()
		{
			long value = 0;

			try
			{
				value = (int)m_server.getAttribute(m_objectName, m_attribute);
			}
			catch (Exception e)
			{
				logger.debug("Failed to read JMX attribute "+m_objectName+": "+m_attribute, e);
			}

			return new LongValue(value);
		}

	}

	private class LongAttributeSource extends AttributeSource
	{
		private LongAttributeSource(ObjectName objectName, String attribute)
		{
			super(objectName, attribute);
		}

		@Override
		public MetricValue getValue()
		{
			long value = 0;

			try
			{
				value = (long)m_server.getAttribute(m_objectName, m_attribute);
			}
			catch (Exception e)
			{
				logger.debug("Failed to read JMX attribute "+m_objectName+": "+m_attribute, e);
			}

			return new LongValue(value);
		}
	}

	private class FloatAttributeSource extends AttributeSource
	{

		private FloatAttributeSource(ObjectName objectName, String attribute)
		{
			super(objectName, attribute);
		}

		@Override
		public MetricValue getValue()
		{
			float value = 0;

			try
			{
				value = (float)m_server.getAttribute(m_objectName, m_attribute);
			}
			catch (Exception e)
			{
				logger.debug("Failed to read JMX attribute "+m_objectName+": "+m_attribute, e);
			}

			return new DoubleValue(value);
		}
	}

	private class DoubleAttributeSource extends AttributeSource
	{

		private DoubleAttributeSource(ObjectName objectName, String attribute)
		{
			super(objectName, attribute);
		}

		@Override
		public MetricValue getValue()
		{
			double value = 0.0;

			try
			{
				value = (double)m_server.getAttribute(m_objectName, m_attribute);
			}
			catch (Exception e)
			{
				logger.debug("Failed to read JMX attribute "+m_objectName+": "+m_attribute, e);
			}

			return new DoubleValue(value);
		}
	}

	private class CompositeAttributeSource extends AttributeSource
	{
		private CompositeAttributeSource(ObjectName objectName, String attribute)
		{
			super(objectName, attribute);
		}

		@Override
		protected MetricValue getValue()
		{
			return null;
		}

		@Override
		public void reportMetric(MetricReporter metricReporter)
		{
			try
			{
				CompositeData data = (CompositeData) m_server.getAttribute(m_objectName, m_attribute);
				if (data != null)
				{
					CompositeType type = data.getCompositeType();

					for (String key : type.keySet())
					{
						OpenType<?> openType = type.getType(key);
						if (openType == SimpleType.LONG)
						{
							metricReporter.put(key, new LongValue((long) data.get(key)));
						}
						else if (openType == SimpleType.INTEGER)
						{
							metricReporter.put(key, new LongValue((int) data.get(key)));
						}
						else if (openType == SimpleType.FLOAT)
						{
							metricReporter.put(key, new DoubleValue((float) data.get(key)));
						}
						else if (openType == SimpleType.DOUBLE)
						{
							metricReporter.put(key, new DoubleValue((double) data.get(key)));
						}
					}
				}
			}
			catch (Exception e)
			{
				logger.debug("Failed to read JMX attribute "+m_objectName+": "+m_attribute, e);
			}
		}
	}
}
