package org.interledger.connector.ccp.codecs;

import org.interledger.connector.ccp.CcpRouteControlRequest;
import org.interledger.connector.ccp.CcpRouteControlResponse;
import org.interledger.connector.ccp.CcpRoutePathPart;
import org.interledger.connector.ccp.CcpRouteProperty;
import org.interledger.connector.ccp.CcpRouteUpdateRequest;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.encoding.asn.framework.CodecContextFactory;

import java.util.Objects;
import java.util.UUID;

/**
 * A factory class for constructing a CodecContext that can read and write Interledger Connector-to-Connector (CCP)
 * Routing Protocol objects using ASN.1 OER encoding.
 *
 * @see "TBD"
 */
public class CcpCodecContextFactory {

  /**
   * Create an instance of {@link CodecContext} that encodes and decodes CCP packets using ASN.1 OER encoding.
   *
   * @return A new instance of {@link CodecContext}.
   */
  public static CodecContext oer() {
    final CodecContext ccpCodecContext = CodecContextFactory.oer();
    return register(ccpCodecContext);
  }

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
