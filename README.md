---
description: A Java implementation of an Interledger v4 Connector.
---

# Java ILPv4 Connector

## Java ILPv4 Connector [![](https://img.shields.io/badge/Discuss-Interledger%20Forum-blue.svg)](https://forum.interledger.org/tags/java-ilpv4-connector)

[![](https://circleci.com/gh/sappenin/java-ilpv4-connector.svg?style=shield)](https://circleci.com/gh/sappenin/java-ilpv4-connector) [![](https://codecov.io/gh/sappenin/java-ilpv4-connector/branch/master/graph/badge.svg)](https://codecov.io/gh/sappenin/java-ilpv4-connector) [![](https://img.shields.io/github/license/sappenin/java-ilp-connector.svg)](https://github.com/sappenin/java-ilp-connector/blob/master/LICENSE) [![](https://api.codacy.com/project/badge/Grade/49e43210600d462f861e1813230d855d)](https://www.codacy.com/app/sappenin/java-ilpv4-connector?utm_source=github.com&amp;utm_medium=referral&amp;utm_content=sappenin/java-ilpv4-connector&amp;utm_campaign=Badge_Grade) [![](https://img.shields.io/github/issues/sappenin/java-ilpv4-connector.svg)](https://github.com/sappenin/java-ilpv4-connector/issues) [![Total alerts](https://img.shields.io/lgtm/alerts/g/sappenin/java-ilpv4-connector.svg?logo=lgtm&logoWidth=18)](https://lgtm.com/projects/g/sappenin/java-ilpv4-connector/alerts/)

## Supported Features

This Connector implementation is intended for operation as a server-based ILSP, meaning it will listen for, and accept, and respond to potentially _many_ incoming connections. Specifically, this implementation supports the follwoing ILP features:

* **ILDCP**: Interledger Dynamic Configuration Protocol as specified in [IL-RFC-0031](https://github.com/interledger/rfcs/blob/master/0031-dynamic-configuration-protocol/0031-dynamic-configuration-protocol.md).
* **ILP-over-HTTP**: Also known as BLAST \(**B**i**L**ateral **A**synchronous **S**eder **T**ransport\), defined in [IL-RFC-0030](https://github.com/interledger/rfcs/pull/504).
* **Route Broadcast Protocol**: Defines how Connectors can exchange routing table updates as defined in [Route Broadcast Protocol](https://github.com/interledger/rfcs/pull/455).
* **Balance Tracking**: [Redis](https://redis.io) is used to durably track balance updates in a high-performance manner.
* **Persistent Data Storage**: Account and other data can be stored using Postgres, MySQL, Oracle, MSSQL, and more.

## Architecture & Design

To learn more about how this implementation is designed, see the [docs](https://github.com/sappenin/java-ilpv4-connector/tree/8b48e3aba253bd604564d78deecc445f2ab2d3dc/docs/README.md) folder, specifically [Connector Design](design/connector-design.md).

## Development

To learn more about how to contribute to this project, see the [docs/development](documentation/development.md) folder.

## Operating a Connector

**Disclaimer:** _**This implementation is currently a prototype and SHOULD NOT be used in a production deployment!**_

To configure this connector, see [Configuration](documentation/configuration.md) in the docs folder.

## Connector Releases

This implementation follows [Semantic Versioning](https://semver.org/) as closely as possible. To view releases, see [here](https://github.com/sappenin/java-ilpv4-connector/releases).

