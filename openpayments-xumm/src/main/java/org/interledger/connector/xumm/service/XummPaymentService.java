package org.interledger.connector.xumm.service;

import static org.slf4j.LoggerFactory.getLogger;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.payid.PayIdClient;
import org.interledger.connector.payid.PayIdResponse;
import org.interledger.connector.xumm.client.XummClient;
import org.interledger.connector.xumm.model.CustomMeta;
import org.interledger.connector.xumm.model.callback.PayloadCallback;
import org.interledger.connector.xumm.model.payload.ImmutablePayloadRequest;
import org.interledger.connector.xumm.model.payload.Payload;
import org.interledger.connector.xumm.model.payload.PayloadRequest;
import org.interledger.connector.xumm.model.payload.PayloadRequestResponse;
import org.interledger.connector.xumm.model.payload.TxJson;
import org.interledger.openpayments.CorrelationId;
import org.interledger.openpayments.Invoice;
import org.interledger.openpayments.PayId;
import org.interledger.openpayments.PaymentNetwork;
import org.interledger.openpayments.SendXrpPaymentRequest;
import org.interledger.openpayments.XrpPaymentDetails;
import org.interledger.openpayments.events.XrpPaymentCompletedEvent;
import org.interledger.openpayments.xrpl.Memo;
import org.interledger.openpayments.xrpl.MemoWrapper;
import org.interledger.openpayments.xrpl.XrplTransaction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import com.google.common.io.BaseEncoding;
import com.google.common.primitives.UnsignedLong;
import io.xpring.xrpl.ClassicAddress;
import io.xpring.xrpl.Utils;
import org.interleger.openpayments.PaymentSystemFacade;
import org.slf4j.Logger;

public class XummPaymentService implements PaymentSystemFacade<XrplTransaction, XrpPaymentDetails> {

  private static final Logger LOGGER = getLogger(XummPaymentService.class);

  private final XummClient xummClient;
  private final PayIdClient payIdClient;
  private final XummUserTokenService xummUserTokenService;
  private final EventBus eventBus;
  private final ObjectMapper objectMapper;

  public XummPaymentService(XummClient xummClient, PayIdClient payIdClient, XummUserTokenService xummUserTokenService, EventBus eventBus, ObjectMapper objectMapper) {
    this.xummClient = xummClient;
    this.payIdClient = payIdClient;
    this.xummUserTokenService = xummUserTokenService;
    this.eventBus = eventBus;
    this.objectMapper = objectMapper;
  }

  @Override
  public XrplTransaction payInvoice(
    XrpPaymentDetails paymentDetails,
    AccountId senderAccountId,
    UnsignedLong amount,
    CorrelationId correlationId) {
    ImmutablePayloadRequest.Builder builder = PayloadRequest.builder()
      .txjson(filterToClassicAddress(TxJson.builder()
        .transactionType("Payment")
        .destination(paymentDetails.address())
        .destinationTag(paymentDetails.addressTag())
        .amount(amount)
        .fee(UnsignedLong.valueOf(12))
        .addMemos(invoicePaymentMemo(correlationId.value()))
        .build())
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
        return XrplTransaction.builder()
          .build();

      }).orElseGet(() -> {
        try {
          LOGGER.info("Sending: " + objectMapper.writeValueAsString(builder.build()));
        } catch (JsonProcessingException e) {
        }
        PayloadRequestResponse response = xummClient.createPayload(builder.build());
        return XrplTransaction.builder()
          .userAuthorizationUrl(response.next().always())
          .build();
      });
  }

  @Override
  public XrpPaymentDetails getPaymentDetails(Invoice invoice) {
    PayIdResponse response = payIdClient.getPayId(PayId.of(invoice.subject()),
      PaymentNetwork.XRPL,
      "testnet" // FIXME make config
    );

    if (response.addresses().isEmpty()) {
      throw new IllegalStateException("no XRPL address found for payid " + invoice.subject());
    }

    return XrpPaymentDetails.builder()
      .address(response.addresses().get(0).addressDetails().address())
      .invoiceIdHash(invoice.correlationId().value())
      .instructions("Invoice from " + invoice.subject().replaceFirst("payid:", ""))
      .build();
  }

  @Override
  public Class<XrplTransaction> getResultType() {
    return XrplTransaction.class;
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

        if (payloadCallback.payloadResponse().signed()) {
          onSignedPayload(request, payload);
        } else {
          onRejectedPayload(request, payload);
        }
        return true;
      })
      .orElseGet(() -> {
        LOGGER.info("Payload did not contain a paymentRequest. Ignoring.");
        return false;
      });
  }

  private void onRejectedPayload(SendXrpPaymentRequest request, Payload payload) {
    // TODO send payment rejected event
  }

  public void onSignedPayload(SendXrpPaymentRequest request, Payload payload) {
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
  }

  public MemoWrapper invoicePaymentMemo(String correlationId) {
    return MemoWrapper.builder()
      .memo(Memo.builder()
        .memoType(hexEncode("INVOICE_PAYMENT"))
        .memoData(hexEncode(correlationId))
        .build())
      .build();
  }

  private static String hexEncode(String value) {
    return BaseEncoding.base16().encode(value.getBytes()).toUpperCase();
  }

  private static TxJson filterToClassicAddress(TxJson txJson) {
    if (Utils.isValidXAddress(txJson.destination())) {
      ClassicAddress classicAddress = Utils.decodeXAddress(txJson.destination());
      return TxJson.builder().from(txJson)
        .destination(classicAddress.address())
        .destinationTag(classicAddress.tag())
        .build();
    }
    return txJson;
  }

}
