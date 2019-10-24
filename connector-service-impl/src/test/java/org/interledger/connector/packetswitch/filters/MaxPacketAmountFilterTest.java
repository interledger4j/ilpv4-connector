package org.interledger.connector.packetswitch.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.packetswitch.PacketRejector;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerCondition;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;

import com.google.common.primitives.UnsignedLong;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;

public class MaxPacketAmountFilterTest {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private PacketSwitchFilterChain filterChain;

  private Supplier<Optional<InterledgerAddress>> addressSupplier = () -> Optional.of(InterledgerAddress.of("example.source"));

  @Test
  public void filterHatesNullSettings() {
    MaxPacketAmountFilter filter = createFilter();
    expectedException.expect(NullPointerException.class);
    filter.doFilter(null, createPrepareWithAmount(10), filterChain);
  }

  @Test
  public void filterHatesNullPrepare() {
    MaxPacketAmountFilter filter = createFilter();
    expectedException.expect(NullPointerException.class);
    filter.doFilter(createAccountSettingsWithMaxAmount(100), null, filterChain);
  }

  @Test
  public void filterHatesNullChain() {
    MaxPacketAmountFilter filter = createFilter();
    expectedException.expect(NullPointerException.class);
    filter.doFilter(createAccountSettingsWithMaxAmount(100), createPrepareWithAmount(10), null);
  }

  @Test
  public void passAlongWhenBelowMax() {
    MaxPacketAmountFilter filter = createFilter();
    AccountSettings settings = createAccountSettingsWithMaxAmount(1000);
    InterledgerPreparePacket prepare = createPrepareWithAmount(999);
    filter.doFilter(settings, prepare, filterChain);
    verify(filterChain, times(1)).doFilter(settings, prepare);
  }

  @Test
  public void rejectWhenBeyondMax() {
    MaxPacketAmountFilter filter = createFilter();
    AccountSettings settings = createAccountSettingsWithMaxAmount(1000);
    InterledgerPreparePacket prepare = createPrepareWithAmount(1001);
    InterledgerResponsePacket response = filter.doFilter(settings, prepare, filterChain);
    assertThat(response).isInstanceOf(InterledgerRejectPacket.class)
        .extracting("code", "message")
        .containsExactly(InterledgerErrorCode.F08_AMOUNT_TOO_LARGE,
            "Packet size too large: maxAmount=1000 actualAmount=1001");
    verify(filterChain, times(0)).doFilter(settings, prepare);
  }

  private MaxPacketAmountFilter createFilter() {
    return new MaxPacketAmountFilter(new PacketRejector(addressSupplier));
  }

  private AccountSettings createAccountSettingsWithMaxAmount(long max) {
    AccountSettings settings = mock(AccountSettings.class);
    when(settings.maximumPacketAmount()).thenReturn(Optional.of(max));
    when(settings.accountId()).thenReturn(AccountId.of(UUID.randomUUID().toString()));
    return settings;
  }

  private InterledgerPreparePacket createPrepareWithAmount(long amount) {
    return InterledgerPreparePacket.builder()
        .executionCondition(InterledgerCondition.of(new byte[32]))
        .amount(UnsignedLong.valueOf(amount))
        .expiresAt(Instant.now())
        .destination(InterledgerAddress.of("example.destination"))
        .build();


  }
}
