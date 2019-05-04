package com.sappenin.interledger.ilpv4.connector.routing;

import com.google.common.eventbus.EventBus;
import com.sappenin.interledger.ilpv4.connector.accounts.AccountIdResolver;
import com.sappenin.interledger.ilpv4.connector.links.LinkManager;
import com.sappenin.interledger.ilpv4.connector.ccp.codecs.CcpCodecContextFactory;
import com.sappenin.interledger.ilpv4.connector.settings.ConnectorSettings;
import com.sappenin.interledger.ilpv4.connector.settings.ImmutableConnectorSettings;
import org.interledger.core.asn.framework.InterledgerCodecContextFactory;
import org.interledger.encoding.asn.framework.CodecContext;
import org.interledger.ilpv4.connector.persistence.repositories.AccountSettingsRepository;
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
  private AccountSettingsRepository accountSettingsRepositoryMock;

  @Mock
  private LinkManager linkManagerMock;

  @Mock
  private AccountIdResolver accountIdResolverMock;

  private CodecContext codecContext;
  private Supplier<ConnectorSettings> connectorSettingsSupplier;
  private EventBus eventBus;
  private RoutingTable<Route> localRoutingTable;
  private ForwardingRoutingTable<IncomingRoute> incomingRoutingTable;
  private ForwardingRoutingTable<RouteUpdate> outgoingRoutingTable;

  private ExternalRoutingService externalRoutingService;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);

    this.connectorSettingsSupplier = () -> ImmutableConnectorSettings.builder().build();
    this.localRoutingTable = new InMemoryRoutingTable();
    this.eventBus = new EventBus();
    this.incomingRoutingTable = new InMemoryIncomingRouteForwardRoutingTable();
    this.outgoingRoutingTable = new InMemoryRouteUpdateForwardRoutingTable();

    this.codecContext = CcpCodecContextFactory.register(InterledgerCodecContextFactory.oer());

    this.externalRoutingService = new InMemoryExternalRoutingService(
      eventBus,
      codecContext,
      connectorSettingsSupplier,
      localRoutingTable,
      incomingRoutingTable,
      outgoingRoutingTable,
      accountIdResolverMock,
      accountSettingsRepositoryMock,
      linkManagerMock
    );
  }

  // TODO: See tests in route-broadcaster.test.js

}