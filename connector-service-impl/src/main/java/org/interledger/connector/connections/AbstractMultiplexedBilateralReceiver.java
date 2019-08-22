package org.interledger.connector.connections;

// TODO: Depending on the final design, consider renaming this BilateralReceiverMux.
public class AbstractMultiplexedBilateralReceiver {// implements MultiplexedBilateralReceiver {
//
//  private Map<InterledgerAddress, BilateralReceiver> bilateralReceivers;
//
//  public AbstractMultiplexedBilateralReceiver() {
//    this.bilateralReceivers = Maps.newConcurrentMap();
//  }
//
//  @Override
//  public Optional<BilateralReceiver> getBilateralReceiver(final InterledgerAddress sourceAccountAddress) {
//    Objects.requireNonNull(sourceAccountAddress);
//    return Optional.ofNullable(bilateralReceivers.get(sourceAccountAddress));
//  }
//
//  @Override
//  public void registerBilateralReceiver(final InterledgerAddress accountAddress, final BilateralReceiver receiver) {
//    Objects.requireNonNull(accountAddress);
//    Objects.requireNonNull(receiver);
//
//    // If the plugin being added has not been added to this Connection, then register this Connection as an event-listener for the plugin.
//    if (this.bilateralReceivers.put(accountAddress, receiver) == null) {
//      bilateralReceivers.put(accountAddress, receiver);
//    }
//  }
//
//  @Override
//  public void unregisterBilateralReceiver(final InterledgerAddress accountAddress) {
//    Objects.requireNonNull(accountAddress);
//    this.bilateralReceivers.remove(accountAddress);
//  }

}
