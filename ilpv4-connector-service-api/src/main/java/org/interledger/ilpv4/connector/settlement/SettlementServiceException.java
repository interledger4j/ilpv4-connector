package org.interledger.ilpv4.connector.settlement;

import com.sappenin.interledger.ilpv4.connector.settlement.SettlementService;
import org.interledger.connector.accounts.AccountId;

import java.util.Objects;

/**
 * Thrown by instances of {@link SettlementService}, such as when the Settlement service cannot communicate with a
 * particular settlement engine.
 */
public class SettlementServiceException extends RuntimeException {

  /**
   * The {@link AccountId} of the account that threw an exception while communicating to a Settlment Engine.
   */
  private final AccountId accountId;

  /**
   * Constructs a new runtime exception with {@code null} as its detail message.  The cause is not initialized, and may
   * subsequently be initialized by a call to {@link #initCause}.
   *
   * @param accountId
   */
  public SettlementServiceException(AccountId accountId) {
    super(message(null, accountId));
    this.accountId = Objects.requireNonNull(accountId);
  }

  /**
   * Constructs a new runtime exception with the specified detail message. The cause is not initialized, and may
   * subsequently be initialized by a call to {@link #initCause}.
   *
   * @param message   the detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
   *                  method.
   * @param accountId
   */
  public SettlementServiceException(String message, AccountId accountId) {
    super(message(message, accountId));
    this.accountId = Objects.requireNonNull(accountId);
  }

  /**
   * Constructs a new runtime exception with the specified detail message and cause.  <p>Note that the detail message
   * associated with {@code cause} is <i>not</i> automatically incorporated in this runtime exception's detail message.
   *
   * @param message   the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
   * @param cause     the cause (which is saved for later retrieval by the {@link #getCause()} method).  (A
   *                  <tt>null</tt> value is permitted, and indicates that the cause is nonexistent or unknown.)
   * @param accountId
   *
   * @since 1.4
   */
  public SettlementServiceException(String message, Throwable cause, AccountId accountId) {
    super(message(message, accountId), cause);
    this.accountId = Objects.requireNonNull(accountId);
  }

  /**
   * Constructs a new runtime exception with the specified cause and a detail message of <tt>(cause==null ? null :
   * cause.toString())</tt> (which typically contains the class and detail message of
   * <tt>cause</tt>).  This constructor is useful for runtime exceptions
   * that are little more than wrappers for other throwables.
   *
   * @param cause     the cause (which is saved for later retrieval by the {@link #getCause()} method).  (A
   *                  <tt>null</tt> value is permitted, and indicates that the cause is nonexistent or unknown.)
   * @param accountId
   *
   * @since 1.4
   */
  public SettlementServiceException(Throwable cause, AccountId accountId) {
    this(message(null, accountId), cause, accountId);
  }

  /**
   * Constructs a new runtime exception with the specified detail message, cause, suppression enabled or disabled, and
   * writable stack trace enabled or disabled.
   *
   * @param message            the detail message.
   * @param cause              the cause.  (A {@code null} value is permitted, and indicates that the cause is
   *                           nonexistent or unknown.)
   * @param enableSuppression  whether or not suppression is enabled or disabled
   * @param writableStackTrace whether or not the stack trace should be writable
   * @param accountId
   *
   * @since 1.7
   */
  protected SettlementServiceException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, AccountId accountId) {
    super(message(message, accountId), cause, enableSuppression, writableStackTrace);
    this.accountId = accountId;
  }

  private static final String message(String message, AccountId accountId) {
    if (message != null) {
      return String.format("AccountId=%s message=%s", accountId, message);
    } else {
      return String.format("AccountId=%s", accountId);
    }
  }

  public AccountId getAccountId() {
    return accountId;
  }
}
