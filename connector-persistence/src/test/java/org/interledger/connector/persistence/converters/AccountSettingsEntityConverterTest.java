package org.interledger.connector.persistence.converters;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.AccountBalanceSettings;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRateLimitSettings;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.SettlementEngineAccountId;
import org.interledger.connector.accounts.SettlementEngineDetails;
import org.interledger.connector.persistence.entities.AccountSettingsEntity;
import org.interledger.link.LinkType;

import okhttp3.HttpUrl;
import org.junit.Before;
import org.junit.Test;

import java.time.Instant;
import java.util.UUID;

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

    assertThat(actual.accountId()).isEqualTo(accountSettings.accountId());
    assertThat(actual.description()).isEqualTo(accountSettings.description());
    assertThat(actual.assetScale()).isEqualTo(accountSettings.assetScale());
    assertThat(actual.assetCode()).isEqualTo(accountSettings.assetCode());
    assertThat(actual.accountRelationship()).isEqualTo(accountSettings.accountRelationship());
    assertThat(actual.ilpAddressSegment()).isEqualTo(accountSettings.ilpAddressSegment());
    assertThat(actual.linkType()).isEqualTo(accountSettings.linkType());
    assertThat(actual.rateLimitSettings()).isEqualTo(accountSettings.rateLimitSettings());
    assertThat(actual.balanceSettings()).isEqualTo(accountSettings.balanceSettings());
    assertThat(actual.settlementEngineDetails()).isEqualTo(accountSettings.settlementEngineDetails());
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

    assertThat(actual.accountId()).isEqualTo(accountSettings.accountId());
    // These will be set to "now", but it's not possible to know the value exactly, so we don't assert them to finely.
    assertThat(actual.createdAt().minusSeconds(1).isBefore(Instant.now())).isTrue();
    assertThat(actual.modifiedAt().minusSeconds(1).isBefore(Instant.now())).isTrue();
    assertThat(actual.description()).isEqualTo(accountSettings.description());
    assertThat(actual.assetScale()).isEqualTo(accountSettings.assetScale());
    assertThat(actual.assetCode()).isEqualTo(accountSettings.assetCode());
    assertThat(actual.accountRelationship()).isEqualTo(accountSettings.accountRelationship());
    assertThat(actual.ilpAddressSegment()).isEqualTo(accountSettings.ilpAddressSegment());
    assertThat(actual.linkType()).isEqualTo(accountSettings.linkType());
    assertThat(actual.rateLimitSettings()).isEqualTo(accountSettings.rateLimitSettings());
    assertThat(actual.balanceSettings()).isEqualTo(accountSettings.balanceSettings());
    assertThat(actual.settlementEngineDetails()).isEqualTo(accountSettings.settlementEngineDetails());
  }
}
