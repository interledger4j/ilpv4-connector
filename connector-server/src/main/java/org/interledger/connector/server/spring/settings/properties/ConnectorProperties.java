package org.interledger.connector.server.spring.settings.properties;

/**
 * <p>A placeholder for statically defined Connector property names.</p>
 *
 * <pre>
 *   <ul>
 *     <li>Set via JVM system property: -Dfoo=bar</li>
 *     <li>Via OS Environment: `export foo=bar`</li>
 *     <li>Via Maven Property.</li>
 *   </ul>
 * </pre>
 *
 * @see "https://docs.spring.io/spring-boot/docs/current/reference/html/boot-features-external-config.html"
 */
public interface ConnectorProperties {

  /**
   * TODO: BTP Enable/disable should be determined on whether there's an account configured that uses BTP. the only
   * thing needed here is whether to turn on or off the WebSocket server, so perhaps that's another gate. We should
   * investigate the ConditionalOnBean?
   */
  String BTP_ENABLED = "btp.enabled";

  String BLAST_ENABLED = "blastEnabled";
  String BLAST_INCOMING_AUTH_CREDENTIAL = "blast.incoming.auth_credential";
  String DOT = ".";
  String SPRING_PROFILES_ACTIVE = "spring.profiles.active";

  String INTERLEDGER = "interledger";
  String CONNECTOR = "connector";
  String INTERLEDGER_CONNECTOR_PREFIX = INTERLEDGER + DOT + CONNECTOR;

  String ADMIN_PASSWORD = INTERLEDGER_CONNECTOR_PREFIX + DOT + "adminPassword";
  String DEFAULT_JWT_TOKEN_ISSUER = INTERLEDGER_CONNECTOR_PREFIX + DOT + "defaultJwtTokenIssuer";
  //String NODE_ILP_ADDRESS = INTERLEDGER_CONNECTOR_PREFIX + DOT + "nodeIlpAddress";
  //String GLOBAL_PREFIX = INTERLEDGER_CONNECTOR_PREFIX + DOT + "globalPrefix";
  String ENABLED_PROTOCOLS = INTERLEDGER_CONNECTOR_PREFIX + DOT + "enabledProtocols";

  /////////////////
  // Spring Data Properties
  /////////////////

  //String SPRING__DATASOURCE__URL = "spring.datasource.url";
  // Private Helper properties


  /////////////////
  // ILP Connector Properties
  /////////////////

  String INTERLEDGER__CONNECTOR__KEYSTORE = INTERLEDGER_CONNECTOR_PREFIX + DOT + "keystore";
  String INTERLEDGER__CONNECTOR__KEYSTORE__JKS = INTERLEDGER__CONNECTOR__KEYSTORE + DOT + "jks";
  String INTERLEDGER__CONNECTOR__KEYSTORE__JKS__ENABLED = INTERLEDGER__CONNECTOR__KEYSTORE__JKS + DOT + "enabled";
  String INTERLEDGER__CONNECTOR__KEYSTORE__JKS__FILENAME = INTERLEDGER__CONNECTOR__KEYSTORE__JKS + DOT + "filename";
  String INTERLEDGER__CONNECTOR__KEYSTORE__JKS__PASSWORD = INTERLEDGER__CONNECTOR__KEYSTORE__JKS + DOT + "password";
  String INTERLEDGER__CONNECTOR__KEYSTORE__JKS__SECRET0_ALIAS = INTERLEDGER__CONNECTOR__KEYSTORE__JKS + DOT + "secret0_alias";
  String INTERLEDGER__CONNECTOR__KEYSTORE__JKS__SECRET0_PASSWORD = INTERLEDGER__CONNECTOR__KEYSTORE__JKS + DOT + "secret0_password";
  //String INTERLEDGER__CONNECTOR__KEYSTORE__JKS__FILENAME = INTERLEDGER__CONNECTOR__KEYSTORE__JKS + DOT + "";

  String INTERLEDGER__CONNECTOR__GLOBAL_ROUTING_SETTINGS = INTERLEDGER_CONNECTOR_PREFIX + DOT + "globalRoutingSettings";
  String INTERLEDGER__CONNECTOR__GLOBAL_ROUTING_SETTINGS__ROUTING_SECRET = INTERLEDGER__CONNECTOR__GLOBAL_ROUTING_SETTINGS + DOT +
    "routingSecret";

  /**
   * @deprecated This is no longer necessary because the WebSocket server config doens't engage unless BTP is enabled.
   */
  @Deprecated
  String WEBSOCKET_SERVER_ENABLED = INTERLEDGER_CONNECTOR_PREFIX + "websocketServerEnabled";

}
