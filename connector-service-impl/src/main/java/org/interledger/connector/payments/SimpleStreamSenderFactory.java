package org.interledger.connector.payments;

import org.interledger.link.Link;
import org.interledger.stream.crypto.StreamEncryptionService;
import org.interledger.stream.sender.SimpleStreamSender;
import org.interledger.stream.sender.StreamConnectionManager;
import org.interledger.stream.sender.StreamSender;

import java.time.Duration;
import java.util.concurrent.ExecutorService;

public class SimpleStreamSenderFactory implements StreamSenderFactory {

  private final StreamEncryptionService streamEncryptionService;
  private final StreamConnectionManager streamConnectionManager;

  public SimpleStreamSenderFactory(StreamEncryptionService streamEncryptionService, StreamConnectionManager streamConnectionManager) {
    this.streamEncryptionService = streamEncryptionService;
    this.streamConnectionManager = streamConnectionManager;
  }

  @Override
  public StreamSender newStreamSender(Link link, ExecutorService executorService) {
    return new SimpleStreamSender(link,
      Duration.ofMillis(10),
      streamEncryptionService,
      streamConnectionManager,
      executorService);
  }
}
