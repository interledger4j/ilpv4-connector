package org.interledger.ilpv4.connector.settlement;

import com.sappenin.interledger.ilpv4.connector.balances.BalanceTracker;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.ilpv4.connector.core.settlement.Quantity;
import org.interledger.ilpv4.connector.persistence.entities.AccountSettingsEntity;
import org.interledger.ilpv4.connector.persistence.repositories.AccountSettingsRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigInteger;
import java.util.Optional;
import java.util.UUID;

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
  private static final Quantity INCOMING_SETTLEMENT = Quantity.builder().amount(BigInteger.ONE).scale(6).build();

  @Mock
  private AccountSettingsRepository accountSettingsRepositoryMock;
  @Mock
  private BalanceTracker balanceTrackerMock;

  private DefaultSettlementService settlementService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    this.settlementService = new DefaultSettlementService(accountSettingsRepositoryMock, balanceTrackerMock);
  }

  @Test(expected = NullPointerException.class)
  public void handleIncomingSettlementWithNullIdempotenceId() {
    try {
      settlementService.handleIncomingSettlement(null, ACCOUNT_ID, INCOMING_SETTLEMENT);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("idempotencyKey must not be null"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void handleIncomingSettlementWithNullAccountId() {
    try {
      settlementService.handleIncomingSettlement(UUID.randomUUID(), null, INCOMING_SETTLEMENT);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("accountId must not be null"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void handleIncomingSettlementWithNullQuantity() {
    try {
      settlementService.handleIncomingSettlement(UUID.randomUUID(), ACCOUNT_ID, null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("incomingSettlement must not be null"));
      throw e;
    }
  }

  @Test(expected = AccountNotFoundProblem.class)
  public void handleIncomingSettlementWhenAccountNotFound() {
    when(accountSettingsRepositoryMock.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());

    try {
      settlementService.handleIncomingSettlement(UUID.randomUUID(), ACCOUNT_ID, INCOMING_SETTLEMENT);
    } catch (AccountNotFoundProblem e) {
      assertThat(e.getAccountId(), is(ACCOUNT_ID));
      assertThat(e.getMessage(), is("Account Not Found"));
      throw e;
    }
  }

  @Test
  public void handleIncomingSettlementd() {
    UUID idempotencyKey = UUID.randomUUID();
    AccountSettingsEntity accountSettings = mock(AccountSettingsEntity.class);
    when(accountSettings.getAssetScale()).thenReturn(9);
    when(accountSettingsRepositoryMock.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(accountSettings));

    Quantity expectedClearedQuantity = Quantity.builder().amount(BigInteger.valueOf(1000L)).scale(9).build();

    Quantity actualClearedQuantity =
      settlementService.handleIncomingSettlement(idempotencyKey, ACCOUNT_ID, INCOMING_SETTLEMENT);

    assertThat(actualClearedQuantity, is(expectedClearedQuantity));
    verify(accountSettingsRepositoryMock).findByAccountId(ACCOUNT_ID);
    verify(balanceTrackerMock).updateBalanceForSettlement(idempotencyKey, ACCOUNT_ID, expectedClearedQuantity);
    verifyNoMoreInteractions(balanceTrackerMock);
    verifyNoMoreInteractions(accountSettingsRepositoryMock);
  }
}
