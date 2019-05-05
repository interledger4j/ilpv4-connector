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
public class ConnectorProperties {

  public static final String ILPV4_ = "ilpv4.connector.admin_password";
  /**
   * TODO: BTP Enable/disable should be determined on whether there's an account configured that uses BTP. the only
   * thing needed here is whether to turn on or off the WebSocket server, so perhaps that's another gate. We should
   * investigate the ConditionalOnBean?
   */
  public static final String BTP_ENABLED = "btp.enabled";

  public static final String BLAST_ENABLED = "blastEnabled";
  public static final String BLAST_INCOMING_AUTH_CREDENTIAL = "blast.incoming.auth_credential";
  public static final String DOT = ".";
  public static final String SPRING_PROFILES_ACTIVE = "spring.profiles.active";

  /////////////////
  // ILP Properties
  /////////////////
  private static final String ILPV4 = "ilpv4";

  // Private Helper properties
  private static final String CONNECTOR = "connector";
  private static final String ILPV4_CONNECTOR_PREFIX = ILPV4 + DOT + CONNECTOR;

  public static final String ADMIN_PASSWORD = ILPV4_CONNECTOR_PREFIX + DOT + "adminPassword";
  public static final String DEFAULT_JWT_TOKEN_ISSUER = ILPV4_CONNECTOR_PREFIX + DOT + "defaultJwtTokenIssuer";
  public static final String NODE_ILP_ADDRESS = ILPV4_CONNECTOR_PREFIX + DOT + "nodeIlpAddress";
  public static final String GLOBAL_PREFIX = ILPV4_CONNECTOR_PREFIX + DOT + "globalPrefix";
  public static final String ENABLED_PROTOCOLS = ILPV4_CONNECTOR_PREFIX + DOT + "enabledProtocols";

  /**
   * @deprecated This is no longer necessary because the WebSocket server config doens't engage unless BTP is enabled.
   */
  @Deprecated
  public static final String WEBSOCKET_SERVER_ENABLED = ILPV4_CONNECTOR_PREFIX + "websocketServerEnabled";

  /**
   * Prevent instantiation.
   */
  private ConnectorProperties() {

  }

}
