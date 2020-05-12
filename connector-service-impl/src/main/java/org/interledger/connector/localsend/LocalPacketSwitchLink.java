package org.interledger.connector.localsend;

import static org.slf4j.LoggerFactory.getLogger;

import org.interledger.connector.packetswitch.ILPv4PacketSwitch;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.AbstractLink;
import org.interledger.link.LinkType;
import org.interledger.stream.StreamPacket;

import org.slf4j.Logger;

import java.util.function.Supplier;

public class LocalPacketSwitchLink extends AbstractLink<LocalPacketSwitchLinkSettings> {

  public static final String LINK_TYPE_STRING = "LOCAL_PACKET_SWITCH_LINK";
  public static final LinkType LINK_TYPE = LinkType.of(LINK_TYPE_STRING);

  private static final Logger LOGGER = getLogger(LocalPacketSwitchLink.class);

  private final ILPv4PacketSwitch packetSwitch;
  private final LocalPacketSwitchLinkSettings linkSettings;

  public LocalPacketSwitchLink(ILPv4PacketSwitch packetSwitch,
                               Supplier<InterledgerAddress> operatorAddressSupplier,
                               LocalPacketSwitchLinkSettings linkSettings) {
    super(operatorAddressSupplier, linkSettings);
    this.packetSwitch = packetSwitch;
    this.linkSettings = linkSettings;
  }

  @Override
  public InterledgerResponsePacket sendPacket(InterledgerPreparePacket interledgerPreparePacket) {
    return packetSwitch.switchPacket(linkSettings.accountId(), decoratePreparePacket(interledgerPreparePacket));
  }

  private InterledgerPreparePacket.AbstractInterledgerPreparePacket decoratePreparePacket(
    InterledgerPreparePacket interledgerPreparePacket) {
    return InterledgerPreparePacket.builder()
      .from(interledgerPreparePacket)
      .typedData(
        interledgerPreparePacket.typedData().map(
          typedData -> StreamPacketWithSharedSecret.builder().from((StreamPacket) typedData)
            .sharedSecret(linkSettings.sharedSecret())
            .build()
        )
      )
      .build();
  }

}
