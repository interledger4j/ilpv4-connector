package org.interledger.connector.persistence.repositories;

import org.interledger.connector.accounts.AccessToken;
import org.interledger.connector.persistence.entities.AccessTokenEntity;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.core.convert.ConversionService;

import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class AccessTokensRepositoryImpl implements AccessTokensRepositoryCustom {

  @Autowired
  @Lazy
  private ConversionService conversionService;

  private Function<AccessTokenEntity, AccessToken> conversionFunction =
    entity -> conversionService.convert(entity, AccessToken.class);

  @Override
  public List<AccessToken> withConversion(List<AccessTokenEntity> accessTokens) {
    return mapEntitiesToDtos(accessTokens);
  }

  @Override
  public Optional<AccessToken> withConversion(Optional<AccessTokenEntity> entity) {
    return entity.map(conversionFunction);
  }

  @Override
  public AccessToken withConversion(AccessTokenEntity token) {
    return conversionFunction.apply(token);
  }

  private List<AccessToken> mapEntitiesToDtos(Iterable<AccessTokenEntity> tokens) {
    return StreamSupport.stream(tokens.spliterator(), false)
        .map(conversionFunction)
        .collect(Collectors.toList());
  }
}
