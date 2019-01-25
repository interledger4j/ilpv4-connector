package com.sappenin.interledger.ilpv4.connector.routing;

import com.sappenin.interledger.ilpv4.connector.accounts.AccountIdResolver;
import com.sappenin.interledger.ilpv4.connector.accounts.AccountManager;
import com.sappenin.interledger.ilpv4.connector.ccp.codecs.CcpCodecs;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import com.sappenin.interledger.ilpv4.connector.settings.ImmutableConnectorSettings;
import org.interledger.core.asn.framework.InterledgerCodecContextFactory;
import org.interledger.encoding.asn.framework.CodecContext;
import org.junit.Before;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Supplier;

/**
 * Unit tests for {@link InMemoryExternalRoutingService}.
 */
public class InMemoryExternalRoutingServiceTest {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  @Mock
  private AccountManager accountManagerMock;

  @Mock
  private AccountIdResolver accountIdResolverMock;

  private CodecContext codecContext;
  private Supplier<ConnectorSettings> connectorSettingsSupplier;
  private RoutingTable<Route> localRoutingTable;
  private ForwardingRoutingTable<IncomingRoute> incomingRoutingTable;
  private ForwardingRoutingTable<RouteUpdate> outgoingRoutingTable;

  private ExternalRoutingService externalRoutingService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    this.connectorSettingsSupplier = () -> ImmutableConnectorSettings.builder().build();
    this.localRoutingTable = new InMemoryRoutingTable();
    this.incomingRoutingTable = new InMemoryIncomingRouteForwardRoutingTable();
    this.outgoingRoutingTable = new InMemoryRouteUpdateForwardRoutingTable();

    this.codecContext = CcpCodecs.register(InterledgerCodecContextFactory.oer());

    this.externalRoutingService = new InMemoryExternalRoutingService(
      codecContext,
      connectorSettingsSupplier,
      localRoutingTable,
      incomingRoutingTable,
      outgoingRoutingTable,
      accountManagerMock,
      accountIdResolverMock
    );
  }

  // TODO: See tests in route-broadcaster.test.js

}