package org.interledger.connector.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.connector.accounts.sub.SubAccountUtils.PING_ACCOUNT_ID;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.sub.SpspSubAccountUtils;
import org.interledger.connector.persistence.entities.AccountSettingsEntity;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.core.InterledgerAddress;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Optional;

/**
 * Unit tests for {@link SpspSubAccountPaymentRouter}.
 */
public class SpspSubAccountPaymentRouterTest {

  private static final InterledgerAddress OPERATOR_ADDRESS = InterledgerAddress.of("test.foo");

  @Mock(answer = Answers.RETURNS_DEEP_STUBS)
  ConnectorSettings connectorSettingsMock;

  @Mock
  SpspSubAccountUtils spspSubAccountUtilsMock;

  private SpspSubAccountPaymentRouter spspSubAccountPaymentRouter;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(connectorSettingsMock.enabledProtocols().isPingProtocolEnabled()).thenReturn(true);
    when(connectorSettingsMock.operatorAddress()).thenReturn(OPERATOR_ADDRESS);
    when(connectorSettingsMock.globalRoutingSettings().routingSecret()).thenReturn(
      "enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADKZPmASojt1iayb2bPy4D-Toq7TGLTN95HzCQAeJtz0="
    );

    this.spspSubAccountPaymentRouter = new SpspSubAccountPaymentRouter(() -> connectorSettingsMock,
      spspSubAccountUtilsMock, (keyMetadata, encryptionAlgorithm, cipherMessage) -> new byte[32]);
  }

  /////////////////
  // parseSubAccountId
  /////////////////

  // TODO: Move this to other test!

//  @Test(expected = NullPointerException.class)
//  public void parseSubAccountIdWithNullAddress() {
//    try {
//      spspSubAccountPaymentRouter.parseSubAccountId(null);
//    } catch (NullPointerException e) {
//      assertThat(e.getMessage()).isEqualTo("address must not be null!");
//      throw e;
//    }
//  }
//
////  @Test
////  public void parseSubAccountIdWithNoSubAccount() {
////    assertThat(spspSubAccountPaymentRouter.parseSubAccountId(OPERATOR_ADDRESS)).isEqualTo(AccountId.of("foo"));
////  }
////
////  @Test
////  public void parseSubAccountId() {
////    assertThat(spspSubAccountPaymentRouter.parseSubAccountId(InterledgerAddress.of("test.foo.sub123")))
////      .isEqualTo(AccountId.of("sub123"));
////  }
////
////  @Test
////  public void parseSubAccountIdFromAddressWithInteraction() {
////    assertThat(spspSubAccountPaymentRouter.parseSubAccountId(InterledgerAddress.of("g.foo.bar~foo.bar")))
////      .isEqualTo(AccountId.of("bar"));
////    assertThat(spspSubAccountPaymentRouter.parseSubAccountId(InterledgerAddress.of("test.foo.bar~foo.bar")))
////      .isEqualTo(AccountId.of("bar"));
////    assertThat(spspSubAccountPaymentRouter.parseSubAccountId(InterledgerAddress.of("private.foo.bar~foo.bar")))
////      .isEqualTo(AccountId.of("bar"));
////    assertThat(
////      spspSubAccountPaymentRouter
////        .parseSubAccountId(InterledgerAddress.of("test.foo.money.bob.zIsPWqpo2Vf4jJhQExXMZf3y")))
////      .isEqualTo(AccountId.of("bob"));
////  }

  /////////////////
  // isSubAccount
  /////////////////

//  @Test(expected = NullPointerException.class)
//  public void isSubAccountWithNullAddress() {
//    try {
//      spspSubAccountPaymentRouter.isSubAccount(null);
//    } catch (NullPointerException e) {
//      assertThat(e.getMessage()).isEqualTo("interledgerAddress must not be null!");
//      throw e;
//    }
//  }
//
//  @Test
//  public void isSubAccount() {
//    when(connectorSettingsMock.operatorAddress()).thenReturn(OPERATOR_ADDRESS);
//
//    assertThat(spspSubAccountPaymentRouter.isSubAccount(OPERATOR_ADDRESS)).isTrue();
//    assertThat(spspSubAccountPaymentRouter.isSubAccount(InterledgerAddress.of("test.foo.bar"))).isTrue();
//    assertThat(spspSubAccountPaymentRouter.isSubAccount(InterledgerAddress.of("test.foo.bar~foo.bar"))).isTrue();
//    assertThat(
//      spspSubAccountPaymentRouter.isSubAccount(InterledgerAddress.of("test.foo.money.bob.zIsPWqpo2Vf4jJhQExXMZf3y")))
//      .isTrue();
//
//    assertThat(spspSubAccountPaymentRouter.isSubAccount(InterledgerAddress.of("g.foo.bar"))).isFalse();
//    assertThat(spspSubAccountPaymentRouter.isSubAccount(InterledgerAddress.of("g.foo.bar~foo.bar"))).isFalse();
//    assertThat(spspSubAccountPaymentRouter.isSubAccount(InterledgerAddress.of("private.foo.bar~foo.bar"))).isFalse();
//  }

  /////////////////
  // findBestNexHop (uni_ping)
  /////////////////

  @Test(expected = NullPointerException.class)
  public void findBestNexHopWithNullAddress() {
    try {
      spspSubAccountPaymentRouter.findBestNexHop(null);
    } catch (NullPointerException e) {
      assertThat(e.getMessage()).isEqualTo("finalDestinationAddress must not be null!");
      throw e;
    }
  }

  /**
   * The assumption is that no sub-route will exist for the Operator address itself.
   */
  @Test
  public void findBestNexHopForUniPingWhenPingDisabled() {
    when(connectorSettingsMock.enabledProtocols().isPingProtocolEnabled()).thenReturn(false);
    Optional<Route> actual = spspSubAccountPaymentRouter.findBestNexHop(OPERATOR_ADDRESS);
    assertThat(actual.isPresent()).isFalse();
  }

  @Test
  public void findBestNexHopForUniPingWhenPingEnabled() {
    Optional<Route> actual = spspSubAccountPaymentRouter.findBestNexHop(OPERATOR_ADDRESS);
    assertThat(actual.isPresent()).isTrue();

    assertThat(actual.get().nextHopAccountId()).isEqualTo(PING_ACCOUNT_ID);
    assertThat(actual.get().expiresAt().isPresent()).isFalse();
    assertThat(actual.get().auth().length).isEqualTo(32);
    assertThat(actual.get().path().isEmpty()).isTrue();
  }

  /////////////////
  // findBestNexHop (sub)
  /////////////////

  @Test
  public void findBestNexHopForNonExistentSubAccount() {
    Optional<Route> actual = spspSubAccountPaymentRouter.findBestNexHop(OPERATOR_ADDRESS.with("foo"));
    assertThat(actual.isPresent()).isFalse();
  }

  @Test
  public void findBestNexHopForExistingSubAccount() {
    AccountSettingsEntity accountSettingsEntityMock = mock(AccountSettingsEntity.class);
//    when(accountSettingsRepositoryMock.findByAccountId(AccountId.of("foo")))
//      .thenReturn(Optional.of(accountSettingsEntityMock));

    Optional<Route> actual = spspSubAccountPaymentRouter.findBestNexHop(OPERATOR_ADDRESS.with("foo"));
    assertThat(actual.isPresent()).isTrue();

    assertThat(actual.get().nextHopAccountId()).isEqualTo(AccountId.of("foo"));
    assertThat(actual.get().expiresAt().isPresent()).isFalse();
    assertThat(actual.get().auth().length).isEqualTo(32);
    assertThat(actual.get().path().isEmpty()).isTrue();
  }

  @Test
  public void findBestNexHopForSpspAddress() {
    AccountSettingsEntity accountSettingsEntityMock = mock(AccountSettingsEntity.class);
//    when(accountSettingsRepositoryMock.findByAccountId(AccountId.of("bob")))
//      .thenReturn(Optional.of(accountSettingsEntityMock));

    Optional<Route> actual = spspSubAccountPaymentRouter.findBestNexHop(
      OPERATOR_ADDRESS.with("money").with("bob").with("zIsPWqpo2Vf4jJhQExXMZf3y")
    );
    assertThat(actual.isPresent()).isTrue();

    assertThat(actual.get().nextHopAccountId()).isEqualTo(AccountId.of("bob"));
    assertThat(actual.get().expiresAt().isPresent()).isFalse();
    assertThat(actual.get().auth().length).isEqualTo(32);
    assertThat(actual.get().path().isEmpty()).isTrue();
  }

}
