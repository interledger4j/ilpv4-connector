package com.sappenin.interledger.ilpv4.connector.ccp.codecs;

import com.sappenin.interledger.ilpv4.connector.ccp.*;
import org.interledger.encoding.asn.framework.CodecContext;

import java.util.Objects;
import java.util.UUID;

/**
 * Helper class to register all codecs for the CCP protocol.
 */
public class CcpCodecs {

  /**
   * Register the CCP protocol codecs into the provided context.
   *
   * @param context the context to register the codecs into
   */
  public static CodecContext register(final CodecContext context) {
    Objects.requireNonNull(context);

    // Register the codec to be tested...
    return context
      .register(UUID.class, AsnUuidCodec::new)
      .register(CcpRouteControlRequest.class, AsnCcpRouteControlRequestCodec::new)
      .register(CcpRouteControlResponse.class, AsnCcpRouteControlResponseCodec::new)
      .register(CcpRouteUpdateRequest.class, AsnCcpRouteUpdateRequestCodec::new)
      .register(CcpRoutePathPart.class, AsnCcpRoutePathPartCodec::new)
      .register(CcpRouteProperty.class, AsnCcpRoutePropertyCodec::new);
  }
}
