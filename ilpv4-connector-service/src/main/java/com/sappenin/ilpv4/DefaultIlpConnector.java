package com.sappenin.ilpv4;

import com.google.common.annotations.VisibleForTesting;
import com.sappenin.ilpv4.connector.routing.PaymentRouter;
import com.sappenin.ilpv4.connector.routing.Route;
import com.sappenin.ilpv4.fx.ExchangeRateService;
import com.sappenin.ilpv4.model.Account;
import com.sappenin.ilpv4.peer.PeerManager;
import com.sappenin.ilpv4.settings.ConnectorSettings;
import org.interledger.core.*;
import org.javamoney.moneta.Money;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.money.CurrencyUnit;
import javax.money.MonetaryAmount;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;

¬

/**
 * A default implementation of {@link IlpConnector}.
 */
public class DefaultIlpConnector implements IlpConnector {

  // Used to determine if a message is for a peer (such as for routing) for for regular data/payment messages.
  private static final String PEER_PROTOCOL_PREFIX = "peer.";

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final ConnectorSettings connectorSettings;
  private final PeerManager peerManager;
  private final PaymentRouter<Route> paymentRouter;
  private final ExchangeRateService exchangeRateService;

  public DefaultIlpConnector(final ConnectorSettings connectorSettings, final PeerManager peerManager,
                             final PaymentRouter paymentRouter, final ExchangeRateService exchangeRateService) {
    this.connectorSettings = Objects.requireNonNull(connectorSettings);
    this.peerManager = Objects.requireNonNull(peerManager);
    this.paymentRouter = Objects.requireNonNull(paymentRouter);
    this.exchangeRateService = exchangeRateService;
  }

  @PostConstruct
  private final void init() {
    connectorSettings.getPeers().stream().map(ConnectorSettings.PeerSettings::toPeer).forEach(peerManager::add);
  }

  @PreDestroy
  public void shutdown() {
    // peerManager#shutdown is called automatically by spring due to naming convention.
    //this.peerManager.shutdown();
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

    return CompletableFuture.supplyAsync(() -> {
      if (sourcePreparePacket.getDestination().startsWith(PEER_PROTOCOL_PREFIX)) {
        throw new RuntimeException("Not yet implemented!");
        //return this.peerProtocolController.handle(packet, sourceAccount, {parsedPacket})
      } else if (sourcePreparePacket.getDestination().equals(this.getConnectorSettings().getIlpAddress())) {
        throw new RuntimeException("Not yet implemented!");
        //return this.echoController.handle(packet, sourceAccount, { parsedPacket, outbound })
      } else {

        // TODO: Construct the next-hop packet.
        final InterledgerPacket nextHopPacket = getNextHopPacket(sourceAccountAddress, sourcePreparePacket);

        finish !
        // TODO: Send the outboud packet
        //     log.debug('sending outbound ilp prepare. destination=%s amount=%s', destination, nextHopPacket.amount)
        //    const result = await outbound(IlpPacket.serializeIlpPrepare(nextHopPacket), nextHop)


        // TODO: Process fulfillment.
        // It will either be a fulfill, or an exception will be thrown...

        //        if (result[0] === IlpPacket.Type.TYPE_ILP_FULFILL) {
        //          log.debug('got fulfillment. cond=%s nextHop=%s amount=%s', executionCondition.slice(0, 6).toString('base64'), nextHop, nextHopPacket.amount)
        //
        //          this.backend.submitPayment({
        //            sourceAccount: sourceAccount,
        //            sourceAmount: amount,
        //            destinationAccount: nextHop,
        //            destinationAmount: nextHopPacket.amount
        //      })
        //        .catch(err => {
        //          const errInfo = (err && typeof err === 'object' && err.stack) ? err.stack : String(err)
        //          log.warn('error while submitting payment to backend. error=%s', errInfo)
        //        })
        //        }

      }
    });
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
  protected InterledgerPreparePacket getNextHopPacket(
    final InterledgerAddress sourceAccountAddress, final InterledgerPreparePacket sourcePacket
  ) throws RuntimeException {

    if (logger.isDebugEnabled()) {
      logger.debug(
        "Constructing NextHop InterledgerPreparePacket for source: {} from packet: {}",
        sourceAccountAddress, sourcePacket
      );
    }

    final InterledgerAddress destinationAddress = sourcePacket.getDestination();

    final Route nextHop = this.paymentRouter.findBestNexHop(destinationAddress)
      .orElseThrow(() -> new InterledgerProtocolException(
        InterledgerRejectPacket.builder()
          .code(InterledgerErrorCode.F02_UNREACHABLE)
          .message(String.format(
            "No route found from source(%s) to destination(%s).", sourceAccountAddress, destinationAddress)
          )
          .build()
      ));

    if (logger.isDebugEnabled()) {
      logger.debug("Determined next hop: {}", nextHop);
    }

    final MonetaryAmount nextAmount = this.determineNextAmount(sourceAccountAddress, sourcePacket);

    return InterledgerPreparePacket.builder()
      .from(sourcePacket)
      .amount(nextAmount.getNumber().numberValue(BigInteger.class))
      .expiresAt(determineDestinationExpiresAt(sourcePacket.getExpiresAt()))
      .build();
  }
  //const nextAmount = new BigNumber(amount).times(rate).integerValue(BigNumber.ROUND_FLOOR)
}

  /**
   * Given a source address, determine the exchange-rate and new amount that should be returned in order to create the
   * next packet in the chain.
   *
   * TODO: Account for fees here. See https://github.com/fluid-money/fluid-ilp-connector/blob/6105bd747c78a4756b82d0db86fc9e7bd26ced93/src/main/java/money/fluid/ilp/connector/services/ConnectorFeeService.java
   *
   * @param sourceAccountAddress
   * @param sourcePacket
   *
   * @return
   */
  @VisibleForTesting
  protected MonetaryAmount determineNextAmount(
    // TODO: Replace with new ILP Address in gists.
    final InterledgerAddress sourceAccountAddress, final InterledgerPreparePacket sourcePacket
  ) {
    Objects.requireNonNull(sourcePacket);

    final CurrencyUnit sourceCurrencyUnit = this.peerManager.getAccountManager()
      .getAccount(sourceAccountAddress)
      .map(Account::getAssetCode)
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
      return destinationExpiryTime
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


}
