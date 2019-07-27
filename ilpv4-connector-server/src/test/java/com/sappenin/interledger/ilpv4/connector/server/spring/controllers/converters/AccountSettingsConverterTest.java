package com.sappenin.interledger.ilpv4.connector.server.spring.controllers.converters;

import com.sappenin.interledger.ilpv4.connector.links.loopback.LoopbackLink;
import okhttp3.HttpUrl;
import org.interledger.connector.accounts.AccountBalanceSettings;
import org.interledger.connector.accounts.AccountId;
import org.interledger.connector.accounts.AccountRateLimitSettings;
import org.interledger.connector.accounts.AccountRelationship;
import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.SettlementEngineDetails;
import org.interledger.ilpv4.connector.persistence.entities.AccountSettingsEntity;
import org.junit.Before;
import org.junit.Test;

import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for {@link AccountSettingsConverter}.
 */
public class AccountSettingsConverterTest {

  private RateLimitSettingsConverter rateLimitSettingsConverter;
  private AccountBalanceSettingsConverter accountBalanceSettingsConverter;
  private SettlementEngineDetailsConverter settlementEngineDetailsConverter;

  private AccountSettingsConverter converter;

  @Before
  public void setup() {
    this.rateLimitSettingsConverter = new RateLimitSettingsConverter();
    this.accountBalanceSettingsConverter = new AccountBalanceSettingsConverter();
    this.settlementEngineDetailsConverter = new SettlementEngineDetailsConverter();

    this.converter = new AccountSettingsConverter(
      rateLimitSettingsConverter, accountBalanceSettingsConverter, settlementEngineDetailsConverter
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
      .linkType(LoopbackLink.LINK_TYPE)
      .build();
    final AccountSettingsEntity entity = new AccountSettingsEntity(accountSettings);

    AccountSettings actual = converter.convert(entity);

    assertThat(actual.getAccountId(), is(accountSettings.getAccountId()));
    assertThat(actual.getDescription(), is(accountSettings.getDescription()));
    assertThat(actual.getAssetScale(), is(accountSettings.getAssetScale()));
    assertThat(actual.getAssetCode(), is(accountSettings.getAssetCode()));
    assertThat(actual.getAccountRelationship(), is(accountSettings.getAccountRelationship()));
    assertThat(actual.getIlpAddressSegment(), is(accountSettings.getIlpAddressSegment()));
    assertThat(actual.getLinkType(), is(accountSettings.getLinkType()));
    assertThat(actual.getRateLimitSettings(), is(accountSettings.getRateLimitSettings()));
    assertThat(actual.getBalanceSettings(), is(accountSettings.getBalanceSettings()));
    assertThat(actual.settlementEngineDetails(), is(accountSettings.settlementEngineDetails()));
  }

  @Test
  public void convertFullObject() {
    final AccountBalanceSettings balanceSettings = AccountBalanceSettings.builder()
      .settleThreshold(100L)
      .settleTo(1L)
      .minBalance(50L)
      .maxBalance(100L)
      .build();

    final AccountRateLimitSettings rateLimitSettings = AccountRateLimitSettings.builder()
      .maxPacketsPerSecond(2)
      .build();

    final SettlementEngineDetails settlementEngineDetails = SettlementEngineDetails.builder()
      .baseUrl(HttpUrl.parse("https://example.com"))
      .assetScale(2)
      .settlementEngineAccountId(UUID.randomUUID().toString())
      .build();

    final AccountSettings accountSettings = AccountSettings.builder()
      .accountId(AccountId.of("123"))
      .description("test description")
      .assetCode("USD")
      .assetScale(2)
      .accountRelationship(AccountRelationship.PEER)
      .ilpAddressSegment("g.foo")
      .linkType(LoopbackLink.LINK_TYPE)
      .balanceSettings(balanceSettings)
      .rateLimitSettings(rateLimitSettings)
      .settlementEngineDetails(settlementEngineDetails)
      .build();
    final AccountSettingsEntity entity = new AccountSettingsEntity(accountSettings);

    AccountSettings actual = converter.convert(entity);

    assertThat(actual.getAccountId(), is(accountSettings.getAccountId()));
    assertThat(actual.getDescription(), is(accountSettings.getDescription()));
    assertThat(actual.getAssetScale(), is(accountSettings.getAssetScale()));
    assertThat(actual.getAssetCode(), is(accountSettings.getAssetCode()));
    assertThat(actual.getAccountRelationship(), is(accountSettings.getAccountRelationship()));
    assertThat(actual.getIlpAddressSegment(), is(accountSettings.getIlpAddressSegment()));
    assertThat(actual.getLinkType(), is(accountSettings.getLinkType()));
    assertThat(actual.getRateLimitSettings(), is(accountSettings.getRateLimitSettings()));
    assertThat(actual.getBalanceSettings(), is(accountSettings.getBalanceSettings()));
    assertThat(actual.settlementEngineDetails(), is(accountSettings.settlementEngineDetails()));
  }
}
