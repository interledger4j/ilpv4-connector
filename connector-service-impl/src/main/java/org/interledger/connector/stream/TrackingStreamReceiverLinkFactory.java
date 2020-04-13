package org.interledger.connector.stream;

import org.interledger.core.InterledgerAddress;
import org.interledger.link.Link;
import org.interledger.link.LinkFactory;
import org.interledger.link.LinkId;
import org.interledger.link.LinkSettings;
import org.interledger.link.LinkType;
import org.interledger.link.exceptions.LinkException;

import com.google.common.base.Preconditions;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * An implementation of {@link LinkFactory} for constructing instances of {@link TrackingStreamReceiverLinkSettings}.
 */
public class TrackingStreamReceiverLinkFactory implements LinkFactory {

  private final TrackingStreamReceiverSupplier trackingStreamReceiverSupplier;

  /**
   * Required-args Constructor.
   *
   * @param trackingStreamReceiverSupplier A supplier for a {@link TrackingStreamReceiver}
   */
  public TrackingStreamReceiverLinkFactory(final TrackingStreamReceiverSupplier trackingStreamReceiverSupplier) {
    this.trackingStreamReceiverSupplier = Objects
      .requireNonNull(trackingStreamReceiverSupplier, "trackingStreamReceiverSupplier must not be null");
  }


  @Override
  public Link<?> constructLink(
    final Supplier<InterledgerAddress> operatorAddressSupplier, final LinkSettings linkSettings
  ) {
    Objects.requireNonNull(operatorAddressSupplier, "operatorAddressSupplier must not be null");
    Objects.requireNonNull(linkSettings, "linkSettings must not be null");

    if (!this.supports(linkSettings.getLinkType())) {
      throw new LinkException(
        String.format("LinkType not supported by this factory. linkType=%s", linkSettings.getLinkType()),
        LinkId.of("n/a")
      );
    }

    Preconditions.checkArgument(
      TrackingStreamReceiverLinkSettings.class.isAssignableFrom(linkSettings.getClass()),
      "Constructing an instance of TrackingStreamReceiverLink requires an instance of TrackingStreamReceiverLinkSettings"
    );

    TrackingStreamReceiverLinkSettings trackingReceiverLinkSettings = (TrackingStreamReceiverLinkSettings) linkSettings;
    return new TrackingStreamReceiverLink(
      operatorAddressSupplier,
      trackingReceiverLinkSettings,
      trackingStreamReceiverSupplier.get(trackingReceiverLinkSettings)
    );
  }

  @Override
  public boolean supports(LinkType linkType) {
    return TrackingStreamReceiverLink.LINK_TYPE.equals(linkType);
  }

}
