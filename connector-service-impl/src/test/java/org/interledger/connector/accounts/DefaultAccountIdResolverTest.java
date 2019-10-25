package org.interledger.connector.accounts;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.interledger.btp.BtpSession;
import org.interledger.btp.BtpSessionCredentials;
import org.interledger.link.Link;
import org.interledger.link.LinkId;
import org.interledger.link.StatefulLink;
import org.interledger.link.exceptions.LinkNotConnectedException;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.security.core.Authentication;

import java.util.Optional;

/**
 * Unit tests for {@link DefaultAccountIdResolver}.
 */
public class DefaultAccountIdResolverTest {

  private static final LinkId LINK_ID = LinkId.of("the-link");

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  Link linkMock;

  @Mock
  StatefulLink statefulLinkMock;

  private DefaultAccountIdResolver accountIdResolver;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    when(linkMock.getLinkId()).thenReturn(LINK_ID);
    when(statefulLinkMock.getLinkId()).thenReturn(LINK_ID);
    this.accountIdResolver = new DefaultAccountIdResolver();
  }

  @Test
  public void resolveAccountIdWithNullLink() {
    expectedException.expect(NullPointerException.class);
    Link link = null;
    accountIdResolver.resolveAccountId(link);
  }

  @Test
  public void resolveAccountIdWithLink() {
    //  Link link = new LoopbackLink(addressSupplier, linkSettings, packetRejector);
    // link.setLinkId(LINK_ID);
    assertThat(accountIdResolver.resolveAccountId(linkMock)).isEqualTo(AccountId.of(LINK_ID.value()));
  }

  @Test
  public void resolveAccountIdWithStatefulLinkNotConnected() {
    when(statefulLinkMock.isConnected()).thenReturn(false);
    expectedException.expect(LinkNotConnectedException.class);
    accountIdResolver.resolveAccountId(statefulLinkMock);
  }

  @Test
  public void resolveAccountIdWithStatefulLinkConnected() {
    when(statefulLinkMock.isConnected()).thenReturn(true);
    accountIdResolver.resolveAccountId(statefulLinkMock);
    assertThat(accountIdResolver.resolveAccountId(statefulLinkMock)).isEqualTo(AccountId.of(LINK_ID.value()));
  }

  @Test
  public void testResolveAccountWithNullAuthentication() {
    expectedException.expect(NullPointerException.class);
    Authentication authentication = null;
    accountIdResolver.resolveAccountId(authentication);
  }

  @Test
  public void testResolveAccountWithAuthentication() {
    Authentication authenticationMock = mock(Authentication.class);
    when(authenticationMock.getPrincipal()).thenReturn("foo");
    assertThat(accountIdResolver.resolveAccountId(authenticationMock)).isEqualTo(AccountId.of("foo"));
  }

  @Test
  public void testResolveAccountWithNullBtpSession() {
    expectedException.expect(NullPointerException.class);
    BtpSession btpSession = null;
    accountIdResolver.resolveAccountId(btpSession);
  }

  @Test
  public void testResolveAccountWithBtpSessionWithNoCreds() {
    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage("No BtpSessionCredentials found!");
    BtpSession btpSessionMock = mock(BtpSession.class);
    Optional<BtpSessionCredentials> btpSessionCredentials = Optional.empty();
    when(btpSessionMock.getBtpSessionCredentials()).thenReturn(btpSessionCredentials);
    assertThat(accountIdResolver.resolveAccountId(btpSessionMock));
  }

  @Test
  public void testResolveAccountWithBtpSessionWithNoAuthUsername() {
    expectedException.expect(RuntimeException.class);
    expectedException.expectMessage("Not yet implemented");
    BtpSession btpSessionMock = mock(BtpSession.class);
    Optional<BtpSessionCredentials> btpSessionCredentials = Optional.of(BtpSessionCredentials.builder()
      .authToken("authToken")
      .build());
    when(btpSessionMock.getBtpSessionCredentials()).thenReturn(btpSessionCredentials);
    assertThat(accountIdResolver.resolveAccountId(btpSessionMock)).isEqualTo(AccountId.of("foo"));
  }

  @Test
  public void testResolveAccountWithBtpSession() {
    BtpSession btpSessionMock = mock(BtpSession.class);
    Optional<BtpSessionCredentials> btpSessionCredentials = Optional.of(BtpSessionCredentials.builder()
      .authToken("authToken")
      .authUsername("authUsername")
      .build());
    when(btpSessionMock.getBtpSessionCredentials()).thenReturn(btpSessionCredentials);
    assertThat(accountIdResolver.resolveAccountId(btpSessionMock)).isEqualTo(AccountId.of("authUsername"));
  }

}
