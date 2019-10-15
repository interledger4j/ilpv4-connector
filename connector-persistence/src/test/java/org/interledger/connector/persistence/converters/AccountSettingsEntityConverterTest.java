package org.interledger.connector.persistence.converters;

import okhttp3.HttpUrl;
import org.interledger.connector.accounts.AccountBalanceSettings;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRateLimitSettings;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.SettlementEngineAccountId;
import org.interledger.connector.accounts.SettlementEngineDetails;
import org.interledger.connector.link.LinkType;
import org.interledger.connector.persistence.entities.AccountSettingsEntity;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for {@link AccountSettingsEntityConverter}.
 */
public class AccountSettingsEntityConverterTest {

  private AccountSettingsEntityConverter converter;

  @Before
  public void setUp() {
    this.converter = new AccountSettingsEntityConverter(
      new RateLimitSettingsEntityConverter(), new AccountBalanceSettingsEntityConverter(),
      new SettlementEngineDetailsEntityConverter()
    );
  }

  @Test
  public void convertWithEmptyEmbeds() {

    final AccountSettings accountSettings = AccountSettings.builder()
      .accountId(AccountId.of("123"))
      .description("test description")
      .assetCode("USD")
      .assetScale(2)
      .accountRelationship(AccountRelationship.PEER)
      .ilpAddressSegment("g.foo")
      .linkType(LinkType.of("foo"))
      .build();
    final AccountSettingsEntity entity = new AccountSettingsEntity(accountSettings);

    AccountSettings actual = converter.convert(entity);

    assertThat(actual.accountId(), is(accountSettings.accountId()));
    assertThat(actual.description(), is(accountSettings.description()));
    assertThat(actual.assetScale(), is(accountSettings.assetScale()));
    assertThat(actual.assetCode(), is(accountSettings.assetCode()));
    assertThat(actual.accountRelationship(), is(accountSettings.accountRelationship()));
    assertThat(actual.ilpAddressSegment(), is(accountSettings.ilpAddressSegment()));
    assertThat(actual.linkType(), is(accountSettings.linkType()));
    assertThat(actual.rateLimitSettings(), is(accountSettings.rateLimitSettings()));
    assertThat(actual.balanceSettings(), is(accountSettings.balanceSettings()));
    assertThat(actual.settlementEngineDetails(), is(accountSettings.settlementEngineDetails()));
  }

  @Test
  public void convertFullObject() {
    final AccountBalanceSettings balanceSettings = AccountBalanceSettings.builder()
      .settleThreshold(100L)
      .settleTo(1L)
      .minBalance(50L)
      .build();

    final AccountRateLimitSettings rateLimitSettings = AccountRateLimitSettings.builder()
      .maxPacketsPerSecond(2)
      .build();

    final SettlementEngineDetails settlementEngineDetails = SettlementEngineDetails.builder()
      .baseUrl(HttpUrl.parse("https://example.com"))
      .settlementEngineAccountId(SettlementEngineAccountId.of(UUID.randomUUID().toString()))
      .build();

    final AccountSettings accountSettings = AccountSettings.builder()
      .accountId(AccountId.of("123"))
      .createdAt(Instant.MAX)
      .modifiedAt(Instant.MAX)
      .description("test description")
      .assetCode("USD")
      .assetScale(2)
      .accountRelationship(AccountRelationship.PEER)
      .ilpAddressSegment("g.foo")
      .linkType(LinkType.of("foo"))
      .balanceSettings(balanceSettings)
      .rateLimitSettings(rateLimitSettings)
      .settlementEngineDetails(settlementEngineDetails)
      .build();
    final AccountSettingsEntity entity = new AccountSettingsEntity(accountSettings);

    AccountSettings actual = converter.convert(entity);

    assertThat(actual.accountId(), is(accountSettings.accountId()));
    // These will be set to "now", but it's not possible to know the value exactly, so we don't assert them to finely.
    assertThat(actual.createdAt().minusSeconds(1).isBefore(Instant.now()), is(true));
    assertThat(actual.modifiedAt().minusSeconds(1).isBefore(Instant.now()), is(true));
    assertThat(actual.description(), is(accountSettings.description()));
    assertThat(actual.assetScale(), is(accountSettings.assetScale()));
    assertThat(actual.assetCode(), is(accountSettings.assetCode()));
    assertThat(actual.accountRelationship(), is(accountSettings.accountRelationship()));
    assertThat(actual.ilpAddressSegment(), is(accountSettings.ilpAddressSegment()));
    assertThat(actual.linkType(), is(accountSettings.linkType()));
    assertThat(actual.rateLimitSettings(), is(accountSettings.rateLimitSettings()));
    assertThat(actual.balanceSettings(), is(accountSettings.balanceSettings()));
    assertThat(actual.settlementEngineDetails(), is(accountSettings.settlementEngineDetails()));
  }
}
