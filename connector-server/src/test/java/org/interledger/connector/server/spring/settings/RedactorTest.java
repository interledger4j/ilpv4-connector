package org.interledger.connector.server.spring.settings;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.connector.server.spring.settings.Redactor.REDACTED;

import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ImmutableAccountSettings;
import org.interledger.link.http.IlpOverHttpLink;
import org.interledger.link.http.IncomingLinkSettings;
import org.interledger.link.http.OutgoingLinkSettings;

import org.junit.Test;

public class RedactorTest {

  private Redactor redactor = new Redactor();

  @Test
  public void redactJwtAuth() {

    ImmutableAccountSettings accountWithoutRedactable = accountBuilder().build();
    assertThat(redactor.redact(accountWithoutRedactable)).isEqualTo(accountWithoutRedactable);

    assertThat(redactor.redact(accountWithoutRedactable.customSettings()))
      .isEqualTo(accountWithoutRedactable.customSettings());

    AccountSettings needsToBeRedacted =
      accountBuilder()
        .putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_SHARED_SECRET, "big incoming secret")
        .putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_TOKEN_SUBJECT, "foo")
        .putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_SHARED_SECRET, "big outoing secret")
        .putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_URL, "http://test.com")
        .putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_TOKEN_SUBJECT, "bar")
        .build();

    AccountSettings redacted = redactor.redact(needsToBeRedacted);
    assertThat(redacted.customSettings()).isNotEqualTo(needsToBeRedacted.customSettings());
    assertThat(redacted.customSettings().get(IncomingLinkSettings.HTTP_INCOMING_SHARED_SECRET))
      .isEqualTo(REDACTED);
    assertThat(redacted.customSettings().get(OutgoingLinkSettings.HTTP_OUTGOING_SHARED_SECRET))
      .isEqualTo(REDACTED);
  }

  @Test
  public void redactSimpleAuth() {

    ImmutableAccountSettings accountWithoutRedactable = accountBuilder().build();
    assertThat(redactor.redact(accountWithoutRedactable)).isEqualTo(accountWithoutRedactable);

    assertThat(redactor.redact(accountWithoutRedactable.customSettings()))
      .isEqualTo(accountWithoutRedactable.customSettings());

    AccountSettings needsToBeRedacted =
      accountBuilder()
        .putCustomSettings(IncomingLinkSettings.HTTP_INCOMING_SIMPLE_AUTH_TOKEN, "big incoming secret")
        .putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_SIMPLE_AUTH_TOKEN, "big outoing secret")
        .putCustomSettings(OutgoingLinkSettings.HTTP_OUTGOING_URL, "http://test.com")
        .build();

    AccountSettings redacted = redactor.redact(needsToBeRedacted);
    assertThat(redacted.customSettings()).isNotEqualTo(needsToBeRedacted.customSettings());
    assertThat(redacted.customSettings().get(IncomingLinkSettings.HTTP_INCOMING_SIMPLE_AUTH_TOKEN))
      .isEqualTo(REDACTED);
    assertThat(redacted.customSettings().get(OutgoingLinkSettings.HTTP_OUTGOING_SIMPLE_AUTH_TOKEN))
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