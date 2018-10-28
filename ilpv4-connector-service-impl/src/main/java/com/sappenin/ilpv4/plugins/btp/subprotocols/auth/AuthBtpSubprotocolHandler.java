package com.sappenin.ilpv4.plugins.btp.subprotocols.auth;

import com.google.common.base.Preconditions;
import com.sappenin.ilpv4.plugins.btp.BtpSession;
import com.sappenin.ilpv4.plugins.btp.BtpSessionCredentials;
import com.sappenin.ilpv4.plugins.btp.ImmutableBtpSessionCredentials;
import com.sappenin.ilpv4.plugins.btp.subprotocols.BtpSubProtocolHandler;
import com.sappenin.ilpv4.plugins.btp.subprotocols.BtpSubProtocolHandlerRegistry;
import org.interledger.btp.*;
import org.interledger.plugin.lpiv2.support.Completions;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;

import static com.sappenin.ilpv4.plugins.btp.subprotocols.BtpSubProtocolHandlerRegistry.*;
import static org.interledger.btp.BtpErrorCode.F00_NotAcceptedError;

/**
 * An extension of {@link BtpSubProtocolHandler} for handling the <tt>auth</tt> sub-protocol as defined in IL-RFC-23.
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0023-bilateral-transfer-protocol/0023-bilateral-transfer-protocol.md#authentication"
 */
public class AuthBtpSubprotocolHandler extends BtpSubProtocolHandler {

  /**
   * No-args Constructor.
   */
  public AuthBtpSubprotocolHandler() {
  }

  /**
   * Construct a {@link BtpSubProtocol} response for using the <tt>auth</tt> sub-protocol.
   *
   * @return A {@link BtpSubProtocol}.
   */
  static BtpSubProtocol authResponse() {
    return BtpSubProtocol.builder()
      .protocolName(BtpSubProtocols.AUTH)
      .contentType(BtpSubProtocol.ContentType.MIME_APPLICATION_OCTET_STREAM)
      .build();
  }

  @Override
  public CompletableFuture<BtpSubProtocol> handleBinaryMessage(
    final BtpSession btpSession, final BtpMessage incomingBtpMessage
  ) throws BtpRuntimeException {
    Objects.requireNonNull(btpSession);
    Objects.requireNonNull(incomingBtpMessage);

    try {
      // Before anything else, when a client connects to a server, it sends a special Message request. Its primary
      // protocolData entry MUST have name 'auth', content type MIME_APPLICATION_OCTET_STREAM, and empty data.
      Preconditions.checkArgument(
        incomingBtpMessage.getPrimarySubProtocol().getProtocolName()
          .equals(BtpSubProtocolHandlerRegistry.BTP_SUB_PROTOCOL_AUTH),
        String.format("Expected BTP SubProtocol with Name: `%s`", BTP_SUB_PROTOCOL_AUTH)
      );

      // `auth_username` is optional...
      final Optional<String> auth_user = incomingBtpMessage.getSubProtocol(BTP_SUB_PROTOCOL_AUTH_USERNAME)
        .map(BtpSubProtocol::getDataAsString);

      // ...among the secondary entries, there MUST be a UTF-8 'auth_token' entry
      final String auth_token = incomingBtpMessage.getSubProtocol(BTP_SUB_PROTOCOL_AUTH_TOKEN)
        .map(BtpSubProtocol::getDataAsString)
        .orElseThrow(
          () -> new BtpRuntimeException(BtpErrorCode.F00_NotAcceptedError,
            String.format("Expected BTP SubProtocol with Id: %s", BTP_SUB_PROTOCOL_AUTH_TOKEN))
        );

      if (isValidAuthToken(auth_user, auth_token)) {
        this.storeAuthInBtpSession(btpSession, auth_user, auth_token);
        // SUCCESS! Respond with an empty Ack message...
        return Completions.supplyAsync(() -> AuthBtpSubprotocolHandler.authResponse()).toCompletableFuture();
      } else {
        throw new BtpRuntimeException(BtpErrorCode.F00_NotAcceptedError,
          String.format("invalid %s", BTP_SUB_PROTOCOL_AUTH_TOKEN)
        );
      }
    } catch (BtpRuntimeException e) {
      throw e;
    } catch (Exception e) {
      throw new BtpRuntimeException(BtpErrorCode.F00_NotAcceptedError, e.getMessage(), e);
    }
  }

  /**
   * Checks to see if the provided <tt>incomingAuthToken</tt> matches the secret configured for this peer.
   *
   * @param incomingAuthToken
   *
   * @return
   */
  private boolean isValidAuthToken(
    final Optional<String> incomingAuthUser,
    final String incomingAuthToken
  ) {
    Objects.requireNonNull(incomingAuthToken, "incomingAuthToken must not be null!");

    if (incomingAuthToken.equals("")) {
      return false;
    }
    // TODO: Compare the presented auth_token with a secret configured for this plugin. As a simplistic example, a
    // plugin might be configured on a particular port, and will have a `secret` defined. However, this is surprising
    // since it seems like we would want to run multiple BTP plugins on the same port. For example, plugin1 and
    // plugin2. For that to work, we would need some sort of identifier to link a connection (WsSession) to the
    // plugin instance. However, it seems like this is not the current design of the Plugin interface. Instead, LPIv2
    // seems to assume that only a given channel will run on a given Websocket port. If this is the case, then we
    // would need to configure a new listener/port combination for each plugin in the SpringBtpConfig. If that holds,
    // then a particular BtpSocketHandler would have only a single secret, which can be found here. However, this
    // doesn't feel right -- adding a new port per plugin seems like a difficult system to scale in a production
    // environment, so more research is required before implementing that.

    //    if (incomingAuthUser.equals(this.expectedCredentials.username()) && incomingAuthToken.equals(this.expectedCredentials.token())) {
    //      return true;
    //    } else {
    //      return false;
    //    }
    return true;
  }

  /**
   * Store the username and token into this Websocket session.
   *
   * @param username The username of the signed-in account.
   * @param token    An authorization token used to authenticate the indicated user.
   */
  private void storeAuthInBtpSession(
    final BtpSession btpSession, final Optional<String> username, final String token
  ) {
    Objects.requireNonNull(username);
    Objects.requireNonNull(token);

    final String name = username.orElse(token);

    final BtpSessionCredentials credentials = ImmutableBtpSessionCredentials.builder().name(name).build();
    btpSession.setValidAuthentication(credentials);
  }

  /**
   * Construct a {@link BtpError} for the supplied request-id that can be returned when authentication is invalid.
   *
   * @param requestId
   *
   * @return
   */
  private BtpError constructAuthError(final long requestId, final String errorMessage) {
    return BtpError.builder()
      .requestId(requestId)
      .errorCode(F00_NotAcceptedError)
      .errorData(errorMessage.getBytes())
      .build();
  }
}
