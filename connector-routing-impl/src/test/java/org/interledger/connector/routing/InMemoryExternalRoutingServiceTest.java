package org.interledger.connector.routing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.persistence.repositories.AccountSettingsRepository;
import org.interledger.connector.persistence.repositories.StaticRoutesRepository;
import org.interledger.connector.settings.ConnectorSettings;
import org.interledger.connector.settings.GlobalRoutingSettings;
import org.interledger.core.InterledgerAddress;
import org.interledger.core.InterledgerAddressPrefix;
import org.interledger.crypto.Decryptor;
import org.interledger.crypto.EncryptedSecret;
import org.interledger.link.LinkType;

import com.google.common.collect.Lists;
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
        .nextHopAccountId(AccountId.of("shawnSpencer"))
        .routePrefix(InterledgerAddressPrefix.of("g.psych"))
        .build();

    lassiter = StaticRoute.builder()
        .nextHopAccountId(AccountId.of("carltonLassiter"))
        .routePrefix(InterledgerAddressPrefix.of("g.sbpd"))
        .build();

    woody = StaticRoute.builder()
        .nextHopAccountId(AccountId.of("woody"))
        .routePrefix(InterledgerAddressPrefix.of("g.sbpd.morgue"))
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
            tuple(shawn.nextHopAccountId(), shawn.routePrefix()),
            tuple(lassiter.nextHopAccountId(), lassiter.routePrefix())
        );
    verify(routeBroadcaster, times(1)).registerCcpEnabledAccount(shawn.nextHopAccountId());
    verify(routeBroadcaster, times(1)).registerCcpEnabledAccount(lassiter.nextHopAccountId());

    when(staticRoutesRepository.saveStaticRoute(woody)).thenReturn(woody);
    when(staticRoutesRepository.getAllStaticRoutes()).thenReturn(Sets.newHashSet(shawn, lassiter, woody));
    service.createStaticRoute(woody);
    assertThat(service.getAllRoutes())
        .extracting("nextHopAccountId", "routePrefix")
        .containsOnly(
            tuple(shawn.nextHopAccountId(), shawn.routePrefix()),
            tuple(woody.nextHopAccountId(), woody.routePrefix()),
            tuple(lassiter.nextHopAccountId(), lassiter.routePrefix())
        );
    verify(routeBroadcaster, times(1)).registerCcpEnabledAccount(woody.nextHopAccountId());
    verify(staticRoutesRepository, times(1)).saveStaticRoute(woody);

    when(staticRoutesRepository.deleteStaticRouteByPrefix(shawn.routePrefix())).thenReturn(true);
    service.deleteStaticRouteByPrefix(shawn.routePrefix());
    assertThat(service.getAllRoutes())
        .extracting("nextHopAccountId", "routePrefix")
        .containsOnly(
            tuple(woody.nextHopAccountId(), woody.routePrefix()),
            tuple(lassiter.nextHopAccountId(), lassiter.routePrefix())
        );
    verify(staticRoutesRepository, times(1)).deleteStaticRouteByPrefix(shawn.routePrefix());
  }

  @Test
  public void peersAndParentsGetsAutoRegisteredForCcp() {

    LinkType ilpoverhttp = LinkType.of("ILPOVERHTTP");
    AccountSettings peer = AccountSettings.builder()
      .assetCode("XRP")
      .assetScale(9)
      .linkType(ilpoverhttp)
      .accountId(AccountId.of("peer"))
      .accountRelationship(AccountRelationship.PEER)
      .build();

    AccountSettings child = AccountSettings.builder()
      .assetCode("XRP")
      .assetScale(9)
      .linkType(ilpoverhttp)
      .accountId(AccountId.of("child"))
      .accountRelationship(AccountRelationship.CHILD)
      .build();

    AccountSettings parent = AccountSettings.builder()
      .assetCode("XRP")
      .assetScale(9)
      .linkType(ilpoverhttp)
      .accountId(AccountId.of("parent"))
      .accountRelationship(AccountRelationship.PARENT)
      .build();

    when(accountSettingsRepository.findByAccountRelationshipIsWithConversion(AccountRelationship.PEER))
      .thenReturn(Lists.newArrayList(peer));
    when(accountSettingsRepository.findByAccountRelationshipIsWithConversion(AccountRelationship.PARENT))
      .thenReturn(Lists.newArrayList(parent));

    service.start();
    verify(routeBroadcaster, times(1)).registerCcpEnabledAccount(peer);
    verify(routeBroadcaster, times(1)).registerCcpEnabledAccount(parent);
    verify(routeBroadcaster, times(0)).registerCcpEnabledAccount(child);
  }

  @Test
  public void misconfiguredAccountsAreGracefullyHandled() {

    LinkType ilpoverhttp = LinkType.of("ILPOVERHTTP");
    AccountSettings goodPeer = AccountSettings.builder()
      .assetCode("XRP")
      .assetScale(9)
      .linkType(ilpoverhttp)
      .accountId(AccountId.of("good_peer"))
      .accountRelationship(AccountRelationship.PEER)
      .build();

    AccountSettings badPeer = AccountSettings.builder()
      .assetCode("XRP")
      .assetScale(9)
      .linkType(ilpoverhttp)
      .accountId(AccountId.of("bad_peer"))
      .accountRelationship(AccountRelationship.PEER)
      .build();


    when(accountSettingsRepository.findByAccountRelationshipIsWithConversion(AccountRelationship.PEER))
      .thenReturn(Lists.newArrayList(badPeer, goodPeer));

    when(routeBroadcaster.registerCcpEnabledAccount(badPeer)).thenThrow(new RuntimeException("fake exception!"));
    when(routeBroadcaster.registerCcpEnabledAccount(goodPeer)).thenReturn(Optional.empty());

    service.start();
    verify(routeBroadcaster, times(1)).registerCcpEnabledAccount(badPeer);
    verify(routeBroadcaster, times(1)).registerCcpEnabledAccount(goodPeer);
  }


  private Set<StaticRoute> defaultRoutes() {
    return Sets.newHashSet(shawn, lassiter);
  }
}
