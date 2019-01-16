/**
 * <p>Holds profile-specific configuration files for an ILP Connector.</p>
 *
 * <p>A connector may operate in one of three operational profiles:</p>
 *
 * <pre>
 *   <ol>
 *     <li><b>Plugin Mode</b>: The Connector hosts a single-plugin with a link to a parent connector. The
 *     connector MAY utilize IL-DCP to obtain an Interledger Address, or it MAY use a preconfigured Interledger
 *     Address. In this mode the Connector disables all routing logic.</li>
 *
 *     <li>Connector Mode: The Connector hosts one or more transport servers (e.g., WebSocket Server, gRPC Server, HTTP
 *     Server, etc), each of which listen on a single port (as appropriate to the protocol). The Connector will allow
 *     _many_ bilateral peers to connect to a given transport, and the multiplexer will handle construction of an
 *     appropriate Plugin instance for each incoming connection.</li>
 *   </ol>
 * </pre>
 */
package com.sappenin.interledger.ilpv4.connector.server.spring.settings;