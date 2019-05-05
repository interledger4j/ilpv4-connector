package org.interledger.connector.link.blast;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import okhttp3.HttpUrl;
import org.interledger.core.InterledgerAddress;
import org.interledger.crypto.Decryptor;
import org.interledger.crypto.EncryptedSecret;
import org.springframework.web.client.RestTemplate;

import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * Encapsulates how to communicate with a BLAST peer.
 */
public class JwtBlastHttpSender extends AbstractBlastHttpSender implements BlastHttpSender {

  // For HS256, this is tolerable because there will be a new JwtBlastHttpSender for each peer.
  private final byte[] sharedSecret;

  /**
   * Required-args Constructor.
   */
  public JwtBlastHttpSender(
    final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier,
    final RestTemplate restTemplate,
    final OutgoingLinkSettings outgoingLinkSettings,
    final Decryptor decryptor
  ) {
    super(operatorAddressSupplier, restTemplate, outgoingLinkSettings);
    final EncryptedSecret encryptedSecret =
      EncryptedSecret.fromEncodedValue(getOutgoingLinkSettings().encryptedTokenSharedSecret());
    this.sharedSecret = Objects.requireNonNull(decryptor).decrypt(encryptedSecret);
  }

  // TODO: For performance reasons, we probably want to cache the outgoing token for some amount of time less than
  //  the expiry so that we don't have to sign on every outgoing request.
  @Override
  protected String constructAuthToken() {
    return JWT.create()
      .withIssuedAt(new Date())
      .withIssuer(getOutgoingLinkSettings()
        .tokenIssuer()
        .map(HttpUrl::toString)
        .orElseThrow(() -> new RuntimeException("JWT Blast Senders require an Outgoing Issuer!"))
      )
      .withSubject(getOutgoingLinkSettings().tokenSubject()) // account identifier at the remote server.
      .withAudience(getOutgoingLinkSettings().tokenAudience())
      .sign(Algorithm.HMAC256(sharedSecret));
  }
}
