package com.sappenin.ilpv4;

import com.sappenin.ilpv4.packetswitch.IlpPacketSwitch;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.plugin.lpiv2.Plugin;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.CompletableFuture;

/**
 * A default implementation of {@link org.interledger.plugin.lpiv2.Plugin.IlpDataHandler} for use by plugins to handle
 * incoming ILP packets by linking them to the {@link IlpPacketSwitch}.
 *
 * @deprecated This class is no longer used, in-favor of the Fabric. It should probably be removed in favor of the
 * packetswitch-variant.
 */
@Deprecated
public class DefaultIlpDataHandler implements Plugin.IlpDataHandler {

  private static final Logger LOGGER = LoggerFactory.getLogger(DefaultIlpDataHandler.class);

  //private final Supplier<ConnectorSettings> connectorSettingsSupplier;
  private final IlpPacketSwitch fabric;
  //  private final AccountManager accountManager;
  //  private final PaymentRouter<Route> paymentRouter;
  //  private final ExchangeRateService exchangeRateService;

  public DefaultIlpDataHandler(
    //final Supplier<ConnectorSettings> connectorSettingsSupplier,
    //    final AccountManager accountManager,
    //    final PaymentRouter<Route> paymentRouter,
    //    final ExchangeRateService exchangeRateService,
    final IlpPacketSwitch fabric) {
    //this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
    this.fabric = Objects.requireNonNull(fabric);
    //    this.accountManager = Objects.requireNonNull(accountManager);
    //    this.paymentRouter = Objects.requireNonNull(paymentRouter);
    //    this.exchangeRateService = Objects.requireNonNull(exchangeRateService);
  }

  /**
   * <p>Handle an incoming prepare-packet by either fulfilling it (if local) or by forwarding it to a remote peer.</p>
   *
   * <p>Repeat calls to this method using the same transfer information must be idempotent.</p>
   *
   * @param sourceAccountAddress The {@link InterledgerAddress} for the account that sent this incoming ILP packet.
   * @param sourcePreparePacket  An {@link InterledgerPreparePacket} containing data about an ILP payment.
   *
   * @throws InterledgerProtocolException If the response from the remote peer is a rejection.
   */
  @Override
  public CompletableFuture<InterledgerFulfillPacket> handleIncomingData(
    final InterledgerAddress sourceAccountAddress, final InterledgerPreparePacket sourcePreparePacket
  ) throws InterledgerProtocolException {

    Objects.requireNonNull(sourceAccountAddress);
    Objects.requireNonNull(sourcePreparePacket);

    return this.fabric.sendData(sourceAccountAddress, sourcePreparePacket);

    //    if (sourcePreparePacket.getDestination().startsWith(PEER_PROTOCOL_PREFIX)) {
    //      // TODO: FINISH!
    //      throw new RuntimeException("Not yet implemented!");
    //      //return this.peerProtocolController.handle(packet, sourceAccount, {parsedPacket})
    //    } else if (sourcePreparePacket.getDestination().equals(this.getConnectorSettings().getIlpAddress())) {
    //      // TODO: FINISH!
    //      throw new RuntimeException("Not yet implemented!");
    //      //return this.echoController.handle(packet, sourceAccount, { parsedPacket, outbound })
    //    } else {
    //      final NextHopInfo nextHopInfo = getNextHopPacket(sourceAccountAddress, sourcePreparePacket);
    //
    //      // Don't reflect payments...
    //      // TODO: Make this configurable. See JS Connector for details.
    //      if (sourceAccountAddress.equals(nextHopInfo.nextHopAccountAddress())) {
    //        throw new InterledgerProtocolException(
    //          InterledgerRejectPacket.builder()
    //            .code(InterledgerErrorCode.T01_LEDGER_UNREACHABLE)
    //            .triggeredBy(this.getConnectorSettings().getIlpAddress())
    //            .message(
    //              String.format("Refusing to route payments back to sender. sourceAccount=`%s` destinationAccount=`%s`",
    //                sourceAccountAddress, nextHopInfo.nextHopAccountAddress()))
    //            .build()
    //        );
    //      }
    //
    //      if (LOGGER.isDebugEnabled()) {
    //        LOGGER.debug(
    //          "Sending outbound ILP Prepare. destination={} packet={}", nextHopInfo.nextHopAccountAddress(),
    //          nextHopInfo.nextHopPacket()
    //        );
    //      }
    //
    //      // Throws an exception if the lpi2 cannot be found...
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
    //    }
  }

  //  /**
  //   * Construct the next ILP prepare packet.
  //   *
  //   * Given a previous ILP prepare packet ({@code sourcePacket}), return the next ILP prepare packet in the chain.
  //   *
  //   * * @param {string} sourceAccount ILP address of our peer who sent us the packet * @param {IlpPrepare} sourcePacket
  //   * (Parsed packet that we received * @returns {NextHopPacketInfo} Account and packet for next hop
  //   *
  //   * @param sourceAccountAddress The {@link InterledgerAddress} of the peer who sent this packet into the Connector.
  //   *                             This is typically the remote peer-address configured in a plugin.
  //   * @param sourcePacket         The {@link InterledgerPreparePacket} that we received from the source address.
  //   *
  //   * @return A {@link InterledgerPreparePacket} that can be sent to the next-hop.
  //   */
  //  @VisibleForTesting
  //  protected NextHopInfo getNextHopPacket(
  //    final InterledgerAddress sourceAccountAddress, final InterledgerPreparePacket sourcePacket
  //  ) throws RuntimeException {
  //
  //    if (LOGGER.isDebugEnabled()) {
  //      LOGGER.debug(
  //        "Constructing NextHop InterledgerPreparePacket for source: {} from packet: {}",
  //        sourceAccountAddress, sourcePacket
  //      );
  //    }
  //
  //    final InterledgerAddress destinationAddress = sourcePacket.getDestination();
  //
  //    final Route nextHopRoute = this.paymentRouter.findBestNexHop(destinationAddress)
  //      .orElseThrow(() -> new InterledgerProtocolException(
  //        InterledgerRejectPacket.builder()
  //          .triggeredBy(this.getConnectorSettings().getIlpAddress())
  //          .code(InterledgerErrorCode.F02_UNREACHABLE)
  //          .message(String.format(
  //            "No route found from source(%s) to destination(%s).", sourceAccountAddress, destinationAddress)
  //          )
  //          .build()
  //      ));
  //
  //    if (LOGGER.isDebugEnabled()) {
  //      LOGGER.debug("Determined next hop: {}", nextHopRoute);
  //    }
  //
  //    final MonetaryAmount nextAmount = this.determineNextAmount(sourceAccountAddress, sourcePacket);
  //
  //    return ImmutableNextHopInfo.builder()
  //      .nextHopAccountAddress(nextHopRoute.getNextHopAccount())
  //      .nextHopPacket(
  //        InterledgerPreparePacket.builder()
  //          .from(sourcePacket)
  //          .amount(nextAmount.getNumber().numberValue(BigInteger.class))
  //          .expiresAt(determineDestinationExpiresAt(sourcePacket.getExpiresAt()))
  //          .build())
  //      .build();
  //  }
  //
  //  /**
  //   * Given a source address, determine the exchange-rate and new amount that should be returned in order to create the
  //   * next packet in the chain.
  //   *
  //   * @param sourceAccountAddress
  //   * @param sourcePacket
  //   *
  //   * @return
  //   */
  //  @VisibleForTesting
  //  protected MonetaryAmount determineNextAmount(
  //    final InterledgerAddress sourceAccountAddress, final InterledgerPreparePacket sourcePacket
  //  ) {
  //    Objects.requireNonNull(sourcePacket);
  //
  //    final CurrencyUnit sourceCurrencyUnit = this.accountManager
  //      .getAccountSettings(sourceAccountAddress)
  //      .map(AccountSettings::getAssetCode)
  //      .map(Monetary::getCurrency)
  //      .orElseThrow(() -> new RuntimeException(String.format("No Source Account found for `%s`", sourceAccountAddress)));
  //    final MonetaryAmount sourceAmount = Money.of(sourcePacket.getAmount(), sourceCurrencyUnit);
  //    return this.exchangeRateService.convert(sourceAmount, sourceCurrencyUnit);
  //  }
  //
  //  @VisibleForTesting
  //  protected Instant determineDestinationExpiresAt(final Instant sourceExpiry) {
  //    Objects.requireNonNull(sourceExpiry);
  //
  //    final Instant nowTime = Instant.now();
  //    if (sourceExpiry.isBefore(nowTime)) {
  //      throw new InterledgerProtocolException(
  //        InterledgerRejectPacket.builder()
  //          .triggeredBy(this.getConnectorSettings().getIlpAddress())
  //          .code(InterledgerErrorCode.R02_INSUFFICIENT_TIMEOUT)
  //          .message(String.format(
  //            "Source transfer has already expired. sourceExpiry: {%s}, currentTime: {%s}", sourceExpiry, nowTime))
  //          .build()
  //      );
  //    }
  //
  //    // We will set the next transfer's expiry based on the source expiry and our minMessageWindow, but cap it at our
  //    // maxHoldTime.
  //
  //    final int minMessageWindow = 5000; //TODO: Enable this --> this.getConnectorSettings().getMinMessageWindow();
  //    final int maxHoldTime = 5000; //TODO: Enable this --> this.getConnectorSettings().getMaxHoldTime();
  //
  //    // The expiry of the packet, reduced by the minMessageWindow, which is "the minimum time the connector wants to
  //    // budget for getting a message to the accounts its trading on. In milliseconds."
  //    final Instant adjustedSourceExpiryInstant = sourceExpiry.minusMillis(minMessageWindow);
  //
  //    // The point in time after which this Connector will not wait around for a fulfillment.
  //    final Instant maxHoldInstant = nowTime.plusMillis(maxHoldTime);
  //
  //    final Instant destinationExpiryTime = lesser(adjustedSourceExpiryInstant, maxHoldInstant);
  //    if (destinationExpiryTime.minusMillis(minMessageWindow).isBefore(nowTime)) {
  //      throw new InterledgerProtocolException(
  //        InterledgerRejectPacket.builder()
  //          .triggeredBy(this.getConnectorSettings().getIlpAddress())
  //          .code(InterledgerErrorCode.R02_INSUFFICIENT_TIMEOUT)
  //          .message(String.format(
  //            "Source transfer expires too soon to complete payment. SourceExpiry: {%s}, " +
  //              "RequiredSourceExpiry: {%s}, CurrentTime: {%s}",
  //            sourceExpiry,
  //            nowTime.plusMillis(minMessageWindow * 2),
  //            nowTime))
  //          .build()
  //      );
  //    } else {
  //      return destinationExpiryTime;
  //    }
  //  }
  //
  //  @VisibleForTesting
  //  protected Instant lesser(final Instant first, final Instant second) {
  //    if (first.isBefore(second)) {
  //      return first;
  //    } else {
  //      return second;
  //    }
  //  }

  //  // TODO: Make this a supplier.
  //  public ConnectorSettings getConnectorSettings() {
  //    return this.connectorSettingsSupplier.get();
  //  }

  //  /**
  //   * A container that holds the next-hop packet (with a final destination) as well as the address of the next-hop
  //   * account to send the packet to.
  //   */
  //  @Value.Immutable
  //  interface NextHopInfo {
  //
  //    /**
  //     * The {@link InterledgerAddress} of the next-hop account to send a prepare packet to.
  //     */
  //    InterledgerAddress nextHopAccountAddress();
  //
  //    /**
  //     * The packet to send to the next hop.
  //     */
  //    InterledgerPreparePacket nextHopPacket();
  //
  //  }
}
