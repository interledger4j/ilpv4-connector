package org.interledger.connector.settlement;

import static java.math.BigInteger.ONE;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.InvalidAccountIdProblem;
import org.interledger.connector.accounts.SettlementEngineAccountId;
import org.interledger.connector.accounts.SettlementEngineDetails;
import org.interledger.connector.balances.BalanceTracker;
import org.interledger.connector.core.settlement.SettlementQuantity;
import org.interledger.connector.links.LinkManager;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.link.LoopbackLink;

import com.google.common.eventbus.EventBus;
import okhttp3.HttpUrl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.math.BigInteger;
import java.util.Optional;
import java.util.UUID;

/**
 * Unit tests for {@link DefaultSettlementService}.
 */
@SuppressWarnings("UnstableApiUsage")
public class DefaultSettlementServiceTest {

  private static final AccountId ACCOUNT_ID = AccountId.of("alice");

  private static final SettlementEngineAccountId SETTLEMENT_ACCOUNT_ID =
    SettlementEngineAccountId.of(UUID.randomUUID().toString());

  private static final SettlementQuantity INCOMING_SETTLEMENT =
    SettlementQuantity.builder().amount(ONE).scale(6).build();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

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

  @Test
  public void onLocalSettlementPaymentWithNullIdempotenceId() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("idempotencyKey must not be null");
    settlementService.onIncomingSettlementPayment(null, SETTLEMENT_ACCOUNT_ID, INCOMING_SETTLEMENT);
  }

  @Test
  public void onLocalSettlementPaymentWithNullAccountId() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("settlementEngineAccountId must not be null");
    settlementService.onIncomingSettlementPayment(UUID.randomUUID().toString(), null, INCOMING_SETTLEMENT);
  }

  @Test
  public void onLocalSettlementPaymentWithNullQuantity() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("incomingSettlementInSettlementUnits must not be null");
    settlementService.onIncomingSettlementPayment(UUID.randomUUID().toString(), SETTLEMENT_ACCOUNT_ID, null);
  }

  @Test
  public void onLocalSettlementPaymentWhenAccountNotFound() {

    expectedException.expect(InvalidAccountIdProblem.class);
    expectedException
      .expectMessage("AccountIds may only contain the following characters: 'a–z', '0–9', '-', '_', or '~'");
    when(accountSettingsRepositoryMock.findBySettlementEngineAccountId(SETTLEMENT_ACCOUNT_ID))
      .thenReturn(Optional.empty());

    settlementService
      .onIncomingSettlementPayment(UUID.randomUUID().toString(), SETTLEMENT_ACCOUNT_ID, INCOMING_SETTLEMENT);
  }

  @Test
  public void onLocalSettlementPayment() {
    String idempotencyKey = UUID.randomUUID().toString();

    final AccountSettings accountSettings = AccountSettings.builder()
      .accountId(ACCOUNT_ID)
      .accountRelationship(AccountRelationship.PEER)
      .assetCode("USD")
      .assetScale(9)
      .linkType(LoopbackLink.LINK_TYPE)
      .settlementEngineDetails(
        SettlementEngineDetails.builder()
          .settlementEngineAccountId(SettlementEngineAccountId.of(ACCOUNT_ID.value()))
          .baseUrl(HttpUrl.parse("https://example.com"))
          .build()
      )
      .build();

    when(accountSettingsRepositoryMock.findBySettlementEngineAccountIdWithConversion(SETTLEMENT_ACCOUNT_ID))
      .thenReturn(Optional.of(accountSettings));

    SettlementQuantity expectedClearedSettlementQuantity = SettlementQuantity.builder()
      .amount(BigInteger.valueOf(1000L))
      .scale(9)
      .build();

    SettlementQuantity actualClearedSettlementQuantity =
      settlementService.onIncomingSettlementPayment(idempotencyKey, SETTLEMENT_ACCOUNT_ID, INCOMING_SETTLEMENT);

    assertThat(actualClearedSettlementQuantity).isEqualTo(expectedClearedSettlementQuantity);
    verify(accountSettingsRepositoryMock).findBySettlementEngineAccountIdWithConversion(SETTLEMENT_ACCOUNT_ID);
    verify(balanceTrackerMock).updateBalanceForIncomingSettlement(
      idempotencyKey, ACCOUNT_ID, expectedClearedSettlementQuantity.amount().longValue()
    );
    verifyNoMoreInteractions(balanceTrackerMock);
    verifyNoMoreInteractions(accountSettingsRepositoryMock);
  }

  // TODO: Cover other methods in DefaultSettlementService.
}
