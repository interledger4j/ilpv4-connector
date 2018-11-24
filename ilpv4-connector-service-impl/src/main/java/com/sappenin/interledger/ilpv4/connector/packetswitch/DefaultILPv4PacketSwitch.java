package com.sappenin.interledger.ilpv4.connector.packetswitch;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.sappenin.interledger.ilpv4.connector.fx.ExchangeRateService;
import com.sappenin.interledger.ilpv4.connector.model.settings.ConnectorSettings;
import com.sappenin.interledger.ilpv4.connector.packetswitch.filters.DefaultSendDataFilterChain;
import com.sappenin.interledger.ilpv4.connector.packetswitch.filters.SendDataFilter;
import com.sappenin.interledger.ilpv4.connector.packetswitch.filters.SendDataFilterChain;
import com.sappenin.interledger.ilpv4.connector.routing.PaymentRouter;
import com.sappenin.interledger.ilpv4.connector.routing.Route;
import org.immutables.value.Value;
import org.interledger.core.*;
import com.sappenin.interledger.ilpv4.connector.accounts.AccountManager;
import com.sappenin.interledger.ilpv4.connector.model.settings.AccountSettings;
import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.exceptions.PluginNotFoundException;
import org.interledger.plugin.lpiv2.settings.PluginSettings;
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
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

/**
 * A default implementation of {@link ILPv4PacketSwitch}.
 */
public class DefaultILPv4PacketSwitch implements ILPv4PacketSwitch {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultILPv4PacketSwitch.class);

  private final Supplier<ConnectorSettings> connectorSettingsSupplier;
  private final PaymentRouter<Route> paymentRouter;
  private final ExchangeRateService exchangeRateService;
  private final AccountManager accountManager;

  private final List<SendDataFilter> sendDataFilters;

  public DefaultILPv4PacketSwitch(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final PaymentRouter<Route> paymentRouter,
    final ExchangeRateService exchangeRateService,
    final AccountManager accountManager
  ) {
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
    this.paymentRouter = Objects.requireNonNull(paymentRouter);
    this.exchangeRateService = Objects.requireNonNull(exchangeRateService);
    this.accountManager = Objects.requireNonNull(accountManager);

    this.sendDataFilters = Lists.newArrayList();
  }

  @Override
  public final CompletableFuture<Optional<InterledgerResponsePacket>> sendData(
    final InterledgerAddress sourceAccountAddress, final InterledgerPreparePacket sourcePreparePacket
  ) {

    Objects.requireNonNull(sourceAccountAddress);
    Objects.requireNonNull(sourcePreparePacket);

    //    This logic should move to 1 or more Packet filters so that this method can apply each filter?
    //    Or, we can assume that the Fabric always does this, and then applies any filters before hand? The problem is that it's
    //  difficult to create the notion of a pre and post filter. So, either we do that, or we should move all of this into its
    //   own packetfilter. Look at ServletFilter for an exmaple...maybe we just have pre and post filters...

    //    if (sourcePreparePacket.getDestination().startsWith(DefaultILPv4Connector.PEER_PROTOCOL_PREFIX)) {
    //      // TODO: FINISH!
    //      throw new RuntimeException("Peer packets should be routed to a different switch!");
    //      //return this.peerProtocolController.handle(packet, sourceAccount, {parsedPacket})
    //    } else if (sourcePreparePacket.getDestination().equals(connectorSettings.getIlpAddress())) {
    //      final InterledgerPreparePacket returnPacket =
    //        this.echoController.handleIncomingData(sourceAccountAddress, sourcePreparePacket);
    //      // Send the echo payment....
    //      this.sendData(sourceAccountAddress, returnPacket);
    //      // Fulfill the original payment...
    //      return CompletableFuture.completedFuture(Optional.of(
    //        InterledgerFulfillPacket.builder()
    //          .fulfillment(ECHO_FULFILLMENT)
    //          .build()
    //        )
    //      );
    //    } else {
    final NextHopInfo nextHopInfo = getNextHopPacket(sourceAccountAddress, sourcePreparePacket);

    // TODO: Make this configurable. See JS Connector for details.
    // TODO: Move this to the routing layer. If the router is configured to not return a NextHop like this, then
    // this should never happen, and can be removed.
    if (sourceAccountAddress.equals(nextHopInfo.nextHopAccountAddress())) {
      throw new InterledgerProtocolException(
        InterledgerRejectPacket.builder()
          .code(InterledgerErrorCode.T01_LEDGER_UNREACHABLE)
          .triggeredBy(connectorSettingsSupplier.get().getIlpAddress())
          .message(
            String.format("Refusing to route payments back to sender. sourceAccount=`%s` destinationAccount=`%s`",
              sourceAccountAddress, nextHopInfo.nextHopAccountAddress()))
          .build()
      );
    }

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
        "Sending outbound ILP Prepare. destination={} packet={}", nextHopInfo.nextHopAccountAddress(),
        nextHopInfo.nextHopPacket()
      );
    }

    // Create a new FilterChain, and use it to both filter the sendData call, as well as to make the actual call.
    final Plugin<? extends PluginSettings> plugin =
      this.accountManager.getPluginManager()
        .getPlugin(nextHopInfo.nextHopAccountAddress())
        .orElseThrow(() -> new PluginNotFoundException(nextHopInfo.nextHopAccountAddress()));

    final SendDataFilterChain filterChain = new DefaultSendDataFilterChain(this.sendDataFilters, plugin);

    return filterChain.doFilter(sourceAccountAddress, sourcePreparePacket);

    // TODO: Add Logging Filter...
    // TODO: Add ExchangeRate Update filter...

    // Throws an exception if the lpi2 cannot be found...
    //      return this.accountManager
    //        .getOrCreatePlugin(nextHopInfo.nextHopAccountAddress())
    //        .sendData(nextHopInfo.nextHopPacket())
    //        .thenApplyAsync((result) -> {
    //          // Log the fulfillment...
    //          if (LOGGER.isDebugEnabled()) {
    //            LOGGER.debug(
    //              "Received fulfillment: {}", result
    //            );
    //          }
    //          return result;
    //        })
    //        .thenApplyAsync((result) -> {
    //          // Log statistics in the ExchangeRateService...
    //          this.exchangeRateService.logPaymentStats(
    //            ImmutableUpdateRatePaymentParams.builder()
    //              .sourceAccountAddress(sourceAccountAddress)
    //              .sourceAmount(sourcePreparePacket.getAmount())
    //              .destinationAccountAddress(nextHopInfo.nextHopAccountAddress())
    //              .destinationAmount(nextHopInfo.nextHopPacket().getAmount())
    //              .build()
    //          );
    //          return result;
    //        });

  }

  /**
   * Construct the next ILP prepare packet.
   *
   * Given a previous ILP prepare packet ({@code sourcePacket}), return the next ILP prepare packet in the chain.
   *
   * * @param {string} sourceAccount ILP address of our peer who sent us the packet * @param {IlpPrepare} sourcePacket
   * (Parsed packet that we received * @returns {NextHopPacketInfo} Account and packet for next hop
   *
   * @param sourceAccountAddress The {@link InterledgerAddress} of the peer who sent this packet into the Connector.
   *                             This is typically the remote peer-address configured in a plugin.
   * @param sourcePacket         The {@link InterledgerPreparePacket} that we received from the source address.
   *
   * @return A {@link InterledgerPreparePacket} that can be sent to the next-hop.
   */
  @VisibleForTesting
  protected NextHopInfo getNextHopPacket(
    final InterledgerAddress sourceAccountAddress, final InterledgerPreparePacket sourcePacket
  ) throws RuntimeException {

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug(
        "Constructing NextHop InterledgerPreparePacket for source: {} from packet: {}",
        sourceAccountAddress, sourcePacket
      );
    }

    final InterledgerAddress destinationAddress = sourcePacket.getDestination();

    final Route nextHopRoute = this.paymentRouter.findBestNexHop(destinationAddress)
      .orElseThrow(() -> new InterledgerProtocolException(
        InterledgerRejectPacket.builder()
          .triggeredBy(connectorSettingsSupplier.get().getIlpAddress())
          .code(InterledgerErrorCode.F02_UNREACHABLE)
          .message(String.format("No route found from source(`%s`) to destination(`%s`).",
            sourceAccountAddress.getValue(), destinationAddress.getValue())
          )
          .build()
      ));

    if (LOGGER.isDebugEnabled()) {
      LOGGER.debug("Determined next hop: {}", nextHopRoute);
    }

    final MonetaryAmount nextAmount = this.determineNextAmount(sourceAccountAddress, sourcePacket);

    return ImmutableNextHopInfo.builder()
      .nextHopAccountAddress(nextHopRoute.getNextHopAccount())
      .nextHopPacket(
        InterledgerPreparePacket.builder()
          .from(sourcePacket)
          .amount(nextAmount.getNumber().numberValue(BigInteger.class))
          .expiresAt(determineDestinationExpiresAt(sourcePacket.getExpiresAt()))
          .build())
      .build();
  }

  /**
   * Given a source address, determine the exchange-rate and new amount that should be returned in order to create the
   * next packet in the chain.
   *
   * @param sourceAccountAddress
   * @param sourcePacket
   *
   * @return
   */
  @VisibleForTesting
  protected MonetaryAmount determineNextAmount(
    final InterledgerAddress sourceAccountAddress, final InterledgerPreparePacket sourcePacket
  ) {
    Objects.requireNonNull(sourcePacket);

    final CurrencyUnit sourceCurrencyUnit = this.accountManager
      .getAccountSettings(sourceAccountAddress)
      .map(AccountSettings::getAssetCode)
      .map(Monetary::getCurrency)
      .orElseThrow(() -> new RuntimeException(String.format("No Source Account found for `%s`", sourceAccountAddress)));
    final MonetaryAmount sourceAmount = Money.of(sourcePacket.getAmount(), sourceCurrencyUnit);
    return this.exchangeRateService.convert(sourceAmount, sourceCurrencyUnit);
  }

  @VisibleForTesting
  protected Instant determineDestinationExpiresAt(final Instant sourceExpiry) {
    Objects.requireNonNull(sourceExpiry);

    final Instant nowTime = Instant.now();
    if (sourceExpiry.isBefore(nowTime)) {
      throw new InterledgerProtocolException(
        InterledgerRejectPacket.builder()
          .triggeredBy(connectorSettingsSupplier.get().getIlpAddress())
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
          .triggeredBy(connectorSettingsSupplier.get().getIlpAddress())
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

  @VisibleForTesting
  protected Instant lesser(final Instant first, final Instant second) {
    if (first.isBefore(second)) {
      return first;
    } else {
      return second;
    }
  }

  @Override
  public boolean add(final SendDataFilter sendDataFilter) {
    Objects.requireNonNull(sendDataFilter);
    return this.sendDataFilters.add(sendDataFilter);
  }

  @Override
  public void addFirst(final SendDataFilter sendDataFilter) {
    Objects.requireNonNull(sendDataFilter);
    this.sendDataFilters.add(0, sendDataFilter);
  }

  @Override
  public SendDataFilterChain getSendDataFilterChain() {
    return null;
  }

  @Override
  public PaymentRouter<Route> getPaymentRouter() {
    return this.paymentRouter;
  }

  /**
   * A container that holds the next-hop packet (with a final destination) as well as the address of the next-hop
   * account to send the packet to.
   */
  @Value.Immutable
  interface NextHopInfo {

    /**
     * The {@link InterledgerAddress} of the next-hop account to send a prepare packet to.
     */
    InterledgerAddress nextHopAccountAddress();

    /**
     * The packet to send to the next hop.
     */
    InterledgerPreparePacket nextHopPacket();

  }
}