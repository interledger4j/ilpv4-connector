package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import com.sappenin.interledger.ilpv4.connector.packetswitch.PacketRejector;
import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.interledger.core.InterledgerErrorCode.R02_INSUFFICIENT_TIMEOUT;


/**
 * An implementation of {@link PacketSwitchFilter} for enforcing packet expiry (i.e., not processing an expired packet
 * but also timing out a pending, outgoing request that has not returned within a given threshold).
 */
public class ExpiryPacketFilter extends AbstractPacketFilter implements PacketSwitchFilter {

  public ExpiryPacketFilter(final PacketRejector packetRejector) {
    super(packetRejector);
  }

  @Override
  public InterledgerResponsePacket doFilter(
    final AccountId sourceAccountId,
    final InterledgerPreparePacket sourcePreparePacket,
    final PacketSwitchFilterChain filterChain
  ) {

    final Duration timeoutDuration = Duration.between(Instant.now(), sourcePreparePacket.getExpiresAt());
    if (timeoutDuration.isNegative() || timeoutDuration.isZero()) {
      return reject(
        sourceAccountId, sourcePreparePacket, R02_INSUFFICIENT_TIMEOUT,
        "The connector could not forward the payment, because the timeout was too low to subtract its safety margin"
      );
    }

    try {
      return CompletableFuture.supplyAsync(() -> filterChain.doFilter(sourceAccountId, sourcePreparePacket))
        .get(timeoutDuration.getSeconds(), TimeUnit.SECONDS);
    } catch (InterruptedException | ExecutionException e) {
      throw (RuntimeException) e.getCause();
    } catch (TimeoutException e) {
      logger.error(e.getMessage(), e);
      return reject(sourceAccountId, sourcePreparePacket,
        InterledgerErrorCode.R00_TRANSFER_TIMED_OUT, "Transfer Timed-out"
      );
    }
  }
}
