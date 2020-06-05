package org.interledger.connector.payid;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.jackson.ObjectMapperFactory;
import org.interledger.connector.opa.model.PayId;
import org.interledger.connector.opa.model.PaymentNetwork;

import org.junit.Test;

public class PayIdClientTest {

  private PayIdClient client = FeignPayIdClient.construct(ObjectMapperFactory.createObjectMapperForProblemsJson());
  public static final PayId PAY_ID = PayId.of("payid:nhartner$stg.payid.xpring.money");

  @Test
  public void getPayIdIlp() {
    PayIdResponse response = client.getPayId(PAY_ID, PaymentNetwork.INTERLEDGER, "testnet");
    assertThat(response.addresses()).isNotEmpty();
    assertThat(response.addresses().get(0).addressDetails().address()).isEqualTo("$xpring.money/nhartner-stage");
  }


  @Test
  public void getPayIdXrpl() {
    PayIdResponse response = client.getPayId(PAY_ID, PaymentNetwork.XRPL, "testnet");
    assertThat(response.addresses()).isNotEmpty();
    assertThat(response.addresses().get(0).addressDetails().address()).
      isEqualTo("TVMfQcUPnGXSUtmheSiXafrzG1RWygkdmsBxUqWooyA37pz");
  }



}