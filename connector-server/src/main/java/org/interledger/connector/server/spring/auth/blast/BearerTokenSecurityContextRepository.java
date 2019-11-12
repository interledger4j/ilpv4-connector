package org.interledger.connector.server.spring.auth.blast;

import com.google.common.hash.Hashing;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpRequestResponseHolder;
import org.springframework.security.web.context.SecurityContextRepository;

import java.util.Optional;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class BearerTokenSecurityContextRepository implements SecurityContextRepository {
  private final byte[] ephemeralBytes;

  public BearerTokenSecurityContextRepository(byte[] ephemeralBytes) {
    this.ephemeralBytes = ephemeralBytes;
  }

  @Override
  public SecurityContext loadContext(HttpRequestResponseHolder requestResponseHolder) {
    SecurityContext context = SecurityContextHolder.createEmptyContext();
    parseToken(requestResponseHolder.getRequest()).ifPresent(token ->
        context.setAuthentication(BearerAuthentication.builder()
            .isAuthenticated(false)
            .hmacSha256(Hashing.hmacSha256(ephemeralBytes).hashBytes(token))
            .bearerToken(token)
            .build()));
    return context;
  }

  @Override
  public void saveContext(SecurityContext context, HttpServletRequest request, HttpServletResponse response) {
    // stateless. do nothing
  }

  @Override
  public boolean containsContext(HttpServletRequest request) {
    return parseToken(request).isPresent();
  }

  private Optional<byte[]> parseToken(HttpServletRequest request) {
    return Optional.ofNullable(request.getHeader("Authorization"))
        .map(authHeader -> {
          int bearerIndex = authHeader.indexOf("Bearer ");
          if (bearerIndex == 0) {
            byte[] token = authHeader.substring(7).getBytes();
            return token;
          } else {
            return null;
          }
        });
  }
}
