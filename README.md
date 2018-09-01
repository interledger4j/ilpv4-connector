# java-ilp-connector
A Java implementation of an Interledger Connector

# Configuration
Configuration of this Connector is sourced from a variety of potential sources when the connector
starts up. This includes property files, environment variables, and more, per Spring Boot functionality.

At runtime, configuration values can be changed (this facility is not yet implemented, but will be via
an adminstrative API and possibly via JMX).

TODO: Document diff between config values, runtime attributes, etc, and how each changes at runtime.
e.g., Peer address can't change, but the routeUpdateInterval can. Things that can change at runtime
aren't generally accessible via something like a `Peer` object (which is immutable), but instead
must be accessed via PeerManager.

