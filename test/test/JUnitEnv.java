package test;

import java.util.Properties;

/**
 * Copyright (c) 2018 China Systems Corp.
 *
 * @author ben.pan (ben.pan@chinasystems.com)
 * @date 2018-06-01.
 */
public class JUnitEnv {
    public static void init() {
        Properties prop = new Properties();
        prop.setProperty("log4j.rootLogger", "debug, stdout");
        prop.setProperty("log4j.appender.stdout", "org.apache.log4j.ConsoleAppender");
        prop.setProperty("log4j.appender.stdout.layout", "org.apache.log4j.PatternLayout");
        String pat =
//				"%-5p %d{ISO8601} (%C:%L) (%F:%L) %M %x - %m\n";
                "%5p (%F:%L) %M() - %m%n";
        prop.setProperty("log4j.appender.stdout.layout.ConversionPattern", pat);
        org.apache.log4j.PropertyConfigurator.configure(prop);
    }
}
