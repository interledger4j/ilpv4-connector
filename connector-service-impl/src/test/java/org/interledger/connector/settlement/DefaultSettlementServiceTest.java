package org.interledger.connector.settlement;

import com.google.common.eventbus.EventBus;
import org.interledger.connector.balances.BalanceTracker;
import org.interledger.connector.links.LinkManager;
import org.interledger.connector.settlement.DefaultSettlementService;
import org.interledger.connector.settlement.SettlementEngineClient;
import org.interledger.connector.accounts.AccountBySettlementEngineAccountNotFoundProblem;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.connector.accounts.SettlementEngineAccountId;
import org.interledger.connector.core.settlement.SettlementQuantity;
import org.interledger.connector.persistence.entities.AccountSettingsEntity;
import org.interledger.connector.persistence.entities.SettlementEngineDetailsEntity;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigInteger;
import java.util.Optional;
import java.util.UUID;

import static java.math.BigInteger.ONE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@linkn DefaultSettlementService}.
 */
public class DefaultSettlementServiceTest {

  private static final AccountId ACCOUNT_ID = AccountId.of("alice");

  private static final SettlementEngineAccountId SETTLEMENT_ACCOUNT_ID =
    SettlementEngineAccountId.of(UUID.randomUUID().toString());

  private static final SettlementQuantity INCOMING_SETTLEMENT =
    SettlementQuantity.builder().amount(ONE).scale(6).build();

  @Mock
  private AccountSettingsRepository accountSettingsRepositoryMock;
  @Mock
  private BalanceTracker balanceTrackerMock;
  @Mock
  private LinkManager linkManagerMock;
  @Mock
  private SettlementEngineClient settlementEngineClientMock;
  @Mock
  private EventBus eventBus;

  private DefaultSettlementService settlementService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    this.settlementService = new DefaultSettlementService(
      balanceTrackerMock, linkManagerMock, accountSettingsRepositoryMock, settlementEngineClientMock, eventBus
    );
  }

  @Test(expected = NullPointerException.class)
  public void onLocalSettlementPaymentWithNullIdempotenceId() {
    try {
      settlementService.onLocalSettlementPayment(null, SETTLEMENT_ACCOUNT_ID, INCOMING_SETTLEMENT);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("idempotencyKey must not be null"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void onLocalSettlementPaymentWithNullAccountId() {
    try {
      settlementService.onLocalSettlementPayment(UUID.randomUUID().toString(), null, INCOMING_SETTLEMENT);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("settlementEngineAccountId must not be null"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void onLocalSettlementPaymentWithNullQuantity() {
    try {
      settlementService.onLocalSettlementPayment(UUID.randomUUID().toString(), SETTLEMENT_ACCOUNT_ID, null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("incomingSettlement must not be null"));
      throw e;
    }
  }

  @Test(expected = AccountBySettlementEngineAccountNotFoundProblem.class)
  public void onLocalSettlementPaymentWhenAccountNotFound() {
    when(accountSettingsRepositoryMock.findBySettlementEngineAccountId(SETTLEMENT_ACCOUNT_ID))
      .thenReturn(Optional.empty());

    try {
      settlementService
        .onLocalSettlementPayment(UUID.randomUUID().toString(), SETTLEMENT_ACCOUNT_ID, INCOMING_SETTLEMENT);
    } catch (AccountNotFoundProblem e) {
      assertThat(e.getAccountId(), is(SETTLEMENT_ACCOUNT_ID));
      assertThat(e.getMessage(), is("Account Not Found (`alice`)"));
      throw e;
    }
  }

  @Test
  public void onLocalSettlementPayment() {
    String idempotencyKey = UUID.randomUUID().toString();
    AccountSettingsEntity accountSettingsEntityMock = mock(AccountSettingsEntity.class);
    SettlementEngineDetailsEntity settlementEngineDetailsEntityMock = mock(SettlementEngineDetailsEntity.class);
    when(accountSettingsEntityMock.getAccountId()).thenReturn(ACCOUNT_ID);
    when(accountSettingsEntityMock.getAssetScale()).thenReturn(9);
    when(accountSettingsEntityMock.getSettlementEngineDetailsEntity()).thenReturn(settlementEngineDetailsEntityMock);
    when(settlementEngineDetailsEntityMock.getSettlementEngineAccountId()).thenReturn(SETTLEMENT_ACCOUNT_ID.value());
    when(accountSettingsRepositoryMock.findBySettlementEngineAccountId(SETTLEMENT_ACCOUNT_ID))
      .thenReturn(Optional.of(accountSettingsEntityMock));

    SettlementQuantity expectedClearedSettlementQuantity = SettlementQuantity.builder()
      .amount(BigInteger.valueOf(1000L))
      .scale(9)
      .build();

    SettlementQuantity actualClearedSettlementQuantity =
      settlementService.onLocalSettlementPayment(idempotencyKey, SETTLEMENT_ACCOUNT_ID, INCOMING_SETTLEMENT);

    assertThat(actualClearedSettlementQuantity, is(expectedClearedSettlementQuantity));
    verify(accountSettingsRepositoryMock).findBySettlementEngineAccountId(SETTLEMENT_ACCOUNT_ID);
    verify(balanceTrackerMock).updateBalanceForIncomingSettlement(
      idempotencyKey, ACCOUNT_ID, expectedClearedSettlementQuantity.amount().longValue()
    );
    verifyNoMoreInteractions(balanceTrackerMock);
    verifyNoMoreInteractions(accountSettingsRepositoryMock);
  }

  // TODO: Cover other methods in DefaultSettlementService.
}
