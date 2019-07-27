package org.interledger.ilpv4.connector.settlement;

import com.sappenin.interledger.ilpv4.connector.balances.BalanceTracker;
import com.sappenin.interledger.ilpv4.connector.links.LinkManager;
import com.sappenin.interledger.ilpv4.connector.settlement.SettlementEngineClient;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.ilpv4.connector.core.settlement.SettlementQuantity;
import org.interledger.ilpv4.connector.persistence.entities.AccountSettingsEntity;
import org.interledger.ilpv4.connector.persistence.repositories.AccountSettingsRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.web.client.RestTemplate;

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
  private static final SettlementQuantity
    INCOMING_SETTLEMENT = SettlementQuantity.builder().amount(1L).scale(6).build();

  @Mock
  AccountSettingsRepository accountSettingsRepositoryMock;
  @Mock
  BalanceTracker balanceTrackerMock;
  @Mock
  LinkManager linkManagerMock;
  @Mock
  RestTemplate settlementEngineRestTemplateMock;

  DefaultSettlementService settlementService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    this.settlementService = new DefaultSettlementService(
      balanceTrackerMock, linkManagerMock, accountSettingsRepositoryMock, settlementEngineRestTemplateMock
    );
  }

  @Test(expected = NullPointerException.class)
  public void handleIncomingSettlementWithNullIdempotenceId() {
    try {
      settlementService.onLocalSettlementPayment(null, ACCOUNT_ID, INCOMING_SETTLEMENT);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("idempotencyKey must not be null"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void handleIncomingSettlementWithNullAccountId() {
    try {
      settlementService.onLocalSettlementPayment(UUID.randomUUID(), null, INCOMING_SETTLEMENT);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("accountId must not be null"));
      throw e;
    }
  }

  @Test(expected = NullPointerException.class)
  public void handleIncomingSettlementWithNullQuantity() {
    try {
      settlementService.onLocalSettlementPayment(UUID.randomUUID(), ACCOUNT_ID, null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("incomingSettlement must not be null"));
      throw e;
    }
  }

  @Test(expected = AccountNotFoundProblem.class)
  public void handleIncomingSettlementWhenAccountNotFound() {
    when(accountSettingsRepositoryMock.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.empty());

    try {
      settlementService.onLocalSettlementPayment(UUID.randomUUID(), ACCOUNT_ID, INCOMING_SETTLEMENT);
    } catch (AccountNotFoundProblem e) {
      assertThat(e.getAccountId(), is(ACCOUNT_ID));
      assertThat(e.getMessage(), is("Account Not Found (`alice`)"));
      throw e;
    }
  }

  @Test
  public void handleIncomingSettlement() {
    UUID idempotencyKey = UUID.randomUUID();
    AccountSettingsEntity accountSettingsMock = mock(AccountSettingsEntity.class);
    when(accountSettingsMock.getAccountId()).thenReturn(ACCOUNT_ID);
    when(accountSettingsMock.getAssetScale()).thenReturn(9);
    when(accountSettingsRepositoryMock.findByAccountId(ACCOUNT_ID)).thenReturn(Optional.of(accountSettingsMock));

    SettlementQuantity expectedClearedSettlementQuantity = SettlementQuantity.builder().amount(1000L).scale(9).build();

    SettlementQuantity actualClearedSettlementQuantity =
      settlementService.onLocalSettlementPayment(idempotencyKey, ACCOUNT_ID, INCOMING_SETTLEMENT);

    assertThat(actualClearedSettlementQuantity, is(expectedClearedSettlementQuantity));
    verify(accountSettingsRepositoryMock).findByAccountId(ACCOUNT_ID);
    verify(balanceTrackerMock)
      .updateBalanceForIncomingSettlement(idempotencyKey, ACCOUNT_ID, expectedClearedSettlementQuantity.amount());
    verifyNoMoreInteractions(balanceTrackerMock);
    verifyNoMoreInteractions(accountSettingsRepositoryMock);
  }
}
