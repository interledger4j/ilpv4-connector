package com.sappenin.interledger.ilpv4.connector.links;

import com.google.common.annotations.VisibleForTesting;
import com.sappenin.interledger.ilpv4.connector.fx.JavaMoneyUtils;
import com.sappenin.interledger.ilpv4.connector.packetswitch.InterledgerAddressUtils;
import com.sappenin.interledger.ilpv4.connector.routing.PaymentRouter;
import com.sappenin.interledger.ilpv4.connector.routing.Route;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.ilpv4.connector.persistence.repositories.AccountSettingsRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.money.CurrencyUnit;
import javax.money.Monetary;
import javax.money.MonetaryAmount;
import javax.money.convert.CurrencyConversion;
import javax.money.convert.MonetaryConversions;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Objects;
import java.util.function.Supplier;

/**
 * A default implementation of {@link NextHopPacketMapper}.
 */
public class DefaultNextHopPacketMapper implements NextHopPacketMapper {

  private static final String DESTINATION_ADDRESS_IS_UNREACHABLE = "Destination address is unreachable";
  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private final Supplier<ConnectorSettings> connectorSettingsSupplier;
  private final PaymentRouter<Route> externalRoutingService;
  private final AccountSettingsRepository accountSettingsRepository;
  private final InterledgerAddressUtils addressUtils;
  private final JavaMoneyUtils javaMoneyUtils;

  public DefaultNextHopPacketMapper(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final PaymentRouter<Route> externalRoutingService,
    final AccountSettingsRepository accountSettingsRepository,
    final InterledgerAddressUtils addressUtils,
    final JavaMoneyUtils javaMoneyUtils
  ) {
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
    this.externalRoutingService = Objects.requireNonNull(externalRoutingService);
    this.accountSettingsRepository = Objects.requireNonNull(accountSettingsRepository);
    this.addressUtils = Objects.requireNonNull(addressUtils);
    this.javaMoneyUtils = Objects.requireNonNull(javaMoneyUtils);
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
  public NextHopInfo getNextHopPacket(
    final AccountId sourceAccountId, final InterledgerPreparePacket sourcePacket
  ) throws RuntimeException {

    if (logger.isDebugEnabled()) {
      logger.debug(
        "Constructing NextHop InterledgerPreparePacket for source: {} from packet: {}",
        sourceAccountId, sourcePacket
      );
    }

    final InterledgerAddress destinationAddress = sourcePacket.getDestination();

    final Route nextHopRoute = this.externalRoutingService.findBestNexHop(destinationAddress)
      .orElseThrow(() -> new InterledgerProtocolException(
          InterledgerRejectPacket.builder()
            .triggeredBy(connectorSettingsSupplier.get().getOperatorAddressSafe())
            .code(InterledgerErrorCode.F02_UNREACHABLE)
            .message(DESTINATION_ADDRESS_IS_UNREACHABLE)
            .build(),
          String.format("No route found from accountId(`%s`) to destination(`%s`).", sourceAccountId,
            destinationAddress.getValue())
        )
      );

    if (logger.isDebugEnabled()) {
      logger.debug("Determined next hop: {}", nextHopRoute);
    }

    if (sourceAccountId.equals(nextHopRoute.getNextHopAccountId())) {
      throw new InterledgerProtocolException(
        InterledgerRejectPacket.builder()
          .triggeredBy(connectorSettingsSupplier.get().getOperatorAddressSafe())
          .code(InterledgerErrorCode.F02_UNREACHABLE)
          .message(DESTINATION_ADDRESS_IS_UNREACHABLE)
          .build(),
        String.format("Refusing to route payments back to sender. sourceAccount=`%s` destinationAccount=`%s`",
          sourceAccountId, nextHopRoute.getNextHopAccountId())
      );
    }

    final BigInteger nextAmount = this.determineNextAmount(
      sourceAccountId, nextHopRoute.getNextHopAccountId(), sourcePacket
    );

    Instant nowTime = Instant.now();
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
    final AccountId sourceAccountId, final AccountId destinationAccountId, final InterledgerPreparePacket sourcePacket
  ) {
    Objects.requireNonNull(sourcePacket);

    if (!this.addressUtils.isExternalForwardingAllowed(sourcePacket.getDestination())) {
      return sourcePacket.getAmount();
    } else {
      // TODO: Consider a cache here for the source/dest conversion?
      final AccountSettings sourceAccountSettings = this.accountSettingsRepository.safeFindByAccountId(sourceAccountId);
      final CurrencyUnit sourceCurrencyUnit = Monetary.getCurrency(sourceAccountSettings.getAssetCode());
      final int sourceScale = sourceAccountSettings.getAssetScale();
      final MonetaryAmount sourceAmount =
        javaMoneyUtils.toMonetaryAmount(sourceCurrencyUnit, sourcePacket.getAmount(), sourceScale);

      final AccountSettings destinationAccountSettings =
        this.accountSettingsRepository.safeFindByAccountId(destinationAccountId);
      final CurrencyUnit destinationCurrencyUnit = Monetary.getCurrency(destinationAccountSettings.getAssetCode());
      final int destinationScale = destinationAccountSettings.getAssetScale();
      final CurrencyConversion destCurrencyConversion = MonetaryConversions.getConversion(destinationCurrencyUnit);

      return javaMoneyUtils.toInterledgerAmount(sourceAmount.with(destCurrencyConversion), destinationScale);
    }
  }

  // TODO: Unit test this!
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
            .triggeredBy(connectorSettingsSupplier.get().getOperatorAddressSafe())
            .code(InterledgerErrorCode.R02_INSUFFICIENT_TIMEOUT)
            .message(String.format(
              "Source transfer has already expired. sourceExpiry: {%s}, currentTime: {%s}", sourceExpiry, nowTime))
            .build()
        );
      }

      ////////////////////
      // We will set the next transfer's expiry based on the source expiry and our minMessageWindow, but cap it at our
      // maxHoldTime.
      ////////////////////
      final int minMessageWindow = 5000; //TODO: Enable this --> accountSettings.get().getMinMessageWindow();
      final int maxHoldTime = 5000; //TODO: Enable this --> accountSettings.get().getMaxHoldTime();

      // The expiry of the packet, reduced by the minMessageWindow, which is "the minimum time the connector wants to
      // budget for getting a message to the accounts its trading on. In milliseconds."
      final Instant adjustedSourceExpiryInstant = sourceExpiry.minusMillis(minMessageWindow);
      // The point in time after which this Connector will not wait around for a fulfillment.
      final Instant maxHoldInstant = nowTime.plusMillis(maxHoldTime);
      final Instant destinationExpiryTime = lesser(adjustedSourceExpiryInstant, maxHoldInstant);

      // One final check for a "too soon" expiry...
      if (destinationExpiryTime.minusMillis(minMessageWindow).isBefore(nowTime)) {
        throw new InterledgerProtocolException(
          InterledgerRejectPacket.builder()
            .triggeredBy(connectorSettingsSupplier.get().getOperatorAddressSafe())
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
}
