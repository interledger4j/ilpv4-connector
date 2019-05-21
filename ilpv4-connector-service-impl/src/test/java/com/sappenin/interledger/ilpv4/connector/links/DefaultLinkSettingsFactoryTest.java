package com.sappenin.interledger.ilpv4.connector.links;

import com.google.common.collect.Maps;
import com.sappenin.interledger.ilpv4.connector.links.loopback.LoopbackLink;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.link.LinkSettings;
import org.interledger.connector.link.LinkType;
import org.interledger.connector.link.blast.BlastLink;
import org.interledger.connector.link.blast.BlastLinkSettings;
import org.interledger.connector.link.blast.IncomingLinkSettings;
import org.interledger.connector.link.blast.OutgoingLinkSettings;
import org.junit.Before;
import org.junit.Test;

import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;

/**
 * Unit tests for {@link DefaultLinkSettingsFactory}.
 */
public class DefaultLinkSettingsFactoryTest {

  DefaultLinkSettingsFactory factory;

  @Before
  public void setup() {
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
    customSettings.put(IncomingLinkSettings.BLAST_INCOMING_AUTH_TYPE, BlastLinkSettings.AuthType.JWT_HS_256.name());
    customSettings.put(IncomingLinkSettings.BLAST_INCOMING_TOKEN_ISSUER, "https://alice.example.com/");
    customSettings.put(IncomingLinkSettings.BLAST_INCOMING_TOKEN_AUDIENCE, "https://connie.example.com/");
    customSettings.put(IncomingLinkSettings.BLAST_INCOMING_SHARED_SECRET, "shh");
    customSettings.put(OutgoingLinkSettings.BLAST_OUTGOING_AUTH_TYPE, BlastLinkSettings.AuthType.JWT_HS_256.name());
    customSettings.put(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_ISSUER, "https://connie.example.com/");
    customSettings.put(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_AUDIENCE, "https://alice.example.com/");
    customSettings.put(OutgoingLinkSettings.BLAST_OUTGOING_TOKEN_SUBJECT, "connie");
    customSettings.put(OutgoingLinkSettings.BLAST_OUTGOING_SHARED_SECRET, "shh");
    customSettings.put(OutgoingLinkSettings.BLAST_OUTGOING_URL, "https://alice.example.com");

    AccountSettings accountSettings = AccountSettings.builder()
      .accountId(AccountId.of("foo"))
      .linkType(BlastLink.LINK_TYPE)
      .accountRelationship(AccountRelationship.PEER)
      .assetScale(2)
      .assetCode("XRP")
      .customSettings(customSettings)
      .build();
    final LinkSettings actual = factory.construct(accountSettings);
    assertThat(actual.getLinkType(), is(BlastLink.LINK_TYPE));
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
}