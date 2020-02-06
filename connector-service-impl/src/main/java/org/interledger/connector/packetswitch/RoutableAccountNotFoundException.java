package org.interledger.connector.packetswitch;

import org.interledger.connector.accounts.AccountId;

import java.util.Objects;

/**
 * Thrown by a {@link org.interledger.connector.packetswitch.filters.PeerProtocolPacketFilter} in the
 * event that a Peer sends a {@link org.interledger.connector.ccp.CcpRouteUpdateRequest} or a
 * {@link org.interledger.connector.ccp.CcpRouteControlRequest} and the connector does not have a CCP enabled
 * account associated with the peer.
 */
public class RoutableAccountNotFoundException extends RuntimeException {

  /**
   * The {@link AccountId} of the account that threw an exception while handling CCP requests
   */
  private final AccountId accountId;

  /**
   * Constructs a new runtime exception with {@code null} as its detail message.  The cause is not initialized, and may
   * subsequently be initialized by a call to {@link #initCause}.
   *
   * @param accountId                 The {@link AccountId} of the account that threw an exception while handling
   *                                  CCP requests.
   */
  public RoutableAccountNotFoundException(AccountId accountId) {
    super(message(null, accountId));
    this.accountId = Objects.requireNonNull(accountId);
  }

  /**
   * Constructs a new runtime exception with the specified detail message. The cause is not initialized, and may
   * subsequently be initialized by a call to {@link #initCause}.
   *
   * @param message                   the detail message. The detail message is saved for later retrieval by the {@link
   *                                  #getMessage()} method.
   * @param accountId                 The {@link AccountId} of the account that threw an exception while handling
   *                                  CCP requests.
   */
  public RoutableAccountNotFoundException(String message, AccountId accountId) {
    super(message(message, accountId));
    this.accountId = Objects.requireNonNull(accountId);
  }

  /**
   * Constructs a new runtime exception with the specified detail message. The cause is not initialized, and may
   * subsequently be initialized by a call to {@link #initCause}.
   *
   * @param message                   the detail message. The detail message is saved for later retrieval by the {@link
   *                                  #getMessage()} method.
   * @param cause                     the cause (which is saved for later retrieval by the {@link #getCause()} method).
   *                                  (A
   *                                  <tt>null</tt> value is permitted, and indicates that the cause is nonexistent or
   *                                  unknown.)
   * @param accountId                 The {@link AccountId} of the account that threw an exception while handling
   *                                  CCP requests.
   */
  public RoutableAccountNotFoundException(String message, Throwable cause, AccountId accountId) {
    super(message(message, accountId), cause);
    this.accountId = Objects.requireNonNull(accountId);
  }

  /**
   * Constructs a new runtime exception with the specified detail message. The cause is not initialized, and may
   * subsequently be initialized by a call to {@link #initCause}.
   *
   * @param cause                     the cause (which is saved for later retrieval by the {@link #getCause()} method).
   *                                  (A
   *                                  <tt>null</tt> value is permitted, and indicates that the cause is nonexistent or
   *                                  unknown.)
   * @param accountId                 The {@link AccountId} of the account that threw an exception while handling
   *                                  CCP requests.
   */
  public RoutableAccountNotFoundException(Throwable cause, AccountId accountId) {
    this(message(null, accountId), cause, accountId);
  }

  private static final String message(
    String message, AccountId accountId
  ) {
    if (message != null) {
      return String.format(
        "accountId=%s, message=%s",
        accountId.value(), message
      );
    } else {
      return String.format(
        "accountId=%s",
        accountId.value()
      );
    }
  }

  public AccountId getAccountId() {
    return accountId;
  }
}
