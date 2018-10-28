package com.sappenin.ilpv4.plugins.btp.subprotocols;

import com.sappenin.ilpv4.plugins.btp.BtpSession;
import org.interledger.btp.BtpMessage;
import org.interledger.btp.BtpRuntimeException;
import org.interledger.btp.BtpSubProtocol;

import java.util.concurrent.CompletableFuture;

/**
 * Handles sub-protocol data from a BTP connection. Sub-protocol data is contained in the data portion of a particular
 * {@link BtpSubProtocol} found in {@link BtpMessage#getSubProtocols()}.
 *
 * @deprecated Replace with the quilt variant.
 */
@Deprecated
public abstract class BtpSubProtocolHandler {

  /**
   * Handle a primary {@link BtpSubProtocol} whose data payload should be treated as binary data.
   *
   * @param btpSession         A {@link BtpSession} with information about the current BTP Session.
   * @param incomingBtpMessage A {@link BtpMessage} that contains the data for the BTP sub-protocol to be handled.
   *
   * @return A {@link BtpSubProtocol} containing properly encoded response data from this handler. Note that per BTP,
   * only primary sub-protocols should send a response. If a secondary sub-protocol needs a response, a separate BTP
   * message should be used.
   *
   * @throws BtpRuntimeException If anything goes wrong at the BTP level.
   */
  public abstract CompletableFuture<BtpSubProtocol> handleBinaryMessage(BtpSession btpSession, BtpMessage incomingBtpMessage) throws BtpRuntimeException;

  /**
   * Handle a primary {@link BtpSubProtocol} whose data payload should be treated as UTF-8 String data.
   *
   * @param btpSession         A {@link BtpSession} with information about the current BTP Session.
   * @param incomingBtpMessage A {@link BtpMessage} that contains the data for the BTP sub-protocol to be handled.
   *
   * @return A {@link BtpSubProtocol} containing properly encoded response data from this handler.
   *
   * @throws BtpRuntimeException If anything goes wrong at the BTP level.
   */
  public CompletableFuture<BtpSubProtocol> handleTextMessage(final BtpSession btpSession, final BtpMessage incomingBtpMessage) throws BtpRuntimeException {
    throw new RuntimeException("Text data handling is not yet implemented!");
  }

  /**
   * Handle a primary {@link BtpSubProtocol} whose data payload should be treated as JSON data.
   *
   * @param btpSession         A {@link BtpSession} with information about the current BTP Session.
   * @param incomingBtpMessage A {@link BtpMessage} that contains the data for the BTP sub-protocol to be handled.
   *
   * @return A {@link BtpSubProtocol} containing properly encoded response data from this handler.
   *
   * @throws BtpRuntimeException If anything goes wrong at the BTP level.
   */
  public CompletableFuture<BtpSubProtocol> handleJsonMessage(final BtpSession btpSession, final BtpMessage incomingBtpMessage) throws BtpRuntimeException {
    throw new RuntimeException("JSON data handling is not yet implemented!");
  }

}
