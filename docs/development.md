# Connector Development

We welcome any and all submissions, whether it's a typo, bug fix, or new feature.

## Requirements

This project the following software in order to build:

* **Java**: This project requires Java JDK8 or above. To install, follow the directions [here](https://www.oracle.com/technetwork/java/javase/downloads/jdk8-downloads-2133151.html).
* **Maven**: This project uses Maven to manage dependencies and other aspects of the build. To install Maven, follow the instructions at [https://maven.apache.org/install.html](https://maven.apache.org/install.html).

## Download & Build

To get started, download the code and then build the project:

```bash
$ git clone https://github.com/sappenin/java-ilpv4-connector
$ mvn clean install
```

## Checkstyle

The project uses checkstyle to keep code-style consistent. All Checkstyle checks are run by default during the build, but if you would like to run checkstyle checks independently, use the following command:

```bash
$ mvn checkstyle:checkstyle
```

