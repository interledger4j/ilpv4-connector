package org.interledger.connector.link.blast;

import org.springframework.http.MediaType;

/**
 * Common headers used in the BLAST protocol.
 */
public interface BlastHeaders {

  String APPLICATION_ILP_OCTET_STREAM_VALUE = "application/ilp+octet-stream";
  String APPLICATION_ILP_HEADER_OCTET_STREAM_VALUE = "application/ilp-header+octet-stream";
  MediaType ILP_OCTET_STREAM = MediaType.valueOf(APPLICATION_ILP_OCTET_STREAM_VALUE);
  MediaType ILP_HEADER_OCTET_STREAM = MediaType.valueOf(APPLICATION_ILP_HEADER_OCTET_STREAM_VALUE);

  String ILP_OPERATOR_ADDRESS_VALUE = "ILP-Operator-Address";
}
