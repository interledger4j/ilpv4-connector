package org.interledger.openpayments;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import okhttp3.HttpUrl;
import org.immutables.value.Value;

import java.util.Optional;

/**
 * Urls that user can visit to authorize a payment
 */
@Value.Immutable
@JsonSerialize(as = ImmutableAuthorizationUrls.class)
@JsonDeserialize(as = ImmutableAuthorizationUrls.class)
public interface AuthorizationUrls {

  static ImmutableAuthorizationUrls.Builder builder() {
    return ImmutableAuthorizationUrls.builder();
  }

  /**
   * Web page url for user authorization flow
   * @return
   */
  Optional<HttpUrl> pageUrl();

  /**
   * Image/QR code that user can scan for authorization flow
   * @return
   */
  Optional<HttpUrl> imageUrl();

}
