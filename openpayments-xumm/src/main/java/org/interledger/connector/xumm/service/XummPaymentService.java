package org.interledger.connector.xumm.service;

import static org.slf4j.LoggerFactory.getLogger;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.payid.PayIdClient;
import org.interledger.connector.payid.PayIdResponse;
import org.interledger.connector.xumm.client.XummClient;
import org.interledger.connector.xumm.model.ApproveMandateRequestWrapper;
import org.interledger.connector.xumm.model.CustomMeta;
import org.interledger.connector.xumm.model.ReturnUrl;
import org.interledger.connector.xumm.model.SendXrpRequestWrapper;
import org.interledger.connector.xumm.model.callback.PayloadCallback;
import org.interledger.connector.xumm.model.payload.ImmutablePayloadRequest;
import org.interledger.connector.xumm.model.payload.Options;
import org.interledger.connector.xumm.model.payload.Payload;
import org.interledger.connector.xumm.model.payload.PayloadRequest;
import org.interledger.connector.xumm.model.payload.PayloadRequestResponse;
import org.interledger.connector.xumm.model.payload.TxJson;
import org.interledger.openpayments.ApproveMandateRequest;
import org.interledger.openpayments.AuthorizationUrls;
import org.interledger.openpayments.CorrelationId;
import org.interledger.openpayments.Invoice;
import org.interledger.openpayments.PayId;
import org.interledger.openpayments.PaymentNetwork;
import org.interledger.openpayments.SendXrpPaymentRequest;
import org.interledger.openpayments.UserAuthorizationRequiredException;
import org.interledger.openpayments.XrpPaymentDetails;
import org.interledger.openpayments.events.MandateApprovedEvent;
import org.interledger.openpayments.events.MandateDeclinedEvent;
import org.interledger.openpayments.events.PaymentDeclinedEvent;
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
    CorrelationId correlationId
  )
    throws UserAuthorizationRequiredException {
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
        .blob(
          SendXrpRequestWrapper.of(
            senderAccountId,
            SendXrpPaymentRequest.builder()
              .destinationTag(paymentDetails.addressTag())
              .correlationId(correlationId.value())
              .destinationAddress(paymentDetails.address())
              .amountInDrops(amount)
              .build()
          )
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
          .amount(amount)
          .destination(paymentDetails.address())
          .account("pending")
          .hash("pending")
          .build();

      }).orElseThrow(() -> {
        try {
          LOGGER.info("Sending: " + objectMapper.writeValueAsString(builder.build()));
        } catch (JsonProcessingException e) {
        }
        PayloadRequestResponse response = xummClient.createPayload(builder.build());
        return new UserAuthorizationRequiredException(response.next().always());
      });
  }

  @Override
  public AuthorizationUrls getMandateAuthorizationUrls(ApproveMandateRequest request) {
    ImmutablePayloadRequest.Builder builder = PayloadRequest.builder()
      .txjson(TxJson.builder()
        .transactionType("SignIn")
        .build()
      )
      .customMeta(CustomMeta.builder()
        .instruction(request.memoToUser())
        .blob(
          ApproveMandateRequestWrapper.of(request.accountId(), request)
        )
        .build()
      );

    request.redirectUrl().ifPresent(url ->
    {
      String returnUrl = url.newBuilder()
        .addEncodedQueryParameter("txid", "{txid}").toString();
      builder.options(Options.builder()
        .returnUrl(ReturnUrl.builder()
          .web(returnUrl)
          .app(returnUrl)
          .build())
        .build()
      );
    });

    xummUserTokenService.findByUserId(request.accountId().value())
      .ifPresent(token -> builder.userToken(token.userToken()));

    try {
      LOGGER.info("Sending: " + objectMapper.writeValueAsString(builder.build()));
    } catch (JsonProcessingException e) {
      // shouldn't happen
      LOGGER.error("unexpected json error", e);
      throw new RuntimeException(e);
    }
    PayloadRequestResponse response = xummClient.createPayload(builder.build());
    return AuthorizationUrls.builder()
      .pageUrl(response.next().always())
      .imageUrl(response.refs().qrPng())
      .build();
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
        xummUserTokenService.saveUserToken(request.accountId().value(), payloadCallback.userToken());
        Payload payload = xummClient.getPayload(payloadCallback.meta().payloadUuidV4());

        if (request instanceof SendXrpRequestWrapper) {
          handleSendXrpRequestCallback(payloadCallback, payload, ((SendXrpRequestWrapper) request).value());
        } else if (request instanceof ApproveMandateRequestWrapper) {
          handleApproveMandateCallback(payloadCallback, ((ApproveMandateRequestWrapper) request).value());
        }
        return true;
      })
      .orElseGet(() -> {
        LOGGER.info("Payload did not contain a paymentRequest. Ignoring.");
        return false;
      });
  }

  private void handleApproveMandateCallback(PayloadCallback payloadCallback,
                                            ApproveMandateRequest request) {
    if (isSigned(payloadCallback)) {
      eventBus.post(MandateApprovedEvent.builder()
        .accountId(request.accountId())
        .mandateId(request.mandateId())
        .build()
      );
    } else {
      eventBus.post(MandateDeclinedEvent.builder()
        .accountId(request.accountId())
        .mandateId(request.mandateId())
        .build()
      );
    }
  }

  public void handleSendXrpRequestCallback(PayloadCallback payloadCallback,
                                           Payload payload,
                                           SendXrpPaymentRequest request) {
    if (isSigned(payloadCallback)) {
      onSignedPayload(request, payload);
    } else {
      onRejectedPayload(request, payload);
    }
  }

  public boolean isSigned(PayloadCallback payloadCallback) {
    return payloadCallback.payloadResponse().signed();
  }

  private void onRejectedPayload(SendXrpPaymentRequest request, Payload payload) {
    eventBus.post(PaymentDeclinedEvent.builder()
      .paymentCorrelationId(CorrelationId.of(request.correlationId()))
      .build());
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
    return txJson.destination().map(destination -> {
      if (Utils.isValidXAddress(destination)) {
        ClassicAddress classicAddress = Utils.decodeXAddress(destination);
        return TxJson.builder().from(txJson)
          .destination(classicAddress.address())
          .destinationTag(classicAddress.tag())
          .build();
      }
      return txJson;
    }).orElse(txJson);
  }

}
