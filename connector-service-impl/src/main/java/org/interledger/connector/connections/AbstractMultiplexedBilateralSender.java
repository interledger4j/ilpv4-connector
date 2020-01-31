package org.interledger.connector.connections;

// TODO: Depending on the final design, consider renaming this BilateralSenderMux.
@Deprecated
public class AbstractMultiplexedBilateralSender {//implements MultiplexedBilateralSender {
  //
  //  private Map<InterledgerAddress, BilateralSender> bilateralSenders;
  //
  //  public AbstractMultiplexedBilateralSender() {
  //    this.bilateralSenders = Maps.newConcurrentMap();
  //  }
  //
  //  @Override
  //  public Optional<BilateralSender> getBilateralSender(final InterledgerAddress sourceAccountAddress) {
  //    Objects.requireNonNull(sourceAccountAddress);
  //    return Optional.ofNullable(bilateralSenders.get(sourceAccountAddress));
  //  }
  //
  //  @Override
  //  public void registerBilateralSender(final InterledgerAddress accountAddress, final BilateralSender sender) {
  //    Objects.requireNonNull(accountAddress);
  //    Objects.requireNonNull(sender);
  //
  //    // If the plugin being added has not been added to this Connection, then register this Connection as an event-listener for the plugin.
  //    if (this.bilateralSenders.put(accountAddress, sender) == null) {
  //      bilateralSenders.put(accountAddress, sender);
  //    }
  //  }
  //
  //  @Override
  //  public void unregisterBilateralSender(final InterledgerAddress accountAddress) {
  //    Objects.requireNonNull(accountAddress);
  //    this.bilateralSenders.remove(accountAddress);
  //  }
  //
  //  /**
  //   * Called to handle an {@link BilateralConnectionConnectedEvent}.
  //   *
  //   * @param event A {@link BilateralConnectionConnectedEvent}.
  //   */
  //  @Override
  //  public void onConnect(BilateralConnectionConnectedEvent event) {
  //    // Connect all senders...
  //    bilateralSenders.values().stream()
  //      .forEach(bilateralSender -> bilateralSender.onConnect());
  //  }
  //
  //  /**
  //   * Called to handle an {@link BilateralConnectionDisconnectedEvent}.
  //   *
  //   * @param event A {@link BilateralConnectionDisconnectedEvent}.
  //   */
  //  @Override
  //  public void onDisconnect(BilateralConnectionDisconnectedEvent event) {
  //
  //  }
}
