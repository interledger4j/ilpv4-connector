package org.interledger.connector.link;

import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.netflix.hystrix.HystrixCommand;
import com.netflix.hystrix.HystrixCommandGroupKey;
import com.netflix.hystrix.HystrixCommandProperties;
import com.netflix.hystrix.HystrixObservableCommand;
import org.interledger.connector.link.events.LinkEventListener;
import org.interledger.connector.link.exceptions.LinkHandlerAlreadyRegisteredException;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerResponsePacketMapper;

import java.time.Duration;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.function.Supplier;

/**
 * A {@link Link} that wraps an internel Link delegate and provides Circuit breaking functionality.
 */
public class CircuitBreakingLink implements Link<LinkSettings> {

  private final Link<?> linkDelegate;
  private final LoadingCache<LinkId, HystrixCommand.Setter> hystrixSetterCache;

  /**
   * Required-args constructor.
   *
   * @param requestTimeoutMs Timeout value in milliseconds for a command.
   * @param linkDelegate     The {@link Link} to wrap in a circuit breaker.
   */
  public CircuitBreakingLink(final Supplier<Integer> requestTimeoutMs, final Link<?> linkDelegate) {
    this.linkDelegate = Objects.requireNonNull(linkDelegate);

    this.hystrixSetterCache = CacheBuilder.newBuilder()
      //.maximumSize(100) // Not enabled for now in order to support many accounts.
      // Expire after 1 minutes of inactivity. This cache is a perhaps an over-optimization, so constructing a new
      // one every minute is fine.
      .expireAfterAccess(Duration.ofMinutes(1))
      .build(new CacheLoader<LinkId, HystrixCommand.Setter>() {
        public HystrixCommand.Setter load(final LinkId linkId) {
          return HystrixCommand.Setter
            .withGroupKey(HystrixCommandGroupKey.Factory.asKey(linkId.value())) //"GroupName"
            // CommandName is by default derived from the class-name.
            .andCommandPropertiesDefaults(
              HystrixCommandProperties.defaultSetter()
                .withRequestCacheEnabled(false)
                // TODO: Disable in prod.
                //.withRequestLogEnabled(false) // default is true
                // Only change the default if the specified timeout is positive.
                .withExecutionTimeoutInMilliseconds(requestTimeoutMs.get() > 0 ? requestTimeoutMs.get() : 1000)
            );
        }
      });
  }

  @Override
  public InterledgerResponsePacket sendPacket(final InterledgerPreparePacket preparePacket) {
    Objects.requireNonNull(preparePacket);

    try {
      final HystrixCommand.Setter setter =
        hystrixSetterCache.get(linkDelegate.getLinkId()
          .orElseThrow(() -> new RuntimeException("Can't construct a HystrixCommand without a LinkId!")));
      final InterledgerResponsePacket responsePacket =
        new CommandSendPacket(setter, linkDelegate, preparePacket).execute();

      return new InterledgerResponsePacketMapper<InterledgerResponsePacket>() {
        @Override
        protected InterledgerResponsePacket mapFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
          return interledgerFulfillPacket;
        }

        @Override
        protected InterledgerResponsePacket mapRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
          // Throw an exception to trigger the circuit breaker, but only if the peer returns a T03 (in this case we
          // should open the circuit for something like 5 seconds). If the peer returns anything else (including a
          // T02), then our peer's peer is busy, and we expect our peer to rate-limit us appropriately, so we don't
          // open the circuit breaker manually under any other circumstances.

          if (interledgerRejectPacket.getCode().equals(InterledgerErrorCode.T03_CONNECTOR_BUSY)) {
            throw new InterledgerProtocolException(interledgerRejectPacket);
          } else {
            return interledgerRejectPacket;
          }
        }
      }.map(responsePacket);
    } catch (ExecutionException e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  @Override
  public Optional<LinkId> getLinkId() {
    return this.linkDelegate.getLinkId();
  }

  @Override
  public Supplier<Optional<InterledgerAddress>> getOperatorAddressSupplier() {
    return this.linkDelegate.getOperatorAddressSupplier();
  }

  @Override
  public LinkSettings getLinkSettings() {
    return this.linkDelegate.getLinkSettings();
  }

  @Override
  public void registerLinkHandler(LinkHandler dataHandler) throws LinkHandlerAlreadyRegisteredException {
    this.linkDelegate.registerLinkHandler(dataHandler);
  }

  @Override
  public Optional<LinkHandler> getLinkHandler() {
    return this.linkDelegate.getLinkHandler();
  }

  @Override
  public void unregisterLinkHandler() {
    this.linkDelegate.unregisterLinkHandler();
  }

  @Override
  public void addLinkEventListener(LinkEventListener eventListener) {
    this.linkDelegate.addLinkEventListener(eventListener);
  }


  @Override
  public void removeLinkEventListener(LinkEventListener eventListener) {
    this.linkDelegate.removeLinkEventListener(eventListener);
  }

  @Override
  public CompletableFuture<Void> connect() {
    return this.linkDelegate.connect();
  }

  @Override
  public CompletableFuture<Void> disconnect() {
    return this.linkDelegate.disconnect();
  }

  @Override
  public boolean isConnected() {
    return this.linkDelegate.isConnected();
  }

  /**
   * A {@link HystrixObservableCommand} that wraps the outgoing sendPacket call for any {@link Link} wrapped by {@link
   * CircuitBreakingLink}..
   */
  static class CommandSendPacket extends HystrixCommand<InterledgerResponsePacket> {

    private final Link<?> linkDelegate;
    private final InterledgerPreparePacket preparePacket;

    /**
     * Required-args Constructor.
     *
     * @param linkDelegate  Each outgoing, potentially expensive, request out of this connector occurs in a {@link
     *                      Link}. Thus, we use this command to wrap each Link by its unique identifier so that circuit
     *                      breaking and other functionality occurs on a per-linkDelegate basis.
     * @param preparePacket
     */
    public CommandSendPacket(
      final Setter setter, final Link<?> linkDelegate, final InterledgerPreparePacket preparePacket
    ) {
      super(setter);
      this.linkDelegate = Objects.requireNonNull(linkDelegate);
      this.preparePacket = Objects.requireNonNull(preparePacket);
    }

    /**
     * Implement this method with code to be executed when {@link #execute()} or {@link #queue()} are invoked.
     *
     * @return R response type
     *
     * @throws Exception if command execution fails
     */
    @Override
    protected InterledgerResponsePacket run() {
      return this.linkDelegate.sendPacket(preparePacket);
    }

    @Override
    protected InterledgerResponsePacket getFallback() {
      return InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.T02_PEER_BUSY)
        .message(
          String.format("Circuit-breaker triggered for Link `%s`", linkDelegate.getLinkId()))
        .triggeredBy(linkDelegate.getOperatorAddressSupplier().get())
        .build();
    }

  }
}
