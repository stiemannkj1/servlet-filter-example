# Servlet `Filter` Example

[![Build Status](https://travis-ci.org/stiemannkj1/servlet-filter-example.svg?branch=master)](https://travis-ci.org/stiemannkj1/servlet-filter-example)

This example Servlet `Filter` demonstrates how to record request/response data and provide additional views. Specifically
[`MetricsFilter`](metrics-filter/src/main/java/com/github/stiemannkj1/servlet/filter/example/MetricsFilter.java) tracks
response sizes and times. At any time you can request the metrics page to view the minimum, maximum, and average response
sizes and times along with a list of all previous response sizes and times by navigating to
http://localhost:8080/your-app/com_github_stiemannkj1_servlet_filter_example_Metrics.jsp. To use `MetricsFilter` for
your own project, build it as directed below and include it on the classpath for your WAR; either inside your WAR's
**`WEB-INF/lib`** directory or in your server's global **`lib/`** directory (such as **`$TOMCAT/lib`**). The easiest way
to include `MetricsFilter` is to add it as a Maven dependency after building this project:

```
<dependency>
    <groupId>com.github.stiemannkj1</groupId>
    <artifactId>metrics-filter</artifactId>
    <version>1.0</version>
</dependency>
```

If you are including **`metrics-filter.jar`** in your application server's global **`lib/`** directory, you may need to
include the following in your WAR's **`WEB-INF/web.xml`** to activate the filter:

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

This filter is provided for example purposes only and should not be used in production.

## Configuration Options:

By default, `MetricsFilter` uses
[`AtomicLong`](https://docs.oracle.com/javase/8/docs/api/java/util/concurrent/atomic/AtomicLong.html) to generate unique
response ids per `MetricsFilter` instance. However, `MetricsFilter` can instead be configured to use
[`UUID`](https://docs.oracle.com/javase/8/docs/api/java/util/UUID.html) to generate ids that will be unique across
multiple instances of `MetricsFilter` (with an extremely small possibility of collisions). Set the following
`<init-param>` for `MetricsFilter` in your **`web.xml`** to use `UUID` instead of `AtomicLong` to generate response ids:

```
<init-param>
    <param-name>com.github.stiemannkj1.servlet.filter.example.MetricsFilter.USE_UUID_UNIQUE_RESPONSE_ID</param-name>
    <param-value>true</param-value>
</init-param>
```

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
mvn clean install -P wildfly
```

To build/test with `UUID` instead of `AtomicLong` response ids, add the following command line property:
`-Duse.uuid.unique.response.id=true`.

## Running The Test Web App

This project includes a test web application that is used for integration testing as mentioned above. The test web
application can also be accessed for manual testing and evaluation of `MetricsFilter`. You must build the project
using the instructions above before running the test web application.

To start the test web application in Tomcat 7, run:

```
(cd test-web-app/ && mvn org.apache.tomcat.maven:tomcat7-maven-plugin:2.2:run)
```

### Maven Plugin Configuration

This project takes advantage of several fantastic Maven plugins to run the test web application within the Maven build
lifecycle for integration testing. The configuration used in **`test-web-app/pom.xml`** can be used as an example for
your own projects. Click the following links to see example configuration for each plugin:

- [`tomcat7-maven-plugin`](https://github.com/stiemannkj1/servlet-filter-example/blob/a237583/test-web-app/pom.xml#L136-L178)
- [`maven-embedded-glassfish-plugin`](https://github.com/stiemannkj1/servlet-filter-example/blob/a237583/test-web-app/pom.xml#L179-L221)
- [`jetty-maven-plugin`](https://github.com/stiemannkj1/servlet-filter-example/blob/a237583/test-web-app/pom.xml#L222-L270)
- [`wildfly-maven-plugin`](https://github.com/stiemannkj1/servlet-filter-example/blob/a237583/test-web-app/pom.xml#L271-L313)

### For more Servlet `Filter` examples, take a look at:

- [Spring's
`ContentCachingResponseWrapper`](https://github.com/spring-projects/spring-framework/blob/dc6f63f/spring-web/src/main/java/org/springframework/web/util/ContentCachingResponseWrapper.java)
- [How to read and copy the HTTP servlet response output stream content for
logging](https://stackoverflow.com/questions/8933054/how-to-read-and-copy-the-http-servlet-response-output-stream-content-for-logging#8972088)
