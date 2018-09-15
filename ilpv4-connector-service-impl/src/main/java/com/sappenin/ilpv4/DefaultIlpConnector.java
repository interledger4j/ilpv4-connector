package com.sappenin.ilpv4;

import com.google.common.annotations.VisibleForTesting;
import com.sappenin.ilpv4.accounts.AccountManager;
import com.sappenin.ilpv4.connector.routing.PaymentRouter;
import com.sappenin.ilpv4.connector.routing.Route;
import com.sappenin.ilpv4.fx.ExchangeRateService;
import com.sappenin.ilpv4.fx.ImmutableUpdateRatePaymentParams;
import com.sappenin.ilpv4.model.settings.AccountSettings;
import com.sappenin.ilpv4.model.settings.ConnectorSettings;
import org.immutables.value.Value;
import org.interledger.core.*;
import org.javamoney.moneta.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * A default implementation of {@link IlpConnector}.
 */
public class DefaultIlpConnector implements IlpConnector {

  // TODO: Use Prefix...
  // Used to determine if a message is for a peer (such as for routing) for for regular data/payment messages.
  private static final String PEER_PROTOCOL_PREFIX = "peer.";

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final ConnectorSettings connectorSettings;
  private final AccountManager accountManager;
  private final PaymentRouter<Route> paymentRouter;
  private final ExchangeRateService exchangeRateService;

  public DefaultIlpConnector(
    final ConnectorSettings connectorSettings, final AccountManager accountManager,
    final PaymentRouter<Route> paymentRouter, final ExchangeRateService exchangeRateService
  ) {
    this.connectorSettings = Objects.requireNonNull(connectorSettings);
    this.accountManager = Objects.requireNonNull(accountManager);
    this.paymentRouter = Objects.requireNonNull(paymentRouter);
    this.exchangeRateService = exchangeRateService;
  }

  @PostConstruct
  private final void init() {
    connectorSettings.getAccountSettings().stream()
      .forEach(accountManager::add);
  }

  @PreDestroy
  public void shutdown() {
    // accountManager#shutdown is called automatically by spring due to naming convention, so no need to call it here.
  }

  @Override
  public ConnectorSettings getConnectorSettings() {
    return this.connectorSettings;
  }

  @Override
  public CompletableFuture<InterledgerFulfillPacket> handleIncomingData(
    final InterledgerAddress sourceAccountAddress, final InterledgerPreparePacket sourcePreparePacket
  ) throws InterledgerProtocolException {

    Objects.requireNonNull(sourceAccountAddress);
    Objects.requireNonNull(sourcePreparePacket);

    if (sourcePreparePacket.getDestination().startsWith(PEER_PROTOCOL_PREFIX)) {
      throw new RuntimeException("Not yet implemented!");
      //return this.peerProtocolController.handle(packet, sourceAccount, {parsedPacket})
    } else if (sourcePreparePacket.getDestination().equals(this.getConnectorSettings().getIlpAddress())) {
      throw new RuntimeException("Not yet implemented!");
      //return this.echoController.handle(packet, sourceAccount, { parsedPacket, outbound })
    } else {

      final NextHopInfo nextHopInfo = getNextHopPacket(sourceAccountAddress, sourcePreparePacket);

      if (logger.isDebugEnabled()) {
        logger.debug(
          "Sending outbound ILP Prepare. destination={} packet={}", nextHopInfo.nextHopAccountAddress(),
          nextHopInfo.nextHopPacket()
        );
      }

      // Throws an exception if the lpi2 cannot be found...
      return this.accountManager
        .getOrCreatePlugin(nextHopInfo.nextHopAccountAddress())
        .sendData(nextHopInfo.nextHopPacket())
        .thenApplyAsync((result) -> {
          // Log the fulfillment...
          if (logger.isDebugEnabled()) {
            logger.debug(
              "Received fulfillment: {}", result
            );
          }
          return result;
        })
        .thenApplyAsync((result) -> {
          // Log statistics in the ExchangeRateService...
          this.exchangeRateService.logPaymentStats(
            ImmutableUpdateRatePaymentParams.builder()
              .sourceAccountAddress(sourceAccountAddress)
              .sourceAmount(sourcePreparePacket.getAmount())
              .destinationAccountAddress(nextHopInfo.nextHopAccountAddress())
              .destinationAmount(nextHopInfo.nextHopPacket().getAmount())
              .build()
          );
          return result;
        });
    }
  }

  /**
   * Find the appropriate lpi2 to send the outbound packet to.
   *
   * @param nextHopAccount
   * @param nextHopPacket
   */
  @VisibleForTesting
  protected CompletableFuture<InterledgerFulfillPacket> outbound(
    final InterledgerAddress nextHopAccount, final InterledgerPreparePacket nextHopPacket
  ) {
    Objects.requireNonNull(nextHopAccount);
    Objects.requireNonNull(nextHopPacket);

    return this.accountManager.getOrCreatePlugin(nextHopAccount).sendData(nextHopPacket);
  }

  /**
   * Construct the next ILP prepare packet.
   *
   * Given a previous ILP prepare packet ({@code sourcePacket}), return the next ILP prepare packet in the chain.
   *
   * * @param {string} sourceAccount ILP address of our peer who sent us the packet * @param {IlpPrepare} sourcePacket
   * (Parsed packet that we received * @returns {NextHopPacketInfo} Account and packet for next hop
   *
   * @param sourceAccountAddress The {@link InterledgerAddress} of our peer who sent us the packet.
   * @param sourcePacket         The {@link InterledgerPreparePacket} that we received from the source address.
   *
   * @return A {@link InterledgerPreparePacket} that can be sent to the next-hop.
   */
  @VisibleForTesting
  protected NextHopInfo getNextHopPacket(
    final InterledgerAddress sourceAccountAddress, final InterledgerPreparePacket sourcePacket
  ) throws RuntimeException {

    if (logger.isDebugEnabled()) {
      logger.debug(
        "Constructing NextHop InterledgerPreparePacket for source: {} from packet: {}",
        sourceAccountAddress, sourcePacket
      );
    }

    final InterledgerAddress destinationAddress = sourcePacket.getDestination();

    final Route nextHopRoute = this.paymentRouter.findBestNexHop(destinationAddress)
      .orElseThrow(() -> new InterledgerProtocolException(
        InterledgerRejectPacket.builder()
          .triggeredBy(this.connectorSettings.getIlpAddress())
          .code(InterledgerErrorCode.F02_UNREACHABLE)
          .message(String.format(
            "No getRoute found from source(%s) to destination(%s).", sourceAccountAddress, destinationAddress)
          )
          .build()
      ));

    if (logger.isDebugEnabled()) {
      logger.debug("Determined next hop: {}", nextHopRoute);
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
      .orElseThrow(() -> new RuntimeException(String.format("No Source Account found for ", sourceAccountAddress)));
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
          .triggeredBy(this.connectorSettings.getIlpAddress())
          .code(InterledgerErrorCode.R02_INSUFFICIENT_TIMEOUT)
          .message(String.format(
            "Source transfer has already expired. sourceExpiry: {%s}, currentTime: {%s}", sourceExpiry, nowTime))
          .build()
      );
    }

    // We will set the next transfer's expiry based on the source expiry and our minMessageWindow, but cap it at our
    // maxHoldTime.

    final int minMessageWindow = 5000; //TODO: Enable this --> this.getConnectorSettings().getMinMessageWindow();
    final int maxHoldTime = 5000; //TODO: Enable this --> this.getConnectorSettings().getMaxHoldTime();

    // The expiry of the packet, reduced by the minMessageWindow, which is "the minimum time the connector wants to
    // budget for getting a message to the accounts its trading on. In milliseconds."
    final Instant adjustedSourceExpiryInstant = sourceExpiry.minusMillis(minMessageWindow);

    // The point in time after which this Connector will not wait around for a fulfillment.
    final Instant maxHoldInstant = nowTime.plusMillis(maxHoldTime);

    final Instant destinationExpiryTime = lesser(adjustedSourceExpiryInstant, maxHoldInstant);
    if (destinationExpiryTime.minusMillis(minMessageWindow).isBefore(nowTime)) {
      throw new InterledgerProtocolException(
        InterledgerRejectPacket.builder()
          .triggeredBy(this.connectorSettings.getIlpAddress())
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

  @Override
  public CompletableFuture<Void> handleIncomingMoney(BigInteger amount) {
    return CompletableFuture.supplyAsync(() -> null);
  }

  @VisibleForTesting
  protected Instant lesser(final Instant first, final Instant second) {
    if (first.isBefore(second)) {
      return first;
    } else {
      return second;
    }
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
