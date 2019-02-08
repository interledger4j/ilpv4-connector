package com.sappenin.interledger.ilpv4.connector.server.spring.controllers;

import com.sappenin.interledger.ilpv4.connector.AccountId;
import com.sappenin.interledger.ilpv4.connector.accounts.AccountIdResolver;
import com.sappenin.interledger.ilpv4.connector.accounts.BlastAccountIdResolver;
import com.sappenin.interledger.ilpv4.connector.packetswitch.ILPv4PacketSwitch;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import org.interledger.core.InterledgerErrorCode;
import org.interledger.core.InterledgerFulfillPacket;
import org.interledger.core.InterledgerPreparePacket;
import org.interledger.core.InterledgerRejectPacket;
import org.interledger.core.InterledgerResponsePacket;
import org.interledger.core.InterledgerResponsePacketMapper;
import org.interledger.plugin.lpiv2.btp2.spring.factories.PluginFactoryProvider;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;
import org.zalando.problem.spring.common.MediaTypes;

import java.security.Principal;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.function.Supplier;

import static org.interledger.lpiv2.blast.BlastHeaders.ILP_OCTET_STREAM_VALUE;

/**
 * A RESTful controller for handling ILP over HTTP request/response payloads.
 *
 * TODO: Fix this ULR once spec if finalized.
 *
 * @see "https://github.com/interledger/rfcs/blob/30729b78fb554589f2e5202a8dfb7344b21090a0/0000-ilp-over-http.md"
 */
@RestController
public class IlpHttpController {

  public static final String ILP_PATH = "/ilp";

  //  public static final String ILP_OCTET_STREAM = "application/ilp+octet-stream";
  //  public static final String ILP_HEADER_OCTET_STREAM = "application/ilp-header+octet-stream";

  private final Supplier<ConnectorSettings> connectorSettingsSupplier;
  private final BlastAccountIdResolver accountIdResolver;
  private final PluginFactoryProvider pluginFactoryProvider;
  private final ILPv4PacketSwitch ilPv4PacketSwitch;

  public IlpHttpController(
    final Supplier<ConnectorSettings> connectorSettingsSupplier,
    final BlastAccountIdResolver accountIdResolver,
    final PluginFactoryProvider pluginFactoryProvider,
    final ILPv4PacketSwitch ilPv4PacketSwitch
  ) {
    this.connectorSettingsSupplier = Objects.requireNonNull(connectorSettingsSupplier);
    this.accountIdResolver = Objects.requireNonNull(accountIdResolver);
    this.pluginFactoryProvider = Objects.requireNonNull(pluginFactoryProvider);
    this.ilPv4PacketSwitch = Objects.requireNonNull(ilPv4PacketSwitch);
  }

  /**
   * This handler conforms to the RFC, accepting an OER-encoded payload in the body of the request. The {@code
   * accountId} is found via the {@link AccountIdResolver}.
   *
   * @param preparePacket An {@link InterledgerPreparePacket} containing information about an ILP `sendData` request.
   *
   * @return All ILP Packets MUST be returned with the HTTP status code 200: OK. An endpoint MAY return standard HTTP
   * errors, including but not limited to: a malformed or unauthenticated request, rate limiting, or an unresponsive
   * upstream service. Connectors SHOULD either retry the request, if applicable, or relay an ILP Reject packet back to
   * the original sender with an appropriate Final or Temporary error code.
   */
  @RequestMapping(
    value = ILP_PATH, method = {RequestMethod.POST},
    produces = {ILP_OCTET_STREAM_VALUE},
    consumes = {ILP_OCTET_STREAM_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public InterledgerResponsePacket sendData(
    final Principal principal, @RequestBody final InterledgerPreparePacket preparePacket
  ) {

    final AccountId accountId = this.accountIdResolver.resolveAccountId(principal);
    CompletableFuture<Optional<InterledgerResponsePacket>> response =
      this.ilPv4PacketSwitch.routeData(accountId, preparePacket);

    return response
      // If an error response...
      .exceptionally((error) -> Optional.of(InterledgerRejectPacket.builder()
        .triggeredBy(connectorSettingsSupplier.get().getOperatorAddress())
        .code(InterledgerErrorCode.R02_INSUFFICIENT_TIMEOUT)
        .build()))
      // If valid response...
      .thenApply(responsePacket -> new InterledgerResponsePacketMapper<InterledgerResponsePacket>() {
        @Override
        protected InterledgerResponsePacket mapFulfillPacket(InterledgerFulfillPacket interledgerFulfillPacket) {
          return interledgerFulfillPacket;
        }

        @Override
        protected InterledgerResponsePacket mapRejectPacket(InterledgerRejectPacket interledgerRejectPacket) {
          return interledgerRejectPacket;
        }

        @Override
        protected InterledgerResponsePacket mapExpiredPacket() {
          return InterledgerRejectPacket.builder()
            .triggeredBy(connectorSettingsSupplier.get().getOperatorAddress())
            .code(InterledgerErrorCode.R02_INSUFFICIENT_TIMEOUT)
            .build();
        }
      }.map(responsePacket)).join();
  }

  @RequestMapping(
    value = ILP_PATH, method = {RequestMethod.HEAD},
    produces = {ILP_OCTET_STREAM_VALUE},
    consumes = {ILP_OCTET_STREAM_VALUE, MediaType.APPLICATION_OCTET_STREAM_VALUE, MediaTypes.PROBLEM_VALUE}
  )
  public void headData() {
  }
}