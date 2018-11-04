package com.sappenin.ilpv4.packetswitch;

import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Lists;
import com.sappenin.ilpv4.accounts.AccountManager;
import com.sappenin.ilpv4.connector.routing.PaymentRouter;
import com.sappenin.ilpv4.connector.routing.Route;
import com.sappenin.ilpv4.fx.ExchangeRateService;
import com.sappenin.ilpv4.model.settings.AccountSettings;
import com.sappenin.ilpv4.model.settings.ConnectorSettings;
import com.sappenin.ilpv4.packetswitch.filters.DefaultSendDataFilterChain;
import com.sappenin.ilpv4.packetswitch.filters.SendDataFilter;
import com.sappenin.ilpv4.packetswitch.filters.SendDataFilterChain;
import com.sappenin.ilpv4.packetswitch.filters.SendMoneyFilter;
import com.sappenin.ilpv4.packetswitch.preemptors.EchoController;
import org.immutables.value.Value;
import org.interledger.core.*;
import org.interledger.plugin.lpiv2.Plugin;
import org.interledger.plugin.lpiv2.PluginSettings;
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
import java.util.concurrent.CompletableFuture;

import static com.sappenin.ilpv4.DefaultIlpConnector.PEER_PROTOCOL_PREFIX;

/**
 * A default implementation of {@link IlpPacketSwitch}.
 */
public class DefaultIlpPacketSwitch implements IlpPacketSwitch {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultIlpPacketSwitch.class);

  private final InterledgerFulfillment ECHO_FULFILLMENT = InterledgerFulfillment.of(new byte[32]);

  private final ConnectorSettings connectorSettings;
  private final PaymentRouter<Route> paymentRouter;
  private final ExchangeRateService exchangeRateService;
  private final AccountManager accountManager;

  private final List<SendDataFilter> sendDataFilters;
  private final List<SendMoneyFilter> sendMoneyFilters;

  private final EchoController echoController;

  public DefaultIlpPacketSwitch(
    final ConnectorSettings connectorSettings,
    final PaymentRouter<Route> paymentRouter,
    final ExchangeRateService exchangeRateService,
    final AccountManager accountManager,
    EchoController echoController) {
    this.connectorSettings = Objects.requireNonNull(connectorSettings);
    this.paymentRouter = Objects.requireNonNull(paymentRouter);
    this.exchangeRateService = Objects.requireNonNull(exchangeRateService);
    this.accountManager = Objects.requireNonNull(accountManager);
    this.echoController = echoController;

    this.sendDataFilters = Lists.newArrayList();
    this.sendMoneyFilters = Lists.newArrayList();
  }

  @Override
  public CompletableFuture<Void> sendMoney(BigInteger amount) throws InterledgerProtocolException {
    // TODO: Implement this!
    return CompletableFuture.completedFuture(null);
  }

  @Override
  public final CompletableFuture<InterledgerFulfillPacket> sendData(
    final InterledgerAddress sourceAccountAddress, final InterledgerPreparePacket sourcePreparePacket)
    throws InterledgerProtocolException {

    Objects.requireNonNull(sourceAccountAddress);
    Objects.requireNonNull(sourcePreparePacket);

    //    This logic should move to 1 or more Packet filters so that this method can apply each filter?
    //    Or, we can assume that the Fabric always does this, and then applies any filters before hand? The problem is that it's
    //  difficult to create the notion of a pre and post filter. So, either we do that, or we should move all of this into its
    //   own packetfilter. Look at ServletFilter for an exmaple...maybe we just have pre and post filters...

    Objects.requireNonNull(sourceAccountAddress);
    Objects.requireNonNull(sourcePreparePacket);

    if (sourcePreparePacket.getDestination().startsWith(PEER_PROTOCOL_PREFIX)) {
      // TODO: FINISH!
      throw new RuntimeException("Not yet implemented!");
      //return this.peerProtocolController.handle(packet, sourceAccount, {parsedPacket})
    } else if (sourcePreparePacket.getDestination().equals(connectorSettings.getIlpAddress())) {
      final InterledgerPreparePacket returnPacket =
        this.echoController.handleIncomingData(sourceAccountAddress, sourcePreparePacket);
      // Send the echo payment....
      this.sendData(sourceAccountAddress, returnPacket);
      // Fulfill the original payment...
      return CompletableFuture.completedFuture(InterledgerFulfillPacket.builder()
        .fulfillment(ECHO_FULFILLMENT)
        .build()
      );
    } else {
      final NextHopInfo nextHopInfo = getNextHopPacket(sourceAccountAddress, sourcePreparePacket);

      // TODO: Make this configurable. See JS Connector for details.
      // TODO: Move this to the routing layer. If the router is configured to not return a NextHop like this, then
      // this should never happen, and can be removed.
      if (sourceAccountAddress.equals(nextHopInfo.nextHopAccountAddress())) {
        throw new InterledgerProtocolException(
          InterledgerRejectPacket.builder()
            .code(InterledgerErrorCode.T01_LEDGER_UNREACHABLE)
            .triggeredBy(connectorSettings.getIlpAddress())
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
        this.accountManager.getOrCreatePlugin(nextHopInfo.nextHopAccountAddress());
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
          .triggeredBy(connectorSettings.getIlpAddress())
          .code(InterledgerErrorCode.F02_UNREACHABLE)
          .message(String.format(
            "No route found from source(%s) to destination(%s).", sourceAccountAddress, destinationAddress)
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
          .triggeredBy(connectorSettings.getIlpAddress())
          .code(InterledgerErrorCode.R02_INSUFFICIENT_TIMEOUT)
          .message(String.format(
            "Source transfer has already expired. sourceExpiry: {%s}, currentTime: {%s}", sourceExpiry, nowTime))
          .build()
      );
    }

    // We will set the next transfer's expiry based on the source expiry and our minMessageWindow, but cap it at our
    // maxHoldTime.

    final int minMessageWindow = 5000; //TODO: Enable this --> connectorSettings.getMinMessageWindow();
    final int maxHoldTime = 5000; //TODO: Enable this --> connectorSettings.getMaxHoldTime();

    // The expiry of the packet, reduced by the minMessageWindow, which is "the minimum time the connector wants to
    // budget for getting a message to the accounts its trading on. In milliseconds."
    final Instant adjustedSourceExpiryInstant = sourceExpiry.minusMillis(minMessageWindow);

    // The point in time after which this Connector will not wait around for a fulfillment.
    final Instant maxHoldInstant = nowTime.plusMillis(maxHoldTime);

    final Instant destinationExpiryTime = lesser(adjustedSourceExpiryInstant, maxHoldInstant);
    if (destinationExpiryTime.minusMillis(minMessageWindow).isBefore(nowTime)) {
      throw new InterledgerProtocolException(
        InterledgerRejectPacket.builder()
          .triggeredBy(connectorSettings.getIlpAddress())
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
  public boolean add(final SendMoneyFilter sendMoneyFilter) {
    Objects.requireNonNull(sendMoneyFilter);
    return this.sendMoneyFilters.add(sendMoneyFilter);
  }

  @Override
  public void addFirst(final SendMoneyFilter sendMoneyFilter) {
    Objects.requireNonNull(sendMoneyFilter);
    this.sendMoneyFilters.add(0, sendMoneyFilter);
  }

  @Override
  public SendDataFilterChain getSendDataFilterChain() {
    return null;
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