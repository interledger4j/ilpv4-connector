package org.interledger.connector.packetswitch;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.mockito.Mockito.when;

import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.core.InterledgerAddress;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

/**
 * Unit tests for {@link InterledgerAddressUtils}.
 */
public class InterledgerAddressUtilsTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private ConnectorSettings connectorSettingsMock;

  @Mock
  private AccountSettingsRepository accountSettingsRepositoryMock;

  private InterledgerAddressUtils interledgerAddressUtils;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    this.interledgerAddressUtils = new InterledgerAddressUtils(
      () -> connectorSettingsMock,
      accountSettingsRepositoryMock
    );
  }

  @Test
  public void testConstructorWithNullConnectorSettings() {
    expectedException.expect(NullPointerException.class);
    new InterledgerAddressUtils(
      null,
      accountSettingsRepositoryMock
    );
  }

  @Test
  public void testConstructorWithNullAccountSettingsRepository() {
    expectedException.expect(NullPointerException.class);
    new InterledgerAddressUtils(
      () -> connectorSettingsMock,
      null
    );
  }

  @Test
  public void isExternalForwardingAllowedWithNull() {
    expectedException.expect(NullPointerException.class);
    interledgerAddressUtils.isExternalForwardingAllowed(null);
  }

  @Test
  public void isExternalForwardingAllowed() {
    // Starts with Operator Address.
    when(connectorSettingsMock.operatorAddress()).thenReturn(InterledgerAddress.of("test.bar"));
    assertThat(interledgerAddressUtils.isExternalForwardingAllowed(InterledgerAddress.of("test.bar"))).isFalse();
    assertThat(interledgerAddressUtils.isExternalForwardingAllowed(InterledgerAddress.of("test.bar.bar"))).isFalse();
    // Doesn't start with operator address.
    assertThat(interledgerAddressUtils.isExternalForwardingAllowed(InterledgerAddress.of("test.b"))).isTrue();

    assertThat(interledgerAddressUtils.isExternalForwardingAllowed(InterledgerAddress.of("g.foo"))).isTrue();
    assertThat(interledgerAddressUtils.isExternalForwardingAllowed(InterledgerAddress.of("test.foo"))).isTrue();
    assertThat(interledgerAddressUtils.isExternalForwardingAllowed(InterledgerAddress.of("test1.foo"))).isTrue();
    assertThat(interledgerAddressUtils.isExternalForwardingAllowed(InterledgerAddress.of("test2.foo"))).isTrue();
    assertThat(interledgerAddressUtils.isExternalForwardingAllowed(InterledgerAddress.of("test3.foo"))).isTrue();

    assertThat(interledgerAddressUtils.isExternalForwardingAllowed(InterledgerAddress.of("example.foo"))).isFalse();
    assertThat(interledgerAddressUtils.isExternalForwardingAllowed(InterledgerAddress.of("private.foo"))).isFalse();
    assertThat(interledgerAddressUtils.isExternalForwardingAllowed(InterledgerAddress.of("peer.foo"))).isFalse();
    assertThat(interledgerAddressUtils.isExternalForwardingAllowed(InterledgerAddress.of("self.foo"))).isFalse();
    assertThat(interledgerAddressUtils.isExternalForwardingAllowed(InterledgerAddress.of("local.foo"))).isFalse();
  }

  @Test
  public void isDestinationAllowedFromAccount() {
  }
}
