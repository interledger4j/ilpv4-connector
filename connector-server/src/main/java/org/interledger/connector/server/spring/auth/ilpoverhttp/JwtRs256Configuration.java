package org.interledger.connector.server.spring.auth.ilpoverhttp;

import com.auth0.jwt.interfaces.RSAKeyProvider;
import okhttp3.HttpUrl;
import org.immutables.value.Value;

@Value.Immutable
public interface JwtRs256Configuration {

  static ImmutableJwtRs256Configuration.Builder builder() {
    return ImmutableJwtRs256Configuration.builder();
  }

  RSAKeyProvider keyProvider();

  HttpUrl issuer();

  String subject();

  String audience();

}
