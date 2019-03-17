package org.interledger.connector.link.blast;

import com.google.common.collect.ImmutableMap;
import okhttp3.HttpUrl;
import org.junit.Test;

import java.time.Duration;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for {@link BlastLinkSettings}.
 */
public class BlastLinkSettingsTest {

  /**
   * Tests the builder when customAttributes is a flat collection of key/value pairs using dotted-notation.
   */
  @Test
  public void applyCustomSettingsWithFlatDottedNotation() {
    final Map<String, Object> customSettings = ImmutableMap.<String, Object>builder()
      .put(BlastLinkSettings.BLAST_INCOMING_ACCOUNT_ID, "incomingAccountId")
      .put(BlastLinkSettings.BLAST_INCOMING_TOKEN_ISSUER, "https://incoming-issuer.example.com")
      .put(BlastLinkSettings.BLAST_INCOMING_ACCOUNT_SECRET, "incomingSecret")

      .put(BlastLinkSettings.BLAST_OUTGOING_ACCOUNT_ID, "outgoingAccountId")
      .put(BlastLinkSettings.BLAST_OUTGOING_AUTH_TYPE, BlastLinkSettings.AuthType.SIMPLE.name())
      .put(BlastLinkSettings.BLAST_OUTGOING_TOKEN_ISSUER, "https://outgoing-issuer.example.com")
      .put(BlastLinkSettings.BLAST_OUTGOING_TOKEN_EXPIRY, Duration.ofDays(1).toString())
      .put(BlastLinkSettings.BLAST_OUTGOING_ACCOUNT_SECRET, "outgoingSecret")
      .put(BlastLinkSettings.BLAST_OUTGOING_URL, "https://outgoing.example.com")

      .build();

    final ImmutableBlastLinkSettings.Builder builder =
      BlastLinkSettings.builder().linkType(BlastLink.LINK_TYPE).minMessageWindow(Duration.ofMillis(20));
    final BlastLinkSettings settings = BlastLinkSettings.applyCustomSettings(builder, customSettings).build();

    assertThat(settings.getLinkType(), is(BlastLink.LINK_TYPE));
    assertThat(settings.getMinMessageWindow(), is(Duration.ofMillis(20)));

    assertThat(settings.getIncomingAccountId(), is("incomingAccountId"));
    assertThat(settings.getIncomingTokenIssuer(), is(HttpUrl.parse("https://incoming-issuer.example.com")));
    assertThat(settings.getIncomingAccountSecret(), is("incomingSecret"));

    assertThat(settings.getOutgoingAccountId(), is("outgoingAccountId"));
    assertThat(settings.getOutgoingAuthType(), is(BlastLinkSettings.AuthType.SIMPLE));
    assertThat(settings.getOutgoingTokenIssuer(), is(HttpUrl.parse("https://outgoing-issuer.example.com")));
    assertThat(settings.getOutgoingUrl(), is(HttpUrl.parse("https://outgoing.example.com")));
    assertThat(settings.getOutingTokenExpiry(), is(Duration.ofDays(1)));
    assertThat(settings.getOutgoingAccountSecret(), is("outgoingSecret"));
  }

  /**
   * Tests the builder when customAttributes is a Map of Maps.
   */
  @Test
  public void applyCustomSettingsWithMapHeirarchy() {
    final Map<String, Object> incomingMap = new HashMap<>();
    incomingMap.put(BlastLinkSettings.ACCOUNT_ID, "incomingAccountId");
    incomingMap.put(BlastLinkSettings.AUTH_TYPE, BlastLinkSettings.AuthType.JWT.name());
    incomingMap.put(BlastLinkSettings.TOKEN_ISSUER, "https://incoming-issuer.example.com");
    incomingMap.put(BlastLinkSettings.ACCOUNT_SECRET, "incomingSecret");

    final Map<String, Object> outgoingMap = new HashMap<>();
    outgoingMap.put(BlastLinkSettings.ACCOUNT_ID, "outgoingAccountId");
    outgoingMap.put(BlastLinkSettings.AUTH_TYPE, BlastLinkSettings.AuthType.SIMPLE.name());
    outgoingMap.put(BlastLinkSettings.TOKEN_ISSUER, "https://outgoing-issuer.example.com");
    outgoingMap.put(BlastLinkSettings.TOKEN_EXPIRY, Duration.ofDays(1).toString());
    outgoingMap.put(BlastLinkSettings.ACCOUNT_SECRET, "outgoingSecret");
    outgoingMap.put(BlastLinkSettings.URL, "https://outgoing.example.com");

    final Map<String, Object> blastMap = new HashMap<>();
    blastMap.put(BlastLinkSettings.INCOMING, incomingMap);
    blastMap.put(BlastLinkSettings.OUTGOING, outgoingMap);

    final Map<String, Object> customSettings = new HashMap<>();
    customSettings.put(BlastLinkSettings.BLAST, blastMap);


    final ImmutableBlastLinkSettings.Builder builder =
      BlastLinkSettings.builder().linkType(BlastLink.LINK_TYPE).minMessageWindow(Duration.ofMillis(20));
    final BlastLinkSettings settings = BlastLinkSettings.applyCustomSettings(builder, customSettings).build();

    assertThat(settings.getLinkType(), is(BlastLink.LINK_TYPE));
    assertThat(settings.getMinMessageWindow(), is(Duration.ofMillis(20)));

    assertThat(settings.getIncomingAccountId(), is("incomingAccountId"));
    assertThat(settings.getIncomingTokenIssuer(), is(HttpUrl.parse("https://incoming-issuer.example.com")));
    assertThat(settings.getIncomingAccountSecret(), is("incomingSecret"));

    assertThat(settings.getOutgoingAccountId(), is("outgoingAccountId"));
    assertThat(settings.getOutgoingAuthType(), is(BlastLinkSettings.AuthType.SIMPLE));
    assertThat(settings.getOutgoingTokenIssuer(), is(HttpUrl.parse("https://outgoing-issuer.example.com")));
    assertThat(settings.getOutgoingUrl(), is(HttpUrl.parse("https://outgoing.example.com")));
    assertThat(settings.getOutingTokenExpiry(), is(Duration.ofDays(1)));
    assertThat(settings.getOutgoingAccountSecret(), is("outgoingSecret"));
  }

  @Test
  public void testWithoutCustomSettings() {
    final BlastLinkSettings settings = BlastLinkSettings.builder()
      .linkType(BlastLink.LINK_TYPE)
      .incomingAccountId("incomingAccountId")
      .incomingTokenIssuer(HttpUrl.parse("https://incoming-issuer.example.com"))
      .incomingAccountSecret("incomingSecret")

      .outgoingAccountId("outgoingAccountId")
      .outgoingAuthType(BlastLinkSettings.AuthType.SIMPLE)
      .outgoingTokenIssuer(HttpUrl.parse("https://outgoing-issuer.example.com"))
      .outgoingUrl(HttpUrl.parse("https://outgoing.example.com"))
      .outingTokenExpiry(Duration.ofDays(1))
      .outgoingAccountSecret("outgoingSecret")

      .minMessageWindow(Duration.ofMillis(20)).build();

    assertThat(settings.getLinkType(), is(BlastLink.LINK_TYPE));
    assertThat(settings.getMinMessageWindow(), is(Duration.ofMillis(20)));

    assertThat(settings.getIncomingAccountId(), is("incomingAccountId"));
    assertThat(settings.getIncomingTokenIssuer(), is(HttpUrl.parse("https://incoming-issuer.example.com")));
    assertThat(settings.getIncomingAccountSecret(), is("incomingSecret"));

    assertThat(settings.getOutgoingAccountId(), is("outgoingAccountId"));
    assertThat(settings.getOutgoingAuthType(), is(BlastLinkSettings.AuthType.SIMPLE));
    assertThat(settings.getOutgoingTokenIssuer(), is(HttpUrl.parse("https://outgoing-issuer.example.com")));
    assertThat(settings.getOutgoingUrl(), is(HttpUrl.parse("https://outgoing.example.com")));
    assertThat(settings.getOutingTokenExpiry(), is(Duration.ofDays(1)));
    assertThat(settings.getOutgoingAccountSecret(), is("outgoingSecret"));
  }
}