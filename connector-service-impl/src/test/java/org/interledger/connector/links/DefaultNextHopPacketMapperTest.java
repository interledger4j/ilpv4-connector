package org.interledger.connector.links;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ImmutableAccountSettings;
import org.interledger.connector.caching.AccountSettingsLoadingCache;
import org.interledger.connector.fx.JavaMoneyUtils;
import org.interledger.connector.packetswitch.InterledgerAddressUtils;
import org.interledger.connector.routing.ImmutableRoute;
import org.interledger.connector.routing.PaymentRouter;
import org.interledger.connector.routing.Route;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.settings.ModifiableConnectorSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerPreparePacketBuilder;
import org.interledger.core.InterledgerProtocolException;
import org.interledger.link.LoopbackLink;

import com.google.common.primitives.UnsignedLong;
import org.javamoney.moneta.spi.DefaultNumberValue;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.stubbing.Answer;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.Base64;
import java.util.Optional;
import java.util.function.Supplier;

import javax.money.MonetaryAmount;
import javax.money.convert.CurrencyConversion;
import javax.money.convert.ExchangeRate;

/**
 * Unit tests for {@link DefaultNextHopPacketMapper}.
 */
@FixMethodOrder
public class DefaultNextHopPacketMapperTest {

  private static final ImmutableRoute NEXT_HOP = Route.builder().nextHopAccountId(AccountId.of("ovaltine-jenkins"))
    .routePrefix(InterledgerAddressPrefix.GLOBAL)
    .build();
  private static final ImmutableRoute NEXT_HOP_2 = Route.builder().nextHopAccountId(AccountId.of("galileo-humpkins"))
    .routePrefix(InterledgerAddressPrefix.GLOBAL)
    .build();

  private static final int MIN_MESSAGE_WINDOW_MILLIS = 1000;
  private static final AccountId RECEIVER_ACCOUNT_ID = AccountId.of("trapezius-milkington");
  private static final InterledgerAddress RECEIVER = InterledgerAddress.of("g.test").with(RECEIVER_ACCOUNT_ID.value());
  private static final AccountId SENDER_ACCOUNT_ID = AccountId.of("galileo-humpkins");
  private static final InterledgerCondition CONDITION = InterledgerCondition.of(
    Base64.getDecoder().decode("jAC8DGFPZPfh4AtZpXuvXFe2oRmpDVSvSJg2oT+bx34="));

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private DefaultNextHopPacketMapper mapper;
  private ModifiableConnectorSettings connectorSettings;
  @Mock
  private PaymentRouter<Route> mockRoutingService;
  @Mock
  private AccountSettingsLoadingCache mockAccountCache;
  @Mock
  private InterledgerAddressUtils mockAddressUtils;
  @Mock
  private CurrencyConversion mockCurrencyConversion;
  @Mock
  private ExchangeRate mockExchangeRate;

  private Clock clock;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    clock = Clock.fixed(Instant.now(), ZoneId.of("UTC"));
    connectorSettings = ModifiableConnectorSettings.create();
    connectorSettings.setMinMessageWindowMillis(MIN_MESSAGE_WINDOW_MILLIS);
    connectorSettings.setMaxHoldTimeMillis(30000);
    connectorSettings.setOperatorAddress(InterledgerAddress.of("g.safe"));
    mockConversionRate(1);

    mockExternalForwardingAllowed(true);
    Supplier<ConnectorSettings> connectorSettingsSupplier = () -> connectorSettings;
    mapper = new DefaultNextHopPacketMapper(connectorSettingsSupplier,
      mockRoutingService,
      mockAddressUtils,
      new JavaMoneyUtils(),
      mockAccountCache,
      (currencyUnit -> mockCurrencyConversion));
  }

  private void mockExternalForwardingAllowed(boolean value) {
    when(mockAddressUtils.isExternalForwardingAllowed(any())).thenReturn(value);
  }

  @Test
  public void getNextHopPacket() {
    Instant now = Instant.now(clock);
    AccountSettings settings = defaultSenderAccountSettings().build();
    InterledgerPreparePacket preparePacket = defaultPreparePacket(now).build();

    when(mockAccountCache.safeGetAccountId(NEXT_HOP.nextHopAccountId()))
      .thenReturn(defaultNextHopSettings().build());

    when(mockRoutingService.findBestNexHop(RECEIVER)).thenReturn(Optional.of(NEXT_HOP));

    NextHopInfo result = mapper.getNextHopPacket(settings, preparePacket);
    InterledgerPreparePacket expectedPreparePacket = defaultExpectedPreparePacket(preparePacket).build();
    assertPreparePacket(result, expectedPreparePacket);
  }

  @Test
  public void getNextHopPacketNoDestinationAddressFoundFromRoutingService() {
    Instant now = Instant.now(clock);
    AccountSettings settings = defaultSenderAccountSettings().build();
    InterledgerPreparePacket preparePacket = defaultPreparePacket(now).build();

    when(mockAccountCache.safeGetAccountId(NEXT_HOP.nextHopAccountId()))
      .thenReturn(defaultNextHopSettings().build());
    when(mockRoutingService.findBestNexHop(RECEIVER)).thenReturn(Optional.empty());
    expectedException.expect(InterledgerProtocolException.class);
    expectedException.expectMessage("No route found from accountId to destination");
    mapper.getNextHopPacket(settings, preparePacket);
  }

  @Test
  public void getNextHopPacketSourceAccountIdMatchesNextHopAccountId() {
    Instant now = Instant.now(clock);
    AccountSettings settings = defaultSenderAccountSettings().build();
    InterledgerPreparePacket preparePacket = defaultPreparePacket(now).build();

    when(mockAccountCache.safeGetAccountId(NEXT_HOP_2.nextHopAccountId()))
      .thenReturn(defaultNextHopSettings().build());

    when(mockRoutingService.findBestNexHop(RECEIVER)).thenReturn(Optional.of(NEXT_HOP_2));

    expectedException.expect(InterledgerProtocolException.class);
    expectedException.expectMessage("Refusing to route payments back to sender");
    mapper.getNextHopPacket(settings, preparePacket);
  }

  @Test
  public void determineNextAmountWithConversion() {
    Instant now = Instant.now(clock);
    AccountSettings sourceSettings = defaultSenderAccountSettings()
      .assetCode("EUR")
      .build();
    InterledgerPreparePacket preparePacket = defaultPreparePacket(now)
      .amount(UnsignedLong.valueOf(100))
      .build();

    when(mockAccountCache.safeGetAccountId(NEXT_HOP.nextHopAccountId()))
      .thenReturn(defaultNextHopSettings().build());

    when(mockRoutingService.findBestNexHop(RECEIVER)).thenReturn(Optional.of(NEXT_HOP));

    int conversionRate = 2;
    mockConversionRate(conversionRate);

    UnsignedLong result = mapper.determineNextAmount(sourceSettings, defaultNextHopSettings().build(), preparePacket);
    assertThat(result).isEqualTo(UnsignedLong.valueOf(200));
  }

  @Test
  public void determineNextAmountExternalForwardingNotAllowedForDestination() {
    mockExternalForwardingAllowed(false);
    Instant now = Instant.now(clock);
    AccountSettings sourceSettings = defaultSenderAccountSettings().build();
    InterledgerPreparePacket preparePacket = defaultPreparePacket(now).build();

    UnsignedLong result = mapper.determineNextAmount(sourceSettings, defaultNextHopSettings().build(), preparePacket);
    assertThat(result).isEqualTo(UnsignedLong.valueOf(10000));
  }

  @Test
  public void determineDestinationExpiresAtNoExternalForwarding() {
    mockExternalForwardingAllowed(false);
    Instant expiry = Instant.now(clock).plusSeconds(10);
    mockExternalForwardingAllowed(false);
    assertThat(mapper.determineDestinationExpiresAt(clock, expiry, RECEIVER)).isEqualTo(expiry);
  }

  @Test
  public void determineDestinationExpiresAtLimitedByMaxHold() {
    Instant now = Instant.now(clock);
    Instant expiry = now.plusMillis(10000);
    int maxHoldTimeMillis = 5000;
    int minMessageWindowMillis = 1000;
    connectorSettings.setMaxHoldTimeMillis(maxHoldTimeMillis);
    connectorSettings.setMinMessageWindowMillis(minMessageWindowMillis);
    mockExternalForwardingAllowed(true);
    assertThat(mapper.determineDestinationExpiresAt(clock, expiry, RECEIVER))
      .isEqualTo(now.plusMillis(maxHoldTimeMillis));
  }

  @Test
  public void determineNextAmountWithNullSourceAccountSettings() {
    expectedException.expect(NullPointerException.class);
    mapper.determineNextAmount(null, mock(AccountSettings.class), mock(InterledgerPreparePacket.class));
  }

  @Test
  public void determineNextAmountWithNullDestinationAccountSettings() {
    expectedException.expect(NullPointerException.class);
    mapper.determineNextAmount(mock(AccountSettings.class), null, mock(InterledgerPreparePacket.class));
  }

  @Test
  public void determineNextAmountWithNullSourcePacket() {
    expectedException.expect(NullPointerException.class);
    mapper.determineNextAmount(mock(AccountSettings.class), mock(AccountSettings.class), null);
  }

  @Test
  public void determineDestinationExpiresReducedByMinMessageWindow() {
    Instant now = Instant.now(clock);
    Instant expiry = now.plusMillis(10000);
    int maxHoldTimeMillis = 10000;
    int minMessageWindowMillis = 1000;
    connectorSettings.setMaxHoldTimeMillis(maxHoldTimeMillis);
    connectorSettings.setMinMessageWindowMillis(minMessageWindowMillis);
    mockExternalForwardingAllowed(true);
    assertThat(mapper.determineDestinationExpiresAt(clock, expiry, RECEIVER))
      .isEqualTo(expiry.minusMillis(minMessageWindowMillis));
  }

  @Test
  public void determineDestinationExpiresAfterMinMessageWindow() {
    Instant now = Instant.now(clock);
    Instant expiry = now.plusMillis(1000);
    int maxHoldTimeMillis = 10000;
    int minMessageWindowMillis = 2000;
    connectorSettings.setMaxHoldTimeMillis(maxHoldTimeMillis);
    connectorSettings.setMinMessageWindowMillis(minMessageWindowMillis);
    mockExternalForwardingAllowed(true);
    expectedException.expect(InterledgerProtocolException.class);
    mapper.determineDestinationExpiresAt(clock, expiry, RECEIVER);
  }

  @Test
  public void determineDestinationExpiresExactlyAtMinMessageWindow() {
    Instant now = Instant.now(clock);
    Instant expiry = now.plusMillis(1000);
    int maxHoldTimeMillis = 10000;
    int minMessageWindowMillis = 1000;
    connectorSettings.setMaxHoldTimeMillis(maxHoldTimeMillis);
    connectorSettings.setMinMessageWindowMillis(minMessageWindowMillis);
    mockExternalForwardingAllowed(true);
    expectedException.expect(InterledgerProtocolException.class);
    expectedException.expectMessage("Interledger Rejection: Source transfer expires too soon to complete payment");
    mapper.determineDestinationExpiresAt(clock, expiry, RECEIVER);
  }

  @Test
  public void determineDestinationExpiresAlreadyExpired() {
    Instant now = Instant.now(clock);
    Instant expiry = now.minusMillis(500);
    int maxHoldTimeMillis = 10000;
    int minMessageWindowMillis = 2000;
    connectorSettings.setMaxHoldTimeMillis(maxHoldTimeMillis);
    connectorSettings.setMinMessageWindowMillis(minMessageWindowMillis);
    mockExternalForwardingAllowed(true);
    expectedException.expect(InterledgerProtocolException.class);
    expectedException.expectMessage("Interledger Rejection: Source transfer has already expired");
    mapper.determineDestinationExpiresAt(clock, expiry, RECEIVER);
  }

  @Test
  public void determineDestinationExpiresAtSourceExpiryRequired() {
    int maxHoldTimeMillis = 10000;
    int minMessageWindowMillis = 1000;
    connectorSettings.setMaxHoldTimeMillis(maxHoldTimeMillis);
    connectorSettings.setMinMessageWindowMillis(minMessageWindowMillis);
    mockExternalForwardingAllowed(true);
    expectedException.expect(NullPointerException.class);
    mapper.determineDestinationExpiresAt(clock, null, RECEIVER);
  }

  @Test
  public void lesser() {
    Instant now = Instant.now(clock);
    Instant nowPlusOne = Instant.now(clock).plusSeconds(1);
    assertThat(mapper.lesser(now, nowPlusOne)).isEqualTo(now);
    assertThat(mapper.lesser(nowPlusOne, now)).isEqualTo(now);
    assertThat(mapper.lesser(now, now)).isEqualTo(now);
  }


  private ImmutableAccountSettings.Builder defaultNextHopSettings() {
    return AccountSettings.builder()
      .accountId(NEXT_HOP.nextHopAccountId())
      .linkType(LoopbackLink.LINK_TYPE)
      .accountRelationship(AccountRelationship.PEER)
      .assetCode("USD")
      .assetScale(3);
  }

  private InterledgerPreparePacketBuilder defaultPreparePacket(Instant now) {
    return InterledgerPreparePacket.builder()
      .destination(RECEIVER)
      .expiresAt(now.plusSeconds(5))
      .amount(UnsignedLong.valueOf(10000))
      .executionCondition(CONDITION);
  }

  private ImmutableAccountSettings.Builder defaultSenderAccountSettings() {
    return AccountSettings.builder()
      .accountRelationship(AccountRelationship.PEER)
      .assetCode("USD")
      .assetScale(3)
      .linkType(LoopbackLink.LINK_TYPE)
      .accountId(SENDER_ACCOUNT_ID);
  }

  private void assertPreparePacket(NextHopInfo result, InterledgerPreparePacket expectedPreparePacket) {
    assertThat(result).isEqualTo(NextHopInfo.builder().nextHopAccountId(NEXT_HOP.nextHopAccountId())
      .nextHopPacket(expectedPreparePacket).build());
  }

  private InterledgerPreparePacketBuilder defaultExpectedPreparePacket(InterledgerPreparePacket preparePacket) {
    return InterledgerPreparePacket.builder()
      .from(preparePacket)
      .expiresAt(preparePacket.getExpiresAt().minusMillis(MIN_MESSAGE_WINDOW_MILLIS));
  }

  /**
   * Sets the conversion rate to a fixed rate
   *
   * @param rate A {@link long} representing the rate.
   */
  private void mockConversionRate(long rate) {
    reset(mockExchangeRate, mockCurrencyConversion);
    when(mockExchangeRate.getFactor()).thenReturn(new DefaultNumberValue(rate));
    when(mockCurrencyConversion.apply(any())).thenAnswer(
      (Answer<MonetaryAmount>) invocationOnMock -> invocationOnMock
        .getArgument(0, MonetaryAmount.class)
        .multiply(rate)
    );
  }

}
