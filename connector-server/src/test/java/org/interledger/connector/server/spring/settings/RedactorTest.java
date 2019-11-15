package org.interledger.connector.server.spring.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.connector.server.spring.auth.ilpoverhttp.IlpOverHttpAuthenticationProviderTest.ILP_OVER_HTTP_INCOMING;
import static org.interledger.connector.server.spring.auth.ilpoverhttp.IlpOverHttpAuthenticationProviderTest.ILP_OVER_HTTP_OUTGOING;
import static org.interledger.connector.server.spring.settings.Redactor.REDACTED;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ImmutableAccountSettings;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IlpOverHttpLinkSettings;

import org.junit.Test;

public class RedactorTest {

  private Redactor redactor = new Redactor();

  @Test
  public void redact() {

    ImmutableAccountSettings accountWithoutRedactable = accountBuilder().build();
    assertThat(redactor.redact(accountWithoutRedactable)).isEqualTo(accountWithoutRedactable);

    assertThat(redactor.redact(accountWithoutRedactable.customSettings()))
      .isEqualTo(accountWithoutRedactable.customSettings());

    AccountSettings needsToBeRedacted =
      accountBuilder().putCustomSettings(ILP_OVER_HTTP_INCOMING + IlpOverHttpLinkSettings.URL, "http://test.com")
        .putCustomSettings(ILP_OVER_HTTP_INCOMING + IlpOverHttpLinkSettings.SHARED_SECRET, "big secret")
        .putCustomSettings(ILP_OVER_HTTP_INCOMING + IlpOverHttpLinkSettings.TOKEN_SUBJECT, "foo")
        .putCustomSettings(ILP_OVER_HTTP_OUTGOING + IlpOverHttpLinkSettings.SHARED_SECRET, "big secret")
        .putCustomSettings(ILP_OVER_HTTP_OUTGOING + IlpOverHttpLinkSettings.URL, "http://test.com")
        .putCustomSettings(ILP_OVER_HTTP_OUTGOING +
          IlpOverHttpLinkSettings.AUTH_TYPE, IlpOverHttpLinkSettings.AuthType.SIMPLE.toString())
        .putCustomSettings(ILP_OVER_HTTP_OUTGOING + IlpOverHttpLinkSettings.TOKEN_SUBJECT, "bar")
        .build();

    AccountSettings redacted = redactor.redact(needsToBeRedacted);
    assertThat(redacted.customSettings()).isNotEqualTo(needsToBeRedacted.customSettings());
    assertThat(redacted.customSettings().get(ILP_OVER_HTTP_INCOMING + IlpOverHttpLinkSettings.SHARED_SECRET))
      .isEqualTo(REDACTED);
    assertThat(redacted.customSettings().get(ILP_OVER_HTTP_OUTGOING + IlpOverHttpLinkSettings.SHARED_SECRET))
      .isEqualTo(REDACTED);
  }

  private ImmutableAccountSettings.Builder accountBuilder() {
    return AccountSettings.builder()
      .accountRelationship(AccountRelationship.CHILD)
      .assetCode("XRP")
      .assetScale(9)
      .accountId(AccountId.of("foo"))
      .linkType(IlpOverHttpLink.LINK_TYPE);
  }

}