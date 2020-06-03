package org.interledger.connector.xumm.service;

import static org.slf4j.LoggerFactory.getLogger;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.opa.PaymentSystemFacade;
import org.interledger.connector.opa.model.CorrelationId;
import org.interledger.connector.opa.model.ImmutableMemoWrapper;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.Memo;
import org.interledger.connector.opa.model.MemoWrapper;
import org.interledger.connector.opa.model.XrpPayment;
import org.interledger.connector.opa.model.XrpPaymentDetails;
import org.interledger.connector.opa.model.XrplTransaction;
import org.interledger.connector.xumm.client.XummClient;
import org.interledger.connector.xumm.model.CustomMeta;
import org.interledger.connector.xumm.model.XummPaymentRequest;
import org.interledger.connector.xumm.model.callback.PayloadCallback;
import org.interledger.connector.xumm.model.payload.ImmutablePayloadRequest;
import org.interledger.connector.xumm.model.payload.Payload;
import org.interledger.connector.xumm.model.payload.PayloadRequest;
import org.interledger.connector.xumm.model.payload.PayloadRequestResponse;
import org.interledger.connector.xumm.model.payload.TxJson;
import org.interledger.openpayments.SendXrpPaymentRequest;
import org.interledger.openpayments.events.XrpPaymentCompletedEvent;

import com.google.common.eventbus.EventBus;
import com.google.common.primitives.UnsignedLong;
import org.slf4j.Logger;

public class XummPaymentService implements PaymentSystemFacade<XrpPayment, XrpPaymentDetails> {

  private static final Logger LOGGER = getLogger(XummPaymentService.class);

  private final XummClient xummClient;
  private final XummUserTokenService xummUserTokenService;
  private final EventBus eventBus;

  public XummPaymentService(XummClient xummClient, XummUserTokenService xummUserTokenService, EventBus eventBus) {
    this.xummClient = xummClient;
    this.xummUserTokenService = xummUserTokenService;
    this.eventBus = eventBus;
  }

  @Override
  public XrpPayment payInvoice(
    XrpPaymentDetails paymentDetails,
    AccountId senderAccountId,
    UnsignedLong amount,
    CorrelationId correlationId) {
    ImmutablePayloadRequest.Builder builder = PayloadRequest.builder()
      .txjson(TxJson.builder()
        .destination(paymentDetails.address())
        .destinationTag(paymentDetails.addressTag())
        .amount(amount)
        .fee(UnsignedLong.valueOf(12))
        .addMemos(invoicePaymentMemo(correlationId.value()))
        .build()
      )
      .customMeta(CustomMeta.builder()
        .instruction(paymentDetails.instructions().orElse(""))
        .blob(SendXrpPaymentRequest.builder()
          .destinationTag(paymentDetails.addressTag())
          .instructionsToUser(paymentDetails.instructions())
          .correlationId(correlationId.value())
          .destinationAddress(paymentDetails.address())
          .amountInDrops(amount)
          .accountId(senderAccountId.value())
          .build()
        )
        .build()
      );
    xummUserTokenService.findByUserId(senderAccountId.value()).ifPresent(userToken ->
      builder.userToken(userToken.userToken()));

    PayloadRequestResponse response = xummClient.createPayload(builder.build());
    return XrpPayment.builder()
      .userAuthorizationUrl(response.next().always())
      .build();
  }

  @Override
  public XrpPaymentDetails getPaymentDetails(Invoice invoice) {
    return XrpPaymentDetails.builder()
      .address("rP3t3JStqWPYd8H88WfBYh3v84qqYzbHQ6") // FIXME
      .invoiceIdHash(invoice.correlationId().value())
      .build();
  }

  public boolean handle(PayloadCallback payloadCallback) {
    LOGGER.debug("Got callback with value={}", payloadCallback);
    return payloadCallback.customMeta().blob()
      .filter(value -> value instanceof XummPaymentRequest)
      .map(value -> (SendXrpPaymentRequest) value)
      .map(request -> {
        xummUserTokenService.saveUserToken(request.accountId(), payloadCallback.userToken());

        Payload payload = xummClient.getPayload(payloadCallback.meta().payloadUuidV4());

        // TODO check if the XRP transaction was successful. For now, assume it was.
        eventBus.post(XrpPaymentCompletedEvent.builder()
          .payment(XrplTransaction.builder()
            .addMemos(invoicePaymentMemo(request.correlationId()))
            .account(payload.response().account())
            .destination(payload.payload().destination())
            .destinationTag(payload.payload().destinationTag().orElse(null))
            .amount(request.amountInDrops())
            .createdAt(payload.response().resolvedAt())
            .build()
          )
          .build()
        );
        return true;
      })
      .orElseGet(() -> {
        LOGGER.info("Payload did not contain a paymentRequest. Ignoring.");
        return false;
      });
  }

  public ImmutableMemoWrapper invoicePaymentMemo(String correlationId) {
    return MemoWrapper.builder()
      .memo(Memo.builder()
        .memoType("INVOICE_PAYMENT")
        .memoData(correlationId)
        .build())
      .build();
  }
}
