package org.interledger.connector.links.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.mockito.Mockito.when;

import org.interledger.connector.accounts.AccountBalanceSettings;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ImmutableAccountSettings.Builder;
import org.interledger.connector.accounts.SettlementEngineAccountId;
import org.interledger.connector.accounts.SettlementEngineDetails;
import org.interledger.connector.balances.AccountBalance;
import org.interledger.connector.balances.BalanceTracker;
import org.interledger.connector.balances.BalanceTracker.UpdateBalanceForFulfillResponse;
import org.interledger.connector.balances.BalanceTrackerException;
import org.interledger.connector.core.settlement.SettlementQuantity;
import org.interledger.connector.settlement.SettlementService;
import org.interledger.connector.settlement.SettlementServiceException;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerFulfillment;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.link.LoopbackLink;

import com.google.common.eventbus.EventBus;
import com.google.common.primitives.UnsignedLong;
import okhttp3.HttpUrl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.MockitoAnnotations;

import java.math.BigInteger;
import java.time.Instant;
import java.util.function.Supplier;

/**
 * Unit tests for {@link OutgoingBalanceLinkFilter}.
 */
public class OutgoingBalanceLinkFilterTest {

  private static final SettlementEngineAccountId SETTLEMENT_ENGINE_ACCOUNT_ID
      = SettlementEngineAccountId.of("seAccountId");
  private static final AccountId ACCOUNT_ID = AccountId.of("testAccountId");
  private static final Supplier<InterledgerAddress> operatorAddressSupplier = () ->
      InterledgerAddress.of("example.operator");
  private static final InterledgerAddress DESTINATION_ADDRESS = InterledgerAddress.of("example.destination");
  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private BalanceTracker balanceTrackerMock;
  @Mock
  private SettlementService settlementServiceMock;
  @Mock
  private EventBus eventBusMock;
  @Mock
  private LinkFilterChain filterChainMock;

  private OutgoingBalanceLinkFilter linkFilter;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    this.linkFilter = new OutgoingBalanceLinkFilter(
        operatorAddressSupplier, balanceTrackerMock, settlementServiceMock, eventBusMock
    );
  }

  @Test
  public void constructorWithNullFirstArg() {
    expectedException.expect(NullPointerException.class);
    new OutgoingBalanceLinkFilter(
        null, balanceTrackerMock, settlementServiceMock, eventBusMock
    );
  }

  @Test
  public void constructorWithNullSecondArg() {
    expectedException.expect(NullPointerException.class);
    new OutgoingBalanceLinkFilter(
        operatorAddressSupplier, null, settlementServiceMock, eventBusMock
    );
  }

  @Test
  public void constructorWithNullThirdArg() {
    expectedException.expect(NullPointerException.class);
    new OutgoingBalanceLinkFilter(
        operatorAddressSupplier, balanceTrackerMock, null, eventBusMock
    );
  }

  @Test
  public void constructorWithNullFourthArg() {
    expectedException.expect(NullPointerException.class);
    new OutgoingBalanceLinkFilter(
        operatorAddressSupplier, balanceTrackerMock, settlementServiceMock, null
    );
  }

  @Test
  public void doFilterWithNullAccountSettings() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("destinationAccountSettings must not be null");
    linkFilter.doFilter(null, preparePacket(), filterChainMock);
  }

  @Test
  public void doFilterWithNullPreparePacket() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("outgoingPreparePacket must not be null");
    linkFilter.doFilter(accountSettings(), null, filterChainMock);
  }

  @Test
  public void doFilterWithNulFilterChain() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("filterChain must not be null");
    linkFilter.doFilter(accountSettings(), preparePacket(), null);
  }

  @Test
  public void doFilterForFulfillZeroValuePacket() {
    when(filterChainMock.doFilter(accountSettings(), preparePacket())).thenReturn(fulfillPacket());
    InterledgerResponsePacket actual = linkFilter.doFilter(
        accountSettings(),
        preparePacket(),
        filterChainMock
    );

    assertThat(actual).isEqualTo(fulfillPacket());

    verifyNoMoreInteractions(balanceTrackerMock);
    verifyNoMoreInteractions(settlementServiceMock);
    verifyNoMoreInteractions(eventBusMock);
  }

  @Test
  public void doFilterForFulfillWithNonZerorPacketValueAndNoSettlementEngine() {
    when(filterChainMock.doFilter(accountSettings(), preparePacket(UnsignedLong.ONE))).thenReturn(fulfillPacket());
    when(balanceTrackerMock.updateBalanceForFulfill(accountSettings(), 1L)).thenReturn(
        UpdateBalanceForFulfillResponse.builder()
            .accountBalance(AccountBalance.builder()
                .accountId(ACCOUNT_ID)
                .clearingBalance(10L)
                .prepaidAmount(10L)
                .build())
            .clearingAmountToSettle(10L)
            .build()
    );

    final InterledgerResponsePacket actual = linkFilter.doFilter(
        accountSettings(),
        preparePacket(UnsignedLong.ONE),
        filterChainMock
    );

    assertThat(actual).isEqualTo(fulfillPacket());

    verify(balanceTrackerMock).updateBalanceForFulfill(eq(accountSettings()), eq(1L));
    verifyNoMoreInteractions(balanceTrackerMock);
    verifyNoMoreInteractions(settlementServiceMock);
    verifyNoMoreInteractions(eventBusMock);
  }

  @Test
  public void testMaybeSettleWithNoThreshold() {
    final AccountSettings accountSettings = accountSettingsBuilder()
        .settlementEngineDetails(
            SettlementEngineDetails.builder()
                .settlementEngineAccountId(SETTLEMENT_ENGINE_ACCOUNT_ID)
                .baseUrl(HttpUrl.parse("http://example.com"))
                .build()
        )
        .build();

    when(filterChainMock.doFilter(accountSettings, preparePacket(UnsignedLong.ONE))).thenReturn(fulfillPacket());
    when(balanceTrackerMock.updateBalanceForFulfill(accountSettings, 1L)).thenReturn(
        UpdateBalanceForFulfillResponse.builder()
            .accountBalance(AccountBalance.builder()
                .accountId(ACCOUNT_ID)
                .clearingBalance(10L)
                .prepaidAmount(10L)
                .build())
            .clearingAmountToSettle(10L)
            .build()
    );

    final InterledgerResponsePacket actual = linkFilter.doFilter(
        accountSettings,
        preparePacket(UnsignedLong.ONE),
        filterChainMock
    );

    assertThat(actual).isEqualTo(fulfillPacket());

    verify(balanceTrackerMock).updateBalanceForFulfill(eq(accountSettings()), eq(1L));
    verifyNoMoreInteractions(balanceTrackerMock);
    verifyNoMoreInteractions(settlementServiceMock);
    verifyNoMoreInteractions(eventBusMock);
  }

  @Test
  public void testMaybeSettleWhenClearingAmountToSettleIsBelowThreshold() {
    final AccountSettings accountSettings = accountSettingsWithSettlementEngine(100L).build();
    when(filterChainMock.doFilter(accountSettings, preparePacket(UnsignedLong.ONE))).thenReturn(fulfillPacket());
    when(balanceTrackerMock.updateBalanceForFulfill(accountSettings, 1L)).thenReturn(
        UpdateBalanceForFulfillResponse.builder()
            .accountBalance(AccountBalance.builder()
                .accountId(ACCOUNT_ID)
                .clearingBalance(10L)
                .prepaidAmount(10L)
                .build())
            .clearingAmountToSettle(10L)
            .build()
    );

    final InterledgerResponsePacket actual = linkFilter.doFilter(
        accountSettings,
        preparePacket(UnsignedLong.ONE),
        filterChainMock
    );

    assertThat(actual).isEqualTo(fulfillPacket());

    verify(balanceTrackerMock).updateBalanceForFulfill(eq(accountSettings()), eq(1L));
    verifyNoMoreInteractions(balanceTrackerMock);
    verifyNoMoreInteractions(settlementServiceMock);
    verifyNoMoreInteractions(eventBusMock);
  }

  @Test
  public void testMaybeSettleWhenClearingAmountToSettleIsAtThreshold() {
    final AccountSettings accountSettings = accountSettingsWithSettlementEngine(100L).build();

    when(filterChainMock.doFilter(accountSettings, preparePacket(UnsignedLong.ONE))).thenReturn(fulfillPacket());
    when(balanceTrackerMock.updateBalanceForFulfill(accountSettings, 1L)).thenReturn(
        UpdateBalanceForFulfillResponse.builder()
            .accountBalance(AccountBalance.builder()
                .accountId(ACCOUNT_ID)
                .clearingBalance(1L)
                .prepaidAmount(0L)
                .build())
            .clearingAmountToSettle(100L)
            .build()
    );
    final SettlementQuantity expectedSettlementQuantityInClearingUnits = SettlementQuantity.builder()
        .scale(9)
        .amount(BigInteger.valueOf(100L))
        .build();
    when(settlementServiceMock.initiateLocalSettlement(
        anyString(), eq(accountSettings), eq(expectedSettlementQuantityInClearingUnits)
    )).thenReturn(SettlementQuantity.builder()
        .amount(BigInteger.valueOf(100L))
        .scale(2)
        .build());

    final InterledgerResponsePacket actual = linkFilter.doFilter(
        accountSettings,
        preparePacket(UnsignedLong.ONE),
        filterChainMock
    );

    assertThat(actual).isEqualTo(fulfillPacket());

    verify(balanceTrackerMock).updateBalanceForFulfill(eq(accountSettings()), eq(1L));
    verify(settlementServiceMock).initiateLocalSettlement(
        anyString(), eq(accountSettings), eq(expectedSettlementQuantityInClearingUnits)
    );
    verifyNoMoreInteractions(balanceTrackerMock);
    verifyNoMoreInteractions(settlementServiceMock);
    verify(eventBusMock).post(Mockito.any());
    verifyNoMoreInteractions(eventBusMock);
  }

  @Test
  public void testMaybeSettleWhenClearingAmountToSettleIsAboveThreshold() {
    final AccountSettings accountSettings = accountSettingsWithSettlementEngine(100L).build();

    when(filterChainMock.doFilter(accountSettings, preparePacket(UnsignedLong.ONE))).thenReturn(fulfillPacket());
    when(balanceTrackerMock.updateBalanceForFulfill(accountSettings, 1L)).thenReturn(
        UpdateBalanceForFulfillResponse.builder()
            .accountBalance(AccountBalance.builder()
                .accountId(ACCOUNT_ID)
                .clearingBalance(0L)
                .prepaidAmount(0L)
                .build())
            .clearingAmountToSettle(200L)
            .build()
    );
    final SettlementQuantity expectedSettlementQuantityInClearingUnits = SettlementQuantity.builder()
        .scale(9)
        .amount(BigInteger.valueOf(200L))
        .build();
    when(settlementServiceMock.initiateLocalSettlement(
        anyString(), eq(accountSettings), eq(expectedSettlementQuantityInClearingUnits)
    )).thenReturn(SettlementQuantity.builder()
        .amount(BigInteger.valueOf(200L))
        .scale(2)
        .build());

    final InterledgerResponsePacket actual = linkFilter.doFilter(
        accountSettings,
        preparePacket(UnsignedLong.ONE),
        filterChainMock
    );

    assertThat(actual).isEqualTo(fulfillPacket());

    verify(balanceTrackerMock).updateBalanceForFulfill(eq(accountSettings()), eq(1L));
    verify(settlementServiceMock).initiateLocalSettlement(
        anyString(), eq(accountSettings), eq(expectedSettlementQuantityInClearingUnits)
    );
    verifyNoMoreInteractions(balanceTrackerMock);
    verifyNoMoreInteractions(settlementServiceMock);
    verify(eventBusMock).post(Mockito.any());
    verifyNoMoreInteractions(eventBusMock);
  }

  @Test
  public void testMaybeSettleWhenClearingAmountToSettleIsAboveThresholdButSettleServiceThrows() {
    final AccountSettings accountSettings = accountSettingsWithSettlementEngine(100L).build();

    when(filterChainMock.doFilter(accountSettings, preparePacket(UnsignedLong.ONE))).thenReturn(fulfillPacket());
    when(balanceTrackerMock.updateBalanceForFulfill(accountSettings, 1L)).thenReturn(
        UpdateBalanceForFulfillResponse.builder()
            .accountBalance(AccountBalance.builder()
                .accountId(ACCOUNT_ID)
                .clearingBalance(0L)
                .prepaidAmount(0L)
                .build())
            .clearingAmountToSettle(200L)
            .build()
    );
    final SettlementQuantity expectedSettlementQuantityInClearingUnits = SettlementQuantity.builder()
        .scale(9)
        .amount(BigInteger.valueOf(200L))
        .build();

    doThrow(new SettlementServiceException("foo", ACCOUNT_ID, SETTLEMENT_ENGINE_ACCOUNT_ID))
        .when(settlementServiceMock).initiateLocalSettlement(anyString(), Mockito.any(), Mockito.any());

    final InterledgerResponsePacket actual = linkFilter.doFilter(
        accountSettings,
        preparePacket(UnsignedLong.ONE),
        filterChainMock
    );

    assertThat(actual).isEqualTo(fulfillPacket());

    verify(balanceTrackerMock).updateBalanceForFulfill(eq(accountSettings()), eq(1L));
    verify(settlementServiceMock).initiateLocalSettlement(
        anyString(), eq(accountSettings), eq(expectedSettlementQuantityInClearingUnits)
    );
    verifyNoMoreInteractions(balanceTrackerMock);
    verifyNoMoreInteractions(settlementServiceMock);
    verify(eventBusMock).post(Mockito.any());
    verifyNoMoreInteractions(eventBusMock);
  }

  @Test
  public void doFilterForReject() {
    // fulfill by default.
    when(filterChainMock.doFilter(accountSettings(), preparePacket())).thenReturn(rejectPacket());

    InterledgerResponsePacket actual = linkFilter.doFilter(
        accountSettings(),
        preparePacket(),
        filterChainMock
    );

    assertThat(actual).isEqualTo(rejectPacket());
    verify(balanceTrackerMock).balance(ACCOUNT_ID);
    verifyNoMoreInteractions(balanceTrackerMock);
    verifyNoMoreInteractions(settlementServiceMock);
  }

  @Test
  public void doFilterForFailureInBalanceTracker() {
    when(filterChainMock.doFilter(accountSettings(), preparePacket(UnsignedLong.ONE))).thenReturn(fulfillPacket());

    doThrow(new BalanceTrackerException()).when(balanceTrackerMock).updateBalanceForFulfill(
        accountSettings(), 1L
    );

    final InterledgerResponsePacket actual = linkFilter.doFilter(
        accountSettings(),
        preparePacket(UnsignedLong.ONE),
        filterChainMock
    );

    assertThat(actual).isEqualTo(fulfillPacket());

    verify(balanceTrackerMock).updateBalanceForFulfill(eq(accountSettings()), eq(1L));
    verifyNoMoreInteractions(balanceTrackerMock);
    verifyNoMoreInteractions(settlementServiceMock);
    verifyNoMoreInteractions(eventBusMock);
  }

  //////////////////
  // Private Helpers
  //////////////////

  private Builder accountSettingsBuilder() {
    return AccountSettings.builder()
        .accountId(AccountId.of("testAccountId"))
        .accountRelationship(AccountRelationship.PEER)
        .assetScale(9)
        .assetCode("XRP")
        .linkType(LoopbackLink.LINK_TYPE);
  }

  private AccountSettings accountSettings() {
    return accountSettingsBuilder().build();
  }

  private Builder accountSettingsWithSettlementEngine(final long settlementThreshold) {
    return accountSettingsBuilder()
        .settlementEngineDetails(
            SettlementEngineDetails.builder()
                .settlementEngineAccountId(SETTLEMENT_ENGINE_ACCOUNT_ID)
                .baseUrl(HttpUrl.parse("http://example.com"))
                .build()
        )
        .balanceSettings(AccountBalanceSettings.builder()
            .settleThreshold(settlementThreshold)
            .build());
  }

  private InterledgerPreparePacket preparePacket() {
    return preparePacket(UnsignedLong.ZERO);
  }

  private InterledgerPreparePacket preparePacket(final UnsignedLong amount) {
    return InterledgerPreparePacket.builder()
        .destination(DESTINATION_ADDRESS)
        .executionCondition(InterledgerCondition.of(new byte[32]))
        .amount(amount)
        .expiresAt(Instant.MAX)
        .build();
  }

  private InterledgerFulfillPacket fulfillPacket() {
    return InterledgerFulfillPacket.builder().fulfillment(InterledgerFulfillment.of(new byte[32])).build();
  }

  private InterledgerRejectPacket rejectPacket() {
    return InterledgerRejectPacket.builder()
        .code(InterledgerErrorCode.F99_APPLICATION_ERROR)
        .build();
  }
}
