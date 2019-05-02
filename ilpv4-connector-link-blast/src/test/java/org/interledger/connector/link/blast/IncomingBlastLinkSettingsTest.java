package org.interledger.connector.link.blast;

import okhttp3.HttpUrl;
import org.junit.Test;

import java.time.Duration;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for {@link IncomingLinkSettings}.
 */
public class IncomingBlastLinkSettingsTest extends AbstractBlastLinkTest {

  /**
   * Tests the builder when customAttributes is a flat collection of key/value pairs using dotted-notation.
   */
  @Test
  public void applyCustomSettingsWithFlatDottedNotation() {
    final Map<String, Object> customSettings = this.customSettingsFlat();
    final IncomingLinkSettings incomingLinksettings = IncomingLinkSettings.fromCustomSettings(customSettings).build();

    assertThat(incomingLinksettings.authType(), is(BlastLinkSettings.AuthType.JWT_HS_256));
    assertThat(incomingLinksettings.tokenIssuer(), is(HttpUrl.parse("https://incoming-issuer.example.com/")));
    assertThat(incomingLinksettings.tokenAudience(), is("https://incoming-audience.example.com/"));
    assertThat(incomingLinksettings.tokenSubject(), is("incoming-subject"));
    assertThat(incomingLinksettings.encryptedTokenSharedSecret(), is("incoming-credential"));
    assertThat(incomingLinksettings.getMinMessageWindow(), is(Duration.ofSeconds(1)));
  }

  /**
   * Tests the builder when customAttributes is a Map of Maps.
   */
  @Test
  public void applyCustomSettingsWithMapHeirarchy() {
    final Map<String, Object> customSettings = this.customSettingsHeirarchical();
    final IncomingLinkSettings incomingLinksettings = IncomingLinkSettings.fromCustomSettings(customSettings).build();

    assertThat(incomingLinksettings.authType(), is(BlastLinkSettings.AuthType.JWT_HS_256));
    assertThat(incomingLinksettings.tokenIssuer(), is(HttpUrl.parse("https://incoming-issuer.example.com/")));
    assertThat(incomingLinksettings.tokenAudience(), is("https://incoming-audience.example.com/"));
    assertThat(incomingLinksettings.tokenSubject(), is("incoming-subject"));
    assertThat(incomingLinksettings.encryptedTokenSharedSecret(), is("incoming-credential"));
    assertThat(incomingLinksettings.getMinMessageWindow(), is(Duration.ofSeconds(1)));
  }

  @Test
  public void testWithoutCustomSettings() {
    final IncomingLinkSettings incomingLinksettings =
      IncomingLinkSettings.builder()
        .authType(BlastLinkSettings.AuthType.SIMPLE)
        .tokenIssuer(HttpUrl.parse("https://incoming-issuer.example.com"))
        .tokenAudience("https://incoming-audience.example.com/")
        .tokenSubject("incoming-subject")
        .encryptedTokenSharedSecret("shh")
        .minMessageWindow(Duration.ofMillis(30))
        .build();


    assertThat(incomingLinksettings.authType(), is(BlastLinkSettings.AuthType.SIMPLE));
    assertThat(incomingLinksettings.tokenIssuer(), is(HttpUrl.parse("https://incoming-issuer.example.com/")));
    assertThat(incomingLinksettings.tokenAudience(), is("https://incoming-audience.example.com/"));
    assertThat(incomingLinksettings.tokenSubject(), is("incoming-subject"));
    assertThat(incomingLinksettings.encryptedTokenSharedSecret(), is("shh"));
    assertThat(incomingLinksettings.getMinMessageWindow(), is(Duration.ofMillis(30)));
  }
}