package org.interledger.connector.packetswitch;

import org.interledger.connector.ConnectorExceptionHandler;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.caching.AccountSettingsLoadingCache;
import org.interledger.connector.links.LinkManager;
import org.interledger.connector.links.NextHopPacketMapper;
import org.interledger.connector.links.filters.LinkFilter;
import org.interledger.connector.packetswitch.filters.DefaultPacketSwitchFilterChain;
import org.interledger.connector.packetswitch.filters.PacketSwitchFilter;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.LinkId;
import org.interledger.link.PacketRejector;

import com.google.common.eventbus.EventBus;

import java.util.List;
import java.util.Objects;

/**
 * A default implementation of {@link ILPv4PacketSwitch}.
 */
public class DefaultILPv4PacketSwitch implements ILPv4PacketSwitch {

  private final List<PacketSwitchFilter> packetSwitchFilters;
  private final List<LinkFilter> linkFilters;
  private final LinkManager linkManager;
  private final NextHopPacketMapper nextHopPacketMapper;
  private final ConnectorExceptionHandler connectorExceptionHandler;
  private final PacketRejector packetRejector;

  // Loading from the Database is somewhat expensive, so we don't want to do this on every packet processed for a
  // given account. Instead, for higher performance, we only load account settings once per period, and otherwise
  // rely upon AccountSettings found in this cache.
  private final AccountSettingsLoadingCache accountSettingsLoadingCache;

  private final EventBus eventBus;

  public DefaultILPv4PacketSwitch(
    final List<PacketSwitchFilter> packetSwitchFilters,
    final List<LinkFilter> linkFilters,
    final LinkManager linkManager,
    final NextHopPacketMapper nextHopPacketMapper,
    final ConnectorExceptionHandler connectorExceptionHandler,
    final PacketRejector packetRejector,
    final AccountSettingsLoadingCache accountSettingsLoadingCache,
    final EventBus eventBus
  ) {
    this.packetSwitchFilters = Objects.requireNonNull(packetSwitchFilters);
    this.linkFilters = Objects.requireNonNull(linkFilters);
    this.linkManager = Objects.requireNonNull(linkManager);
    this.nextHopPacketMapper = Objects.requireNonNull(nextHopPacketMapper);
    this.connectorExceptionHandler = Objects.requireNonNull(connectorExceptionHandler);
    this.packetRejector = Objects.requireNonNull(packetRejector);
    this.accountSettingsLoadingCache = Objects.requireNonNull(accountSettingsLoadingCache);
    this.eventBus = eventBus;
  }

  /**
   * Perform the logic of packet switching by first applying one or more instance of {@link PacketSwitchFilter}, and
   * then once an appropriate outbound link has been chosen, apply one or more instances of {@link LinkFilter} and then
   * send the {@code incomingSourcePreparePacket} out on the chosen outbound link.
   *
   * @param sourceAccountId             An {@link AccountId} for the account that received the {@code
   *                                    incomingSourcePreparePacket}.
   * @param incomingSourcePreparePacket The packet received from the inbound/source account.
   *
   * @return An {@link InterledgerResponsePacket} as received from the outbound link.
   */
  public final InterledgerResponsePacket switchPacket(
    final AccountId sourceAccountId, final InterledgerPreparePacket incomingSourcePreparePacket
  ) {
    Objects.requireNonNull(sourceAccountId);
    Objects.requireNonNull(incomingSourcePreparePacket);

    // The value stored in the Cache is the AccountSettings converted from the entity so we don't have to convert
    // on every ILPv4 packet switch.
    return this.accountSettingsLoadingCache.getAccount(sourceAccountId)
      .map(accountSettings -> {
        try {
          return new DefaultPacketSwitchFilterChain(
            packetSwitchFilters,
            linkFilters,
            linkManager,
            nextHopPacketMapper,
            accountSettingsLoadingCache, // Necessary to load the 'next-hop' account.
            eventBus).doFilter(accountSettings, incomingSourcePreparePacket);

        } catch (Exception e) {
          // Any rejections should be caught here, and returned as such....
          return this.connectorExceptionHandler.handleException(sourceAccountId, incomingSourcePreparePacket, e);
        }
      })
      .orElseThrow(() -> {
        // REJECT due to no account...
        return new InterledgerProtocolException(
          packetRejector.reject(
            LinkId.of(sourceAccountId.value()),
            incomingSourcePreparePacket,
            InterledgerErrorCode.T00_INTERNAL_ERROR,
            String.format("No Account found: `%s`", sourceAccountId))
        );
      });
  }
}
