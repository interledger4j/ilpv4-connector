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
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerProtocolException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.money.CurrencyUnit;
import javax.money.MonetaryAmount;
import java.math.BigInteger;
import java.time.Instant;
import java.util.UUID;
import java.util.function.Supplier;

import static java.time.temporal.ChronoUnit.MILLIS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultNextHopPacketMapper}.
 */
@RunWith(MockitoJUnitRunner.class)
public class DefaultNextHopPacketMapperTest {

	private Logger logger = LoggerFactory.getLogger(this.getClass());

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

	@Before
	public void setup() {
		defaultNextHopPacketMapper = new DefaultNextHopPacketMapper(connectorSettingsSupplier,
				externalRoutingService, addressUtils, javaMoneyUtils, accountSettingsLoadingCache);
	}

	@Test
	public void getNextHopPacket() {
		logger.error("TODO: Fixme!");
	}

	@Test(expected = NullPointerException.class)
	public void determineNextAmountRequireNonNull() {
		defaultNextHopPacketMapper.determineNextAmount(null,
				null, null);
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
	public void determineCurrencyUnit() {
		logger.error("TODO: Fixme!");
	}

	@Test(expected = NullPointerException.class)
	public void determineDestinationExpiresAtSourceExpiryRequired() {
		final InterledgerAddress destinationAddress = InterledgerAddress.of("test.foo");
		defaultNextHopPacketMapper.determineDestinationExpiresAt(null, destinationAddress);
	}

	@Test
	public void determineDestinationExpiresAtExternalForwardingNotAllowed() {
		final Instant sourceExpiry = Instant.now();
		final InterledgerAddress destinationAddress = InterledgerAddress.of("test.foo");
		when(addressUtils.isExternalForwardingAllowed(destinationAddress)).thenReturn(false);

		final Instant expiresAt = defaultNextHopPacketMapper.determineDestinationExpiresAt(sourceExpiry,
									destinationAddress);

		assertThat(expiresAt).isEqualTo(sourceExpiry);
	}

	@Test(expected = InterledgerProtocolException.class)
	public void determineDestinationExpiresAtExternalForwardingAllowedTransferExpired() {
		final Instant sourceExpiry = Instant.now();
		ConnectorSettings connectorSettings = mock(ConnectorSettings.class);
		final InterledgerAddress destinationAddress = InterledgerAddress.of("test.foo");
		when(addressUtils.isExternalForwardingAllowed(destinationAddress)).thenReturn(true);
		when(connectorSettings.operatorAddressSafe()).thenReturn(InterledgerAddress.of("test.bar"));
		when(connectorSettingsSupplier.get()).thenReturn(connectorSettings);

		defaultNextHopPacketMapper.determineDestinationExpiresAt(sourceExpiry, destinationAddress);
	}

	@Test(expected = InterledgerProtocolException.class)
	public void determineDestinationExpiresAtExternalForwardingAllowedExpiryTooSoon() {
		final Instant sourceExpiry = Instant.now().plus(2000, MILLIS);
		ConnectorSettings connectorSettings = mock(ConnectorSettings.class);
		final InterledgerAddress destinationAddress = InterledgerAddress.of("test.foo");
		when(addressUtils.isExternalForwardingAllowed(destinationAddress)).thenReturn(true);
		when(connectorSettings.operatorAddressSafe()).thenReturn(InterledgerAddress.of("test.bar"));
		when(connectorSettingsSupplier.get()).thenReturn(connectorSettings);

		defaultNextHopPacketMapper.determineDestinationExpiresAt(sourceExpiry, destinationAddress);
	}

	@Test
	public void determineDestinationExpiresAtExternalForwardingAllowed() {
		final Instant sourceExpiry = Instant.now().plus(20000, MILLIS);
		final Instant destinationExpiry = Instant.now().plus(1000, MILLIS);
		final InterledgerAddress destinationAddress = InterledgerAddress.of("test.foo");
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
				.destination(InterledgerAddress.of("test.foo"))
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
