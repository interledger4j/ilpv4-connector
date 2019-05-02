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

  public AccountSettingsConverter(
    RateLimitSettingsConverter rateLimitSettingsConverter,
    AccountBalanceSettingsConverter accountBalanceSettingsConverter
  ) {
    this.rateLimitSettingsConverter = rateLimitSettingsConverter;
    this.accountBalanceSettingsConverter = accountBalanceSettingsConverter;
  }

  @Override
  public AccountSettings convert(final AccountSettingsEntity accountSettingsEntity) {
    Objects.requireNonNull(accountSettingsEntity);

    final ImmutableAccountSettings.Builder builder = AccountSettings.builder()
      .accountId(accountSettingsEntity.getAccountId())
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
      .ifPresent(entity -> builder.rateLimitSettings(rateLimitSettingsConverter.convert(entity)));

    Optional.ofNullable(accountSettingsEntity.getBalanceSettingsEntity())
      .ifPresent(entity -> builder.balanceSettings(accountBalanceSettingsConverter.convert(entity)));

    return builder.build();
  }
}
