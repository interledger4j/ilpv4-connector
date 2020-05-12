package org.interledger.connector.localsend;

import org.interledger.connector.packetswitch.ILPv4PacketSwitch;
import org.interledger.core.InterledgerAddress;
import org.interledger.link.LinkFactory;
import org.interledger.link.LinkId;
import org.interledger.link.LinkSettings;
import org.interledger.link.LinkType;
import org.interledger.link.exceptions.LinkException;

import com.google.common.base.Preconditions;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * An implementation of {@link LinkFactory} for constructing instances of {@link LocalPacketSwitchLinkSettings}.
 */
public class LocalPacketSwitchLinkFactory implements LinkFactory {

  private final ILPv4PacketSwitch ilPv4PacketSwitch;

  /**
   * Required-args Constructor.
   * @param ilPv4PacketSwitch
   */
  public LocalPacketSwitchLinkFactory(ILPv4PacketSwitch ilPv4PacketSwitch) {
    this.ilPv4PacketSwitch = Objects
      .requireNonNull(ilPv4PacketSwitch, "ilPv4PacketSwitch must not be null");
  }


  @Override
  public LocalPacketSwitchLink constructLink(
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
      LocalPacketSwitchLinkSettings.class.isAssignableFrom(linkSettings.getClass()),
      "Constructing an instance of LocalPacketSwitchLink requires an instance of LocalPacketSwitchLinkSettings"
    );

    LocalPacketSwitchLinkSettings localPacketSwitchLinkSettings = (LocalPacketSwitchLinkSettings) linkSettings;
    return new LocalPacketSwitchLink(
      ilPv4PacketSwitch,
      operatorAddressSupplier,
      localPacketSwitchLinkSettings);
  }

  @Override
  public boolean supports(LinkType linkType) {
    return LocalPacketSwitchLink.LINK_TYPE.equals(linkType);
  }

}
