package org.interledger.connector.accounts.sub;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountNotFoundProblem;
import org.interledger.connector.accounts.InvalidAccountIdProblem;
import org.interledger.core.InterledgerAddress;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.MockitoAnnotations;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

/**
 * Unit tests for {@link LocalDestinationAddressUtils}.
 */
public class LocalDestinationAddressUtilsTest {

  public static final String EXAMPLE_CONNECTOR = "example.connector";
  public static final String BOB = "bob";
  public static final String XYZ123 = "xyz123";
  public static final String TILDE = "~";
  public static final String FOO = "foo";
  public static final String BAR = "bar";

  private static final InterledgerAddress CONNECTOR_OPERATOR_ADDRESS = InterledgerAddress.of(EXAMPLE_CONNECTOR);

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private AtomicBoolean spspEnabled;
  private LocalDestinationAddressUtils localDestinationAddressUtils;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    this.spspEnabled = new AtomicBoolean(true);
    this.localDestinationAddressUtils = new LocalDestinationAddressUtils() {
      @Override
      public Supplier<InterledgerAddress> getConnectorOperatorAddress() {
        return () -> CONNECTOR_OPERATOR_ADDRESS;
      }

      @Override
      public boolean isLocalSpspFulfillmentEnabled() {
        return spspEnabled.get();
      }
    };
  }

  @Test
  public void getConnectorOperatorAddress() {
    assertThat(localDestinationAddressUtils.getConnectorOperatorAddress().get()).isEqualTo(CONNECTOR_OPERATOR_ADDRESS);
  }

  @Test
  public void isLocalSpspFulfillmentEnabled() {
    spspEnabled.set(false);
    assertThat(localDestinationAddressUtils.isLocalSpspFulfillmentEnabled()).isFalse();

    spspEnabled.set(true);
    assertThat(localDestinationAddressUtils.isLocalSpspFulfillmentEnabled()).isTrue();
  }

  @Test
  public void getPingAccountinterledgerAddress() {
    assertThat(localDestinationAddressUtils.getPingAccountinterledgerAddress()).isEqualTo(CONNECTOR_OPERATOR_ADDRESS);
  }

  @Test
  public void getSpspAddressPrefixSegment() {
    assertThat(localDestinationAddressUtils.getSpspAddressPrefixSegment()).isEqualTo("spsp");
  }

  @Test
  public void getLocalAccountsAddressPrefixSegment() {
    assertThat(localDestinationAddressUtils.getLocalAccountsAddressPrefixSegment()).isEqualTo("accounts");
  }

  @Test
  public void isLocalDestinationAddressWithNull() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("interledgerAddress must not be null");
    localDestinationAddressUtils.isLocalDestinationAddress(null);
  }

  @Test
  public void isLocalDestinationAddress() {
    assertThat(localDestinationAddressUtils.isLocalDestinationAddress(CONNECTOR_OPERATOR_ADDRESS)).isTrue();
    assertThat(localDestinationAddressUtils.isLocalDestinationAddress(
      CONNECTOR_OPERATOR_ADDRESS.with(localDestinationAddressUtils.getSpspAddressPrefixSegment()).with("alice")))
      .isTrue();
    assertThat(localDestinationAddressUtils.isLocalDestinationAddress(
      CONNECTOR_OPERATOR_ADDRESS.with(localDestinationAddressUtils.getSpspAddressPrefixSegment()).with(BOB)))
      .isTrue();
    assertThat(localDestinationAddressUtils.isLocalDestinationAddress(
      CONNECTOR_OPERATOR_ADDRESS.with(localDestinationAddressUtils.getLocalAccountsAddressPrefixSegment())
        .with("alice"))).isTrue();
    assertThat(localDestinationAddressUtils.isLocalDestinationAddress(
      CONNECTOR_OPERATOR_ADDRESS.with(localDestinationAddressUtils.getLocalAccountsAddressPrefixSegment()).with(BOB)))
      .isTrue();
    assertThat(localDestinationAddressUtils.isLocalDestinationAddress(CONNECTOR_OPERATOR_ADDRESS.with("alice")))
      .isTrue();
    assertThat(localDestinationAddressUtils.isLocalDestinationAddress(InterledgerAddress.of("g.foo.bar")))
      .isFalse();
  }

  @Test
  public void isAddressForConnectorPingAccountWithNull() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("interledgerAddress must not be null");
    localDestinationAddressUtils.isAddressForConnectorPingAccount(null);
  }

  @Test
  public void isAddressForConnectorPingAccount() {
    assertThat(localDestinationAddressUtils.isAddressForConnectorPingAccount(CONNECTOR_OPERATOR_ADDRESS)).isTrue();
    assertThat(localDestinationAddressUtils.isAddressForConnectorPingAccount(
      CONNECTOR_OPERATOR_ADDRESS.with(localDestinationAddressUtils.getSpspAddressPrefixSegment()).with("alice")))
      .isFalse();
    assertThat(localDestinationAddressUtils.isAddressForConnectorPingAccount(
      CONNECTOR_OPERATOR_ADDRESS.with(localDestinationAddressUtils.getSpspAddressPrefixSegment()).with(BOB)))
      .isFalse();
    assertThat(localDestinationAddressUtils.isAddressForConnectorPingAccount(
      CONNECTOR_OPERATOR_ADDRESS.with(localDestinationAddressUtils.getLocalAccountsAddressPrefixSegment())
        .with("alice"))).isFalse();
    assertThat(localDestinationAddressUtils.isAddressForConnectorPingAccount(
      CONNECTOR_OPERATOR_ADDRESS.with(localDestinationAddressUtils.getLocalAccountsAddressPrefixSegment()).with(BOB)))
      .isFalse();
    assertThat(localDestinationAddressUtils.isAddressForConnectorPingAccount(CONNECTOR_OPERATOR_ADDRESS.with("alice")))
      .isFalse();
    assertThat(localDestinationAddressUtils.isAddressForConnectorPingAccount(InterledgerAddress.of("g.foo.bar")))
      .isFalse();
  }

  @Test
  public void isLocalSpspDestinationAddressWithNull() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("interledgerAddress must not be null");
    localDestinationAddressUtils.isLocalSpspDestinationAddress(null);
  }

  @Test
  public void isLocalSpspDestinationAddressWhenEnabled() {
    spspEnabled.set(true);
    assertThat(localDestinationAddressUtils.isLocalSpspDestinationAddress(CONNECTOR_OPERATOR_ADDRESS)).isFalse();
    assertThat(localDestinationAddressUtils.isLocalSpspDestinationAddress(
      CONNECTOR_OPERATOR_ADDRESS.with(localDestinationAddressUtils.getSpspAddressPrefixSegment()).with("alice")))
      .isTrue();
    assertThat(localDestinationAddressUtils.isLocalSpspDestinationAddress(
      CONNECTOR_OPERATOR_ADDRESS.with(localDestinationAddressUtils.getSpspAddressPrefixSegment()).with(BOB))).isTrue();
    assertThat(localDestinationAddressUtils.isLocalSpspDestinationAddress(
      CONNECTOR_OPERATOR_ADDRESS.with(localDestinationAddressUtils.getLocalAccountsAddressPrefixSegment())
        .with("alice"))).isFalse();
    assertThat(localDestinationAddressUtils.isLocalSpspDestinationAddress(
      CONNECTOR_OPERATOR_ADDRESS.with(localDestinationAddressUtils.getLocalAccountsAddressPrefixSegment()).with(BOB)))
      .isFalse();
    assertThat(localDestinationAddressUtils.isLocalSpspDestinationAddress(CONNECTOR_OPERATOR_ADDRESS.with("alice")))
      .isFalse();
    assertThat(localDestinationAddressUtils.isLocalSpspDestinationAddress(InterledgerAddress.of("g.foo.bar")))
      .isFalse();
  }

  @Test
  public void isLocalSpspDestinationAddressWhenDisabled() {
    spspEnabled.set(false);
    assertThat(localDestinationAddressUtils.isLocalSpspDestinationAddress(CONNECTOR_OPERATOR_ADDRESS)).isFalse();
    assertThat(localDestinationAddressUtils.isLocalSpspDestinationAddress(
      CONNECTOR_OPERATOR_ADDRESS.with(localDestinationAddressUtils.getSpspAddressPrefixSegment()).with("alice")))
      .isFalse();
    assertThat(localDestinationAddressUtils.isLocalSpspDestinationAddress(
      CONNECTOR_OPERATOR_ADDRESS.with(localDestinationAddressUtils.getSpspAddressPrefixSegment()).with(BOB))).isFalse();
    assertThat(localDestinationAddressUtils.isLocalSpspDestinationAddress(
      CONNECTOR_OPERATOR_ADDRESS.with(localDestinationAddressUtils.getLocalAccountsAddressPrefixSegment())
        .with("alice"))).isFalse();
    assertThat(localDestinationAddressUtils.isLocalSpspDestinationAddress(
      CONNECTOR_OPERATOR_ADDRESS.with(localDestinationAddressUtils.getLocalAccountsAddressPrefixSegment()).with(BOB)))
      .isFalse();
    assertThat(localDestinationAddressUtils.isLocalSpspDestinationAddress(CONNECTOR_OPERATOR_ADDRESS.with("alice")))
      .isFalse();
    assertThat(localDestinationAddressUtils.isLocalSpspDestinationAddress(InterledgerAddress.of("g.foo.bar")))
      .isFalse();
  }

  @Test
  public void isLocalAccountDestinationAddressWithNull() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("interledgerAddress must not be null");
    localDestinationAddressUtils.isLocalAccountDestinationAddress(null);
  }

  @Test
  public void isLocalAccountDestinationAddress() {
    assertThat(localDestinationAddressUtils.isLocalAccountDestinationAddress(CONNECTOR_OPERATOR_ADDRESS)).isFalse();
    assertThat(localDestinationAddressUtils.isLocalAccountDestinationAddress(
      CONNECTOR_OPERATOR_ADDRESS.with(localDestinationAddressUtils.getSpspAddressPrefixSegment()).with("alice")))
      .isFalse();
    assertThat(localDestinationAddressUtils.isLocalAccountDestinationAddress(
      CONNECTOR_OPERATOR_ADDRESS.with(localDestinationAddressUtils.getSpspAddressPrefixSegment()).with(BOB)))
      .isFalse();
    assertThat(localDestinationAddressUtils.isLocalAccountDestinationAddress(
      CONNECTOR_OPERATOR_ADDRESS.with(localDestinationAddressUtils.getLocalAccountsAddressPrefixSegment())
        .with("alice"))).isTrue();
    assertThat(localDestinationAddressUtils.isLocalAccountDestinationAddress(
      CONNECTOR_OPERATOR_ADDRESS.with(localDestinationAddressUtils.getLocalAccountsAddressPrefixSegment()).with(BOB)))
      .isTrue();
    assertThat(localDestinationAddressUtils.isLocalAccountDestinationAddress(CONNECTOR_OPERATOR_ADDRESS.with("alice")))
      .isFalse();
    assertThat(localDestinationAddressUtils.isLocalAccountDestinationAddress(InterledgerAddress.of("g.foo.bar")))
      .isFalse();
  }

  @Test
  public void getConnectorPingAccountId() {
    assertThat(localDestinationAddressUtils.getConnectorPingAccountId())
      .isEqualTo(LocalDestinationAddressUtils.PING_ACCOUNT_ID);
    assertThat(localDestinationAddressUtils.getConnectorPingAccountId().value()).isEqualTo("__ping_account__");
  }

  @Test
  public void isConnectorPingAccountIdWithNull() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("accountId must not be null");
    localDestinationAddressUtils.isConnectorPingAccountId(null);
  }

  @Test
  public void isConnectorPingAccountId() {
    assertThat(localDestinationAddressUtils.isConnectorPingAccountId(AccountId.of("foo"))).isFalse();
    assertThat(localDestinationAddressUtils.isConnectorPingAccountId(AccountId.of("__ping_account__"))).isTrue();
    assertThat(localDestinationAddressUtils.isConnectorPingAccountId(LocalDestinationAddressUtils.PING_ACCOUNT_ID))
      .isTrue();
  }

  @Test
  public void parseSpspAccountIdWithNull() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("interledgerAddress must not be null");
    localDestinationAddressUtils.parseSpspAccountId(null);
  }

  @Test
  public void parseSpspAccountIdWithShortAddress() {
    expectedException.expect(InvalidAccountIdProblem.class);
    expectedException.expectMessage("Invalid AccountId: No SPSP accountId parsed from Interledger Address. "
      + "interledgerAddress=InterledgerAddress{value=example.foo}"
    );
    localDestinationAddressUtils.parseSpspAccountId(InterledgerAddress.of("example.foo"));
  }

  @Test
  public void parseSpspAccountIdWithLongAddress() {
    expectedException.expect(InvalidAccountIdProblem.class);
    expectedException.expectMessage("Invalid AccountId: No SPSP accountId parsed from Interledger Address. "
      + "interledgerAddress=InterledgerAddress{value=example.foo.bar.baz.boo.boo}"
    );
    localDestinationAddressUtils.parseSpspAccountId(InterledgerAddress.of("example.foo.bar.baz.boo.boo"));
  }

  @Test
  public void parseSpspAccountId() {
    //g.connector.spsp.bob.123xyz --> bob
    //g.connector.spsp.bob~foo.bar --> bob~foo
    //g.connector.spsp.bob.~foo --> bob
    //g.connector.spsp.~bob.bar --> bob

    assertThat(localDestinationAddressUtils.parseSpspAccountId(InterledgerAddress.of(EXAMPLE_CONNECTOR)
      .with(localDestinationAddressUtils.getSpspAddressPrefixSegment())
      .with(BOB)
      .with(XYZ123)
    )).isEqualTo(AccountId.of(BOB));

    assertThat(localDestinationAddressUtils.parseSpspAccountId(InterledgerAddress.of(EXAMPLE_CONNECTOR)
      .with(localDestinationAddressUtils.getSpspAddressPrefixSegment())
      .with(TILDE + BOB)
      .with(XYZ123)
    )).isEqualTo(AccountId.of(TILDE + BOB));

    assertThat(localDestinationAddressUtils.parseSpspAccountId(InterledgerAddress.of(EXAMPLE_CONNECTOR)
      .with(localDestinationAddressUtils.getSpspAddressPrefixSegment())
      .with(BOB + TILDE + FOO)
      .with(XYZ123)
    )).isEqualTo(AccountId.of(BOB + TILDE + FOO));

    assertThat(localDestinationAddressUtils.parseSpspAccountId(InterledgerAddress.of(EXAMPLE_CONNECTOR)
      .with(localDestinationAddressUtils.getSpspAddressPrefixSegment())
      .with(BOB + TILDE)
      .with(XYZ123)
    )).isEqualTo(AccountId.of(BOB + TILDE));

    assertThat(localDestinationAddressUtils.parseSpspAccountId(InterledgerAddress.of(EXAMPLE_CONNECTOR)
      .with(localDestinationAddressUtils.getSpspAddressPrefixSegment())
      .with(BOB)
      .with(TILDE + XYZ123)
    )).isEqualTo(AccountId.of(BOB));
  }

  @Test
  public void parseLocalAccountIdWithNull() {
    expectedException.expect(NullPointerException.class);
    expectedException.expectMessage("interledgerAddress must not be null");
    localDestinationAddressUtils.parseLocalAccountId(null);
  }

  @Test
  public void parseLocalAccountIdWithShortAddress() {
    expectedException.expect(InvalidAccountIdProblem.class);
    expectedException.expectMessage("Invalid AccountId: No SPSP accountId parsed from Interledger Address. "
      + "interledgerAddress=InterledgerAddress{value=example.foo}"
    );
    localDestinationAddressUtils.parseLocalAccountId(InterledgerAddress.of("example.foo"));
  }

  @Test
  public void parseLocalAccountIdWithLongAddress() {
    expectedException.expect(InvalidAccountIdProblem.class);
    expectedException.expectMessage("Invalid AccountId: No SPSP accountId parsed from Interledger Address. "
      + "interledgerAddress=InterledgerAddress{value=example.foo.bar.baz.boo.boo}"
    );
    localDestinationAddressUtils.parseLocalAccountId(InterledgerAddress.of("example.foo.bar.baz.boo.boo"));
  }

  @Test
  public void parseLocalAccountId() {
    //g.connector.accounts.bob.123xyz --> bob
    //g.connector.accounts.bob~foo.bar --> bob~foo
    //g.connector.accounts.bob.~foo --> bob
    //g.connector.accounts.~bob.bar --> bob

    assertThat(localDestinationAddressUtils.parseLocalAccountId(
      InterledgerAddress.of(EXAMPLE_CONNECTOR)
      .with(localDestinationAddressUtils.getLocalAccountsAddressPrefixSegment())
      .with(BOB)
      .with(XYZ123)
    )).isEqualTo(AccountId.of(BOB));

    assertThat(localDestinationAddressUtils.parseLocalAccountId(
      InterledgerAddress.of(EXAMPLE_CONNECTOR)
      .with(localDestinationAddressUtils.getLocalAccountsAddressPrefixSegment())
      .with(TILDE + BOB)
      .with(XYZ123)
    )).isEqualTo(AccountId.of(TILDE + BOB));

    assertThat(localDestinationAddressUtils.parseLocalAccountId(
      InterledgerAddress.of(EXAMPLE_CONNECTOR)
      .with(localDestinationAddressUtils.getLocalAccountsAddressPrefixSegment())
      .with(BOB + TILDE + FOO)
      .with(XYZ123)
    )).isEqualTo(AccountId.of(BOB + TILDE + FOO));

    assertThat(localDestinationAddressUtils.parseLocalAccountId(
      InterledgerAddress.of(EXAMPLE_CONNECTOR)
      .with(localDestinationAddressUtils.getLocalAccountsAddressPrefixSegment())
      .with(BOB + TILDE)
      .with(XYZ123)
    )).isEqualTo(AccountId.of(BOB + TILDE));

    assertThat(localDestinationAddressUtils.parseLocalAccountId(
      InterledgerAddress.of(EXAMPLE_CONNECTOR)
      .with(localDestinationAddressUtils.getLocalAccountsAddressPrefixSegment())
      .with(BOB)
      .with(TILDE + XYZ123)
    )).isEqualTo(AccountId.of(BOB));
  }
}
