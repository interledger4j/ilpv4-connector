package org.interledger.connector.link.blast;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.springframework.web.client.RestTemplate;
import org.interledger.core.InterledgerAddress;
import org.interledger.crypto.Decryptor;
import org.interledger.crypto.EncryptedSecret;

import java.time.Duration;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * <p>Encapsulates how to communicate with a bilateral Peer using ILP-over-HTTP.</p>
 *
 * <p>This implementation attempts to reduce the amount of time the shared-secret is in-memory by pre-emmptively
 * generating an authentication token that conforms to `JWT_HS_256`.</p>
 */
public class JwtBlastHttpSender extends AbstractBlastHttpSender implements BlastHttpSender {

  // See Javadoc above for how this is used.
  private final LoadingCache<String, String> ilpOverHttpAuthTokens;

  /**
   * Required-args Constructor.
   */
  public JwtBlastHttpSender(
    final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier,
    final RestTemplate restTemplate,
    final Decryptor decryptor,
    final OutgoingLinkSettings outgoingLinkSettings
  ) {
    super(operatorAddressSupplier, restTemplate, outgoingLinkSettings);
    final EncryptedSecret encryptedSecret =
      EncryptedSecret.fromEncodedValue(getOutgoingLinkSettings().encryptedTokenSharedSecret());

    ilpOverHttpAuthTokens = Caffeine.newBuilder()
      // There should only ever be 1 or 2 tokens in-memory for a given client instance.
      .maximumSize(3)
      // Expire after this duration, which will correspond to the last incoming request from the peer.
      .expireAfterAccess(outgoingLinkSettings.tokenExpiry().orElse(Duration.of(30, ChronoUnit.MINUTES)))
      .removalListener((key, value, cause) ->
        logger.debug("Removing IlpOverHttp AuthToken from Cache for Principal: {}", key)
      )
      .build(accountId -> {
        Objects.requireNonNull(accountId);

        final byte[] sharedSecretBytes = Objects.requireNonNull(decryptor).decrypt(encryptedSecret);
        try {
          return JWT.create()
            //.withIssuedAt(new Date())
            //      .withIssuer(getOutgoingLinkSettings()
            //        .tokenIssuer()
            //        .map(HttpUrl::toString)
            //        .orElseThrow(() -> new RuntimeException("JWT Blast Senders require an Outgoing Issuer!"))
            //      )
            .withSubject(getOutgoingLinkSettings().tokenSubject()) // account identifier at the remote server.
            // Expire at the appointed time, or else after 15 minutes.
            .withExpiresAt(outgoingLinkSettings.tokenExpiry()
              .map(expiry -> Date.from(Instant.now().plus(expiry)))
              .orElseGet(() -> Date.from(Instant.now().plus(15, ChronoUnit.MINUTES))))
            .sign(Algorithm.HMAC256(sharedSecretBytes));
        } finally {
          // Zero-out all bytes in the `sharedSecretBytes` array.
          Arrays.fill(sharedSecretBytes, (byte) 0);
        }
      });
  }

  @Override
  protected String constructAuthToken() {
    return this.ilpOverHttpAuthTokens.get(getOutgoingLinkSettings().tokenSubject());
  }
}
