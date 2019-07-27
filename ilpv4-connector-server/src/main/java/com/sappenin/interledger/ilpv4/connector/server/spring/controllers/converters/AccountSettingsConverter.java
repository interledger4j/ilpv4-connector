package com.sappenin.interledger.ilpv4.connector.server.spring.controllers.converters;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ImmutableAccountSettings;
import org.interledger.ilpv4.connector.persistence.entities.AccountSettingsEntity;
import org.springframework.core.convert.converter.Converter;

import java.util.Objects;
import java.util.Optional;

/**
 * A converter from {@link AccountSettingsEntity} to {@link AccountSettings}.
 */
public class AccountSettingsConverter implements Converter<AccountSettingsEntity, AccountSettings> {

  private final RateLimitSettingsConverter rateLimitSettingsConverter;
  private final AccountBalanceSettingsConverter accountBalanceSettingsConverter;
  private final SettlementEngineDetailsConverter settlementEngineDetailsConverter;

  public AccountSettingsConverter(
    final RateLimitSettingsConverter rateLimitSettingsConverter,
    final AccountBalanceSettingsConverter accountBalanceSettingsConverter,
    final SettlementEngineDetailsConverter settlementEngineDetailsConverter
  ) {
    this.rateLimitSettingsConverter = Objects.requireNonNull(rateLimitSettingsConverter);
    this.accountBalanceSettingsConverter = Objects.requireNonNull(accountBalanceSettingsConverter);
    this.settlementEngineDetailsConverter = Objects.requireNonNull(settlementEngineDetailsConverter);
  }

  @Override
  public AccountSettings convert(final AccountSettingsEntity accountSettingsEntity) {
    Objects.requireNonNull(accountSettingsEntity);

    final ImmutableAccountSettings.Builder builder = AccountSettings.builder()
      .accountId(accountSettingsEntity.getAccountId())
      .description(accountSettingsEntity.getDescription())
      .assetScale(accountSettingsEntity.getAssetScale())
      .assetCode(accountSettingsEntity.getAssetCode())
      .linkType(accountSettingsEntity.getLinkType())
      .accountRelationship(accountSettingsEntity.getAccountRelationship())
      .maximumPacketAmount(accountSettingsEntity.getMaximumPacketAmount())
      .ilpAddressSegment(accountSettingsEntity.getIlpAddressSegment())
      .isSendRoutes(accountSettingsEntity.isSendRoutes())
      .isReceiveRoutes(accountSettingsEntity.isReceiveRoutes())
      .putAllCustomSettings(accountSettingsEntity.getCustomSettings());

    Optional.ofNullable(accountSettingsEntity.getRateLimitSettingsEntity())
      .ifPresent(rateLimitSettingsEntity -> builder
        .rateLimitSettings(rateLimitSettingsConverter.convert(rateLimitSettingsEntity)));

    Optional.ofNullable(accountSettingsEntity.getBalanceSettingsEntity())
      .ifPresent(balanceSettingsEntity -> builder
        .balanceSettings(accountBalanceSettingsConverter.convert(balanceSettingsEntity)));

    Optional.ofNullable(accountSettingsEntity.getSettlementEngineDetailsEntity())
      .ifPresent(settlementEngineDetailsEntity -> builder
        .settlementEngineDetails(settlementEngineDetailsConverter.convert(settlementEngineDetailsEntity)));

    return builder.build();
  }
}
