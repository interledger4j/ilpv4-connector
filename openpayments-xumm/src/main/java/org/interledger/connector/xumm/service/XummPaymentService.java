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
import org.interledger.connector.xumm.model.callback.PayloadCallback;
import org.interledger.connector.xumm.model.payload.ImmutablePayloadRequest;
import org.interledger.connector.xumm.model.payload.Payload;
import org.interledger.connector.xumm.model.payload.PayloadRequest;
import org.interledger.connector.xumm.model.payload.PayloadRequestResponse;
import org.interledger.connector.xumm.model.payload.TxJson;
import org.interledger.openpayments.SendXrpPaymentRequest;
import org.interledger.openpayments.events.XrpPaymentCompletedEvent;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import com.google.common.io.BaseEncoding;
import com.google.common.primitives.UnsignedLong;
import org.slf4j.Logger;

public class XummPaymentService implements PaymentSystemFacade<XrpPayment, XrpPaymentDetails> {

  private static final Logger LOGGER = getLogger(XummPaymentService.class);

  private final XummClient xummClient;
  private final XummUserTokenService xummUserTokenService;
  private final EventBus eventBus;
  private final ObjectMapper objectMapper;

  public XummPaymentService(XummClient xummClient, XummUserTokenService xummUserTokenService, EventBus eventBus, ObjectMapper objectMapper) {
    this.xummClient = xummClient;
    this.xummUserTokenService = xummUserTokenService;
    this.eventBus = eventBus;
    this.objectMapper = objectMapper;
  }

  @Override
  public XrpPayment payInvoice(
    XrpPaymentDetails paymentDetails,
    AccountId senderAccountId,
    UnsignedLong amount,
    CorrelationId correlationId) {
    ImmutablePayloadRequest.Builder builder = PayloadRequest.builder()
      .txjson(TxJson.builder()
        .transactionType("Payment")
        .destination(paymentDetails.address())
        .destinationTag(paymentDetails.addressTag())
        .amount(amount)
        .fee(UnsignedLong.valueOf(12))
        .addMemos(invoicePaymentMemo(correlationId.value()))
        .build()
      )
      .customMeta(CustomMeta.builder()
        .instruction(paymentDetails.instructions())
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
    return xummUserTokenService.findByUserId(senderAccountId.value())
      .map(userToken -> {
        builder.userToken(userToken.userToken());

        try {
          LOGGER.info("Sending: " + objectMapper.writeValueAsString(builder.build()));
        } catch (JsonProcessingException e) {
        }
        xummClient.createPayload(builder.build());
        return XrpPayment.builder()
          .build();

      }).orElseGet(() -> {
        try {
          LOGGER.info("Sending: " + objectMapper.writeValueAsString(builder.build()));
        } catch (JsonProcessingException e) {
        }
        PayloadRequestResponse response = xummClient.createPayload(builder.build());
        return XrpPayment.builder()
          .userAuthorizationUrl(response.next().always())
          .build();
      });
  }

  @Override
  public XrpPaymentDetails getPaymentDetails(Invoice invoice) {
    return XrpPaymentDetails.builder()
      .address("rPdvC6ccq8hCdPKSPJkPmyZ4Mi1oG2FFkT") // FIXME
      .invoiceIdHash(invoice.correlationId().value())
      .instructions("Invoice from " + invoice.subject())
      .build();
  }

  @Override
  public Class<XrpPayment> getResultType() {
    return XrpPayment.class;
  }

  @Override
  public Class<XrpPaymentDetails> getDetailsType() {
    return XrpPaymentDetails.class;
  }

  public boolean handle(PayloadCallback payloadCallback) {
    LOGGER.debug("Got callback with value={}", payloadCallback);
    return payloadCallback.customMeta().blob()
      .map(request -> {
        xummUserTokenService.saveUserToken(request.accountId(), payloadCallback.userToken());

        Payload payload = xummClient.getPayload(payloadCallback.meta().payloadUuidV4());

        // TODO check if the XRP transaction was successful. For now, assume it was.
        eventBus.post(XrpPaymentCompletedEvent.builder()
          .payment(XrplTransaction.builder()
            .addMemos(invoicePaymentMemo(request.correlationId().toLowerCase()))
            .account(payload.response().account())
            .destination(payload.payload().destination())
            .destinationTag(payload.payload().destinationTag().orElse(null))
            .amount(request.amountInDrops())
            .hash(payload.response().txid())
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
        .memoType(hexEncode("INVOICE_PAYMENT"))
        .memoData(hexEncode(correlationId.toUpperCase()))
        .build())
      .build();
  }

  private static String hexEncode(String value) {
    return BaseEncoding.base16().encode(value.getBytes()).toUpperCase();
  }

  private static String hexDecode(String value) {
    return new String(BaseEncoding.base16().decode(value));
  }

}
