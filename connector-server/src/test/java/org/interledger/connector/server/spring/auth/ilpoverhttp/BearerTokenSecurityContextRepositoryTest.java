package org.interledger.connector.server.spring.auth.ilpoverhttp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;
import static org.mockito.MockitoAnnotations.initMocks;

import com.google.common.hash.HashCode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.web.context.HttpRequestResponseHolder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class BearerTokenSecurityContextRepositoryTest {

  private BearerTokenSecurityContextRepository repository;
  @Mock
  private HttpServletRequest mockRequest;
  @Mock
  private HttpServletResponse mockResponse;
  private HttpRequestResponseHolder holder;

  @Before
  public void setUp() {
    initMocks(this);
    repository = new BearerTokenSecurityContextRepository(new byte[32]);
    holder = new HttpRequestResponseHolder(mockRequest, mockResponse);
  }

  @Test
  public void loadContextWithAuthTokenInIlpUrl() {
    mockRequestPath("/accounts/foo");
    verifyBearerAuth();
  }

  @Test
  public void loadContextWithAuthTokenInAccountsUrl() {
    mockRequestPath("/accounts/foo/ilp");
    verifyBearerAuth();
  }

  @Test
  public void loadContextWithAuthTokenInSubAccountsUrl() {
    mockRequestPath("/connectors/123/accounts/foo/ilp");
    verifyBearerAuth();
  }

  @Test
  public void loadContextNoAccountIdInUrl() {
    String token = "foo:bar";
    mockRequestPath("/routes/foo/ilp");
    when(mockRequest.getHeader("Authorization")).thenReturn("Bearer " + token);
    assertThat(repository.containsContext(mockRequest)).isFalse();
  }

  @Test
  public void loadContextNoAuthHeader() {
    mockRequestPath("/accounts/foo/ilp");
    SecurityContext securityContext = repository.loadContext(holder);
    assertThat(securityContext.getAuthentication()).isNull();
  }

  @Test
  public void loadContextDeprecatedNoBearerPrefix() {
    mockRequestPath("/accounts/foo/ilp");
    String deprecatedBearerToken = "undercover token";
    when(mockRequest.getHeader("Authorization")).thenReturn(deprecatedBearerToken);
    SecurityContext securityContext = repository.loadContext(holder);
    assertThat(securityContext.getAuthentication()).isNotNull();
    assertThat(securityContext.getAuthentication().getCredentials()).isEqualTo(deprecatedBearerToken.getBytes());
  }

  @Test
  public void loadContextBasicAuth() {
    mockRequestPath("/accounts/foo/ilp");
    String basicAuth = "Basic token";
    when(mockRequest.getHeader("Authorization")).thenReturn(basicAuth);
    SecurityContext securityContext = repository.loadContext(holder);
    assertThat(securityContext.getAuthentication()).isNull();
  }

  @Test
  public void containsContext() {
    mockRequestPath("/accounts/foo/ilp");
    when(mockRequest.getHeader("Authorization")).thenReturn("Bearer token");
    assertThat(repository.containsContext(mockRequest)).isTrue();
  }

  private void verifyBearerAuth() {
    String token = "foo:bar";
    HashCode expectedHmac = HashCode.fromString("00b85ca599428944cc41dfebdfcd57a3c635c2e8496ae0deee1717c33c30406e");
    when(mockRequest.getHeader("Authorization")).thenReturn("Bearer " + token);
    SecurityContext securityContext = repository.loadContext(holder);
    BearerAuthentication result = (BearerAuthentication) securityContext.getAuthentication();
    assertThat(result).isNotNull();
    assertThat(result.hmacSha256()).isEqualTo(expectedHmac);
    assertThat(result.getBearerToken()).isEqualTo(token.getBytes());
    assertThat(result.isAuthenticated()).isFalse();
    assertThat(result.getPrincipal()).isEqualTo("foo");
  }

  private void mockRequestPath(String path) {
    when(mockRequest.getPathInfo()).thenReturn(path);
    when(mockRequest.getServletPath()).thenReturn("");
    when(mockRequest.getContextPath()).thenReturn("");
  }

}
