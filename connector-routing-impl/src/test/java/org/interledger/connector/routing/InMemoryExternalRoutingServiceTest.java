package org.interledger.connector.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.connector.persistence.repositories.StaticRoutesRepository;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.crypto.Decryptor;

import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.Set;

public class InMemoryExternalRoutingServiceTest {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock
  private EventBus eventBus;
  @Mock
  private ConnectorSettings connectorSettingsSupplier;
  @Mock
  private Decryptor decryptor;
  @Mock
  private AccountSettingsRepository accountSettingsRepository;
  @Mock
  private StaticRoutesRepository staticRoutesRepository;
  @Mock
  private ChildAccountPaymentRouter childAccountPaymentRouter;
  @Mock
  private ForwardingRoutingTable<RouteUpdate> outgoingRoutingTable;
  @Mock
  private RouteBroadcaster routeBroadcaster;

  private InMemoryExternalRoutingService service;

  private StaticRoute shawn;

  private StaticRoute lassiter;

  @Before
  public void setUp() {
    service = new InMemoryExternalRoutingService(
        eventBus,
        () -> connectorSettingsSupplier,
        decryptor,
        accountSettingsRepository,
        staticRoutesRepository,
        childAccountPaymentRouter,
        outgoingRoutingTable,
        routeBroadcaster
    );

    shawn = StaticRoute.builder()
        .accountId(AccountId.of("shawnSpencer"))
        .addressPrefix(InterledgerAddressPrefix.of("g.psych"))
        .build();

    lassiter = StaticRoute.builder()
        .accountId(AccountId.of("carltonLassiter"))
        .addressPrefix(InterledgerAddressPrefix.of("g.sbpd"))
        .build();
  }

  @Test
  public void staticRoutes() {
    when(staticRoutesRepository.getAllStaticRoutes()).thenReturn(Collections.emptySet());
    assertThat(service.getAllStaticRoutes()).isEmpty();
    assertThat(service.getAllRoutes()).isEmpty();

    when(staticRoutesRepository.getAllStaticRoutes()).thenReturn(defaultRoutes());
    assertThat(service.getAllStaticRoutes()).isEqualTo(defaultRoutes());

  }


  private Set<StaticRoute> defaultRoutes() {
    return Sets.newHashSet(shawn, lassiter);
  }
}
