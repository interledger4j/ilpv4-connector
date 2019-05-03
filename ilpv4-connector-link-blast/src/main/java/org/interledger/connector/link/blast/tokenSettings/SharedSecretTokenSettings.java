package org.interledger.connector.link.blast.tokenSettings;

import okhttp3.HttpUrl;
import org.interledger.connector.link.blast.BlastLinkSettings;

import java.util.Optional;

/**
 * Defines settings for a link that uses JWT_HS_256 with HMAC-SHA256 bearer tokens employing a shared secret.
 */
public interface SharedSecretTokenSettings {

  /**
   * The expected `iss` value of the issuer of a Blast token. This value should always be a URL so that it can be rooted
   * in Internet PKI and compared against the TLS certificate issued by the other side of a blast connection (i.e., the
   * remote peer). It is optional in order to support node-wide issuance, or account-level issuance.
   *
   * @return
   */
  Optional<HttpUrl> tokenIssuer();

  /**
   * The expected `sub` value of a Blast token. This typically is the account identifier of the entity who generated the
   * token, but at the Connector indicated in {@link #tokenAudience()}.
   *
   * @return
   */
  String tokenSubject();

  /**
   * The expected `aud` claim value of an incoming JWT_HS_256 token. In general, this value should be the URL of the
   * Connector operating this BLAST link, since the remote will want to narrow the scope of its token to only be valid
   * on this endpoint.
   *
   * @return
   */
  String tokenAudience();

  /**
   * The type of Auth to support for outgoing HTTP connections.
   *
   * @return
   */
  BlastLinkSettings.AuthType authType();

  /**
   * An encrypted shared `secret`, encoded per `EncryptedSecret`, that can be used to authenticate an incoming
   * ILP-over-HTTP (BLAST) connection.
   *
   * @return
   */
  String encryptedTokenSharedSecret();

}
