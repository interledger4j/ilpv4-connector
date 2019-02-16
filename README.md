# java-ilp-connector
A Java implementation of an Interledger v4 Connector

# Architecture & Design







# Development
(how to build from source, contributing, etc)

# Operating a Connector
(how to intall, turn on , etc, configuration)

# Connector Releases
(overview of versioning, and software releases --> see github for this)

#





===============================
===============================
===============================


## Configuration
Configuration of this Connector is obtained from a variety of potential sources when the connector
starts up. This includes property files, environment variables, and more, per Spring Boot functionality.
and can be updated at runtime without server restarts.

TODO: Finish config docs.

# Connector Profiles
This implementation supports a variety of profiles that a Connector can be started in, with each
profile targeting a different use-case.

## Plugin Mode
The simplest of connector profiles, this profile is meant to operate as an Interledger
client operating a single plugin. This profile requires a connection to a parent connector
that will provide more advanced functionality, such as routing, FX, and ping to the broader Interledger
network.

### Supported Functionality
A plugin-mode Connector operates with limited functionality, specifically:

* Only a single Account is supported using a client-plugin. This means the connector will always
be the `child` of some other parent Connector.
* Connection configuration is defined statically, and read by the Connector at startup.
* CCP and other routing functionality is not supported (plugin-mode Connectors delegate this functionality to a parent Connector).
* The Connectors will not track account balances. Instead, balance-tracking occurs entirely
in the parent/peer connector.
* [ILDCP](https://github.com/interledger/rfcs/blob/master/0031-dynamic-configuration-protocol/0031-dynamic-configuration-protocol.md)
is optionally supported.

For an example configuration of a single-account Connector, see [application-single-account-connector.yml]
(FIXME).

### Startup
TODO

## Connector Mode
This mode supports all functionality of an ILP Connector, and is intended for
connectors operating as an ILSP. In this way, this mode is meant to allow
a Connector to listen to many incoming connections from external parties and
respond to them appropriately.

### Supported Functionality
An Infrastructure-mode Connector operates with all functionality enabled, although with some
limitations:

* Connection types are limited to multi-account types, such as BPP or multi-account BTP.

For an example configuration of a mini-connector, see [application-infrastrucutre-connector.yml](FIXME).

### Startup
TODO

## Infrastructure-mode Connector
The next type of supported profile is a connector running in infrastructure` mode. This mode
is meant to allow a Connector to peer with other highly-trusted connectors so that the
 balance tracking and settlement sub-subsystem can be optimized using post-funding of accounts
 and infrequent settlement operations.

### Supported Functionality
An Infrastructure-mode Connector operates with all functionality enabled, although with some
limitations:

* Connection types are limited to multi-account types, such as BPP or multi-account BTP.

For an example configuration of a mini-connector, see [application-infrastrucutre-connector.yml](FIXME).

### Startup
TODO


TODO: Loopback mode,


# Options

* BPP defines a single connection using two MUX transports (grp server + gprc client)
    * BPP supports account multiplexing if desired, but auth happens at the MUX level.



* Mini-connector.
* Clustered Mode:
* [I] BPP Connector (uses BPP to talk to a remote connector). Primary infrastructure-mode?
* [I] BTP Single-Account Client: Creates a new Websocket client for each connection, always of type PARENT. No routing.
* [I] BTP Single-Account Server: Runs a single websocket server, intended for infrastructure mode.

* [R] BTP Multi-Account Server: No routing, implementation-specific settings (via custom-settings or otherwise).