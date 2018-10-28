package com.sappenin.ilpv4.client.exceptions;

import org.interledger.core.InterledgerAddress;

/**
 * A root exception for all exceptions relating to ILPv4A Plugins.
 */
public class ClientException extends RuntimeException {

  /**
   * The account-address of the plugin that threw this exception.
   */
  private final InterledgerAddress pluginAccount;

  /**
   * Constructs a new runtime exception with {@code null} as its detail message.  The cause is not initialized, and may
   * subsequently be initialized by a call to {@link #initCause}.
   */
  public ClientException(InterledgerAddress pluginAccount) {
    this.pluginAccount = pluginAccount;
  }

  /**
   * Constructs a new runtime exception with the specified detail message. The cause is not initialized, and may
   * subsequently be initialized by a call to {@link #initCause}.
   *
   * @param message the detail message. The detail message is saved for later retrieval by the {@link #getMessage()}
   *                method.
   */
  public ClientException(String message, InterledgerAddress pluginAccount) {
    super(message);
    this.pluginAccount = pluginAccount;
  }

  /**
   * Constructs a new runtime exception with the specified detail message and cause.  <p>Note that the detail message
   * associated with {@code cause} is <i>not</i> automatically incorporated in this runtime exception's detail message.
   *
   * @param message the detail message (which is saved for later retrieval by the {@link #getMessage()} method).
   * @param cause   the cause (which is saved for later retrieval by the {@link #getCause()} method).  (A
   *                <tt>null</tt> value is permitted, and indicates that the cause is nonexistent or unknown.)
   *
   * @since 1.4
   */
  public ClientException(String message, Throwable cause,
                         InterledgerAddress pluginAccount) {
    super(message, cause);
    this.pluginAccount = pluginAccount;
  }

  /**
   * Constructs a new runtime exception with the specified cause and a detail message of
   * <tt>(cause==null ? null : cause.toString())</tt> (which typically contains the class and detail
   * message of <tt>cause</tt>).  This constructor is useful for runtime exceptions that are little more than wrappers
   * for other throwables.
   *
   * @param cause the cause (which is saved for later retrieval by the {@link #getCause()} method). (A <tt>null</tt>
   *              value is permitted, and indicates that the cause is nonexistent or unknown.)
   *
   * @since 1.4
   */
  public ClientException(Throwable cause, InterledgerAddress pluginAccount) {
    super(cause);
    this.pluginAccount = pluginAccount;
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
   *
   * @since 1.7
   */
  public ClientException(String message, Throwable cause, boolean enableSuppression,
                         boolean writableStackTrace, InterledgerAddress pluginAccount) {
    super(message, cause, enableSuppression, writableStackTrace);
    this.pluginAccount = pluginAccount;
  }

  public InterledgerAddress getPluginAccount() {
    return pluginAccount;
  }
}
