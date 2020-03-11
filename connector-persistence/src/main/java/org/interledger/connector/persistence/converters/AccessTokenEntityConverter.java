package org.interledger.connector.persistence.converters;

import org.interledger.connector.accounts.AccessToken;
import org.interledger.connector.persistence.entities.AccessTokenEntity;

import org.springframework.core.convert.converter.Converter;

import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

public class AccessTokenEntityConverter implements Converter<AccessTokenEntity, AccessToken> {

  @Override
  public AccessToken convert(AccessTokenEntity accessTokenEntity) {
    Objects.requireNonNull(accessTokenEntity);

    return AccessToken.builder()
        .id(accessTokenEntity.getId())
        .accountId(accessTokenEntity.getAccountId())
        .encryptedToken(accessTokenEntity.getEncryptedToken())
        .createdAt(Optional.ofNullable(accessTokenEntity.getCreatedDate()).orElseGet(() -> Instant.now()))
        .build();
  }
}
