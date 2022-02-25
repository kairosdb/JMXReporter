# JMXReporter
Universal tool to report JMX metrics to any backend supported by Metrics4j.

Yes that is right, say you are running Kafka and want to report JMX metrics to 
InfluxDB or Graphite or both at the same time.  This tool is for you.

The main purpose of this tool was to showcase the power of Metrics4j.  JMXReporter 
is very simple in what it does, it registers JMX metrics as sources for Metrics4j
so they can be reported.

## Getting Started
1. Download the JMXReporter jar file.
1. For your JMX application (say Kafka) add JMXReporter to the java command line
as a javaagent (-javaagent:path_to_jxmreporter_jar)
1. Place the following simple metrics4j.conf file in the applications classpath.  Or specify the location with -DMETRICS4J_CONFIG=/path/to/metrics4j.conf
```hocon
metrics4j: {
	_dump-file: "dump.conf"
}
```

Run your application and when it shuts down Metrics4j will create the file dump.conf
that shows all the JMX sources that were found.

Replace the simple metrics4j.conf file with the one from the dump and then follow
the Metrics4j documentation to add a trigger, formatter and sink so it can 
send off your JMX metrics.  https://github.com/kairosdb/metrics4j

Note: Because all of the JMX metrics are created as custom sources in metrics4j
you don't need to define a collector in your metrics4j.conf file.

Checkout the wiki page for samples as I test this with various applications:
https://github.com/kairosdb/JMXReporter/wiki
