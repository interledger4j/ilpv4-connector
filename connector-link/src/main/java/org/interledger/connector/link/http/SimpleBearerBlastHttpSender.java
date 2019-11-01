package org.interledger.connector.link.http;

import org.interledger.core.InterledgerAddress;
import org.interledger.crypto.Decryptor;
import org.interledger.crypto.EncryptedSecret;
import org.interledger.link.http.OutgoingLinkSettings;

import org.springframework.web.client.RestTemplate;

import java.util.Objects;
import java.util.Optional;
import java.util.function.Supplier;

/**
 * An extension of {@link AbstractBlastHttpSender} that uses a pre-specified Bearer token for Auth.
 *
 * Note: this implementation should not be used in a production environment.
 */
@Deprecated
// TODO: Delete this.
public class SimpleBearerBlastHttpSender extends AbstractBlastHttpSender implements BlastHttpSender {

  private final Decryptor decryptor;

  /**
   * Required-args Constructor.
   */
  public SimpleBearerBlastHttpSender(
    final Supplier<Optional<InterledgerAddress>> operatorAddressSupplier, final RestTemplate restTemplate,
    final OutgoingLinkSettings outgoingLinkSettings, final Decryptor decryptor
  ) {
    super(operatorAddressSupplier, restTemplate, outgoingLinkSettings);
    this.decryptor = Objects.requireNonNull(decryptor);
  }

  @Override
  protected String constructAuthToken() {
    this.logger.warn("SimpleBearerBlastHttpSender SHOULD NOT be used in a Production environment!");
    final EncryptedSecret encryptedSecret =
      EncryptedSecret.fromEncodedValue(getOutgoingLinkSettings().encryptedTokenSharedSecret());
    return new String(this.decryptor.decrypt(encryptedSecret));
  }
}
