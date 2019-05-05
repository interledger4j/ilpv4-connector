package com.sappenin.interledger.ilpv4.connector.server.javamoney;

import com.sappenin.interledger.ilpv4.connector.server.ConnectorServerConfig;
import com.sappenin.interledger.ilpv4.connector.server.spring.settings.properties.ConnectorProperties;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;

import static com.sappenin.interledger.ilpv4.connector.server.spring.controllers.admin.AccountsController.SLASH_ACCOUNTS;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Running-server test that validates the security of the Admin API.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
  webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
  classes = {ConnectorServerConfig.class}
)
@ActiveProfiles({"test"})
@TestPropertySource(
  properties = {
    ConnectorProperties.ENABLED_PROTOCOLS + "." + ConnectorProperties.BLAST_ENABLED + "=false"
  }
)
public class AdminApiSecurityTest {

  private static final String FOO = "/foo";
  private static final String PASSWORD = "password";
  private static final String ADMIN = "admin";
  private static final String USER = "user";

  @Autowired
  TestRestTemplate restTemplate;

  /**
   * Validate admin API security for callers lacking any credentials.
   */
  @Test
  public void testGetAccountsWithNoCredentials() {
    this.doAccountCalls(HttpEntity.EMPTY,
      HttpStatus.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, HttpStatus.UNAUTHORIZED, HttpStatus.UNAUTHORIZED
    );
  }

  /**
   * Validate admin API security for callers with credentials that lack the proper authority.
   */
  @Test
  public void testGetAccountsWithInsufficientCredentials() {
    final HttpHeaders headers = new HttpHeaders();
    // Authorization: Basic admin:password
    headers.setBasicAuth(USER, PASSWORD);
    final HttpEntity httpEntity = new HttpEntity(headers);

    this.doAccountCalls(httpEntity,
      HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN, HttpStatus.FORBIDDEN
    );
  }

  /**
   * Validate admin API security for callers with credentials that have admin authority.
   */
  @Test
  public void testGetAccountsWithAdminCredentials() {
    final HttpHeaders headers = new HttpHeaders();
    // Authorization: Basic user:password
    headers.setBasicAuth(ADMIN, PASSWORD);
    final HttpEntity httpEntity = new HttpEntity(headers);

    this.doAccountCalls(httpEntity,
      HttpStatus.UNSUPPORTED_MEDIA_TYPE, HttpStatus.OK, HttpStatus.NOT_FOUND, HttpStatus.BAD_REQUEST
    );
  }

  /**
   * Helper method to make all admin API calls with varying credentials found in {@code httpEntity}.
   */
  private void doAccountCalls(
    final HttpEntity httpEntity,
    final HttpStatus expectedCreateAccountStatus,
    final HttpStatus expectedGetAccountsStatus,
    final HttpStatus expectedGetAccountStatus,
    final HttpStatus expectedUpdateAccountStatus
  ) {
    final ResponseEntity postAccount =
      restTemplate.exchange(SLASH_ACCOUNTS, HttpMethod.POST, httpEntity, Void.class);
    assertThat(postAccount.getStatusCode(), is(expectedCreateAccountStatus));

    final ResponseEntity getAccounts = restTemplate.exchange(SLASH_ACCOUNTS, HttpMethod.GET, httpEntity, Void.class);
    assertThat(getAccounts.getStatusCode(), is(expectedGetAccountsStatus));

    final ResponseEntity getAccount =
      restTemplate.exchange(SLASH_ACCOUNTS + FOO, HttpMethod.GET, httpEntity, Void.class);
    assertThat(getAccount.getStatusCode(), is(expectedGetAccountStatus));

    final ResponseEntity putAccount =
      restTemplate.exchange(SLASH_ACCOUNTS + FOO, HttpMethod.PUT, httpEntity, Void.class);
    assertThat(putAccount.getStatusCode(), is(expectedUpdateAccountStatus));
  }

}