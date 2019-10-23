package org.interledger.connector.links;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.link.LinkSettings;
import org.interledger.link.LinkType;
import org.interledger.link.LoopbackLink;
import org.interledger.link.PingLoopbackLink;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.OutgoingLinkSettings;

import com.google.common.collect.Maps;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

/**
 * Unit tests for {@link DefaultLinkSettingsFactory}.
 */
public class DefaultLinkSettingsFactoryTest {

  protected static final String ENCRYPTED_SHH
      = "enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADKZPmASojt1iayb2bPy4D-Toq7TGLTN95HzCQAeJtz0=";

  DefaultLinkSettingsFactory factory;

  @Before
  public void setUp() {
    this.factory = new DefaultLinkSettingsFactory();
  }

  @Test(expected = IllegalArgumentException.class)
  public void constructUnsupportedLink() {
    try {
      factory.construct(
          AccountSettings.builder()
              .accountId(AccountId.of("foo"))
              .linkType(LinkType.of("foo"))
              .accountRelationship(AccountRelationship.PEER)
              .assetScale(2)
              .assetCode("XRP")
              .build()
      );
      fail();
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage(), is("Unsupported LinkType: LinkType(FOO)"));
      throw e;
    }
  }

  @Test
  public void constructBlastLink() {
    final Map<String, Object> customSettings = Maps.newHashMap();
    customSettings
        .put(IncomingLinkSettings.HTTP_INCOMING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.JWT_HS_256.name());
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_ISSUER, "https://alice.example.com/");
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_TOKEN_AUDIENCE, "https://connie.example.com/");
    customSettings.put(IncomingLinkSettings.HTTP_INCOMING_SHARED_SECRET, ENCRYPTED_SHH);
    customSettings
        .put(OutgoingLinkSettings.HTTP_OUTGOING_AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.JWT_HS_256.name());
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_ISSUER, "https://connie.example.com/");
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_AUDIENCE, "https://alice.example.com/");
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_SUBJECT, "connie");
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_SHARED_SECRET, ENCRYPTED_SHH);
    customSettings.put(OutgoingLinkSettings.HTTP_OUTGOING_URL, "https://alice.example.com");

    AccountSettings accountSettings = AccountSettings.builder()
        .accountId(AccountId.of("foo"))
        .linkType(IlpOverHttpLink.LINK_TYPE)
        .accountRelationship(AccountRelationship.PEER)
        .assetScale(2)
        .assetCode("XRP")
        .customSettings(customSettings)
        .build();
    final LinkSettings actual = factory.construct(accountSettings);
    assertThat(actual.getLinkType(), is(IlpOverHttpLink.LINK_TYPE));
    assertThat(actual.getCustomSettings(), is(customSettings));
  }

  @Test
  public void constructLoopLink() {
    AccountSettings accountSettings = AccountSettings.builder()
        .accountId(AccountId.of("foo"))
        .linkType(LoopbackLink.LINK_TYPE)
        .accountRelationship(AccountRelationship.PEER)
        .assetScale(2)
        .assetCode("XRP")
        .build();
    final LinkSettings actual = factory.construct(accountSettings);
    assertThat(actual.getLinkType(), is(LoopbackLink.LINK_TYPE));
    assertThat(actual.getCustomSettings().isEmpty(), is(true));
  }

  @Test
  public void testConstructUnidirectionalPingLink() {
    AccountSettings accountSettings = AccountSettings.builder()
        .accountId(AccountId.of("foo"))
        .linkType(PingLoopbackLink.LINK_TYPE)
        .accountRelationship(AccountRelationship.PEER)
        .assetScale(2)
        .assetCode("XRP")
        .build();
    final LinkSettings actual = factory.construct(accountSettings);
    assertThat(actual.getLinkType(), is(PingLoopbackLink.LINK_TYPE));
    assertThat(actual.getCustomSettings().isEmpty(), is(true));
  }
}
