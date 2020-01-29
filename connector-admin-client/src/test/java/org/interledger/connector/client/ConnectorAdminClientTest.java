package org.interledger.connector.client;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.link.LoopbackLink;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import feign.FeignException;
import okhttp3.HttpUrl;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Optional;

/**
 * Unit tests for {@link ConnectorAdminClient}.
 *
 * Note that this client is used for various ITs in the Connector project, so actual verification of client
 * functionality is mostly performed there.
 */
public class ConnectorAdminClientTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Rule
  public WireMockRule wireMockRule = new WireMockRule(0);

  private ConnectorAdminClient adminClient;

  @Before
  public void setUp() {
    this.adminClient = ConnectorAdminClient
      .construct(HttpUrl.parse("https://www.google.com/foo/bar"), template -> {
        return;
      });
  }

  @Test
  public void testConstructWithNullUrl() {
    expectedException.expect(NullPointerException.class);
    ConnectorAdminClient.construct(null, template -> {
    });
  }

  @Test
  public void testConstructWithNullAuthInterceptor() {
    expectedException.expect(NullPointerException.class);
    ConnectorAdminClient.construct(HttpUrl.parse("https://www.google.com/foo/bar"), null);
  }

  @Test
  public void testCreateAccount() {
    try {
      adminClient.createAccount(AccountSettings.builder()
        .accountId(AccountId.of("foo"))
        .assetCode("XRP")
        .assetScale(0)
        .accountRelationship(AccountRelationship.PEER)
        .linkType(LoopbackLink.LINK_TYPE)
        .build());

    } catch (FeignException e) {
      assertThat(e.status()).isEqualTo(404);
    }
  }

  @Test
  public void testUpdateAccount() {
    try {
      adminClient.updateAccount("foo", AccountSettings.builder()
        .accountId(AccountId.of("foo"))
        .assetCode("XRP")
        .assetScale(0)
        .accountRelationship(AccountRelationship.PEER)
        .linkType(LoopbackLink.LINK_TYPE)
        .build());

    } catch (FeignException e) {
      assertThat(e.status()).isEqualTo(404);
    }
  }

  @Test
  public void testFindAccount() {
    final Optional<AccountSettings> result = adminClient.findAccount("foo");
    assertThat(result).isEmpty();
  }

  @Test
  public void testDeleteAccount() {
    try {
      adminClient.deleteAccount("foo");
      fail("Should throw a 404 due to example.com domain");
    } catch (FeignException e) {
      assertThat(e.status()).isEqualTo(404);
    }
  }
}
