package org.interledger.connector.wallet;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.MockitoAnnotations.initMocks;

import org.interledger.connector.jackson.ObjectMapperFactory;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.spsp.PaymentPointerResolver;

import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.Mockito;

public class IlpOpaPaymentServiceTest {

  private IlpOpaPaymentService ilpOpaPaymentService;

  @Mock
  private AccountSettingsRepository accountSettingsRepositoryMock;

  @Mock
  private OpenPaymentsClient openPaymentsClientMock;

  @Mock
  private OkHttpClient okHttpClientMock;

  private HttpUrl connectorUrl;

  private InterledgerAddressPrefix opaAddressPrefix;

  @Before
  public void setUp() {
    initMocks(this);

    connectorUrl = HttpUrl.parse("https://connector.com");
    opaAddressPrefix = InterledgerAddressPrefix.of("test.jc");

    ilpOpaPaymentService = Mockito.spy(
      new IlpOpaPaymentService(
        PaymentPointerResolver.defaultResolver(),
        accountSettingsRepositoryMock,
        openPaymentsClientMock,
        okHttpClientMock,
        ObjectMapperFactory.createObjectMapperForProblemsJson(),
        connectorUrl,
        opaAddressPrefix
      )
    );
  }

  /*@Test
  public void sendOpaPayment() throws Exception {
    PayIdOpaPaymentRequest payIdOpaPaymentRequest = PayIdOpaPaymentRequest.builder()
      .amount(UnsignedLong.valueOf(1000))
      .destinationPaymentPointer("$example.com/foo")
      .build();
    AccountId accountId = AccountId.of("foo");
    String bearerToken = "Bearer password";

    Invoice invoiceMock = Invoice.builder()
      .accountId("foo")
      .amount(UnsignedLong.valueOf(1000))
      .assetCode("XRP")
      .assetScale((short) 9)
      .subject("$example.com/foo")
      .expiresAt(Instant.MAX)
      .received(UnsignedLong.valueOf(1000))
      .build();

    AccountSettings accountSettingsMock = mock(AccountSettings.class);
    when(accountSettingsRepositoryMock.findByAccountIdWithConversion(eq(accountId)))
      .thenReturn(Optional.of(accountSettingsMock));
    when(accountSettingsMock.assetCode()).thenReturn("XRP");
    when(accountSettingsMock.assetScale()).thenReturn(9);
    when(accountSettingsMock.accountId()).thenReturn(accountId);


    OpenPaymentsMetadata openPaymentsMetadataMock = mock(OpenPaymentsMetadata.class);
    when(openPaymentsClientMock.getMetadata("https://example.com/")).thenReturn(openPaymentsMetadataMock);
    when(openPaymentsMetadataMock.invoicesEndpoint()).thenReturn(HttpUrl.parse("https://example.com/invoice"));
    when(openPaymentsClientMock.createInvoice(eq(openPaymentsMetadataMock.invoicesEndpoint().toString()), eq(invoiceMock)))
      .thenReturn(invoiceMock);

    StreamConnectionDetails streamConnectionDetailsMock = mock(StreamConnectionDetails.class);
    when(openPaymentsClientMock.getInvoicePaymentDetails(eq(openPaymentsMetadataMock.invoicesEndpoint().toString()), any()))
      .thenReturn(streamConnectionDetailsMock);
    when(streamConnectionDetailsMock.destinationAddress()).thenReturn(InterledgerAddress.of("test.jc.foo.123~ABC"));
    when(streamConnectionDetailsMock.sharedSecret()).thenReturn(mock(SharedSecret.class));

    SimpleStreamSender simpleStreamSenderMock = mock(SimpleStreamSender.class);
    doReturn(simpleStreamSenderMock).when(ilpOpaPaymentService).createSimpleStreamSender(any());
    SendMoneyResult sendMoneyResultMock = mock(SendMoneyResult.class);
    when(simpleStreamSenderMock.sendMoney(any())).thenReturn(CompletableFuture.completedFuture(sendMoneyResultMock));
    when(sendMoneyResultMock.amountDelivered()).thenReturn(UnsignedLong.valueOf(1000));
    when(sendMoneyResultMock.amountSent()).thenReturn(UnsignedLong.valueOf(1000));
    when(sendMoneyResultMock.originalAmount()).thenReturn(UnsignedLong.valueOf(1000));
    when(sendMoneyResultMock.successfulPayment()).thenReturn(true);

    PaymentResponse paymentResponse = ilpOpaPaymentService.sendOpaPayment(payIdOpaPaymentRequest, accountId.value(), bearerToken);
    assertThat(paymentResponse.amountDelivered()).isEqualTo(UnsignedLong.valueOf(1000));
    assertThat(paymentResponse.amountSent()).isEqualTo(UnsignedLong.valueOf(1000));
    assertThat(paymentResponse.originalAmount()).isEqualTo(UnsignedLong.valueOf(1000));
    assertThat(paymentResponse.successfulPayment()).isEqualTo(true);
  }*/
}
