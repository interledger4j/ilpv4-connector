package org.interledger.connector.stream;

public interface TrackingStreamReceiverSupplier {

  TrackingStreamReceiver get(TrackingStreamReceiverLinkSettings linkSettings);

}
