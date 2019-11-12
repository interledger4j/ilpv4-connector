package org.interledger.connector.server.javamoney;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.interledger.connector.server.spring.controllers.PathConstants.SLASH_ACCOUNTS;

import org.interledger.connector.server.ConnectorServerConfig;

import com.google.common.collect.Lists;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.junit4.SpringRunner;

/**
 * Running-server test that validates the security of the Admin API.
 */
@RunWith(SpringRunner.class)
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    classes = {ConnectorServerConfig.class}
)
@ActiveProfiles( {"test"})
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
  public void testAccountsWithNoCredentials() {
    this.doAccountCalls(HttpEntity.EMPTY,
        HttpStatus.UNAUTHORIZED, // POST
        HttpStatus.UNAUTHORIZED, // GET /accounts
        HttpStatus.UNAUTHORIZED, // GET /accounts/foo
        HttpStatus.UNAUTHORIZED // PUT
    );
  }

  /**
   * Validate admin API security for callers with credentials that lack the proper authority (tests all major
   * `/accounts` HTTP methods).
   */
  @Test
  public void testAccountsWithInsufficientCredentials() {
    final HttpHeaders headers = this.constructHttpHeaders();
    headers.setBasicAuth(USER, PASSWORD);

    final HttpEntity httpEntity = new HttpEntity(headers);

    this.doAccountCalls(httpEntity,
        HttpStatus.FORBIDDEN, // POST
        HttpStatus.FORBIDDEN, // GET /accounts
        HttpStatus.FORBIDDEN, // GET /accounts/foo
        HttpStatus.FORBIDDEN // PUT
    );
  }

  /**
   * Validate admin API security for callers with credentials that have admin authority (tests all major `/accounts`
   * HTTP methods).
   */
  @Test
  public void testAccountsWithAdminCredentials() {
    final HttpHeaders headers = this.constructHttpHeaders();
    headers.setBasicAuth(ADMIN, PASSWORD);
    final HttpEntity httpEntity = new HttpEntity(headers);

    this.doAccountCalls(httpEntity,
        HttpStatus.BAD_REQUEST, // POST
        HttpStatus.OK, // GET /accounts
        HttpStatus.NOT_FOUND, // GET /accounts/foo
        HttpStatus.BAD_REQUEST // PUT
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
    final ResponseEntity postAccount = restTemplate.exchange(SLASH_ACCOUNTS, HttpMethod.POST, httpEntity, Void.class);
    assertThat(postAccount.getStatusCode(), is(expectedCreateAccountStatus));

    final ResponseEntity getAccounts = restTemplate.exchange(SLASH_ACCOUNTS, HttpMethod.GET, httpEntity, Void.class);
    assertThat(getAccounts.getStatusCode(), is(expectedGetAccountsStatus));

    final ResponseEntity getAccount = restTemplate.exchange(SLASH_ACCOUNTS + FOO, HttpMethod.GET, httpEntity, Void.class);
    assertThat(getAccount.getStatusCode(), is(expectedGetAccountStatus));

    final ResponseEntity putAccount = restTemplate.exchange(SLASH_ACCOUNTS + FOO, HttpMethod.PUT, httpEntity, Void.class);
    assertThat(putAccount.getStatusCode(), is(expectedUpdateAccountStatus));
  }

  /**
   * Helper method to initialize HTTP headers with the proper content and accept types.
   *
   * @return A {@link HttpHeaders}.
   */
  private HttpHeaders constructHttpHeaders() {
    final HttpHeaders headers = new HttpHeaders();
    headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_JSON);
    return headers;
  }

}
