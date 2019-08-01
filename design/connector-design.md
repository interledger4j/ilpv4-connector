# Connector Architecture & Design

This page details the architecture, design, and feature-set of this ILPv4 Connector.

## Features

* Account Balance Tracking

  \*

* High-speed ILP Packet Switching Fabric
* ILP LPIv2 Plugin Support
* ILDCP
* Etc, etc.

## Packet Destination Address Handling

Every ILPv4 `Prepare` packet has a destination address that indicates where a packet should ultimately be delivered. These addresses MUST always conform to [IL-RFC-15](https://github.com/interledger/rfcs/blob/master/0015-ilp-addresses) and as such may begin with multiple, valid address-prefixes. Each of these prefixes has special meaning, and is handled by the Connector in slightly different ways.

| Address Prefix | System Applicability | Purpose | Connector Handling | Accepts External Packets | Forwards Out of Connector |
| :---: | :---: | :---: | :---: | :---: | :---: |
| `g.` | Global Allocation Scheme | ILP addresses that are intended to send and receive money from any other address in the global scheme. | Forwarded to a plugin by the Packet-switching fabric. | Yes | Yes |
| `private.` | Private allocation | For ILP addresses that only have meaning in a private subnet or intranet. Analogous to the 192.168.0.0/16 range in IPv4. | Forwarded to a plugin by the Packet-switching fabric. | No | Yes |
| `example.` | Examples | For "non-real" addresses that are used as examples or in documentation. Analogous to "555 phone numbers" in the USA. | Always rejected by the Connector. | No | No |
| `test.`, `test1.`, `test2.`,`test3.` | Interledger testnet and testing | For addresses used on the public Interledger testnet and in local tests, such as unit or integration tests of compatible software. | Handled identically to a `g.` address if the Connector's Operating Address begins with `test`; Otherwise always rejected by the Connector. | Yes | Yes |
| `local.` | Connector-local | For addresses that are valid only in the a local-connector network, such among a cluster of connectors operated by the same entity. | Forwarded to a connected plugin by the Packet-switching fabric. | No | Yes \(only to another `local.`\) |
| `peer.` | Peering | Addresses for exchange of packets only with a direct peer. Connectors MUST NOT forward packets with peer. addresses. Packets exchanged between peers to pass routing and config information will use peer. addresses. | Handled by the Connector as a message from a Peer. | Yes | No |
| `self.` | Local loopback | For addresses that are only valid on the local machine. For example, `self.ping`, `self.echo`, and other internal addresses that the Connector fowards traffic to for internal handling. | External packets _can_ make their way to `self.` handlers, but only indrectly. For example, Ping packets are addressed to this Connector's operator address, and are then forwarded to `self.ping` for handling. | No | No |

