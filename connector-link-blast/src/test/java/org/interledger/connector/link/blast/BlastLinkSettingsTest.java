package org.interledger.connector.link.blast;

import okhttp3.HttpUrl;
import org.junit.Test;

import java.time.Duration;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for {@link BlastLinkSettings}.
 */
public class BlastLinkSettingsTest extends AbstractBlastLinkTest {

  /**
   * Tests the builder when customAttributes is a flat collection of key/value pairs using dotted-notation.
   */
  @Test
  public void applyCustomSettingsWithFlatDottedNotation() {
    final Map<String, Object> flattenedCustomSettings = this.customSettingsFlat();

    final ImmutableBlastLinkSettings.Builder builder = BlastLinkSettings.builder().linkType(BlastLink.LINK_TYPE);
    final ImmutableBlastLinkSettings blastLinkSettings =
      BlastLinkSettings.applyCustomSettings(builder, flattenedCustomSettings).build();

    assertThat(blastLinkSettings.incomingBlastLinkSettings().authType(), is(BlastLinkSettings.AuthType.JWT_HS_256));
    assertThat(blastLinkSettings.incomingBlastLinkSettings().tokenIssuer().get(),
      is(HttpUrl.parse("https://incoming-issuer.example.com/")));
    assertThat(blastLinkSettings.incomingBlastLinkSettings().tokenAudience().get(),
      is(HttpUrl.parse("https://incoming-audience.example.com/")));
    assertThat(blastLinkSettings.incomingBlastLinkSettings().encryptedTokenSharedSecret(), is("incoming-credential"));
    assertThat(blastLinkSettings.incomingBlastLinkSettings().getMinMessageWindow(), is(Duration.ofSeconds(1)));

    assertThat(blastLinkSettings.outgoingBlastLinkSettings().authType(), is(BlastLinkSettings.AuthType.SIMPLE));
    assertThat(blastLinkSettings.outgoingBlastLinkSettings().tokenIssuer().get(),
      is(HttpUrl.parse("https://outgoing-issuer.example.com/")));
    assertThat(blastLinkSettings.outgoingBlastLinkSettings().tokenAudience().get(),
      is(HttpUrl.parse("https://outgoing-audience.example.com/")));
    assertThat(blastLinkSettings.outgoingBlastLinkSettings().tokenSubject(), is("outgoing-subject"));
    assertThat(blastLinkSettings.outgoingBlastLinkSettings().encryptedTokenSharedSecret(), is("outgoing-credential"));
    assertThat(blastLinkSettings.outgoingBlastLinkSettings().tokenExpiry().get(), is(Duration.ofHours(24)));
    assertThat(blastLinkSettings.outgoingBlastLinkSettings().url(), is(HttpUrl.parse("https://outgoing.example.com/")));
  }

  /**
   * Tests the builder when customAttributes is a Map of Maps.
   */
  @Test
  public void applyCustomSettingsWithMapHeirarchy() {
    final Map<String, Object> customSettings = this.customSettingsHeirarchical();

    final ImmutableBlastLinkSettings.Builder builder = BlastLinkSettings.builder().linkType(BlastLink.LINK_TYPE);
    final ImmutableBlastLinkSettings blastLinkSettings =
      BlastLinkSettings.applyCustomSettings(builder, customSettings).build();

    assertThat(blastLinkSettings.incomingBlastLinkSettings().authType(), is(BlastLinkSettings.AuthType.JWT_HS_256));
    assertThat(blastLinkSettings.incomingBlastLinkSettings().tokenIssuer().get(),
      is(HttpUrl.parse("https://incoming-issuer.example.com/")));
    assertThat(blastLinkSettings.incomingBlastLinkSettings().tokenAudience().get(),
      is(HttpUrl.parse("https://incoming-audience.example.com/")));
    assertThat(blastLinkSettings.incomingBlastLinkSettings().encryptedTokenSharedSecret(),
      is("incoming-credential"));
    assertThat(blastLinkSettings.incomingBlastLinkSettings().getMinMessageWindow(), is(Duration.ofSeconds(1)));

    assertThat(blastLinkSettings.outgoingBlastLinkSettings().authType(), is(BlastLinkSettings.AuthType.SIMPLE));
    assertThat(blastLinkSettings.outgoingBlastLinkSettings().tokenIssuer().get(),
      is(HttpUrl.parse("https://outgoing-issuer.example.com/")));
    assertThat(blastLinkSettings.outgoingBlastLinkSettings().tokenAudience().get(),
      is(HttpUrl.parse("https://outgoing-audience.example.com/")));
    assertThat(blastLinkSettings.outgoingBlastLinkSettings().tokenSubject(), is("outgoing-subject"));
    assertThat(blastLinkSettings.outgoingBlastLinkSettings().encryptedTokenSharedSecret(),
      is("outgoing-credential"));
    assertThat(blastLinkSettings.outgoingBlastLinkSettings().tokenExpiry().get(), is(Duration.ofHours(48)));
    assertThat(blastLinkSettings.outgoingBlastLinkSettings().url(), is(HttpUrl.parse("https://outgoing.example.com/")));
  }

  @Test
  public void testWithoutCustomSettings() {
    final IncomingLinkSettings incomingLinksettings =
      IncomingLinkSettings.builder()
        .authType(BlastLinkSettings.AuthType.SIMPLE)
        .tokenIssuer(HttpUrl.parse("https://incoming-issuer.example.com/"))
        .tokenAudience(HttpUrl.parse("https://incoming-audience.example.com/"))
        .minMessageWindow(Duration.ofMillis(30))
        .encryptedTokenSharedSecret("incoming-credential")
        .build();

    final OutgoingLinkSettings outgoingLinksettings =
      OutgoingLinkSettings.builder()
        .authType(BlastLinkSettings.AuthType.SIMPLE)
        .tokenSubject("outgoing-subject")
        .tokenIssuer(HttpUrl.parse("https://outgoing-issuer.example.com/"))
        .tokenAudience(HttpUrl.parse("https://outgoing-audience.example.com/"))
        .encryptedTokenSharedSecret("outgoing-credential")
        .tokenExpiry(Duration.ofMillis(40))
        .url(HttpUrl.parse("https://outgoing.example.com/"))
        .build();

    final BlastLinkSettings blastLinkSettings = BlastLinkSettings.builder()
      .linkType(BlastLink.LINK_TYPE)
      .incomingBlastLinkSettings(incomingLinksettings)
      .outgoingBlastLinkSettings(outgoingLinksettings)
      .build();

    assertThat(blastLinkSettings.getLinkType(), is(BlastLink.LINK_TYPE));

    assertThat(blastLinkSettings.incomingBlastLinkSettings().authType(), is(BlastLinkSettings.AuthType.SIMPLE));
    assertThat(blastLinkSettings.incomingBlastLinkSettings().tokenIssuer().get(),
      is(HttpUrl.parse("https://incoming-issuer.example.com/")));
    assertThat(blastLinkSettings.incomingBlastLinkSettings().tokenAudience().get(),
      is(HttpUrl.parse("https://incoming-audience.example.com/")));
    assertThat(blastLinkSettings.incomingBlastLinkSettings().encryptedTokenSharedSecret(), is("incoming-credential"));
    assertThat(blastLinkSettings.incomingBlastLinkSettings().getMinMessageWindow(), is(Duration.ofMillis(30)));

    assertThat(blastLinkSettings.outgoingBlastLinkSettings().authType(), is(BlastLinkSettings.AuthType.SIMPLE));
    assertThat(blastLinkSettings.outgoingBlastLinkSettings().tokenIssuer().get(),
      is(HttpUrl.parse("https://outgoing-issuer.example.com/")));
    assertThat(blastLinkSettings.outgoingBlastLinkSettings().tokenAudience().get(),
      is(HttpUrl.parse("https://outgoing-audience.example.com/")));
    assertThat(blastLinkSettings.outgoingBlastLinkSettings().tokenSubject(), is("outgoing-subject"));
    assertThat(blastLinkSettings.outgoingBlastLinkSettings().encryptedTokenSharedSecret(), is("outgoing-credential"));
    assertThat(blastLinkSettings.outgoingBlastLinkSettings().tokenExpiry().get(), is(Duration.ofMillis(40)));
    assertThat(blastLinkSettings.outgoingBlastLinkSettings().url(),
      is(HttpUrl.parse("https://outgoing.example.com/")));
  }
}