package org.interledger.connector.routing;

import org.interledger.connector.routing.ChildAccountPaymentRouter;
import org.interledger.connector.routing.Route;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerAddress;
import org.interledger.connector.persistence.entities.AccountSettingsEntity;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

import static org.interledger.connector.routing.PaymentRouter.PING_ACCOUNT_ID;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ChildAccountPaymentRouter}.
 */
public class ChildAccountPaymentRouterTest {

  private static final InterledgerAddress OPERATOR_ADDRESS = InterledgerAddress.of("test.foo");

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  ConnectorSettings connectorSettingsMock;

  @Mock
  AccountSettingsRepository accountSettingsRepositoryMock;

  private ChildAccountPaymentRouter childAccountPaymentRouter;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(connectorSettingsMock.getEnabledProtocols().isPingProtocolEnabled()).thenReturn(true);
    when(connectorSettingsMock.getOperatorAddressSafe()).thenReturn(OPERATOR_ADDRESS);
    when(connectorSettingsMock.getGlobalRoutingSettings().getRoutingSecret()).thenReturn(
      "enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADKZPmASojt1iayb2bPy4D-Toq7TGLTN95HzCQAeJtz0="
    );

    this.childAccountPaymentRouter = new ChildAccountPaymentRouter(() -> connectorSettingsMock,
      accountSettingsRepositoryMock, (keyMetadata, encryptionAlgorithm, cipherMessage) -> new byte[32]);
  }

  /////////////////
  // parseChildAccountId
  /////////////////

  @Test(expected = NullPointerException.class)
  public void parseChildAccountIdWithNullAddress() {
    try {
      childAccountPaymentRouter.parseChildAccountId(null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("address must not be null!"));
      throw e;
    }
  }

  @Test
  public void parseChildAccountIdWithNoChildAccount() {
    assertThat(childAccountPaymentRouter.parseChildAccountId(OPERATOR_ADDRESS),
      is(AccountId.of("foo")));
  }

  @Test
  public void parseChildAccountId() {
    assertThat(childAccountPaymentRouter.parseChildAccountId(InterledgerAddress.of("test.foo.child123")),
      is(AccountId.of("child123")));
  }

  @Test
  public void parseChildAccountIdFromAddressWithInteraction() {
    assertThat(childAccountPaymentRouter.parseChildAccountId(InterledgerAddress.of("g.foo.bar~foo.bar")),
      is(AccountId.of("bar")));
    assertThat(childAccountPaymentRouter.parseChildAccountId(InterledgerAddress.of("test.foo.bar~foo.bar")),
      is(AccountId.of("bar")));
    assertThat(childAccountPaymentRouter.parseChildAccountId(InterledgerAddress.of("private.foo.bar~foo.bar")),
      is(AccountId.of("bar")));
  }

  /////////////////
  // isChildAccount
  /////////////////

  @Test(expected = NullPointerException.class)
  public void isChildAccountWithNullAddress() {
    try {
      childAccountPaymentRouter.isChildAccount(null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("interledgerAddress must not be null!"));
      throw e;
    }
  }

  @Test
  public void isChildAccount() {
    when(connectorSettingsMock.getOperatorAddressSafe()).thenReturn(OPERATOR_ADDRESS);

    assertThat(childAccountPaymentRouter.isChildAccount(OPERATOR_ADDRESS), is(true));
    assertThat(childAccountPaymentRouter.isChildAccount(InterledgerAddress.of("test.foo.bar")), is(true));
    assertThat(childAccountPaymentRouter.isChildAccount(InterledgerAddress.of("test.foo.bar~foo.bar")), is(true));

    assertThat(childAccountPaymentRouter.isChildAccount(InterledgerAddress.of("g.foo.bar")), is(false));
    assertThat(childAccountPaymentRouter.isChildAccount(InterledgerAddress.of("g.foo.bar~foo.bar")), is(false));
    assertThat(childAccountPaymentRouter.isChildAccount(InterledgerAddress.of("private.foo.bar~foo.bar")), is(false));
  }

  /////////////////
  // findBestNexHop (uni_ping)
  /////////////////

  @Test(expected = NullPointerException.class)
  public void findBestNexHopWithNullAddress() {
    try {
      childAccountPaymentRouter.findBestNexHop(null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage(), is("finalDestinationAddress must not be null!"));
      throw e;
    }
  }

  /**
   * The assumption is that no child-route will exist for the Operator address itself.
   */
  @Test
  public void findBestNexHopForUniPingWhenPingDisabled() {
    when(connectorSettingsMock.getEnabledProtocols().isPingProtocolEnabled()).thenReturn(false);
    Optional<Route> actual = childAccountPaymentRouter.findBestNexHop(OPERATOR_ADDRESS);
    assertThat(actual.isPresent(), is(false));
  }

  @Test
  public void findBestNexHopForUniPingWhenPingEnabled() {
    Optional<Route> actual = childAccountPaymentRouter.findBestNexHop(OPERATOR_ADDRESS);
    assertThat(actual.isPresent(), is(true));

    assertThat(actual.get().getNextHopAccountId(), is(PING_ACCOUNT_ID));
    assertThat(actual.get().getExpiresAt().isPresent(), is(false));
    assertThat(actual.get().getAuth().length, is(32));
    assertThat(actual.get().getPath().isEmpty(), is(true));
  }

  /////////////////
  // findBestNexHop (child)
  /////////////////

  @Test
  public void findBestNexHopForNonExistentChildAccount() {
    Optional<Route> actual = childAccountPaymentRouter.findBestNexHop(OPERATOR_ADDRESS.with("foo"));
    assertThat(actual.isPresent(), is(false));
  }

  @Test
  public void findBestNexHopForExistingChildAccount() {
    AccountSettingsEntity accountSettingsEntityMock = mock(AccountSettingsEntity.class);
    when(accountSettingsRepositoryMock.findByAccountId(AccountId.of("foo")))
      .thenReturn(Optional.of(accountSettingsEntityMock));

    Optional<Route> actual = childAccountPaymentRouter.findBestNexHop(OPERATOR_ADDRESS.with("foo"));
    assertThat(actual.isPresent(), is(true));

    assertThat(actual.get().getNextHopAccountId(), is(AccountId.of("foo")));
    assertThat(actual.get().getExpiresAt().isPresent(), is(false));
    assertThat(actual.get().getAuth().length, is(32));
    assertThat(actual.get().getPath().isEmpty(), is(true));
  }

}
