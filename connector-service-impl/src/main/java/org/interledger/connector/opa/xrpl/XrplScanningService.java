package org.interledger.connector.opa.xrpl;

import org.interledger.connector.opa.model.XrplMessage;
import org.interledger.openpayments.events.XrpPaymentCompletedEvent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import javax.annotation.PreDestroy;

public class XrplScanningService {

  private final EventBus eventBus;

  private final OkHttpClient xrplHttpClient;

  private final Supplier<Request> xrplWebsocketRequestSupplier;

  private final ObjectMapper objectMapper;

  private final AtomicReference<WebSocket> xrplWebSocket = new AtomicReference<>();

  private final int failureThreshold;

  private final Logger logger = LoggerFactory.getLogger(getClass());

  public XrplScanningService(final OkHttpClient xrplHttpClient,
                             final Supplier<Request> xrplWebsocketRequestSupplier,
                             final EventBus eventBus,
                             final ObjectMapper objectMapper,
                             final int failureThreshold) {
    this.xrplHttpClient = xrplHttpClient;
    this.xrplWebsocketRequestSupplier = xrplWebsocketRequestSupplier;
    this.eventBus = eventBus;
    this.objectMapper = objectMapper;
    this.failureThreshold = failureThreshold;
  }

  public void init() {
    connect();
  }

  private void connect() {
    XrplListener listener = new XrplListener(objectMapper, failureThreshold, () -> reconnectDueToFailure(), eventBus);
    xrplWebSocket.set(xrplHttpClient.newWebSocket(this.xrplWebsocketRequestSupplier.get(), listener));
  }

  private void reconnectDueToFailure() {
    try {
      xrplWebSocket.get().close(1002, "Problems communicating within Java ILP connector");
    } catch (Exception e) {
      logger.error("Failure attempting to close XRPL websocket", e);
    }

    try {
      connect();
    } catch (Exception e) {
      logger.error("Failure attempting to reconnect to XRPL websocket", e);
    }
  }

  @PreDestroy
  public void preDestroy() {
    // Trigger shutdown of the dispatcher's executor so this process can exit cleanly.
    try {
      if (xrplWebSocket.get() != null) {
        xrplWebSocket.get().close(1001, "Terminating server");
      }
    }
    catch (Exception e) {
      logger.warn("Couldn't close XRPL websocket gracefully", e);
    }
    xrplHttpClient.dispatcher().executorService().shutdown();
  }

  private static class XrplListener extends WebSocketListener {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final ObjectMapper objectMapper;

    private final AtomicInteger failureCount = new AtomicInteger();

    private final int failureThreshold;

    private final Runnable reconnectDueToFailure;

    private final EventBus eventBus;

    public XrplListener(final ObjectMapper objectMapper,
                        final int failureThreshold,
                        final Runnable reconnectDueToFailure,
                        final EventBus eventBus) {
      this.objectMapper = objectMapper;
      this.failureThreshold = failureThreshold;
      this.reconnectDueToFailure = reconnectDueToFailure;
      this.eventBus = eventBus;
    }

    @Override
    public void onOpen(WebSocket webSocket, Response response) {
      logger.info("websocket opened");
      webSocket.send("{ \"command\": \"subscribe\", \"streams\": [\"transactions\"] }");
    }

    @Override
    public void onMessage(WebSocket webSocket, String text) {
      logger.info("MESSAGE text: " + text);
      try {
        XrplMessage xrplMessage = objectMapper.readValue(text, XrplMessage.class);
        if (xrplMessage.isSuccessfulTransaction()) {
          eventBus.post(XrpPaymentCompletedEvent.builder().payment(xrplMessage.transaction()).build());
        }
      } catch (JsonProcessingException e) {
        logger.error("Cannot read transaction JSON " + text, e);
      }
    }

    @Override
    public void onClosing(WebSocket webSocket, int code, String reason) {
      webSocket.close(1000, null);
      logger.info("CLOSE: " + code + " " + reason);
    }

    @Override
    public void onFailure(WebSocket webSocket, Throwable t, Response response) {
      if (failureCount.incrementAndGet() >= failureThreshold) {
        reconnectDueToFailure.run();
      }
      logger.error("Failure reading from XRPL websocket", t);
    }
  }


}
