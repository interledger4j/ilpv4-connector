package com.sappenin.ilpv4.plugins.btp.subprotocols;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * Holds BTP Sub-protocol filters for this Connector.
 *
 * @deprecated Replace with the quilt variant.
 */
@Deprecated
public class BtpSubProtocolHandlerRegistry {

  public static final String BTP_SUB_PROTOCOL_ILP = "ilp";
  public static final String BTP_SUB_PROTOCOL_AUTH = "auth";
  public static final String BTP_SUB_PROTOCOL_AUTH_TOKEN = "auth_token";
  public static final String BTP_SUB_PROTOCOL_AUTH_USERNAME = "auth_username";

  private Map<String, BtpSubProtocolHandler> handlers = new HashMap<>();

  public Optional<BtpSubProtocolHandler> getHandler(final String subProtocolName) {
    Objects.requireNonNull(subProtocolName, "subProtocolName must not be null!");
    return Optional.ofNullable(handlers.get(subProtocolName));
  }

  public BtpSubProtocolHandler putHandler(final String subProtocolName, final BtpSubProtocolHandler handler) {
    Objects.requireNonNull(subProtocolName, "subProtocolName must not be null!");
    Objects.requireNonNull(handler, "handler must not be null!");
    return handlers.put(subProtocolName, handler);
  }
}
