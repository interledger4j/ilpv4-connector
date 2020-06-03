package org.interledger.connector.wallet.mandates;

import static org.slf4j.LoggerFactory.getLogger;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.opa.InvoiceService;
import org.interledger.connector.opa.model.Charge;
import org.interledger.connector.opa.model.ChargeId;
import org.interledger.connector.opa.model.ChargeStatus;
import org.interledger.connector.opa.model.IlpPaymentDetails;
import org.interledger.connector.opa.model.ImmutableCharge;
import org.interledger.connector.opa.model.Invoice;
import org.interledger.connector.opa.model.Mandate;
import org.interledger.connector.opa.model.MandateId;
import org.interledger.connector.opa.model.NewCharge;
import org.interledger.connector.opa.model.NewMandate;
import org.interledger.connector.opa.model.OpenPaymentsSettings;
import org.interledger.connector.opa.model.PayInvoiceRequest;
import org.interledger.connector.opa.model.XrpPayment;
import org.interledger.connector.opa.model.XrpPaymentDetails;
import org.interledger.connector.opa.model.problems.MandateInsufficientBalanceProblem;
import org.interledger.connector.opa.model.problems.MandateNotFoundProblem;
import org.interledger.connector.payments.StreamPayment;

import com.google.common.collect.Lists;
import okhttp3.HttpUrl;
import org.slf4j.Logger;

import java.util.HashMap;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Consumer;
import java.util.function.Supplier;
import java.util.stream.Collectors;

public class InMemoryMandateService implements MandateService {

  private static final Logger LOGGER = getLogger(InMemoryMandateService.class);

  private final HashMap<MandateId, Mandate> mandates = new HashMap<>();

  private final MandateAccrualService mandateAccrualService;
  private final InvoiceService<StreamPayment, IlpPaymentDetails> ilpInvoiceService;
  private final InvoiceService<XrpPayment, XrpPaymentDetails> xrpInvoiceService;
  private final Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier;


  public InMemoryMandateService(MandateAccrualService mandateAccrualService,
                                InvoiceService<StreamPayment, IlpPaymentDetails> ilpInvoiceService,
                                InvoiceService<XrpPayment, XrpPaymentDetails> xrpInvoiceService,
                                Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier) {
    this.mandateAccrualService = mandateAccrualService;
    this.ilpInvoiceService = ilpInvoiceService;
    this.xrpInvoiceService = xrpInvoiceService;
    this.openPaymentsSettingsSupplier = openPaymentsSettingsSupplier;
  }

  @Override
  public Mandate createMandate(AccountId accountId, NewMandate newMandate) {
    MandateId mandateId = MandateId.of(UUID.randomUUID().toString());
    Mandate mandate = Mandate.builder().from(newMandate)
      .mandateId(mandateId)
      .accountId(accountId.value())
      .balance(newMandate.amount())
      .account(makeAccountUrl(accountId))
      .id(makeMandateUrl(accountId, mandateId))
      .build();

    mandates.put(mandate.mandateId(), mandate);

    return mandate;
  }

  @Override
  public Optional<Mandate> findMandateById(AccountId accountId, MandateId mandateId) {
    return Optional.ofNullable(mandates.get(mandateId))
      .filter(mandate -> mandate.accountId().equals(accountId.value()))
      .map(mandate -> Mandate.builder().from(mandate)
        .balance(mandateAccrualService.calculateBalance(mandate))
        .build()
      );
  }

  @Override
  public List<Mandate> findMandatesByAccountId(AccountId accountId) {
    return mandates.values().stream()
      .filter(mandate -> mandate.accountId().equals(accountId.value()))
      .map(mandate -> Mandate.builder().from(mandate)
        .balance(mandateAccrualService.calculateBalance(mandate))
        .build())
      .collect(Collectors.toList());
  }

  @Override
  public Optional<Charge> findChargeById(AccountId accountId, MandateId mandateId, ChargeId chargeId) {
    return findMandateById(accountId, mandateId).flatMap(mandate -> mandate.charges().stream()
      .filter(charge -> charge.chargeId().equals(chargeId))
      .findAny());
  }

  @Override
  public Charge createCharge(AccountId accountId, MandateId mandateId, NewCharge newCharge) {
    Invoice invoice = xrpInvoiceService.findInvoiceByUrl(newCharge.invoice(), accountId)
      .orElseGet(() -> xrpInvoiceService.syncInvoice(newCharge.invoice(), accountId));
    synchronized (mandates) {
      Mandate mandate = findMandateById(accountId, mandateId)
        .orElseThrow(() -> new MandateNotFoundProblem(mandateId));

      if (mandate.balance().compareTo(invoice.amount()) >= 0) {
        ChargeId chargeId = ChargeId.of(UUID.randomUUID().toString());
        Charge charge = Charge.builder()
          .amount(invoice.amount())
          .invoice(invoice.invoiceUrl().get())
          .mandate(mandate.id())
          .mandateId(mandate.mandateId())
          .status(ChargeStatus.CREATED)
          .chargeId(chargeId)
          .id(mandate.id().newBuilder().addPathSegment("charges").addPathSegment(chargeId.value()).build())
          .build();

        Mandate updated = Mandate.builder()
          .from(mandate)
          .addCharges(charge)
          .build();

        mandates.put(mandate.mandateId(), updated);

        try {
          chargeInvoice(invoice).userAuthorizationUrl().ifPresent(authorizationUrl ->
            updateAuthorizationUrl(accountId, mandateId, chargeId, authorizationUrl)
          );
          updateChargeStatus(accountId, mandateId, chargeId, ChargeStatus.PAYMENT_PENDING);
        } catch (Exception e) {
          LOGGER.error("charging invoice {} to mandate {} failed", invoice.id(), mandate.mandateId(), e);
          updateChargeStatus(accountId, mandateId, chargeId, ChargeStatus.PAYMENT_FAILED);
        }
        return findChargeById(accountId, mandateId, chargeId).get();
      } else {
        throw new MandateInsufficientBalanceProblem(mandateId);
      }
    }
  }

  private void updateAuthorizationUrl(AccountId accountId, MandateId mandateId, ChargeId chargeId, String authorizationUrl) {
    updateCharge(accountId, mandateId, chargeId, (builder) -> builder.userAuthorizationUrl(authorizationUrl));
  }

  private void updateChargeStatus(AccountId accountId, MandateId mandateId, ChargeId chargeId, ChargeStatus status) {
    updateCharge(accountId, mandateId, chargeId, (builder) -> builder.status(status));
  }

  private void updateCharge(AccountId accountId,
                            MandateId mandateId,
                            ChargeId chargeId,
                            Consumer<ImmutableCharge.Builder> chargeUpdater) {
    findMandateById(accountId, mandateId).ifPresent(mandate -> {
      List<Charge> chargesToUpdate = Lists.newArrayList(mandate.charges());
      chargesToUpdate.stream()
        .filter(c -> c.chargeId().equals(chargeId))
        .findAny()
        .map(toUpdate -> {
          chargesToUpdate.remove(toUpdate);
          ImmutableCharge.Builder updateBuilder = Charge.builder().from(toUpdate);
          chargeUpdater.accept(updateBuilder);
          Mandate updated = Mandate.builder()
            .from(mandate)
            .charges(chargesToUpdate)
            .addCharges(updateBuilder.build())
            .build();
          mandates.put(mandateId, updated);
          return true;
        }).orElseGet(() -> {
        LOGGER.error("could not update missing charge id {}", chargeId);
        return false;
      });
    });
  }

  private HttpUrl makeAccountUrl(AccountId accountId) {
    return openPaymentsSettingsSupplier.get().metadata().issuer()
      .newBuilder().addPathSegment("accounts")
      .addPathSegment(accountId.value())
      .build();
  }

  private HttpUrl makeMandateUrl(AccountId accountId, MandateId mandateId) {
    return makeAccountUrl(accountId).newBuilder()
      .addPathSegment("mandates")
      .addPathSegment(mandateId.value())
      .build();
  }

  private XrpPayment chargeInvoice(Invoice invoice) {
    return xrpInvoiceService.payInvoice(invoice.id(), invoice.accountId(), Optional.of(
      PayInvoiceRequest.builder().amount(invoice.amount()).build()
    ));
  }


}
