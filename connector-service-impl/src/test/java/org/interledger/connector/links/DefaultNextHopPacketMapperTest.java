package org.interledger.connector.links;

import com.google.common.primitives.UnsignedLong;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ImmutableAccountSettings;
import org.interledger.connector.caching.AccountSettingsLoadingCache;
import org.interledger.connector.fx.JavaMoneyUtils;
import org.interledger.connector.link.LinkType;
import org.interledger.connector.packetswitch.InterledgerAddressUtils;
import org.interledger.connector.routing.PaymentRouter;
import org.interledger.connector.routing.Route;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import javax.money.CurrencyUnit;
import javax.money.MonetaryAmount;
import java.math.BigInteger;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.CoreMatchers.containsString;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultNextHopPacketMapper}.
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultNextHopPacketMapperTest {

	private static final InterledgerAddressPrefix GLOBAL_ROUTING_TABLE_ENTRY = InterledgerAddressPrefix.of("g");
	private static final InterledgerAddressPrefix DEFAULT_TARGET_ADDRESS_PREFIX = GLOBAL_ROUTING_TABLE_ENTRY;
	private static final String TEST_ADDRESS = "test.bar";
	private static final String TEST_ADDRESS_2 = "test.foo";

	private DefaultNextHopPacketMapper defaultNextHopPacketMapper;

	@Mock
	private Supplier<ConnectorSettings> connectorSettingsSupplier;

	@Mock
	private PaymentRouter<Route> externalRoutingService;

	@Mock
	private InterledgerAddressUtils addressUtils;

	@Mock
	private JavaMoneyUtils javaMoneyUtils;

	@Mock
	private AccountSettingsLoadingCache accountSettingsLoadingCache;

	@Rule
	public ExpectedException expectedException = ExpectedException.none();

	@Before
	public void setup() {
		defaultNextHopPacketMapper = new DefaultNextHopPacketMapper(connectorSettingsSupplier,
				externalRoutingService, addressUtils, javaMoneyUtils, accountSettingsLoadingCache);
	}

	@Test
	public void getNextHopPacket() {
		final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
		final AccountId accountIdNextHopRoute = AccountId.of(UUID.randomUUID().toString());
		final AccountSettings sourceAccountSettings = buildAccountSettings(accountId)
				.build();
		final AccountSettings destinationAccountSettings = buildAccountSettings(accountId)
				.build();
		final InterledgerPreparePacket sourcePacket = buildInterLedgerPreparePacket(10);
		final Optional<Route> optionalRoute = Optional.of(Route.builder()
				.nextHopAccountId(accountIdNextHopRoute)
				.routePrefix(DEFAULT_TARGET_ADDRESS_PREFIX)
				.build());
		doReturn(optionalRoute).when(externalRoutingService).findBestNexHop(sourcePacket.getDestination());
		when(accountSettingsLoadingCache.safeGetAccountId(eq(optionalRoute.get().nextHopAccountId()))).thenReturn(destinationAccountSettings);
		when(addressUtils.isExternalForwardingAllowed(sourcePacket.getDestination())).thenReturn(false);

		final NextHopInfo nextHopInfo = defaultNextHopPacketMapper.getNextHopPacket(sourceAccountSettings, sourcePacket);

		assertThat(nextHopInfo.nextHopAccountId()).isEqualTo(optionalRoute.get().nextHopAccountId());
		assertThat(nextHopInfo.nextHopPacket().getAmount().intValue()).isEqualTo(10);
		assertThat(nextHopInfo.nextHopPacket().getDestination().getValue()).isEqualTo(TEST_ADDRESS_2);
		assertThat(nextHopInfo.nextHopPacket().getExpiresAt()).isNotNull();
	}

	@Test
	public void getNextHopPacketNoDestinationAddressFoundFromRoutingService() {
		final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
		final AccountSettings sourceAccountSettings = buildAccountSettings(accountId)
				.build();
		final InterledgerPreparePacket sourcePacket = buildInterLedgerPreparePacket(10);
		final Optional<Route> optionalRoute = Optional.empty();
		doReturn(optionalRoute).when(externalRoutingService).findBestNexHop(sourcePacket.getDestination());
		ConnectorSettings connectorSettings = mock(ConnectorSettings.class);
		when(connectorSettings.operatorAddressSafe()).thenReturn(InterledgerAddress.of(TEST_ADDRESS));
		when(connectorSettingsSupplier.get()).thenReturn(connectorSettings);
		expectedException.expect(InterledgerProtocolException.class);
		expectedException.expectMessage(containsString("No route found from accountId to destination"));
		defaultNextHopPacketMapper.getNextHopPacket(sourceAccountSettings, sourcePacket);
	}

	@Test
	public void getNextHopPacketSourceAccountIdMatchesNextHopAccountId() {
		final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
		final AccountSettings sourceAccountSettings = buildAccountSettings(accountId)
				.build();
		final InterledgerPreparePacket sourcePacket = buildInterLedgerPreparePacket(10);
		final Optional<Route> optionalRoute = Optional.of(Route.builder()
				.nextHopAccountId(accountId)
				.routePrefix(DEFAULT_TARGET_ADDRESS_PREFIX)
				.build());
		ConnectorSettings connectorSettings = mock(ConnectorSettings.class);
		when(connectorSettings.operatorAddressSafe()).thenReturn(InterledgerAddress.of(TEST_ADDRESS));
		when(connectorSettingsSupplier.get()).thenReturn(connectorSettings);
		doReturn(optionalRoute).when(externalRoutingService).findBestNexHop(sourcePacket.getDestination());
		expectedException.expect(InterledgerProtocolException.class);
		expectedException.expectMessage(containsString("Refusing to route payments back to sender"));
		defaultNextHopPacketMapper.getNextHopPacket(sourceAccountSettings, sourcePacket);
	}

	@Test
	public void determineNextAmountRequireNonNull() {
		expectedException.expect(NullPointerException.class);
		defaultNextHopPacketMapper.determineNextAmount(null, null, null);
	}

	@Test
	public void determineNextAmountExternalForwardingNotAllowedForDestination() {
		final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
		final AccountSettings sourceAccountSettings = buildAccountSettings(accountId)
				.build();
		final AccountSettings destinationAccountSettings = buildAccountSettings(accountId)
				.build();
		final InterledgerPreparePacket sourcePacket = buildInterLedgerPreparePacket(10);
		when(addressUtils.isExternalForwardingAllowed(sourcePacket.getDestination())).thenReturn(false);

		final UnsignedLong nextAmount = defaultNextHopPacketMapper.determineNextAmount(sourceAccountSettings,
				destinationAccountSettings, sourcePacket);

		assertThat(nextAmount.bigIntegerValue().intValue()).isEqualTo(10);
	}

	@Test
	public void determineNextAmountExternalForwardingAllowedForDestination() {
		final AccountId accountId = AccountId.of(UUID.randomUUID().toString());
		final AccountSettings sourceAccountSettings = buildAccountSettings(accountId)
				.build();
		final AccountSettings destinationAccountSettings = buildAccountSettings(accountId)
				.build();
		final InterledgerPreparePacket sourcePacket = buildInterLedgerPreparePacket(100);
		when(addressUtils.isExternalForwardingAllowed(sourcePacket.getDestination())).thenReturn(true);
		when(javaMoneyUtils.toInterledgerAmount(any(MonetaryAmount.class), eq(9))).thenCallRealMethod();
		when(javaMoneyUtils.toMonetaryAmount(any(CurrencyUnit.class),
				any(BigInteger.class), eq(9))).thenCallRealMethod();

		final UnsignedLong nextAmount = defaultNextHopPacketMapper.determineNextAmount(sourceAccountSettings,
				destinationAccountSettings, sourcePacket);

		assertThat(nextAmount.bigIntegerValue().intValue()).isEqualTo(100);
	}

	@Test
	public void determineDestinationExpiresAtSourceExpiryRequired() {
		final InterledgerAddress destinationAddress = InterledgerAddress.of(TEST_ADDRESS_2);
		expectedException.expect(NullPointerException.class);
		defaultNextHopPacketMapper.determineDestinationExpiresAt(null, destinationAddress);
	}

	@Test
	public void determineDestinationExpiresAtExternalForwardingNotAllowed() {
		final Instant sourceExpiry = Instant.now();
		final InterledgerAddress destinationAddress = InterledgerAddress.of(TEST_ADDRESS_2);
		when(addressUtils.isExternalForwardingAllowed(destinationAddress)).thenReturn(false);

		final Instant expiresAt = defaultNextHopPacketMapper.determineDestinationExpiresAt(sourceExpiry,
				destinationAddress);

		assertThat(expiresAt).isEqualTo(sourceExpiry);
	}

	@Test
	public void determineDestinationExpiresAtExternalForwardingAllowedTransferExpired() {
		final Instant sourceExpiry = Instant.now();
		ConnectorSettings connectorSettings = mock(ConnectorSettings.class);
		final InterledgerAddress destinationAddress = InterledgerAddress.of(TEST_ADDRESS_2);
		when(addressUtils.isExternalForwardingAllowed(destinationAddress)).thenReturn(true);
		when(connectorSettings.operatorAddressSafe()).thenReturn(InterledgerAddress.of(TEST_ADDRESS));
		when(connectorSettingsSupplier.get()).thenReturn(connectorSettings);
		expectedException.expect(InterledgerProtocolException.class);
		expectedException.expectMessage(containsString("Interledger Rejection: Source transfer has already expired"));
		defaultNextHopPacketMapper.determineDestinationExpiresAt(sourceExpiry, destinationAddress);
	}

	@Test
	public void determineDestinationExpiresAtExternalForwardingAllowedExpiryTooSoon() {
		final Instant sourceExpiry = Instant.now().plus(2000, MILLIS);
		ConnectorSettings connectorSettings = mock(ConnectorSettings.class);
		final InterledgerAddress destinationAddress = InterledgerAddress.of(TEST_ADDRESS_2);
		when(addressUtils.isExternalForwardingAllowed(destinationAddress)).thenReturn(true);
		when(connectorSettings.operatorAddressSafe()).thenReturn(InterledgerAddress.of(TEST_ADDRESS));
		when(connectorSettingsSupplier.get()).thenReturn(connectorSettings);
		expectedException.expect(InterledgerProtocolException.class);
		expectedException.expectMessage(containsString("Interledger Rejection: Source transfer expires too soon to complete payment"));
		defaultNextHopPacketMapper.determineDestinationExpiresAt(sourceExpiry, destinationAddress);
	}

	@Test
	public void determineDestinationExpiresAtExternalForwardingAllowed() {
		final Instant sourceExpiry = Instant.now().plus(20000, MILLIS);
		final Instant destinationExpiry = Instant.now().plus(1000, MILLIS);
		final InterledgerAddress destinationAddress = InterledgerAddress.of(TEST_ADDRESS_2);
		when(addressUtils.isExternalForwardingAllowed(destinationAddress)).thenReturn(true);
		final Instant expiresAt = defaultNextHopPacketMapper.determineDestinationExpiresAt(sourceExpiry,
				destinationAddress);

		assertThat(expiresAt.isAfter(destinationExpiry)).isTrue();
		assertThat(expiresAt.isBefore(sourceExpiry)).isTrue();
	}

	@Test
	public void lesser() {
		final Instant first = Instant.now();
		final Instant second = first.plus(1000, MILLIS);

		Instant lesser = defaultNextHopPacketMapper.lesser(first, second);
		assertThat(lesser).isEqualTo(first);

		lesser = defaultNextHopPacketMapper.lesser(second, first);
		assertThat(lesser).isEqualTo(first);
	}

	private InterledgerPreparePacket.AbstractInterledgerPreparePacket buildInterLedgerPreparePacket(int i) {
		return InterledgerPreparePacket.builder()
				.destination(InterledgerAddress.of(TEST_ADDRESS_2))
				.amount(UnsignedLong.valueOf(i))
				.executionCondition(InterledgerCondition.of(new byte[32]))
				.expiresAt(Instant.now().plusSeconds(50))
				.build();
	}

	private ImmutableAccountSettings.Builder buildAccountSettings(AccountId accountId) {
		return AccountSettings.builder()
				.accountId(accountId)
				.assetCode("XRP")
				.assetScale(9)
				.linkType(LinkType.of("Loopback"))
				.accountRelationship(AccountRelationship.PEER);
	}
}
