package org.interledger.connector.wallet.mandates;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.opa.model.Mandate;
import org.interledger.connector.opa.model.MandateId;
import org.interledger.connector.opa.model.NewMandate;
import org.interledger.connector.opa.model.OpenPaymentsSettings;

import com.google.common.primitives.UnsignedLong;
import okhttp3.HttpUrl;

import java.util.HashMap;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class InMemoryMandateService implements MandateService {

  private final HashMap<MandateId, Mandate> mandates = new HashMap<>();

  private final MandateAccrualService mandateAccrualService;
  private final Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier;


  public InMemoryMandateService(MandateAccrualService mandateAccrualService,
                                Supplier<OpenPaymentsSettings> openPaymentsSettingsSupplier) {
    this.mandateAccrualService = mandateAccrualService;
    this.openPaymentsSettingsSupplier = openPaymentsSettingsSupplier;
  }

  @Override
  public Mandate createMandate(AccountId accountId, NewMandate newMandate) {
    MandateId mandateId = MandateId.of(UUID.randomUUID().toString());
    Mandate mandate = Mandate.builder().from(newMandate)
      .mandateId(mandateId)
      .accountId(accountId.value())
      .balance(UnsignedLong.ZERO)
      .account(makeAccountUrl(accountId))
      .id(makeMandateUrl(accountId, mandateId))
      .build();

    mandates.put(mandate.mandateId(), mandate);

    return mandate;
  }

  @Override
  public Optional<Mandate> findMandateById(MandateId mandateId) {
    return Optional.ofNullable(mandates.get(mandateId))
      .map(mandate -> Mandate.builder().from(mandate)
        .balance(mandateAccrualService.calculateBalance(mandate))
        .build()
      );
  }

  @Override
  public synchronized boolean chargeMandate(MandateId mandateId, UnsignedLong amount) {
    return findMandateById(mandateId)
      .map(mandate -> {
        if (mandate.balance().compareTo(amount) >= 0) {
          Mandate updated = Mandate.builder().from(mandate)
            .totalCharged(mandate.totalCharged().plus(amount))
            .build();
          mandates.put(mandateId, updated);
          return true;
        }
        return false;
      }).orElse(false);
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


}
