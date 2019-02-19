package com.sappenin.interledger.ilpv4.connector.packetswitch;

import com.google.common.annotations.VisibleForTesting;
import com.sappenin.interledger.ilpv4.connector.accounts.AccountManager;
import com.sappenin.interledger.ilpv4.connector.fx.ExchangeRateService;
import com.sappenin.interledger.ilpv4.connector.packetswitch.filters.DefaultPacketSwitchFilterChain;
import com.sappenin.interledger.ilpv4.connector.packetswitch.filters.PacketSwitchFilter;
import com.sappenin.interledger.ilpv4.connector.packetswitch.filters.PacketSwitchFilterChain;
import com.sappenin.interledger.ilpv4.connector.routing.PaymentRouter;
import com.sappenin.interledger.ilpv4.connector.routing.Route;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import org.immutables.value.Value;
import org.interledger.connector.accounts.Account;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.link.Link;
import org.interledger.connector.link.LinkSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.javamoney.moneta.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.math.BigInteger;
import java.time.Instant;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * A default implementation of {@link ILPv4PacketSwitch}.
 */
public class DefaultILPv4PacketSwitch implements ILPv4PacketSwitch {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultILPv4PacketSwitch.class);
  private static final String DESTINATION_ADDRESS_IS_UNREACHABLE = "Destination address is unreachable";

  private final Supplier<ConnectorSettings> connectorSettingsSupplier;
  private final PaymentRouter<Route> internalPaymentRouter;
  private final PaymentRouter<Route> externalRoutingService;
  private final ExchangeRateService exchangeRateService;
  private final AccountManager accountManager;
  private final InterledgerAddressUtils addressUtils;

  private final List<PacketSwitchFilter> packetSwitchFilters;

  /**
   * Required-args Constructor.
   */
  public DefaultILPv4PacketSwitch(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final PaymentRouter<Route> internalPaymentRouter,
    final PaymentRouter<Route> externalRoutingService,
    final ExchangeRateService exchangeRateService,
    final AccountManager accountManager,
    final InterledgerAddressUtils addressUtils,
    final List<PacketSwitchFilter> packetSwitchFilters
  ) {
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
    this.internalPaymentRouter = Objects.requireNonNull(internalPaymentRouter);
    this.externalRoutingService = Objects.requireNonNull(externalRoutingService);
    this.exchangeRateService = Objects.requireNonNull(exchangeRateService);
    this.accountManager = Objects.requireNonNull(accountManager);
    this.addressUtils = Objects.requireNonNull(addressUtils);
    this.packetSwitchFilters = Objects.requireNonNull(packetSwitchFilters);
  }

  @Override
  public final Optional<InterledgerResponsePacket> routeData(
    final AccountId sourceAccountId, final InterledgerPreparePacket incomingSourcePreparePacket
  ) {
    Objects.requireNonNull(sourceAccountId);
    Objects.requireNonNull(incomingSourcePreparePacket);

    try {

      // Before packet-forwarding is engaged, this code ensures the incoming account/packet information is eligible
      // to be packet-switched, considering the destination address as well as characteristics of the source account.
      if (
        !addressUtils.isDestinationAllowedFromAccount(sourceAccountId, incomingSourcePreparePacket.getDestination())
      ) {
        // REJECT!
        return Optional.of(InterledgerRejectPacket.builder()
          .code(InterledgerErrorCode.F02_UNREACHABLE)
          .triggeredBy(connectorSettingsSupplier.get().getOperatorAddress())
          .message(DESTINATION_ADDRESS_IS_UNREACHABLE)
          .build());
      } else {
        /////////////////
        // If we get here, then route the packet according to whatever is in the routing table.
        final NextHopInfo nextHopInfo = getNextHopPacket(sourceAccountId, incomingSourcePreparePacket);

        // Create a new FilterChain, and use it to both filter the routeData call, as well as to make the actual call.
        final Link<? extends LinkSettings> link =
          this.accountManager.safeGetAccount(nextHopInfo.nextHopAccountId()).getLink();

        final PacketSwitchFilterChain filterChain = new DefaultPacketSwitchFilterChain(this.packetSwitchFilters, link);
        return filterChain.doFilter(sourceAccountId, incomingSourcePreparePacket);
      }
    } catch (InterledgerProtocolException e) {
      // Any rejections should be caught here, and returned as such....
      return Optional.of(e.getInterledgerRejectPacket());
    }
  }

  /**
   * Construct the <tt>next-hop</tt> ILP prepare packet, meaning a new packet with potentially new pricing, destination,
   * and expiry characterstics. This method also includes the proper "next hop" account that the new packet should be
   * forwarded to in order to continue the Interledger protocol.
   *
   * Given a previous ILP prepare packet (i.e., a {@code sourcePacket}), return the next ILP prepare packet in the
   * chain.
   *
   * @param sourceAccountId The {@link AccountId} of the peer who sent this packet into the Connector. This is typically
   *                        the remote peer-address configured in a link.
   * @param sourcePacket    The {@link InterledgerPreparePacket} that we received from the source address.
   *
   * @return A {@link InterledgerPreparePacket} that can be sent to the next-hop.
   */
  @VisibleForTesting
  protected NextHopInfo getNextHopPacket(
    final AccountId sourceAccountId, final InterledgerPreparePacket sourcePacket
  ) throws RuntimeException {

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
        "Constructing NextHop InterledgerPreparePacket for source: {} from packet: {}",
        sourceAccountId, sourcePacket
      );
    }

    // We first check the External routing table, because in the general case, _most_ packets will traverse this path.
    // If there is no route here, fallback to the internal routing-table. If no route is found there, then reject.
    final InterledgerAddress destinationAddress = sourcePacket.getDestination();

    final Route nextHopRoute = this.externalRoutingService.findBestNexHop(destinationAddress)
      .orElseGet(() -> this.internalPaymentRouter.findBestNexHop(destinationAddress)
        .orElseThrow(() -> new InterledgerProtocolException(
            InterledgerRejectPacket.builder()
              .triggeredBy(connectorSettingsSupplier.get().getOperatorAddress())
              .code(InterledgerErrorCode.F02_UNREACHABLE)
              .message(DESTINATION_ADDRESS_IS_UNREACHABLE)
              .build(),
            String.format("No route found from accountId(`%s`) to destination(`%s`).", sourceAccountId,
              destinationAddress.getValue())
          )
        ));

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Determined next hop: {}", nextHopRoute);
    }

    // TODO: Make this configurable. See JS Connector for details.
    if (sourceAccountId.equals(nextHopRoute.getNextHopAccountId())) {
      throw new InterledgerProtocolException(
        InterledgerRejectPacket.builder()
          .triggeredBy(connectorSettingsSupplier.get().getOperatorAddress())
          .code(InterledgerErrorCode.F02_UNREACHABLE)
          .message(DESTINATION_ADDRESS_IS_UNREACHABLE)
          .build(),
        String.format("Refusing to route payments back to sender. sourceAccount=`%s` destinationAccount=`%s`",
          sourceAccountId, nextHopRoute.getNextHopAccountId())
      );
    }

    final BigInteger nextAmount = this.determineNextAmount(sourceAccountId, sourcePacket);
    return NextHopInfo.builder()
      .nextHopAccountId(nextHopRoute.getNextHopAccountId())
      .nextHopPacket(
        InterledgerPreparePacket.builder()
          .from(sourcePacket)
          .amount(nextAmount)
          .expiresAt(determineDestinationExpiresAt(sourcePacket.getExpiresAt(), sourcePacket.getDestination()))
          .build())
      .build();
  }

  /**
   * Given a source address, determine the exchange-rate and new amount that should be returned in order to create the
   * next packet in the chain.
   *
   * @param sourceAccountId
   * @param sourcePacket
   *
   * @return A BigInteger is the correct units for the source account.
   */
  @VisibleForTesting
  protected BigInteger determineNextAmount(
    final AccountId sourceAccountId, final InterledgerPreparePacket sourcePacket
  ) {
    Objects.requireNonNull(sourcePacket);

    if (!this.addressUtils.isExternalForwardingAllowed(sourcePacket.getDestination())) {
      return sourcePacket.getAmount();
    } else {

      final CurrencyUnit sourceCurrencyUnit = this.accountManager
        .getAccount(sourceAccountId)
        .map(Account::getAccountSettings)
        .map(AccountSettings::getAssetCode)
        .map(Monetary::getCurrency)
        .orElseThrow(
          () -> new RuntimeException(String.format("No Source Account for AccountId: `%s`", sourceAccountId)));
      final MonetaryAmount sourceAmount = Money.of(sourcePacket.getAmount(), sourceCurrencyUnit);
      return this.exchangeRateService.convert(sourceAmount, sourceCurrencyUnit).getNumber()
        .numberValue(BigInteger.class);
    }
  }

  @VisibleForTesting
  protected Instant determineDestinationExpiresAt(
    final Instant sourceExpiry, final InterledgerAddress destinationAddress
  ) {
    Objects.requireNonNull(sourceExpiry);

    if (!this.addressUtils.isExternalForwardingAllowed(destinationAddress)) {
      // If this packet is not going to be externally-forwarded, then we don't need to wait for a downstream
      // Connector to process it, so we can leave the expiry unchanged.
      return sourceExpiry;
    } else {
      final Instant nowTime = Instant.now();
      if (sourceExpiry.isBefore(nowTime)) {
        throw new InterledgerProtocolException(
          InterledgerRejectPacket.builder()
            .triggeredBy(connectorSettingsSupplier.get().getOperatorAddress())
            .code(InterledgerErrorCode.R02_INSUFFICIENT_TIMEOUT)
            .message(String.format(
              "Source transfer has already expired. sourceExpiry: {%s}, currentTime: {%s}", sourceExpiry, nowTime))
            .build()
        );
      }

      // We will set the next transfer's expiry based on the source expiry and our minMessageWindow, but cap it at our
      // maxHoldTime.

      final int minMessageWindow = 5000; //TODO: Enable this --> connectorSettingsSupplier.get().getMinMessageWindow();
      final int maxHoldTime = 5000; //TODO: Enable this --> connectorSettingsSupplier.get().getMaxHoldTime();

      // The expiry of the packet, reduced by the minMessageWindow, which is "the minimum time the connector wants to
      // budget for getting a message to the accounts its trading on. In milliseconds."
      final Instant adjustedSourceExpiryInstant = sourceExpiry.minusMillis(minMessageWindow);

      // The point in time after which this Connector will not wait around for a fulfillment.
      final Instant maxHoldInstant = nowTime.plusMillis(maxHoldTime);

      final Instant destinationExpiryTime = lesser(adjustedSourceExpiryInstant, maxHoldInstant);
      if (destinationExpiryTime.minusMillis(minMessageWindow).isBefore(nowTime)) {
        throw new InterledgerProtocolException(
          InterledgerRejectPacket.builder()
            .triggeredBy(connectorSettingsSupplier.get().getOperatorAddress())
            .code(InterledgerErrorCode.R02_INSUFFICIENT_TIMEOUT)
            .message(String.format(
              "Source transfer expires too soon to complete payment. SourceExpiry: {%s}, " +
                "RequiredSourceExpiry: {%s}, CurrentTime: {%s}",
              sourceExpiry,
              nowTime.plusMillis(minMessageWindow * 2),
              nowTime))
            .build()
        );
      } else {
        return destinationExpiryTime;
      }
    }
  }

  @VisibleForTesting
  protected Instant lesser(final Instant first, final Instant second) {
    if (first.isBefore(second)) {
      return first;
    } else {
      return second;
    }
  }

  @Override
  public boolean add(final PacketSwitchFilter packetSwitchFilter) {
    Objects.requireNonNull(packetSwitchFilter);
    return this.packetSwitchFilters.add(packetSwitchFilter);
  }

  @Override
  public void addFirst(final PacketSwitchFilter packetSwitchFilter) {
    Objects.requireNonNull(packetSwitchFilter);
    this.packetSwitchFilters.add(0, packetSwitchFilter);
  }

  @Override
  public PacketSwitchFilterChain getSendDataFilterChain() {
    return null;
  }

  @Override
  public PaymentRouter<Route> getExternalPaymentRouter() {
    return this.externalRoutingService;
  }

  @Override
  public PaymentRouter<Route> getInternalPaymentRouter() {
    return this.internalPaymentRouter;
  }

  /**
   * A container that holds the next-hop packet (with a final destination) as well as the address of the next-hop
   * account to send the packet to.
   */
  @Value.Immutable
  interface NextHopInfo {

    static ImmutableNextHopInfo.Builder builder() {
      return ImmutableNextHopInfo.builder();
    }

    /**
     * The {@link InterledgerAddress} of the next-hop account to send a prepare packet to.
     */
    AccountId nextHopAccountId();

    /**
     * The packet to send to the next hop.
     */
    InterledgerPreparePacket nextHopPacket();

  }
}