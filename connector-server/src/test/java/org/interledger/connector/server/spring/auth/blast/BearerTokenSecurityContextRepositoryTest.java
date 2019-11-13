package org.interledger.connector.server.spring.auth.blast;

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
  public void loadContextWithAuthToken() {
    String token = "foo:bar";
    HashCode expectedHmac = HashCode.fromString("00b85ca599428944cc41dfebdfcd57a3c635c2e8496ae0deee1717c33c30406e");

    when(mockRequest.getHeader("Authorization")).thenReturn("Bearer " + token);
    SecurityContext securityContext = repository.loadContext(holder);
    BearerAuthentication result = (BearerAuthentication) securityContext.getAuthentication();
    assertThat(result.hmacSha256()).isEqualTo(expectedHmac);
    assertThat(result.getBearerToken()).isEqualTo(token.getBytes());
    assertThat(result.isAuthenticated()).isFalse();
  }

  @Test
  public void loadContextNoAuthHeader() {
    SecurityContext securityContext = repository.loadContext(holder);
    assertThat(securityContext.getAuthentication()).isNull();
  }

  @Test
  public void loadContextNotBearer() {
    when(mockRequest.getHeader("Authorization")).thenReturn("not bearer token");
    SecurityContext securityContext = repository.loadContext(holder);
    assertThat(securityContext.getAuthentication()).isNull();
  }

  @Test
  public void containsContext() {
    when(mockRequest.getHeader("Authorization")).thenReturn("Bearer token");
    assertThat(repository.containsContext(mockRequest)).isTrue();
  }

  @Test
  public void doesntContainContext() {
    when(mockRequest.getHeader("Authorization")).thenReturn("not bearer token");
    assertThat(repository.containsContext(mockRequest)).isFalse();
  }

}