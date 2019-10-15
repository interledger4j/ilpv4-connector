package org.interledger.connector.ping;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.interledger.connector.link.Link;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;

import com.google.common.primitives.UnsignedLong;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Instant;

/**
 * Unit tests for {@link PingInitiator}.
 */
public class PingInitiatorTest {

  private static final InterledgerAddress DESTINATION_ADDRESS = InterledgerAddress.of("example.foo");

  @Mock
  Link linkMock;

  PingInitiator pingInitiator;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(linkMock.sendPacket(any())).thenReturn(mock(InterledgerResponsePacket.class));
    this.pingInitiator = new DefaultPingInitiator(linkMock, () -> Instant.now().plusSeconds(60L));
  }

  @Test
  public void sendUnidirectionalPing() {
    InterledgerResponsePacket response = this.pingInitiator.ping(DESTINATION_ADDRESS, UnsignedLong.ONE);

    assertThat(response).isNotNull();
    verify(linkMock).sendPacket(any());
  }

  @Test
  public void constructPingPacket() {
    final Instant expectedExpiresAt = Instant.now();

    InterledgerPreparePacket actual = pingInitiator
        .constructPingPacket(DESTINATION_ADDRESS, UnsignedLong.ZERO, expectedExpiresAt);

    assertThat(actual.getDestination()).isEqualTo(DESTINATION_ADDRESS);
    assertThat(actual.getAmount()).isEqualTo(UnsignedLong.ZERO);
    assertThat(actual.getExpiresAt()).isEqualTo(expectedExpiresAt);
    assertThat(actual.getExecutionCondition()).isEqualTo(PingInitiator.PING_PROTOCOL_CONDITION);
  }
}
