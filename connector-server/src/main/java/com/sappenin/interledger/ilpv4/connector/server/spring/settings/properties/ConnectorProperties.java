package com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties;

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

  String ILPV4 = "ilpv4";
  String CONNECTOR = "connector";
  String ILPV4_CONNECTOR_PREFIX = ILPV4 + DOT + CONNECTOR;

  String ADMIN_PASSWORD = ILPV4_CONNECTOR_PREFIX + DOT + "adminPassword";
  String DEFAULT_JWT_TOKEN_ISSUER = ILPV4_CONNECTOR_PREFIX + DOT + "defaultJwtTokenIssuer";
  //String NODE_ILP_ADDRESS = ILPV4_CONNECTOR_PREFIX + DOT + "nodeIlpAddress";
  //String GLOBAL_PREFIX = ILPV4_CONNECTOR_PREFIX + DOT + "globalPrefix";
  String ENABLED_PROTOCOLS = ILPV4_CONNECTOR_PREFIX + DOT + "enabledProtocols";

  /////////////////
  // Spring Data Properties
  /////////////////

  //String SPRING__DATASOURCE__URL = "spring.datasource.url";
  // Private Helper properties


  /////////////////
  // ILP Connector Properties
  /////////////////

  String ILPV4__CONNECTOR__KEYSTORE = ILPV4_CONNECTOR_PREFIX + DOT + "keystore";
  String ILPV4__CONNECTOR__KEYSTORE__JKS = ILPV4__CONNECTOR__KEYSTORE + DOT + "jks";
  String ILPV4__CONNECTOR__KEYSTORE__JKS__ENABLED = ILPV4__CONNECTOR__KEYSTORE__JKS + DOT + "enabled";
  String ILPV4__CONNECTOR__KEYSTORE__JKS__FILENAME = ILPV4__CONNECTOR__KEYSTORE__JKS + DOT + "filename";
  String ILPV4__CONNECTOR__KEYSTORE__JKS__PASSWORD = ILPV4__CONNECTOR__KEYSTORE__JKS + DOT + "password";
  String ILPV4__CONNECTOR__KEYSTORE__JKS__SECRET0_ALIAS = ILPV4__CONNECTOR__KEYSTORE__JKS + DOT + "secret0_alias";
  String ILPV4__CONNECTOR__KEYSTORE__JKS__SECRET0_PASSWORD = ILPV4__CONNECTOR__KEYSTORE__JKS + DOT + "secret0_password";
  //String ILPV4__CONNECTOR__KEYSTORE__JKS__FILENAME = ILPV4__CONNECTOR__KEYSTORE__JKS + DOT + "";

  String ILPV4__CONNECTOR__GLOBAL_ROUTING_SETTINGS = ILPV4_CONNECTOR_PREFIX + DOT + "globalRoutingSettings";
  String ILPV4__CONNECTOR__GLOBAL_ROUTING_SETTINGS__ROUTING_SECRET = ILPV4__CONNECTOR__GLOBAL_ROUTING_SETTINGS + DOT +
    "routingSecret";

  /**
   * @deprecated This is no longer necessary because the WebSocket server config doens't engage unless BTP is enabled.
   */
  @Deprecated
  String WEBSOCKET_SERVER_ENABLED = ILPV4_CONNECTOR_PREFIX + "websocketServerEnabled";

}
