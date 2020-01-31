package org.interledger.connector.links;

import org.interledger.codecs.ildcp.IldcpUtils;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.ildcp.IldcpFetcher;
import org.interledger.ildcp.IldcpRequestPacket;
import org.interledger.link.Link;

import java.util.Objects;

/**
 * Default implementation that fetches ILDCP by sending a {@link IldcpRequestPacket} over the provided {@link Link}.
 */
public class DefaultIldcpFetcherFactory implements IldcpFetcherFactory {

  @Override
  public IldcpFetcher construct(Link link) {
    return ildcpRequest -> {
      Objects.requireNonNull(ildcpRequest);

      final IldcpRequestPacket ildcpRequestPacket = IldcpRequestPacket.builder().build();
      final InterledgerPreparePacket preparePacket =
        InterledgerPreparePacket.builder().from(ildcpRequestPacket).build();

      // Fetch the IL-DCP response using the Link.
      return link.sendPacket(preparePacket)
        .map(
          // If FulfillPacket...
          IldcpUtils::toIldcpResponse,
          // If Reject Packet...
          (interledgerRejectPacket) -> {
            throw new RuntimeException(
              String.format("IL-DCP negotiation failed! Reject: %s", interledgerRejectPacket)
            );
          }
        );
    };
  }

}
