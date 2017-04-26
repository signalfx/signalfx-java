# SignalFx jvm agent

## How to build
mvn clean install

## How to attach to the java application
1. build the project
2. The agent resides in signalfx-jvm-agent.jar in the target directory
3. When launching the java application include the following parameters:
java -javaagent:/some/path/signalfx-jvm-agent.jar=path_to_jmx_trans_config \
-Dcom.signalfx.hostUrl=http://sfproxy-integrations1--aaaa.int.signalfuse.com:28080 \
-Dcom.signalfx.authToken=__YOUR_SIGNALFX_TOKEN__ \
your_class_name

## Optional config:

com.signalfx.hostUrl parameter is optional. The default value is https://ingest.signalfx.com

path_to_jmx_trans_config is optional as well

## Configuration

The configuration is taken from https://github.com/jmxtrans/jmxtrans-agent

Here is the default config
```xml

<jmxtrans-agent>
    <queries>
        <!-- OS -->
        <query objectName="java.lang:type=OperatingSystem" attribute="SystemLoadAverage" resultAlias="os.systemLoadAverage"/>

        <!-- JVM -->
        <query objectName="java.lang:type=Memory" attribute="HeapMemoryUsage"
               resultAlias="jvm.heapMemoryUsage.#key#"/>
        <query objectName="java.lang:type=Memory" attribute="HeapMemoryUsage"
               resultAlias="jvm.heapMemoryUsage.#key#"/>
        <query objectName="java.lang:type=Memory" attribute="NonHeapMemoryUsage"
               resultAlias="jvm.nonHeapMemoryUsage.#key#"/>
        <query objectName="java.lang:type=Memory" attribute="NonHeapMemoryUsage"
               resultAlias="jvm.nonHeapMemoryUsage.#key#"/>
        <query objectName="java.lang:type=ClassLoading" attribute="LoadedClassCount" resultAlias="jvm.loadedClasses"/>

        <query objectName="java.lang:type=MemoryPool,name=PS Eden Space" attribute="Usage"
               resultAlias="jvm.edenSpaceUsage.#key#"/>
        <query objectName="java.lang:type=MemoryPool,name=PS Old Gen" attribute="Usage"
               resultAlias="jvm.oldSpaceUsage.#key#"/>
        <query objectName="java.lang:type=MemoryPool,name=PS Survivor Space" attribute="Usage"
               resultAlias="jvm.survivorSpaceUsage.#key#"/>
        <query objectName="java.lang:type=GarbageCollector,name=PS MarkSweep" attribute="CollectionCount" resultAlias="jvm.gc.PSMarkSweep.collection_count" type="counter"/>
        <query objectName="java.lang:type=GarbageCollector,name=PS MarkSweep" attribute="CollectionTime" resultAlias="jvm.gc.PSMarkSweep.collection_time"/>
        <query objectName="java.lang:type=GarbageCollector,name=PS Scavenge" attribute="CollectionCount" resultAlias="jvm.gc.PSScavenge.collection_count" type="counter"/>
        <query objectName="java.lang:type=GarbageCollector,name=PS Scavenge" attribute="CollectionTime" resultAlias="jvm.gc.PSScavenge.collection_time"/>
        <query objectName="java.lang:type=ClassLoading" attribute="LoadedClassCount" resultAlias="jvm.loadedClasses" type="gauge"/>

        <query objectName="java.lang:type=Threading" attribute="ThreadCount" resultAlias="jvm.thread"/>

        <!--&lt;!&ndash; TOMCAT &ndash;&gt;-->
        <!--<query objectName="Catalina:type=GlobalRequestProcessor,name=*" attribute="requestCount"-->
               <!--resultAlias="tomcat.requestCount"/>-->
        <!--<query objectName="Catalina:type=GlobalRequestProcessor,name=*" attribute="errorCount"-->
               <!--resultAlias="tomcat.errorCount"/>-->
        <!--<query objectName="Catalina:type=GlobalRequestProcessor,name=*" attribute="processingTime"-->
               <!--resultAlias="tomcat.processingTime"/>-->
        <!--<query objectName="Catalina:type=GlobalRequestProcessor,name=*" attribute="bytesSent"-->
               <!--resultAlias="tomcat.bytesSent" type="counter"/>-->
        <!--<query objectName="Catalina:type=GlobalRequestProcessor,name=*" attribute="bytesReceived"-->
               <!--resultAlias="tomcat.bytesReceived" type="counter"/>-->

        <query objectName="java.lang:type=OperatingSystem" attribute="SystemLoadAverage" resultAlias="os.systemLoadAverage"/>

        <!-- APPLICATION -->
        <!--<query objectName="Catalina:type=Manager,context=/,host=localhost" attribute="activeSessions"-->
               <!--resultAlias="application.activeSessions"/>-->
    </queries>
    <outputWriter class="com.signalfx.jvm.agent.SignalFxOutputWriter">
        <!-- we could also specify these params via  jvm properties -Dcom.signalfx.hostUrl=http://sfproxy-integrations1&#45;&#45aaaa.int.signalfuse.com:28080 -Dcom.signalfx.authToken=OO9aPaftRvx_bJMc7aD8OQ-->
        <!--<hostUrl>http://sfproxy-integrations1&#45;&#45;aaaa.int.signalfuse.com:28080</hostUrl>-->
        <!--<authToken>OO9aPaftRvx_bJMc7aD8OQ</authToken>-->
    </outputWriter>
    <collectIntervalInSeconds>11</collectIntervalInSeconds>
</jmxtrans-agent>

```
