package com.sappenin.interledger.ilpv4.connector.server.spring.controllers;

import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerRejectPacket;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.function.Supplier;

import static org.interledger.connector.link.blast.BlastHeaders.ILP_OCTET_STREAM;

@ControllerAdvice
public class WebExceptionHandler {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Autowired
  Supplier<ConnectorSettings> connectorSettingsSupplier;

  /**
   * This exception is emitted if an invalid ILP prepare packet is sent into the `/ilp` endpoint to support ILP over
   * HTTP.
   *
   * @param e
   *
   * @return
   */
  @ExceptionHandler
  public ResponseEntity<InterledgerRejectPacket> handleHttpMessageNotReadableException(
    final HttpMessageNotReadableException e
  ) {
    logger.error(e.getMessage(), e);

    final InterledgerRejectPacket rejectPacket = InterledgerRejectPacket.builder()
      .code(InterledgerErrorCode.F00_BAD_REQUEST)
      .message("Invalid ILP Prepare Packet")
      .triggeredBy(connectorSettingsSupplier.get().getOperatorAddress())
      .build();

    return ResponseEntity.badRequest()
      .contentType(ILP_OCTET_STREAM)
      .body(rejectPacket);
  }

}
