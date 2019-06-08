# Servlet `Filter` Example

## Building/Testing the Project:

To build and test the project, you must have [Maven 3.3.1+](https://maven.apache.org/download.cgi) and JDK 8+ installed.
This project is tested with Maven 3.6.1, JDK 8, and JDK 11.

To build and test the project on Tomcat 7, run:

```
mvn clean verify
```

To build and test the project on GlassFish 5.1 (**JDK 8 only**), run:

```
mvn clean verify -P glassfish
```

To build and test the project on Jetty 9.4.18, run:

```
mvn clean install && (cd test-web-app/ && mvn clean verify -P jetty)
```

To build and test the project on WildFly 16.0.0.Final, run:

```
mvn clean verify -P wildfly
```

## TODO

1. Create example Servlet `Filter` that applies to all Servlets in WAR.
2. Create unit tests for Servlet `Filter`.
3. Create integration tests for Servlet `Filter` using **`test-web-app`**.
