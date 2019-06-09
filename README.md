# `Servlet` `Filter` Example

[![Build Status](https://travis-ci.org/stiemannkj1/servlet-filter-example.svg?branch=master)](https://travis-ci.org/stiemannkj1/servlet-filter-example)

This project is an example `Servlet` `Filter` to demonstrate how to create a filter that records request/response data
and provides additional views. Specifically
[`MetricsFilter`](metrics-filter/src/main/java/com/github/stiemannkj1/servlet/filter/example/MetricsFilter.java) tracks
response sizes and times. At any time you can request the metrics page to see the minimum, maximum, and average response
sizes and times and a list of all response sizes and times by navigating to
http://localhost:8080/your-app/com_github_stiemannkj1_servlet_filter_example_Metrics.jsp. To use `MetricsFilter` for
your own project, build it as directed below and include it on the classpath for your WAR; either inside your WAR's
**`WEB-INF/lib`** directory or in your server's global **`lib/`** directory (such as **`$TOMCAT/lib`**). The easiest way
to include `MetricsFilter` is to add the following Maven dependency after building the project:

```
<dependency>
    <groupId>com.github.stiemannkj1</groupId>
    <artifactId>metrics-filter</artifactId>
    <version>1.0-SNAPSHOT</version>
</dependency>
```

If you are including **`metrics-filter.jar`** in your application server's global **`lib/`** directory, you'll need to
include the following in your WAR's **`WEB-INF/lib`** to activate the filter:

```
<filter>
    <filter-name>com.github.stiemannkj1.servlet.filter.example.MetricsFilter</filter-name>
    <filter-class>com.github.stiemannkj1.servlet.filter.example.MetricsFilter</filter-class>
</filter>
<filter-mapping>
    <filter-name>com.github.stiemannkj1.servlet.filter.example.MetricsFilter</filter-name>
    <url-pattern>/*</url-pattern>
</filter-mapping>
```

This project is provided for example purposes only and should not be used in production projects.

## Building/Testing the Project:

To build and test the project, you must have [Maven 3.3.1+](https://maven.apache.org/download.cgi) and JDK 8+ installed.
This project is tested with Maven 3.6.1, JDK 8, and JDK 11. When the project is built,
[several unit tests](metrics-filter/src/test/java/com/github/stiemannkj1/servlet/filter/example/) are run against
`MetricsFilter`. More importantly,
[automated integration tests](est-web-app/src/test/java/com/github/stiemannkj1/test/web/app/) are run against
`MetricsFilter` using an actual application server (which is automatically downloaded using Maven).

To build and test the project on Tomcat 7, run:

```
mvn clean install
```

To build and test the project on GlassFish 5.1 (**JDK 8 only**), run:

```
mvn clean install -P glassfish
```

To build and test the project on Jetty 9.4.18, run:

```
mvn clean install && (cd test-web-app/ && mvn clean verify -P jetty)
```

To build and test the project on WildFly 16.0.0.Final, run:

```
mvn clean install-P wildfly
```

## Running The Test Web App

This project includes a test web application that is used for integration testing as mentioned above. The test web
application can also allow manual testing and interaction with `MetricsFilter`. You must build the project using the
instructions above before running the test web application.

To run the test web application in Tomcat 7:

```
(cd test-web-app/ && mvn org.apache.tomcat.maven:tomcat7-maven-plugin:2.2:run)
```

### See Also:

- [Spring's
`ContentCachingResponseWrapper`](https://github.com/spring-projects/spring-framework/blob/dc6f63f/spring-web/src/main/java/org/springframework/web/util/ContentCachingResponseWrapper.java)
- [How to read and copy the HTTP servlet response output stream content for
logging](https://stackoverflow.com/questions/8933054/how-to-read-and-copy-the-http-servlet-response-output-stream-content-for-logging#8972088)
