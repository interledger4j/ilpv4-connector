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

  /**
   * @deprecated BTP Enable/disable should be determined on whether there's an account configured that uses BTP. the
   * only thing needed here is whether to turn on or off the WebSocket server, so perhaps that's another gate. We should
   * investigate the ConditionalOnBean?
   */
  @Deprecated
  public static final String BTP_ENABLED = "btp.enabled";

  private static final String DOT = ".";
  private static final String SPRING_PROFILES_ACTIVE = "spring.profiles.active";
  private static final String ILPV4 = "ilpv4";

  /////////////////
  // ILP Properties
  /////////////////

  // Private Helper properties
  private static final String CONNECTOR = "connector";
  private static final String ILPV4_CONNECTOR_PREFIX = ILPV4 + DOT + CONNECTOR + DOT;

  public static final String NODE_ILP_ADDRESS = ILPV4_CONNECTOR_PREFIX + "nodeIlpAddress";
  public static final String GLOBAL_PREFIX = ILPV4_CONNECTOR_PREFIX + "globalPrefix";
  public static final String WEBSOCKET_SERVER_ENABLED = ILPV4_CONNECTOR_PREFIX + "websocketServerEnabled";

  /**
   * Prevent instantiation.
   */
  private ConnectorProperties() {

  }

}
