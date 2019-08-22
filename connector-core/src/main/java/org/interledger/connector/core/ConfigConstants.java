package org.interledger.connector.core;

/**
 *
 */
/**
 * <p>Constants related to this Connector's configuration properties.</p>
 *
 * <p>Note that these properties can be overridden using a variety of mechanisms:</p>
 *
 * <pre>
 *   <ul>
 *     <li>Set via JVM system property: -Dfoo=bar</li>
 *     <li>Via OS Environment: `export foo=bar`</li>
 *     <li>Via Maven Property.</li>
 *   </ul>
 * </pre>
 *
 * @sse "https://java-connector.ilpv4.dev/operating-a-connector/configuration"
 * @see "https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html"
 */

public interface ConfigConstants {

  String DOT = ".";
  String ENABLED = "enabled";
  String TRUE = "true";
  String FALSE = "false";

  String INTERLEDGER = "interledger";
  String CONNECTOR = "connector";
  String INTERLEDGER__CONNECTOR = INTERLEDGER + DOT + CONNECTOR;

  /**
   * @deprecated This value will likely go away if we transition to a BTP proxy that doesn't typically run in the
   * Connector runtime. If removing, be sure to remove from property files too.
   */
  @Deprecated
  String BTP_ENABLED = "btp.enabled";

  String BLAST_ENABLED = "blastEnabled";

  /**
   * @deprecated This is no longer necessary because the WebSocket server config doesn't engage unless BTP is enabled.
   */
  @Deprecated
  String WEBSOCKET_SERVER_ENABLED = INTERLEDGER__CONNECTOR + "websocketServerEnabled";

  String INTERLEDGER__CONNECTOR__GLOBAL_ROUTING_SETTINGS = INTERLEDGER__CONNECTOR + DOT + "globalRoutingSettings";
  String INTERLEDGER__CONNECTOR__GLOBAL_ROUTING_SETTINGS__ROUTING_SECRET
    = INTERLEDGER__CONNECTOR__GLOBAL_ROUTING_SETTINGS + DOT + "routingSecret";
  String INTERLEDGER__CONNECTOR__KEYSTORE = INTERLEDGER__CONNECTOR + DOT + "keystore";
  String INTERLEDGER__CONNECTOR__KEYSTORE__JKS = INTERLEDGER__CONNECTOR__KEYSTORE + DOT + "jks";
  String INTERLEDGER__CONNECTOR__KEYSTORE__JKS__SECRET0_PASSWORD = INTERLEDGER__CONNECTOR__KEYSTORE__JKS + DOT + "secret0_password";
  String INTERLEDGER__CONNECTOR__KEYSTORE__JKS__SECRET0_ALIAS = INTERLEDGER__CONNECTOR__KEYSTORE__JKS + DOT + "secret0_alias";
  String INTERLEDGER__CONNECTOR__KEYSTORE__JKS__PASSWORD = INTERLEDGER__CONNECTOR__KEYSTORE__JKS + DOT + "password";
  String INTERLEDGER__CONNECTOR__KEYSTORE__JKS__FILENAME = INTERLEDGER__CONNECTOR__KEYSTORE__JKS + DOT + "filename";
  String INTERLEDGER__CONNECTOR__KEYSTORE__JKS__ENABLED = INTERLEDGER__CONNECTOR__KEYSTORE__JKS + DOT + "enabled";
  //String NODE_ILP_ADDRESS = INTERLEDGER_CONNECTOR_PREFIX + DOT + "nodeIlpAddress";
  //String GLOBAL_PREFIX = INTERLEDGER_CONNECTOR_PREFIX + DOT + "globalPrefix";
  String ENABLED_PROTOCOLS = INTERLEDGER__CONNECTOR + DOT + "enabledProtocols";
  String DEFAULT_JWT_TOKEN_ISSUER = INTERLEDGER__CONNECTOR + DOT + "defaultJwtTokenIssuer";
  String ADMIN_PASSWORD = INTERLEDGER__CONNECTOR + DOT + "adminPassword";
}
