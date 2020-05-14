package org.interledger.connector.payments;

import org.interledger.link.Link;
import org.interledger.stream.sender.StreamSender;

import java.util.concurrent.ExecutorService;

public interface StreamSenderFactory {

  StreamSender newStreamSender(Link link, ExecutorService executorService);

}
