package org.interledger.connector.link.blast;

import org.interledger.core.InterledgerAddress;
import org.springframework.web.client.RestTemplate;

import java.net.URI;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * An extension of {@link AbstractBlastHttpSender} that uses a pre-specified Bearer token for Auth.
 *
 * Note: this implementation should not be used in a production environment.
 */
public class SimpleBearerBlastHttpSender extends AbstractBlastHttpSender implements BlastHttpSender {

  private final Supplier<byte[]> accountSecretSupplier;

  /**
   * Required-args Constructor.
   *
   * @param operatorAddressSupplier A {@link Supplier} for the ILP address of the node operating this BLAST sender.
   * @param uri                     The URI of the HTTP endpoint to send BLAST requests to.
   * @param restTemplate            A {@link RestTemplate} to use to communicate with the remote BLAST endpoint.
   * @param accountIdSupplier       A {@link Supplier} for the AccountId to use when communicating with the remote
   *                                endpoint.
   * @param accountSecretSupplier   A {@link Supplier} for the Account shared-secret to use when communicating with the
   *                                remote endpoint.
   */
  public SimpleBearerBlastHttpSender(
    final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier,
    final URI uri,
    final RestTemplate restTemplate,
    final Supplier<String> accountIdSupplier,
    final Supplier<byte[]> accountSecretSupplier
  ) {
    super(operatorAddressSupplier, uri, restTemplate, accountIdSupplier);
    this.accountSecretSupplier = Objects.requireNonNull(accountSecretSupplier);
  }

  @Override
  protected byte[] constructAuthToken() {
    this.logger.warn("SimpleBearerBlastHttpSender SHOULD NOT be used in a Production environment!");
    return accountSecretSupplier.get();
  }
}
