package com.signalfx.jvm.agent;

import org.jmxtrans.agent.JmxTransAgent;
import org.jmxtrans.agent.JmxTransConfigurationLoader;
import org.jmxtrans.agent.JmxTransConfigurationXmlLoader;
import org.jmxtrans.agent.JmxTransExporter;
import org.jmxtrans.agent.util.logging.Logger;

import java.lang.instrument.Instrumentation;
import java.util.logging.Level;

/**
 *
 *
 */
public class JvmAgent {
    private static Logger logger = Logger.getLogger(JmxTransAgent.class.getName());

    public static void agentmain(String configFile, Instrumentation inst) {
        initializeAgent(configFile);
    }

    public static void premain(final String configFile, Instrumentation inst) {
        final int delayInSecs = Integer.parseInt(System.getProperty("jmxtrans.agent.premain.delay", "0"));


        if (delayInSecs > 0) {
            logger.info("jmxtrans agent initialization delayed by " + delayInSecs + " seconds");
            new Thread("jmxtrans-agent-delayed-starter-" + delayInSecs + "secs") {
                @Override
                public void run() {
                    try {
                        Thread.sleep(delayInSecs * 1000);
                    } catch (InterruptedException e) {
                        Thread.interrupted();
                        return;
                    }
                    initializeAgent(configFile);
                }
            }.start();
        } else {
            initializeAgent(configFile);
        }
    }

    private static void initializeAgent(String configFile) {
        if (configFile == null || configFile.isEmpty()) {
            configFile = "classpath:jvm-agent-config.xml";
        }
        try {
            JmxTransConfigurationLoader configurationLoader = new JmxTransConfigurationXmlLoader(configFile);

            JmxTransExporter jmxTransExporter = new JmxTransExporter(configurationLoader);
            //START
            jmxTransExporter.start();
            logger.info("JmxTransAgent started with configuration '" + configFile + "'");
        } catch (Exception e) {
            String msg = "Exception loading JmxTransExporter from '" + configFile + "'";
            logger.log(Level.SEVERE, msg, e);
            throw new IllegalStateException(msg, e);
        }
    }

}
