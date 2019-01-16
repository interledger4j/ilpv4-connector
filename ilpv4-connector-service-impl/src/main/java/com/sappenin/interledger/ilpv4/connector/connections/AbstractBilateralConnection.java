package com.sappenin.interledger.ilpv4.connector.connections;

/**
 * An abstract implementation of {@link BilateralConnection}.
 */
public abstract class AbstractBilateralConnection {//<IT extends MultiplexedBilateralReceiver,
//  OT extends MultiplexedBilateralSender,
  //  P extends Plugin<?>>
  //  implements BilateralConnection<IT, OT, P> {
  //
  //  protected final Logger logger = LoggerFactory.getLogger(this.getClass());
  //  //protected final Map<InterledgerAddress, P> plugins;
  //
  //  private final UUID bilateralConnectionId = UUID.randomUUID();
  //  private final InterledgerAddress operatorAddress;
  //
  //  // E.g., a gRPC client or server.
  //  private final IT multiplexedBilateralReceiver;
  //  private final OT multiplexedBilateralSender;
  //
  //  // The emitter used by this plugin.
  //  private BilateralConnectionEventEmitter eventEmitter;
  //
  //  private AtomicBoolean connected = new AtomicBoolean(NOT_CONNECTED);
  //
  //  /**
  //   * Required-args Constructor.
  //   *
  //   * @param operatorAddress              The {@link InterledgerAddress} of the node operating this MUX.
  //   * @param multiplexedBilateralReceiver
  //   * @param multiplexedBilateralSender
  //   */
  //  public AbstractBilateralConnection(final InterledgerAddress operatorAddress, IT multiplexedBilateralReceiver, OT multiplexedBilateralSender) {
  //    this.operatorAddress = Objects.requireNonNull(operatorAddress);
  //    this.multiplexedBilateralReceiver = Objects.requireNonNull(multiplexedBilateralReceiver);
  //    this.multiplexedBilateralSender = Objects.requireNonNull(multiplexedBilateralSender);
  //
  //    // TODO: When a Connection connects/disconnects, the sender/receiver should be triggered, which will trigger each
  //    // associated plugin.
  //    // Synchronize the MUX events with this Connection.
  //    this.multiplexedBilateralReceiver.
  //
  //    // TODO: Use EventBus instead
  //    this.eventEmitter = new SyncBilateralConnectionEventEmitter();
  //  }
  //
  //  @Override
  //  public InterledgerAddress getOperatorAddress() {
  //    return this.operatorAddress;
  //  }
  //
  //  // Plugins may or may not already be registered, but we use the BilateralConnection events as a way to get them to do
  //  // the right thing (i.e., if a Connection connects, then the plugin should connect).
  //  @Override
  //  public final CompletableFuture<Void> connect() {
  //
  //    // TODO: Should this class be aware of the remote's ILP address? On the one hand, that has nothing to do with
  //    //  bilateral connections. But on the other hand, it might be nice to see in logs.
  //    logger.debug("Connecting to bilateral remote: `{}`...", bilateralConnectionId);
  //
  //    try {
  //      if (this.connected.compareAndSet(NOT_CONNECTED, CONNECTED)) {
  //        return this.doConnect().whenComplete((result, error) -> {
  //          this.eventEmitter.emitEvent(BilateralConnectionConnectedEvent.of(this));
  //          logger.debug("ConnectionId `{}` connected!", bilateralConnectionId);
  //        });
  //      } else {
  //        // Nothing todo, we're already connected...
  //        return CompletableFuture.completedFuture(null);
  //      }
  //    } catch (RuntimeException e) {
  //      // If we can't connect, then disconnect this account in order to trigger any listeners.
  //      this.disconnect().join();
  //      throw e;
  //    }
  //  }
  //
  //  /**
  //   * Perform the logic of actually connecting to the remote peer.
  //   */
  //  public abstract CompletableFuture<Void> doConnect();
  //
  //  @Override
  //  public void close() {
  //    this.disconnect().join();
  //  }
  //
  //  @Override
  //  public final CompletableFuture<Void> disconnect() {
  //
  //    logger.debug("Disconnecting ConnectionId `{}`...", bilateralConnectionId);
  //
  //    try {
  //      if (this.connected.compareAndSet(CONNECTED, NOT_CONNECTED)) {
  //        return this.doDisconnect().thenAccept(($) -> {
  //          // In either case above, emit the disconnect event.
  //          this.eventEmitter.emitEvent(BilateralConnectionDisconnectedEvent.of(this));
  //          logger.debug("ConnectionId `{}` disconnected!", bilateralConnectionId);
  //        });
  //      } else {
  //        return CompletableFuture.completedFuture(null);
  //      }
  //    } catch (RuntimeException e) {
  //      // Even if an exception is thrown above, be sure to emit the disconnected event.
  //      this.eventEmitter.emitEvent(BilateralConnectionDisconnectedEvent.of(this));
  //      throw e;
  //    }
  //  }
  //
  //  /**
  //   * Perform the logic of disconnecting from the remote peer.
  //   */
  //  public abstract CompletableFuture<Void> doDisconnect();
  //
  //  @Override
  //  public boolean isConnected() {
  //    return this.connected.get();
  //  }
  //
  //  //  //@Override
  //  //  public Optional<P> getPlugin(final InterledgerAddress account) {
  //  //    Objects.requireNonNull(account);
  //  //    return Optional.ofNullable(this.plugins.get(account));
  //  //  }
  //
  //  //  @Override
  //  //  public void registerPlugin(final InterledgerAddress account, final P plugin) {
  //  //    Objects.requireNonNull(account);
  //  //    Objects.requireNonNull(plugin);
  //  //
  //  //    this.getMultiplexedBilateralSender().registerBilateralSender(account, plugin);
  //  //    this.getMultiplexedBilateralReceiver().registerBilateralReceiver(account, plugin);
  //  //  }
  //  //
  //  //  @Override
  //  //  public void unregisterPlugin(final InterledgerAddress account) {
  //  //    this.getPlugin(account).ifPresent(plugin -> {
  //  //      plugin.removePluginEventListener(this.bilateralConnectionId);
  //  //      plugin.disconnect();
  //  //    });
  //  //
  //  //    this.plugins.remove(account);
  //  //  }
  //
  //  /**
  //   * When a plugin connects, addAccount a reference to it in this plugin.
  //   */
  //  @Override
  //  public void onConnect(final PluginConnectedEvent event) {
  //    // TODO: Determine how plugins should register. Seems like it shouldn't be event-driven since an inoming connection should
  //    // construct a plugin, and then register it in this class.
  //
  //    // If there are any
  //
  //    // If a plugin connects, it means that auth succeeded, so move that plugin into the authenticated Map.
  //    // this.registerPlugin(event.getPlugin().getPluginSettings().getAccountAddress(), (P) event.getPlugin());
  //
  //    // No-op by default.
  //  }
  //
  //  /**
  //   * Remove the plugin from this connection if it's disconnected.
  //   */
  //  @Override
  //  public void onDisconnect(final PluginDisconnectedEvent event) {
  //    this.unregisterPlugin(event.getPlugin().getPluginSettings().getAccountAddress());
  //  }
  //
  //  //  @Override
  //  //  public void onError(final PluginErrorEvent event) {
  //  //    // Log the error.
  //  //    logger.error(event.getError().getMessage(), event.getError());
  //  //  }
  //
  //  @Override
  //  public IT getMultiplexedBilateralReceiver() {
  //    return this.multiplexedBilateralReceiver;
  //  }
  //
  //  @Override
  //  public OT getMultiplexedBilateralSender() {
  //    return this.multiplexedBilateralSender;
  //  }
  //
  //  /**
  //   * An example {@link PluginEventEmitter} that allows events to be synchronously emitted into a {@link Plugin}.
  //   *
  //   * @deprecated Transition this to EventBus.
  //   */
  //  @Deprecated
  //  public static class SyncBilateralConnectionEventEmitter implements BilateralConnectionEventEmitter {
  //
  //    private final Map<UUID, BilateralConnectionEventListener> eventListeners;
  //
  //    public SyncBilateralConnectionEventEmitter() {
  //      this.eventListeners = Maps.newConcurrentMap();
  //    }
  //
  //    /////////////////
  //    // Event Emitters
  //    /////////////////
  //
  //    @Override
  //    public void emitEvent(final BilateralConnectionConnectedEvent event) {
  //      this.eventListeners.values().stream().forEach(handler -> handler.onConnect(event));
  //    }
  //
  //    @Override
  //    public void emitEvent(final BilateralConnectionDisconnectedEvent event) {
  //      this.eventListeners.values().stream().forEach(handler -> handler.onDisconnect(event));
  //    }
  //
  //    @Override
  //    public void addBilateralConnectionEventListener(
  //      final UUID listenerId, final BilateralConnectionEventListener eventListener
  //    ) {
  //      Objects.requireNonNull(eventListener);
  //      this.eventListeners.put(listenerId, eventListener);
  //    }
  //
  //    @Override
  //    public void removeBilateralConnectionEventListener(final UUID eventHandlerId) {
  //      Objects.requireNonNull(eventHandlerId);
  //      this.eventListeners.remove(eventHandlerId);
  //    }
  //  }
}
