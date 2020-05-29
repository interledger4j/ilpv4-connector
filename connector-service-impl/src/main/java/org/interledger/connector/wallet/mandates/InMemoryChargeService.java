package org.interledger.connector.wallet.mandates;

import org.interledger.connector.opa.model.Charge;
import org.interledger.connector.opa.model.ChargeId;
import org.interledger.connector.opa.model.ChargeStatus;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.Mandate;
import org.interledger.connector.opa.model.MandateId;
import org.interledger.connector.opa.model.NewCharge;
import org.interledger.connector.opa.model.problems.MandateNotFoundProblem;
import org.interledger.connector.wallet.RemoteInvoiceService;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public class InMemoryChargeService implements ChargeService {

  private final MandateService mandateService;

  private final RemoteInvoiceService remoteInvoiceService;

  private Map<ChargeId, Charge> charges = new HashMap<>();

  public InMemoryChargeService(MandateService mandateService, RemoteInvoiceService remoteInvoiceService) {
    this.mandateService = mandateService;
    this.remoteInvoiceService = remoteInvoiceService;
  }

  @Override
  public Charge createCharge(MandateId mandateId, NewCharge newCharge) {
    Mandate mandate = mandateService.findMandateById(mandateId)
      .orElseThrow(() -> new MandateNotFoundProblem(mandateId));

    Invoice invoice = remoteInvoiceService.getInvoice(newCharge.invoice());
    if (mandateService.chargeMandate(mandate.mandateId(), invoice.amount())) {
      ChargeId chargeId = ChargeId.of(UUID.randomUUID().toString());
      Charge charge = Charge.builder()
        .amount(invoice.amount())
        .invoice(invoice.invoiceUrl().get())
        .mandate(mandate.id())
        .status(ChargeStatus.CREATED)
        .chargeId(chargeId)
        .id(mandate.id().newBuilder().addPathSegment("charges").addPathSegment(chargeId.value()).build())
        .build();

      charges.put(chargeId, charge);
      return charge;
    } else {
      throw new RuntimeException("FIXME"); // FIXME
    }
  }

  public Optional<Charge> findById(ChargeId chargeId) {
    return Optional.ofNullable(charges.get(chargeId));
  }

}
