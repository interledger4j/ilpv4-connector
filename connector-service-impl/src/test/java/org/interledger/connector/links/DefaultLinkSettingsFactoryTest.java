package org.interledger.connector.links;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.stream.TrackingStreamReceiverLink;
import org.interledger.connector.stream.TrackingStreamReceiverLinkSettings;
import org.interledger.link.LinkSettings;
import org.interledger.link.LinkType;
import org.interledger.link.LoopbackLink;
import org.interledger.link.PingLoopbackLink;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.OutgoingLinkSettings;

import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.Map;

/**
 * Unit tests for {@link DefaultLinkSettingsFactory}.
 */
public class DefaultLinkSettingsFactoryTest {

  protected static final String ENCRYPTED_SHH
    = "enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADKZPmASojt1iayb2bPy4D-Toq7TGLTN95HzCQAeJtz0=";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  DefaultLinkSettingsFactory factory;

  @Before
  public void setUp() {
    this.factory = new DefaultLinkSettingsFactory();
  }

  @Test
  public void constructUnsupportedLink() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage("Unsupported LinkType: LinkType(FOO)");
    factory.construct(
      AccountSettings.builder()
        .accountId(AccountId.of("foo"))
        .linkType(LinkType.of("foo"))
        .accountRelationship(AccountRelationship.PEER)
        .assetCode("XRP")
        .assetScale(2)
        .build()
    );
  }

  @Test
  public void constructIlpOverHttpLink() {
    final Map<String, Object> customSettings = Maps.newHashMap();

    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, "JWT_HS_256");
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_ISSUER, "https://alice.example.com/");
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_AUDIENCE, "https://connie.example.com/");
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_SUBJECT, "connie");
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_SHARED_SECRET, ENCRYPTED_SHH);
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_AUTH_TYPE, "JWT_HS_256");
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_ISSUER, "https://connie.example.com/");
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_AUDIENCE, "https://alice.example.com/");
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_SUBJECT, "connie");
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_SHARED_SECRET, ENCRYPTED_SHH);
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_URL, "https://alice.example.com");

    AccountSettings accountSettings = AccountSettings.builder()
      .accountId(AccountId.of("foo"))
      .linkType(IlpOverHttpLink.LINK_TYPE)
      .accountRelationship(AccountRelationship.PEER)
      .assetCode("XRP")
      .assetScale(2)
      .customSettings(customSettings)
      .build();
    final LinkSettings actual = factory.construct(accountSettings);
    assertThat(actual.getLinkType()).isEqualTo(IlpOverHttpLink.LINK_TYPE);
    assertThat(actual.getCustomSettings()).isEqualTo(customSettings);
  }

  @Test
  public void constructLoopLink() {
    AccountSettings accountSettings = AccountSettings.builder()
      .accountId(AccountId.of("foo"))
      .linkType(LoopbackLink.LINK_TYPE)
      .accountRelationship(AccountRelationship.PEER)
      .assetCode("XRP")
      .assetScale(2)
      .build();
    final LinkSettings actual = factory.construct(accountSettings);
    assertThat(actual.getLinkType()).isEqualTo(LoopbackLink.LINK_TYPE);
    assertThat(actual.getCustomSettings().isEmpty()).isTrue();
  }

  @Test
  public void testConstructUnidirectionalPingLink() {
    AccountSettings accountSettings = AccountSettings.builder()
      .accountId(AccountId.of("foo"))
      .linkType(PingLoopbackLink.LINK_TYPE)
      .accountRelationship(AccountRelationship.PEER)
      .assetCode("XRP")
      .assetScale(2)
      .build();
    final LinkSettings actual = factory.construct(accountSettings);
    assertThat(actual.getLinkType()).isEqualTo(PingLoopbackLink.LINK_TYPE);
    assertThat(actual.getCustomSettings().isEmpty()).isTrue();
  }

  @Test
  public void testConstructTrackingStreamReceiverLink() {
    AccountSettings accountSettings = AccountSettings.builder()
      .accountId(AccountId.of("foo"))
      .linkType(TrackingStreamReceiverLink.LINK_TYPE)
      .accountRelationship(AccountRelationship.PEER)
      .assetCode("XRP")
      .assetScale(2)
      .build();
    final LinkSettings actual = factory.construct(accountSettings);
    assertThat(actual.getLinkType()).isEqualTo(TrackingStreamReceiverLink.LINK_TYPE);
    assertThat(actual.getCustomSettings().isEmpty()).isTrue();

    final TrackingStreamReceiverLinkSettings trackingStreamReceiverLinkSettings =
      (TrackingStreamReceiverLinkSettings) actual;
    assertThat(trackingStreamReceiverLinkSettings.assetCode()).isEqualTo("XRP");
    assertThat(trackingStreamReceiverLinkSettings.assetScale()).isEqualTo(2);
  }
}
