package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;


/**
 * An implementation of {@link PacketSwitchFilter} for enforcing packet expiry.
 */
public class ExpiryPacketFilter implements PacketSwitchFilter {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());
  private final Supplier<InterledgerAddress> operatorAddressSupplier;

  public ExpiryPacketFilter(final Supplier<InterledgerAddress> operatorAddressSupplier) {
    this.operatorAddressSupplier = Objects.requireNonNull(operatorAddressSupplier);
  }

  @Override
  public InterledgerResponsePacket doFilter(
    final AccountId sourceAccountId,
    final InterledgerPreparePacket sourcePreparePacket,
    final PacketSwitchFilterChain filterChain
  ) {

    Duration timeoutDuration = Duration.between(Instant.now(), sourcePreparePacket.getExpiresAt());
    if (timeoutDuration.isNegative()) {
      timeoutDuration = timeoutDuration.withSeconds(0);
    }

    try {
      return CompletableFuture
        .supplyAsync(() -> filterChain.doFilter(sourceAccountId, sourcePreparePacket))
        .get(timeoutDuration.getSeconds(), TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException e) {
      logger.error(e.getMessage(), e);
      return InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.T00_INTERNAL_ERROR)
        .triggeredBy(operatorAddressSupplier.get())
        .message(String.format("Error Expiring Packet: %s", e.getMessage()))
        .build();
    } catch (TimeoutException e) {
      logger.error(e.getMessage(), e);
      return InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.R00_TRANSFER_TIMED_OUT)
        .triggeredBy(operatorAddressSupplier.get())
        .message(String.format("Transfer Timed-out"))
        .build();
    }
  }
}
