package org.interledger.connector.wallet;

import org.interledger.codecs.ilp.InterledgerCodecContextFactory;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.opa.OpaPaymentService;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.OpenPaymentsMetadata;
import org.interledger.connector.opa.model.PayIdOpaPaymentRequest;
import org.interledger.connector.opa.model.PaymentResponse;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.link.LinkId;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.spsp.PaymentPointer;
import org.interledger.spsp.PaymentPointerResolver;
import org.interledger.spsp.StreamConnectionDetails;
import org.interledger.stream.Denomination;
import org.interledger.stream.SendMoneyRequest;
import org.interledger.stream.SendMoneyResult;
import org.interledger.stream.crypto.JavaxStreamEncryptionService;
import org.interledger.stream.sender.FixedSenderAmountPaymentTracker;
import org.interledger.stream.sender.SimpleStreamSender;
import org.interledger.stream.sender.StreamConnectionManager;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.annotations.VisibleForTesting;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class IlpOpaPaymentService implements OpaPaymentService {
  public static final int SEND_TIMEOUT = 60;
  private static final okhttp3.MediaType APPLICATION_JSON = okhttp3.MediaType.parse(MediaType.APPLICATION_JSON_VALUE);
  private PaymentPointerResolver paymentPointerResolver;
  private AccountSettingsRepository accountSettingsRepository;
  private OpenPaymentsClient openPaymentsClient;
  private OkHttpClient okHttpClient;
  private ObjectMapper objectMapper;
  private HttpUrl connectorUrl;
  private ExecutorService executorService;

  public IlpOpaPaymentService(final PaymentPointerResolver paymentPointerResolver,
                              final AccountSettingsRepository accountSettingsRepository,
                              final OpenPaymentsClient openPaymentsClient,
                              final OkHttpClient okHttpClient,
                              final ObjectMapper objectMapper,
                              final HttpUrl connectorUrl,
                              final InterledgerAddressPrefix opaAddressPrefix) {
    this.paymentPointerResolver = Objects.requireNonNull(paymentPointerResolver);
    this.accountSettingsRepository = Objects.requireNonNull(accountSettingsRepository);
    this.openPaymentsClient = Objects.requireNonNull(openPaymentsClient);
    this.okHttpClient = Objects.requireNonNull(okHttpClient);
    this.objectMapper = Objects.requireNonNull(objectMapper);
    this.connectorUrl = Objects.requireNonNull(connectorUrl);
    this.executorService = Executors.newFixedThreadPool(20);
    this.spspAddressPrefix = Objects.requireNonNull(opaAddressPrefix);
  }

  // Used by STREAM sender to tell the receiver what it's ILP address is so that the receiver can theoretically send
  // packets back to the sender, if desired.
  private InterledgerAddressPrefix spspAddressPrefix;

  @Override
  public PaymentResponse sendOpaPayment(PayIdOpaPaymentRequest payIdOpaPaymentRequest,
                                        String accountId,
                                        String bearerToken) throws Exception {
    String payId = payIdOpaPaymentRequest.destinationPayId();


    PaymentPointer desinationPaymentPointer = PaymentPointer.of(payId);
    HttpUrl receiverUrl = paymentPointerResolver.resolveHttpUrl(desinationPaymentPointer);
    HttpUrl receiverBaseUrl = new HttpUrl.Builder()
      .scheme(receiverUrl.scheme())
      .host(receiverUrl.host())
      .port(receiverUrl.port())
      .build();

    AccountId typedAccountId = AccountId.of(accountId);

    AccountSettings accountSettings = accountSettingsRepository.findByAccountIdWithConversion(typedAccountId)
      .orElseThrow(() -> new AccountNotFoundProblem(typedAccountId));

    // Create an invoice on the Open Payments server of the receiver
    Invoice invoice = Invoice.builder()
      .assetCode(accountSettings.assetCode())
      .assetScale((short) accountSettings.assetScale())
      .accountId(accountId)
      .amount(payIdOpaPaymentRequest.amount())
      .subject(payId)
      .expiresAt(Instant.MAX) // TODO: What should this expires time be?
      .build();

//    OpenPaymentsMetadata receiverMetadata = openPaymentsClient.getMetadata(receiverBaseUrl);
    OpenPaymentsMetadata receiverMetadata = this.getReceiverMetadata(receiverBaseUrl);

//    openPaymentsClient.createInvoice(receiverMetadata.invoicesEndpoint().toString(), invoice);
    this.createInvoice(receiverMetadata.invoicesEndpoint(), invoice);

    // Create an invoice receipt
    // TODO: Create receipt stuff

    // if payment amount > 15000 XRP, let the PayId server know
    if (payIdOpaPaymentRequest.amount().longValue() >= (15000 * 10 ^ accountSettings.assetScale())) {
      // TODO: PayId stuff
    }

    // Pay the created invoice
    return this.payInvoice(invoice, receiverMetadata.invoicesEndpoint(), accountSettings, bearerToken);
  }

  private PaymentResponse payInvoice(Invoice invoice,
                                     HttpUrl invoiceEndpoint,
                                     AccountSettings senderAccountSettings,
                                     String bearerToken) throws Exception {
//    StreamConnectionDetails connectionDetails = openPaymentsClient.getInvoicePaymentDetails(
//      invoiceEndpoint.toString(), invoice.id().value()
//    );
    StreamConnectionDetails connectionDetails = this.getInvoicePaymentDetails(invoiceEndpoint, invoice.id().value());

    // TODO: https://github.com/xpring-eng/hermes-ilp/issues/50
    // SenderAddress is {connectorIlpAddress}.{spsp-prefix}.{accountId}.{shared_secret}
    final InterledgerAddress senderAddress = InterledgerAddress.of(
      spspAddressPrefix.with(senderAccountSettings.accountId().value()
      ).with("NotYetImplemented").getValue());

    IlpOverHttpLink link = newIlpOverHttpLink(senderAddress, senderAccountSettings.accountId(), bearerToken);

    SimpleStreamSender simpleStreamSender = createSimpleStreamSender(link);

    // Send payment using STREAM
    SendMoneyResult paymentResult = simpleStreamSender.sendMoney(
      SendMoneyRequest.builder()
        .sourceAddress(senderAddress)
        .amount(invoice.amount())
        .denomination(Denomination.builder()
          .assetCode(senderAccountSettings.assetCode())
          .assetScale((short) senderAccountSettings.assetScale())
          .build())
        .destinationAddress(connectionDetails.destinationAddress())
        .timeout(Duration.ofSeconds(SEND_TIMEOUT))
        .paymentTracker(new FixedSenderAmountPaymentTracker(invoice.amount()))
        .sharedSecret(connectionDetails.sharedSecret())
        .build()
    ).get();

    return PaymentResponse.builder()
      .amountDelivered(paymentResult.amountDelivered())
      .amountSent(paymentResult.amountSent())
      .originalAmount(paymentResult.originalAmount())
      .successfulPayment(paymentResult.successfulPayment())
      .build();
  }

  private OpenPaymentsMetadata getReceiverMetadata(HttpUrl receiverBaseUrl) throws Exception {
    Request request = new Request.Builder()
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .url(receiverBaseUrl.toString() + ".well-known/open-payments")
      .get()
      .build();
    try (Response response = okHttpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new Exception("Unable to get receiver Metadata. Message: " + response.message());
      }
      return objectMapper.readValue(response.body().charStream(), OpenPaymentsMetadata.class);
    } catch (Exception e) {
      throw e;
    }
  }

  private Invoice createInvoice(HttpUrl invoicesEndpoint, Invoice invoice) throws Exception {
    Request request = new Request.Builder()
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .url(invoicesEndpoint)
      .post(RequestBody.create(
        objectMapper.writeValueAsString(invoice),
        APPLICATION_JSON
      ))
      .build();

    try (Response response = okHttpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new Exception("Unable to create invoice. Message: " + response.message());
      }
      return objectMapper.readValue(response.body().charStream(), Invoice.class);
    } catch (Exception e) {
      throw e;
    }
  }

  private StreamConnectionDetails getInvoicePaymentDetails(HttpUrl invoicesEndpoint, String invoiceId) throws Exception {
    Request request = new Request.Builder()
      .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
      .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
      .url(invoicesEndpoint + invoiceId)
      .method("OPTIONS", null)
      .build();

    try (Response response = okHttpClient.newCall(request).execute()) {
      if (!response.isSuccessful()) {
        throw new Exception("Unable to get receiver Metadata. Message: " + response.message());
      }
      return objectMapper.readValue(response.body().charStream(), StreamConnectionDetails.class);
    } catch (Exception e) {
      throw e;
    }
  }

  /**
   * Need this to be extracted to a method so we can mock SimpleStreamSender in tests
   * @param link
   * @return
   */
  @VisibleForTesting
  protected SimpleStreamSender createSimpleStreamSender(IlpOverHttpLink link) {
    return new SimpleStreamSender(
        link, Duration.ofMillis(10L), new JavaxStreamEncryptionService(), new StreamConnectionManager(), executorService
      );
  }

  private IlpOverHttpLink newIlpOverHttpLink(InterledgerAddress senderAddress, AccountId accountId, String bearerToken) {
    HttpUrl ilpHttpUrl = new HttpUrl.Builder()
      .scheme(connectorUrl.scheme())
      .host(connectorUrl.host())
      .port(connectorUrl.port())
      .addPathSegment("accounts")
      .addPathSegment(accountId.value())
      .addPathSegment("ilp")
      .build();

    IlpOverHttpLink link = new IlpOverHttpLink(
      () -> senderAddress,
      ilpHttpUrl,
      okHttpClient,
      objectMapper,
      InterledgerCodecContextFactory.oer(),
      () -> bearerToken
    );
    link.setLinkId(LinkId.of(accountId.value()));
    return link;
  }

}
