package org.interledger.ilpv4.connector.settlement;

import com.sappenin.interledger.ilpv4.connector.settlement.SettlementService;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.SettlementEngineAccountId;

import java.util.Objects;

/**
 * Thrown by instances of {@link SettlementService}, such as when the Settlement service cannot communicate with a
 * particular settlement engine.
 */
public class SettlementServiceException extends RuntimeException {

  /**
   * The {@link AccountId} of the account that threw an exception while communicating to a Settlement Engine.
   */
  private final AccountId accountId;

  /**
   * The {@link SettlementEngineAccountId} of the account on the settlement engine.
   */
  private final SettlementEngineAccountId settlementEngineAccountId;

  /**
   * Constructs a new runtime exception with {@code null} as its detail message.  The cause is not initialized, and may
   * subsequently be initialized by a call to {@link #initCause}.
   *
   * @param accountId                 The {@link AccountId} of the account that threw an exception while communicating
   *                                  to a Settlement Engine.
   * @param settlementEngineAccountId The {@link SettlementEngineAccountId} of the account on the settlement engine.
   */
  public SettlementServiceException(AccountId accountId, SettlementEngineAccountId settlementEngineAccountId) {
    super(message(null, accountId, settlementEngineAccountId));
    this.accountId = Objects.requireNonNull(accountId);
    this.settlementEngineAccountId = settlementEngineAccountId;
  }

  /**
   * Constructs a new runtime exception with the specified detail message. The cause is not initialized, and may
   * subsequently be initialized by a call to {@link #initCause}.
   *
   * @param message                   the detail message. The detail message is saved for later retrieval by the {@link
   *                                  #getMessage()} method.
   * @param accountId                 The {@link AccountId} of the account that threw an exception while communicating
   *                                  to a Settlement Engine.
   * @param settlementEngineAccountId The {@link SettlementEngineAccountId} of the account on the settlement engine.
   */
  public SettlementServiceException(String message, AccountId accountId, SettlementEngineAccountId settlementEngineAccountId) {
    super(message(message, accountId, settlementEngineAccountId));
    this.accountId = Objects.requireNonNull(accountId);
    this.settlementEngineAccountId = settlementEngineAccountId;
  }

  /**
   * Constructs a new runtime exception with the specified detail message and cause.  <p>Note that the detail message
   * associated with {@code cause} is <i>not</i> automatically incorporated in this runtime exception's detail message.
   *
   * @param message                   the detail message (which is saved for later retrieval by the {@link
   *                                  #getMessage()} method).
   * @param cause                     the cause (which is saved for later retrieval by the {@link #getCause()} method).
   *                                  (A
   *                                  <tt>null</tt> value is permitted, and indicates that the cause is nonexistent or
   *                                  unknown.)
   * @param accountId                 The {@link AccountId} of the account that threw an exception while communicating
   *                                  to a Settlement Engine.
   * @param settlementEngineAccountId The {@link SettlementEngineAccountId} of the account on the settlement engine.
   *
   * @since 1.4
   */
  public SettlementServiceException(String message, Throwable cause, AccountId accountId, SettlementEngineAccountId settlementEngineAccountId) {
    super(message(message, accountId, settlementEngineAccountId), cause);
    this.accountId = Objects.requireNonNull(accountId);
    this.settlementEngineAccountId = settlementEngineAccountId;
  }

  /**
   * Constructs a new runtime exception with the specified cause and a detail message of <tt>(cause==null ? null :
   * cause.toString())</tt> (which typically contains the class and detail message of
   * <tt>cause</tt>).  This constructor is useful for runtime exceptions
   * that are little more than wrappers for other throwables.
   *
   * @param cause                     the cause (which is saved for later retrieval by the {@link #getCause()} method).
   *                                  (A
   *                                  <tt>null</tt> value is permitted, and indicates that the cause is nonexistent or
   *                                  unknown.)
   * @param accountId                 The {@link AccountId} of the account that threw an exception while communicating
   *                                  to a Settlement Engine.
   * @param settlementEngineAccountId The {@link SettlementEngineAccountId} of the account on the settlement engine.
   *
   * @since 1.4
   */
  public SettlementServiceException(Throwable cause, AccountId accountId, SettlementEngineAccountId settlementEngineAccountId) {
    this(message(null, accountId, settlementEngineAccountId), cause, accountId, settlementEngineAccountId);
  }

  /**
   * Constructs a new runtime exception with the specified detail message, cause, suppression enabled or disabled, and
   * writable stack trace enabled or disabled.
   *
   * @param message                   the detail message.
   * @param cause                     the cause.  (A {@code null} value is permitted, and indicates that the cause is
   *                                  nonexistent or unknown.)
   * @param enableSuppression         whether or not suppression is enabled or disabled
   * @param writableStackTrace        whether or not the stack trace should be writable
   * @param accountId                 The {@link AccountId} of the account that threw an exception while communicating
   *                                  to a Settlement Engine.
   * @param settlementEngineAccountId The {@link SettlementEngineAccountId} of the account on the settlement engine.
   *
   * @since 1.7
   */
  protected SettlementServiceException(
    String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, AccountId accountId,
    SettlementEngineAccountId settlementEngineAccountId
  ) {
    super(message(message, accountId, settlementEngineAccountId), cause, enableSuppression, writableStackTrace);
    this.accountId = accountId;
    this.settlementEngineAccountId = settlementEngineAccountId;
  }

  private static final String message(
    String message, AccountId accountId, SettlementEngineAccountId settlementEngineAccountId
  ) {
    if (message != null) {
      return String.format(
        "accountId=%s settlementEngineAccountId=%s, message=%s",
        accountId.value(), settlementEngineAccountId.value(), message
      );
    } else {
      return String.format(
        "accountId=%s settlementEngineAccountId=%s",
        accountId.value(), settlementEngineAccountId.value()
      );
    }
  }

  public AccountId getAccountId() {
    return accountId;
  }

  public SettlementEngineAccountId getSettlementEngineAccountId() {
    return settlementEngineAccountId;
  }
}
