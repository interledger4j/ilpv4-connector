package org.interledger.connector.persistence.converters;

import org.interledger.connector.accounts.AccountSettings;
import org.interledger.connector.accounts.ImmutableAccountSettings;
import org.interledger.connector.persistence.entities.AccountSettingsEntity;

import com.google.common.primitives.UnsignedLong;
import org.springframework.core.convert.converter.Converter;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/**
 * A converter from {@link AccountSettingsEntity} to {@link AccountSettings}.
 */
public class AccountSettingsEntityConverter implements Converter<AccountSettingsEntity, AccountSettings> {

  private final RateLimitSettingsEntityConverter rateLimitSettingsEntityConverter;
  private final AccountBalanceSettingsEntityConverter accountBalanceSettingsEntityConverter;
  private final SettlementEngineDetailsEntityConverter settlementEngineDetailsEntityConverter;

  public AccountSettingsEntityConverter(
    final RateLimitSettingsEntityConverter rateLimitSettingsEntityConverter,
    final AccountBalanceSettingsEntityConverter accountBalanceSettingsEntityConverter,
    final SettlementEngineDetailsEntityConverter settlementEngineDetailsEntityConverter
  ) {
    this.rateLimitSettingsEntityConverter = Objects.requireNonNull(rateLimitSettingsEntityConverter);
    this.accountBalanceSettingsEntityConverter = Objects.requireNonNull(accountBalanceSettingsEntityConverter);
    this.settlementEngineDetailsEntityConverter = Objects.requireNonNull(settlementEngineDetailsEntityConverter);
  }

  @Override
  public AccountSettings convert(final AccountSettingsEntity accountSettingsEntity) {
    Objects.requireNonNull(accountSettingsEntity);

    final ImmutableAccountSettings.Builder builder = AccountSettings.builder()
      .accountId(accountSettingsEntity.getAccountId())
      // Entity created and modified dates are automatically set by Spring Data, and are technically read-only from
      // the perspective of a normal Java developer. However, for testing purposes, we need a default value because
      // these dates will be null if an entity is not created by Spring Data.
      .createdAt(Optional.ofNullable(accountSettingsEntity.getCreatedDate()).orElseGet(() -> Instant.now()))
      .modifiedAt(Optional.ofNullable(accountSettingsEntity.getCreatedDate()).orElseGet(() -> Instant.now()))
      .description(accountSettingsEntity.getDescription())
      .assetScale(accountSettingsEntity.getAssetScale())
      .assetCode(accountSettingsEntity.getAssetCode())
      .linkType(accountSettingsEntity.getLinkType())
      .accountRelationship(accountSettingsEntity.getAccountRelationship())
      .ilpAddressSegment(accountSettingsEntity.getIlpAddressSegment())
      .isSendRoutes(accountSettingsEntity.isSendRoutes())
      .isReceiveRoutes(accountSettingsEntity.isReceiveRoutes())
      .putAllCustomSettings(accountSettingsEntity.getCustomSettings())
      .isDeleted(accountSettingsEntity.isDeleted());

    accountSettingsEntity.getMaximumPacketAmount()
      .ifPresent(maxPacketAmount -> builder.maximumPacketAmount(UnsignedLong.valueOf(maxPacketAmount)));

    Optional.ofNullable(accountSettingsEntity.getRateLimitSettingsEntity())
      .ifPresent(rateLimitSettingsEntity -> builder
        .rateLimitSettings(rateLimitSettingsEntityConverter.convert(rateLimitSettingsEntity)));

    Optional.ofNullable(accountSettingsEntity.getBalanceSettingsEntity())
      .ifPresent(balanceSettingsEntity -> builder
        .balanceSettings(accountBalanceSettingsEntityConverter.convert(balanceSettingsEntity)));

    Optional.ofNullable(accountSettingsEntity.getSettlementEngineDetailsEntity())
      .ifPresent(settlementEngineDetailsEntity ->
        builder
          .settlementEngineDetails(settlementEngineDetailsEntityConverter.convert(settlementEngineDetailsEntity))
      );

    return builder.build();
  }
}
