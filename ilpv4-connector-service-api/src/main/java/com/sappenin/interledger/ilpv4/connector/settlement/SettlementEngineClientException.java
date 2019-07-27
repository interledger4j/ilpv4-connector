package com.sappenin.interledger.ilpv4.connector.settlement;

import org.interledger.connector.accounts.AccountId;

/**
 * Thrown by instances of {@link SettlementEngineClient}.
 */
public class SettlementEngineClientException extends RuntimeException {

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
  public SettlementEngineClientException(AccountId accountId) {
    super();
    this.accountId = accountId;
  }

  /**
   * Constructs a new runtime exception with the specified detail message. The cause is not initialized, and may
   * subsequently be initialized by a call to {@link #initCause}.
   *
   * @param message   the detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
   *                  method.
   * @param accountId
   */
  public SettlementEngineClientException(String message, AccountId accountId) {
    super(message);
    this.accountId = accountId;
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
  public SettlementEngineClientException(String message, Throwable cause, AccountId accountId) {
    super(message, cause);
    this.accountId = accountId;
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
  public SettlementEngineClientException(Throwable cause, AccountId accountId) {
    super(cause);
    this.accountId = accountId;
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
  protected SettlementEngineClientException(String message, Throwable cause, boolean enableSuppression, boolean writableStackTrace, AccountId accountId) {
    super(message, cause, enableSuppression, writableStackTrace);
    this.accountId = accountId;
  }

  public AccountId getAccountId() {
    return accountId;
  }
}
