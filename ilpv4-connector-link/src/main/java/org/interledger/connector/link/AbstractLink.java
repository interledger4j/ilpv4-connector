package org.interledger.connector.link;

import com.google.common.collect.Sets;
import org.interledger.connector.link.events.LinkConnectedEvent;
import org.interledger.connector.link.events.LinkDisconnectedEvent;
import org.interledger.connector.link.events.LinkErrorEvent;
import org.interledger.connector.link.events.LinkEventEmitter;
import org.interledger.connector.link.events.LinkEventListener;
import org.interledger.connector.link.exceptions.LinkHandlerAlreadyRegisteredException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * An abstract implementation of a {@link Link} that provides scaffolding for all link implementations.
 */
public abstract class AbstractLink<LS extends LinkSettings> implements Link<LS> {

  protected final Logger logger = LoggerFactory.getLogger(this.getClass());
  /**
   * A typed representation of the configuration options passed-into this ledger link.
   */
  private final LS linkSettings;
  private final AtomicBoolean connected = new AtomicBoolean(NOT_CONNECTED);

  private final AtomicReference<LinkHandler> linkHandlerAtomicReference = new AtomicReference<>();

  // The emitter used by this link.
  private final LinkEventEmitter linkEventEmitter;

  // Non-final for late-binding...
  private LinkId linkId;

  /**
   * Required-args Constructor which utilizes a default {@link LinkEventEmitter} that synchronously connects to any
   * event handlers.
   *
   * @param dataLinkSettings A {@link LS} that specified ledger link options.
   */
  protected AbstractLink(final LS dataLinkSettings) {
    this(dataLinkSettings, new SyncLinkEventEmitter());
  }

  /**
   * Required-args Constructor.
   *
   * @param linkSettings     A {@link LS} that specified ledger link options.
   * @param linkEventEmitter A {@link LinkEventEmitter} that is used to emit events from this link.
   */
  protected AbstractLink(
    final LS linkSettings, final org.interledger.connector.link.events.LinkEventEmitter linkEventEmitter
  ) {
    this.linkSettings = Objects.requireNonNull(linkSettings);
    this.linkEventEmitter = Objects.requireNonNull(linkEventEmitter);
  }

  @Override
  public Optional<LinkId> getLinkId() {
    return Optional.ofNullable(linkId);
  }

  public void setLinkId(final LinkId linkId) {
    Objects.requireNonNull(linkId);

    if (this.linkId == null) {
      this.linkId = linkId;
    } else {
      throw new RuntimeException("LinkId may only be set once!");
    }
  }

  @Override
  public LS getLinkSettings() {
    return this.linkSettings;
  }

  @Override
  public final CompletableFuture<Void> connect() {
    try {
      if (this.connected.compareAndSet(NOT_CONNECTED, CONNECTED)) {
        logger.debug("[{}] `{}` connecting to `{}`...",
          this.linkSettings.getLinkType(), this.linkSettings.getOperatorAddress(), this.getLinkId()
        );

        return this.doConnect()
          .whenComplete(($, error) -> {
            if (error == null) {
              // Emit a connected event...
              this.linkEventEmitter.emitEvent(LinkConnectedEvent.of(this));

              logger.debug("[{}] `{}` connected to `{}`", this.getLinkSettings().getLinkType(),
                this.linkSettings.getOperatorAddress(), this.getLinkId());
            } else {
              this.connected.set(NOT_CONNECTED);
              final String errorMessage = String.format("[%s] `%s` error while trying to connect to `%s`",
                this.linkSettings.getLinkType(),
                this.linkSettings.getOperatorAddress(), this.getLinkId()
              );
              logger.error(errorMessage, error);
            }
          });
      } else {
        logger.debug("[{}] `{}` already connected to `{}`...", this.linkSettings.getLinkType(),
          this.linkSettings.getOperatorAddress(), this.getLinkId());
        // No-op: We're already expectedCurrentState...
        return CompletableFuture.completedFuture(null);
      }
    } catch (RuntimeException e) {
      // If we can't connect, then disconnect this account in order to trigger any listeners.
      this.disconnect().join();
      throw e;
    } catch (Exception e) {
      // If we can't connect, then disconnect this account in order to trigger any listeners.
      this.disconnect().join();
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  /**
   * Perform the logic of actually connecting to the remote peer.
   */
  public abstract CompletableFuture<Void> doConnect();

  @Override
  public void close() {
    this.disconnect().join();
  }

  @Override
  public final CompletableFuture<Void> disconnect() {
    try {
      if (this.connected.compareAndSet(CONNECTED, NOT_CONNECTED)) {
        logger.debug("[{}] `{}` disconnecting from `{}`...", this.linkSettings.getLinkType(),
          this.linkSettings.getOperatorAddress(), this.getLinkId());

        return this.doDisconnect()
          .whenComplete(($, error) -> {
            if (error == null) {
              // emit disconnected event.
              this.linkEventEmitter.emitEvent(LinkDisconnectedEvent.of(this));

              logger.debug("[{}] `{}` disconnected from `{}`.", this.linkSettings.getLinkType(),
                this.linkSettings.getOperatorAddress(), this.getLinkId());
            } else {
              final String errorMessage = String.format("[%s] `%s` error while trying to disconnect from `%s`",
                this.linkSettings.getLinkType(),
                this.linkSettings.getOperatorAddress(), this.getLinkId()
              );
              logger.error(errorMessage, error);
            }
          })
          .thenAccept(($) -> {
            logger.debug("[{}] `{}` disconnected from `{}`...", this.linkSettings.getLinkType(),
              this.linkSettings.getOperatorAddress(), this.getLinkId());
          });
      } else {
        logger.debug("[{}] `{}` already disconnected from `{}`...", this.linkSettings.getLinkType(),
          this.linkSettings.getOperatorAddress(), this.getLinkId());
        // No-op: We're already expectedCurrentState...
        return CompletableFuture.completedFuture(null);
      }
    } catch (Exception e) {
      throw new RuntimeException(e.getMessage(), e);
    }
  }

  /**
   * Perform the logic of disconnecting from the remote peer.
   */
  public abstract CompletableFuture<Void> doDisconnect();

  @Override
  public boolean isConnected() {
    return this.connected.get();
  }

  @Override
  public void registerLinkHandler(final LinkHandler ilpDataHandler)
    throws LinkHandlerAlreadyRegisteredException {
    Objects.requireNonNull(ilpDataHandler, "ilpDataHandler must not be null!");
    if (!this.linkHandlerAtomicReference.compareAndSet(null, ilpDataHandler)) {
      throw new LinkHandlerAlreadyRegisteredException(
        "DataHandler may not be registered twice. Call unregisterDataHandler first!",
        this.getLinkId().orElse(LinkId.of("Not yet set!"))
      );
    }
  }

  @Override
  public void unregisterLinkHandler() {
    this.linkHandlerAtomicReference.set(null);
  }

  @Override
  public Optional<LinkHandler> getLinkHandler() {
    return Optional.ofNullable(linkHandlerAtomicReference.get());
  }

  @Override
  public void addLinkEventListener(final LinkEventListener linkEventListener) {
    Objects.requireNonNull(linkEventListener);
    this.linkEventEmitter.addLinkEventListener(linkEventListener);
  }

  @Override
  public void removeLinkEventListener(final LinkEventListener linkEventListener) {
    Objects.requireNonNull(linkEventListener);
    this.linkEventEmitter.removeLinkEventListener(linkEventListener);
  }

  @Override
  public void removeAllLinkEventListeners() {
    this.linkEventEmitter.removeAllLinkEventListeners();
  }

  /**
   * An example {@link LinkEventEmitter} that allows events to be synchronously emitted into a {@link Link}.
   *
   * @deprecated Transition this to EventBus.
   */
  @Deprecated
  public static class SyncLinkEventEmitter implements org.interledger.connector.link.events.LinkEventEmitter {

    private final Set<LinkEventListener> dataLinkEventListeners;

    public SyncLinkEventEmitter() {
      this.dataLinkEventListeners = Sets.newHashSet();
    }

    /////////////////
    // Event Emitters
    /////////////////

    @Override
    public void emitEvent(final LinkConnectedEvent event) {
      this.dataLinkEventListeners.stream().forEach(handler -> handler.onConnect(event));
    }

    @Override
    public void emitEvent(final LinkDisconnectedEvent event) {
      this.dataLinkEventListeners.stream().forEach(handler -> handler.onDisconnect(event));
    }

    @Override
    public void emitEvent(final LinkErrorEvent event) {
      this.dataLinkEventListeners.stream().forEach(handler -> handler.onError(event));
    }

    @Override
    public void addLinkEventListener(final LinkEventListener linkEventListener) {
      Objects.requireNonNull(linkEventListener);
      this.dataLinkEventListeners.add(linkEventListener);
    }

    @Override
    public void removeLinkEventListener(final LinkEventListener linkEventListener) {
      Objects.requireNonNull(linkEventListener);
      this.dataLinkEventListeners.remove(linkEventListener);
    }

    /**
     * Removes all event listeners registered with this emitter.
     */
    @Override
    public void removeAllLinkEventListeners() {
      this.dataLinkEventListeners.clear();
    }
  }
}
