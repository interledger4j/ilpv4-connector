package org.interledger.connector.link.blast;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import okhttp3.HttpUrl;
import org.interledger.core.InterledgerAddress;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

import static org.interledger.connector.link.blast.BlastHeaders.BLAST_AUDIENCE;

/**
 * Encapsulates how to communicate with a BLAST peer.
 */
public class JwtBlastHttpSender extends AbstractBlastHttpSender implements BlastHttpSender {

  private final Supplier<HttpUrl> tokenIssuerSupplier;
  private final Supplier<byte[]> accountSecretSupplier;

  /**
   * Required-args Constructor.
   *
   * @param operatorAddressSupplier A {@link Supplier} for the ILP address of the node operating this BLAST sender.
   * @param uri                     The URI of the HTTP endpoint to send BLAST requests to.
   * @param restTemplate            A {@link RestTemplate} to use to communicate with the remote BLAST endpoint.
   * @param accountIdSupplier       A {@link Supplier} for the AccountId to use when communicating with the remote
   *                                endpoint.
   * @param tokenIssuerSupplier     A {@link Supplier} for accessing the Auth token to use when communicating with the
   *                                remote endpoint.
   * @param accountSecretSupplier   A {@link Supplier} for the Account shared-secret to use when communicating with the
   *                                remote endpoint.
   */
  public JwtBlastHttpSender(
    final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier,
    final URI uri,
    final RestTemplate restTemplate,
    final Supplier<String> accountIdSupplier,
    final Supplier<HttpUrl> tokenIssuerSupplier,
    final Supplier<byte[]> accountSecretSupplier
  ) {
    super(operatorAddressSupplier, uri, restTemplate, accountIdSupplier);

    this.tokenIssuerSupplier = Objects.requireNonNull(tokenIssuerSupplier);
    this.accountSecretSupplier = Objects.requireNonNull(accountSecretSupplier);
  }

  @Override
  protected byte[] constructAuthToken() {
    return JWT.create()
      .withIssuedAt(new Date())
      .withIssuer(tokenIssuerSupplier.get().toString())
      .withSubject(getAccountIdSupplier().get()) // account identifier at the remote server.
      .withAudience(BLAST_AUDIENCE)
      .sign(Algorithm.HMAC256(accountSecretSupplier.get())).getBytes();
  }
}
