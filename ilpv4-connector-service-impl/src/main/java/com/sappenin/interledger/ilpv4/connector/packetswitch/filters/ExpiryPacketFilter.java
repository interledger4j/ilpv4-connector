package com.sappenin.interledger.ilpv4.connector.packetswitch.filters;

import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.core.InterledgerResponsePacket;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;


/**
 * An implementation of {@link PacketSwitchFilter} for enforcing packet expiry (i.e., not processing an expired packet
 * but also timing out a pending, outgoing request that has not returned within a given threshold).
 */
public class ExpiryPacketFilter extends AbstractPacketFilter implements PacketSwitchFilter {

  public ExpiryPacketFilter(final Supplier<InterledgerAddress> operatorAddressSupplier) {
    super(operatorAddressSupplier);
  }

  @Override
  public InterledgerResponsePacket doFilter(
    final AccountId sourceAccountId,
    final InterledgerPreparePacket sourcePreparePacket,
    final PacketSwitchFilterChain filterChain
  ) {

    Duration timeoutDuration = Duration.between(Instant.now(), sourcePreparePacket.getExpiresAt());
    if (timeoutDuration.isNegative() || timeoutDuration.isZero()) {
      return reject(
        sourceAccountId, sourcePreparePacket, InterledgerErrorCode.R00_TRANSFER_TIMED_OUT, "Transfer Timed-out"
      );
    }

    try {
      return CompletableFuture.supplyAsync(() -> filterChain.doFilter(sourceAccountId, sourcePreparePacket))
        .get(timeoutDuration.getSeconds(), TimeUnit.SECONDS);
    } catch (InterledgerProtocolException e) {
      if (logger.isDebugEnabled()) {
        logger.warn(e.getMessage(), e);
      } else {
        logger.warn(e.getMessage());
      }
      return e.getInterledgerRejectPacket();
    } catch (InterruptedException | ExecutionException e) {
      if (e.getCause() != null && InterledgerProtocolException.class.isAssignableFrom(e.getCause().getClass())) {
        if (logger.isDebugEnabled()) {
          logger.warn(e.getCause().getMessage(), e.getCause());
        } else {
          logger.warn(e.getCause().getMessage());
        }
        return ((InterledgerProtocolException) e.getCause()).getInterledgerRejectPacket();
      } else {
        logger.error(e.getMessage(), e);
        return reject(
          sourceAccountId, sourcePreparePacket, InterledgerErrorCode.T00_INTERNAL_ERROR,
          String.format("Error in ExpiryPacketFilter: %s", e.getMessage())
        );
      }
    } catch (TimeoutException e) {
      logger.error(e.getMessage(), e);
      return reject(sourceAccountId, sourcePreparePacket,
        InterledgerErrorCode.R00_TRANSFER_TIMED_OUT, "Transfer Timed-out"
      );
    }
  }
}
