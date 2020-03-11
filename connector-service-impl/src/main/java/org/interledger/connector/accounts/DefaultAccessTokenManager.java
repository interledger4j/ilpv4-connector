package org.interledger.connector.accounts;

import org.interledger.connector.accounts.event.AccountCredentialsUpdatedEvent;
import org.interledger.connector.persistence.entities.AccessTokenEntity;
import org.interledger.connector.persistence.repositories.AccessTokensRepository;

import com.google.common.eventbus.EventBus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;

import java.util.Base64;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Default {@link AccessTokenManager} that generates random secure tokens and persists them to the database.
 */
public class DefaultAccessTokenManager implements AccessTokenManager {

  private final PasswordEncoder passwordEncoder;

  private final AccessTokensRepository accessTokensRepository;

  private final EventBus eventBus;

  public DefaultAccessTokenManager(PasswordEncoder passwordEncoder,
                                   AccessTokensRepository accessTokensRepository,
                                   EventBus eventBus) {
    this.passwordEncoder = passwordEncoder;
    this.accessTokensRepository = accessTokensRepository;
    this.eventBus = eventBus;
  }

  @Override
  public AccessToken createToken(AccountId accountId) {
    String newRandomToken = Base64.getEncoder().encodeToString(UUID.randomUUID().toString().getBytes());
    AccessTokenEntity newEntity = new AccessTokenEntity();
    newEntity.setAccountId(accountId);
    newEntity.setEncryptedToken(encryptToken(newRandomToken));
    AccessTokenEntity saved = accessTokensRepository.save(newEntity);
    return AccessToken.builder()
      .from(accessTokensRepository.withConversion(saved))
      .rawToken(newRandomToken)
      .build();
  }

  @Override
  @Transactional
  public void deleteByAccountIdAndId(AccountId accountId, long id) {
    accessTokensRepository.deleteByAccountIdAndId(accountId, id);
    eventBus.post(AccountCredentialsUpdatedEvent.builder().accountId(accountId).build());
  }

  @Override
  @Transactional
  public void deleteByAccountId(AccountId accountId) {
    accessTokensRepository.deleteByAccountId(accountId);
    eventBus.post(AccountCredentialsUpdatedEvent.builder().accountId(accountId).build());
  }

  @Override
  public List<AccessToken> findTokensByAccountId(AccountId accountId) {
    return accessTokensRepository.withConversion(accessTokensRepository.findByAccountId(accountId));
  }

  @Override
  public Optional<AccessToken> findByAccountIdAndRawToken(AccountId accountId, String rawToken) {
    return findTokensByAccountId(accountId).stream()
      .filter(token -> passwordEncoder.matches(rawToken, token.encryptedToken()))
      .findFirst();
  }

  private String encryptToken(String rawToken) {
    return passwordEncoder.encode(rawToken);
  }

}
