package org.interledger.openpayments.mandates;

import static org.slf4j.LoggerFactory.getLogger;

import org.interledger.connector.accounts.AccountId;
import org.interledger.openpayments.ApproveMandateRequest;
import org.interledger.openpayments.Charge;
import org.interledger.openpayments.ChargeId;
import org.interledger.openpayments.ChargeStatus;
import org.interledger.openpayments.ImmutableCharge;
import org.interledger.openpayments.ImmutableMandate;
import org.interledger.openpayments.Invoice;
import org.interledger.openpayments.Mandate;
import org.interledger.openpayments.MandateId;
import org.interledger.openpayments.MandateStatus;
import org.interledger.openpayments.NewCharge;
import org.interledger.openpayments.NewMandate;
import org.interledger.openpayments.PayInvoiceRequest;
import org.interledger.openpayments.UserAuthorizationRequiredException;
import org.interledger.openpayments.config.OpenPaymentsSettings;
import org.interledger.openpayments.events.MandateApprovedEvent;
import org.interledger.openpayments.events.MandateDeclinedEvent;
import org.interledger.openpayments.problems.MandateInsufficientBalanceProblem;
import org.interledger.openpayments.problems.MandateNotApprovedProblem;
import org.interledger.openpayments.problems.MandateNotFoundProblem;

import com.google.common.collect.Lists;
import com.google.common.eventbus.EventBus;
import com.google.common.eventbus.Subscribe;
import okhttp3.HttpUrl;
import org.interleger.openpayments.InvoiceService;
import org.interleger.openpayments.InvoiceServiceFactory;
import org.interleger.openpayments.PaymentSystemFacade;
import org.interleger.openpayments.PaymentSystemFacadeFactory;
import org.interleger.openpayments.mandates.MandateAccrualService;
import org.interleger.openpayments.mandates.MandateService;
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
  private final InvoiceServiceFactory invoiceServiceFactory;
  private final PaymentSystemFacadeFactory paymentSystemFacadeFactory;
  private final Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier;

  public InMemoryMandateService(MandateAccrualService mandateAccrualService,
                                InvoiceServiceFactory invoiceServiceFactory,
                                PaymentSystemFacadeFactory paymentSystemFacadeFactory,
                                EventBus eventBus,
                                Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier) {
    this.mandateAccrualService = mandateAccrualService;
    this.invoiceServiceFactory = invoiceServiceFactory;
    this.paymentSystemFacadeFactory = paymentSystemFacadeFactory;
    this.openPaymentsSettingsSupplier = openPaymentsSettingsSupplier;
    eventBus.register(this);
  }

  @Override
  public Mandate createMandate(AccountId accountId, NewMandate newMandate) {
    MandateId mandateId = MandateId.of(UUID.randomUUID().toString());

    PaymentSystemFacade paymentSystemFacade = paymentSystemFacadeFactory.get(newMandate.paymentNetwork())
      .orElseThrow(() -> new UnsupportedOperationException("No payment facade service for " + newMandate.paymentNetwork()));

    Mandate mandate = Mandate.builder().from(newMandate)
      .mandateId(mandateId)
      .accountId(accountId.value())
      .balance(newMandate.amount())
      .status(MandateStatus.AWAITING_APPROVAL)
      .account(makeAccountUrl(accountId))
      .id(makeMandateUrl(accountId, mandateId))
      .userAuthorizationUrl(paymentSystemFacade.getMandateAuthorizationUrl(
        ApproveMandateRequest.builder()
          .mandateId(mandateId)
          .accountId(accountId)
          .memoToUser("Approve " + newMandate.description().orElse("recurring payment"))
          .redirectUrl(newMandate.userRedirectUrl())
          .build()
      ))
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
    Mandate mandate = findMandateById(accountId, mandateId)
      .orElseThrow(() -> new MandateNotFoundProblem(mandateId));

    if (!mandate.status().equals(MandateStatus.APPROVED)) {
      throw new MandateNotApprovedProblem(mandateId);
    }

    InvoiceService<?, ?> invoiceService = invoiceServiceFactory.get(mandate.paymentNetwork())
      .orElseThrow(() -> new UnsupportedOperationException("No invoice service for " + mandate.paymentNetwork()));

    Invoice invoice = invoiceService.findInvoiceByUrl(newCharge.invoice(), accountId)
      .orElseGet(() -> invoiceService.syncInvoice(newCharge.invoice(), accountId));

    synchronized (mandates) {
      if (mandate.balance().compareTo(invoice.amount()) >= 0) {
        ChargeId chargeId = ChargeId.of(UUID.randomUUID().toString());
        Charge charge = Charge.builder()
          .amount(invoice.amount())
          .invoice(invoice.receiverInvoiceUrl())
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
          invoiceService.payInvoice(
            invoice.id(),
            invoice.accountId(),
            Optional.of(PayInvoiceRequest.builder().amount(invoice.amount()).build())
          );
          updateChargeStatus(accountId, mandateId, chargeId, ChargeStatus.PAYMENT_INITIATED);
        } catch (UserAuthorizationRequiredException e) {
          updateAuthorizationUrl(accountId, mandateId, chargeId, e.getUserAuthorizationUrl().toString());
          updateChargeStatus(accountId, mandateId, chargeId, ChargeStatus.PAYMENT_AWAITING_USER_AUTH);
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

  @Subscribe
  public void onMandateApproved(MandateApprovedEvent mandateApprovedEvent) {
    updateMandateStatus(mandateApprovedEvent.accountId(), mandateApprovedEvent.mandateId(), MandateStatus.APPROVED);
  }

  @Subscribe
  public void onMandateDeclined(MandateDeclinedEvent mandateApprovedEvent) {
    updateMandateStatus(mandateApprovedEvent.accountId(), mandateApprovedEvent.mandateId(), MandateStatus.DECLINED);
  }

  private void updateMandateStatus(AccountId accountId, MandateId mandateId, MandateStatus status) {
    updateMandate(accountId, mandateId, (builder) -> builder.status(status));
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

  private void updateMandate(AccountId accountId,
                             MandateId mandateId,
                             Consumer<ImmutableMandate.Builder> mandateUpdater) {
    findMandateById(accountId, mandateId).ifPresent(mandate -> {
      ImmutableMandate.Builder updateBuilder = Mandate.builder().from(mandate);
      mandateUpdater.accept(updateBuilder);
      mandates.put(mandateId, updateBuilder.build());
    });
  }

  private HttpUrl makeAccountUrl(AccountId accountId) {
    return openPaymentsSettingsSupplier.get().metadata().issuer()
      .newBuilder()
      .addPathSegment(accountId.value())
      .build();
  }

  private HttpUrl makeMandateUrl(AccountId accountId, MandateId mandateId) {
    return makeAccountUrl(accountId).newBuilder()
      .addPathSegment("mandates")
      .addPathSegment(mandateId.value())
      .build();
  }

}
