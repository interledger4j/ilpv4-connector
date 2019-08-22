package org.interledger.connector.server.spring.controllers;

import org.interledger.connector.accounts.AccountIdResolver;
import org.interledger.connector.accounts.BlastAccountIdResolver;
import org.interledger.connector.packetswitch.ILPv4PacketSwitch;
import org.interledger.connector.accounts.AccountId;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerResponsePacket;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.spring.common.MediaTypes;

import java.util.Objects;

import static org.interledger.connector.server.spring.settings.properties.ConnectorProperties.BLAST_ENABLED;
import static org.interledger.connector.server.spring.settings.properties.ConnectorProperties.ENABLED_PROTOCOLS;
import static org.interledger.connector.link.blast.BlastHeaders.APPLICATION_ILP_OCTET_STREAM_VALUE;

/**
 * A RESTful controller for handling ILP over HTTP request/response payloads.
 *
 * @see "https://github.com/interledger/rfcs/blob/master/0035-ilp-over-http/0035-ilp-over-http.md"
 */
@RestController
@ConditionalOnProperty(prefix = ENABLED_PROTOCOLS, name = BLAST_ENABLED, havingValue = "true")
public class IlpHttpController {

  public static final String ILP_PATH = "/ilp";

  private final BlastAccountIdResolver accountIdResolver;
  private final ILPv4PacketSwitch ilPv4PacketSwitch;

  public IlpHttpController(final BlastAccountIdResolver accountIdResolver, final ILPv4PacketSwitch ilPv4PacketSwitch) {
    this.accountIdResolver = Objects.requireNonNull(accountIdResolver);
    this.ilPv4PacketSwitch = Objects.requireNonNull(ilPv4PacketSwitch);
  }

  /**
   * This handler conforms to the RFC, accepting an OER-encoded payload in the body of the request. The {@code
   * accountId} is found via the {@link AccountIdResolver}.
   *
   * @param preparePacket An {@link InterledgerPreparePacket} containing information about an ILP `sendPacket` request.
   *
   * @return All ILP Packets MUST be returned with the HTTP status code 200: OK. An endpoint MAY return standard HTTP
   * errors, including but not limited to: a malformed or unauthenticated request, rate limiting, or an unresponsive
   * upstream service. Connectors SHOULD either retry the request, if applicable, or relay an ILP Reject packet back to
   * the original sender with an appropriate Final or Temporary error code.
   */
  @RequestMapping(
    value = ILP_PATH, method = {RequestMethod.POST},
    produces = {APPLICATION_ILP_OCTET_STREAM_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaTypes.PROBLEM_VALUE},
    consumes = {APPLICATION_ILP_OCTET_STREAM_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE}
  )
  public InterledgerResponsePacket sendData(
    Authentication authentication, @RequestBody final InterledgerPreparePacket preparePacket
  ) {
    final AccountId accountId = this.accountIdResolver.resolveAccountId(authentication);

    return this.ilPv4PacketSwitch.switchPacket(accountId, preparePacket);
  }

}
