package org.interledger.connector.opa.model;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.jackson.ObjectMapperFactory;
import org.interledger.spsp.PaymentPointer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.primitives.UnsignedLong;
import okhttp3.HttpUrl;
import org.junit.Test;
import org.springframework.boot.test.json.BasicJsonTester;
import org.springframework.boot.test.json.JsonContentAssert;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

/**
 * Unit tests for (de)serialization from/to JSON of all objects in org.interledger.connector.opay.model.
 */
public class ModelJsonTests {

  private ObjectMapper objectMapper = ObjectMapperFactory.createObjectMapperForProblemsJson();
  private BasicJsonTester jsonTester = new BasicJsonTester(getClass());

  @Test
  public void testOpenPaymentsMetadata() throws JsonProcessingException {

    String issuer = "https://wallet.example/";
    String authorizationIssuer = "https://auth.wallet.example/";
    List<SupportedAsset> supportedAssets = Arrays.asList(SupportedAssets.USD_CENTS, SupportedAssets.EUR_CENTS);

    OpenPaymentsMetadata metadata = OpenPaymentsMetadata.builder()
      .issuer(HttpUrl.parse(issuer))
      .authorizationIssuer(HttpUrl.parse(authorizationIssuer))
      .addAssetsSupported(SupportedAssets.USD_CENTS, SupportedAssets.EUR_CENTS)
      .build();

    String metadataJson = objectMapper.writeValueAsString(metadata);

    JsonContentAssert assertJson = assertThat(jsonTester.from(metadataJson));
    assertJson.extractingJsonPathValue("issuer").isEqualTo(issuer);
    assertJson.extractingJsonPathValue("authorization_issuer").isEqualTo(authorizationIssuer);
    assertJson.extractingJsonPathValue("authorization_endpoint").isEqualTo(metadata.authorizationEndpoint().toString());
    assertJson.extractingJsonPathValue("token_endpoint").isEqualTo(metadata.tokenEndpoint().toString());
    assertJson.extractingJsonPathValue("invoices_endpoint").isEqualTo(metadata.invoicesEndpoint().toString());
    assertJson.extractingJsonPathValue("mandates_endpoint").isEqualTo(metadata.mandatesEndpoint().toString());
    assertJson.extractingJsonPathValue("$.assets_supported[0].code").isEqualTo(supportedAssets.get(0).assetCode());
    assertJson.extractingJsonPathValue("$.assets_supported[0].scale").isEqualTo((int) supportedAssets.get(0).assetScale());
    assertJson.extractingJsonPathValue("$.assets_supported[1].code").isEqualTo(supportedAssets.get(1).assetCode());
    assertJson.extractingJsonPathValue("$.assets_supported[1].scale").isEqualTo((int) supportedAssets.get(1).assetScale());

    OpenPaymentsMetadata deserializedMetadata = objectMapper.readValue(metadataJson, OpenPaymentsMetadata.class);
    assertThat(deserializedMetadata).isEqualTo(metadata);
  }

  @Test
  public void testInvoice() throws JsonProcessingException {
    String invoiceId = UUID.randomUUID().toString();
    PaymentPointer subject = PaymentPointer.of("$acquirer.wallet/merchant");
    UnsignedLong amount = UnsignedLong.valueOf(200);
    String assetCode = "USD";
    short assetScale = (short) 2;
    String description = "Paying for goods";
    UnsignedLong received = UnsignedLong.valueOf(158);

    Invoice invoice = Invoice.builder()
      .id(InvoiceId.of(invoiceId))
      .assetCode(assetCode)
      .assetScale(assetScale)
      .amount(amount)
      .received(received)
      .subject(subject.toString())
      .description(description)
      .expiresAt(Instant.now().plusSeconds(30))
      .build();

    String invoiceJson = objectMapper.writeValueAsString(invoice);

    JsonContentAssert assertJson = assertThat(jsonTester.from(invoiceJson));
    assertJson.extractingJsonPathValue("subject").isEqualTo(subject.toString());
    assertJson.extractingJsonPathValue("id").isEqualTo(invoiceId);
    assertJson.extractingJsonPathValue("amount").isEqualTo(amount.intValue());
    assertJson.extractingJsonPathValue("assetCode").isEqualTo(assetCode);
    assertJson.extractingJsonPathValue("assetScale").isEqualTo((int) assetScale);
    assertJson.extractingJsonPathValue("description").isEqualTo(description);
    assertJson.extractingJsonPathValue("received").isEqualTo(received.intValue());

    Invoice deserializedInvoice = objectMapper.readValue(invoiceJson, Invoice.class);
    assertThat(deserializedInvoice).isEqualToIgnoringGivenFields(invoice, "finalizedAt", "createdAt", "updatedAt");
  }

}
