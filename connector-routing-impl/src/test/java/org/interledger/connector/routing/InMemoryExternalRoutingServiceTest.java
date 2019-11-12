package org.interledger.connector.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.connector.persistence.repositories.StaticRoutesRepository;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.settings.GlobalRoutingSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.crypto.Decryptor;
import org.interledger.crypto.EncryptedSecret;

import com.google.common.collect.Sets;
import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;

import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Supplier;

public class InMemoryExternalRoutingServiceTest {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Mock
  private EventBus eventBus;
  @Mock
  private ConnectorSettings connectorSettings;
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
  @Mock
  private GlobalRoutingSettings globalRoutingSettings;

  private final String encryptedSecret = EncryptedSecret.ENCODING_PREFIX +
      ":GCPKMS:KR1:Foo_password:1:GS:VGhpcyBpcyBhIHRoZSBzZWNyZXQ=";

  private InMemoryExternalRoutingService service;

  private StaticRoute shawn;

  private StaticRoute lassiter;

  private StaticRoute woody;

  @Before
  public void setUp() {
    Supplier<ConnectorSettings> connectorSettingsSupplier = () -> connectorSettings;
    service = new InMemoryExternalRoutingService(
        eventBus,
        connectorSettingsSupplier,
        decryptor,
        accountSettingsRepository,
        staticRoutesRepository,
        childAccountPaymentRouter,
        outgoingRoutingTable,
        routeBroadcaster
    );

    when(connectorSettings.globalRoutingSettings()).thenReturn(globalRoutingSettings);

    shawn = StaticRoute.builder()
        .accountId(AccountId.of("shawnSpencer"))
        .addressPrefix(InterledgerAddressPrefix.of("g.psych"))
        .build();

    lassiter = StaticRoute.builder()
        .accountId(AccountId.of("carltonLassiter"))
        .addressPrefix(InterledgerAddressPrefix.of("g.sbpd"))
        .build();

    woody = StaticRoute.builder()
        .accountId(AccountId.of("woody"))
        .addressPrefix(InterledgerAddressPrefix.of("g.sbpd.morgue"))
        .build();
  }

  @Test
  public void staticRoutes() {
    when(staticRoutesRepository.getAllStaticRoutes()).thenReturn(Collections.emptySet());
    assertThat(service.getAllStaticRoutes()).isEmpty();
    assertThat(service.getAllRoutes()).isEmpty();

    when(staticRoutesRepository.getAllStaticRoutes()).thenReturn(defaultRoutes());
    assertThat(service.getAllStaticRoutes()).isEqualTo(defaultRoutes());

    when(globalRoutingSettings.isUseParentForDefaultRoute()).thenReturn(false);
    when(globalRoutingSettings.defaultRoute()).thenReturn(Optional.empty());
    when(globalRoutingSettings.routingSecret()).thenReturn(encryptedSecret);
    when(connectorSettings.operatorAddress()).thenReturn(InterledgerAddress.of("test.example"));
    when(accountSettingsRepository.findByAccountRelationshipIsWithConversion(AccountRelationship.PEER))
        .thenReturn(Collections.emptyList());
    when(routeBroadcaster.registerCcpEnabledAccount((AccountId) any())).thenReturn(Optional.empty());
    when(decryptor.decrypt(any())).thenReturn(new byte[32]);


    service.start();
    assertThat(service.getAllRoutes())
        .extracting("nextHopAccountId", "routePrefix")
        .containsOnly(
            tuple(shawn.accountId(), shawn.addressPrefix()),
            tuple(lassiter.accountId(), lassiter.addressPrefix())
        );
    verify(routeBroadcaster, times(1)).registerCcpEnabledAccount(shawn.accountId());
    verify(routeBroadcaster, times(1)).registerCcpEnabledAccount(lassiter.accountId());

    when(staticRoutesRepository.saveStaticRoute(woody)).thenReturn(woody);
    when(staticRoutesRepository.getAllStaticRoutes()).thenReturn(Sets.newHashSet(shawn, lassiter, woody));
    service.createStaticRoute(woody);
    assertThat(service.getAllRoutes())
        .extracting("nextHopAccountId", "routePrefix")
        .containsOnly(
            tuple(shawn.accountId(), shawn.addressPrefix()),
            tuple(woody.accountId(), woody.addressPrefix()),
            tuple(lassiter.accountId(), lassiter.addressPrefix())
        );
    verify(routeBroadcaster, times(1)).registerCcpEnabledAccount(woody.accountId());
    verify(staticRoutesRepository, times(1)).saveStaticRoute(woody);

    when(staticRoutesRepository.deleteStaticRoute(shawn.addressPrefix())).thenReturn(true);
    service.deleteStaticRouteByPrefix(shawn.addressPrefix());
    assertThat(service.getAllRoutes())
        .extracting("nextHopAccountId", "routePrefix")
        .containsOnly(
            tuple(woody.accountId(), woody.addressPrefix()),
            tuple(lassiter.accountId(), lassiter.addressPrefix())
        );
    verify(staticRoutesRepository, times(1)).deleteStaticRoute(shawn.addressPrefix());
  }

  private Set<StaticRoute> defaultRoutes() {
    return Sets.newHashSet(shawn, lassiter);
  }
}
