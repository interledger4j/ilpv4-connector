package org.interledger.connector.accounts;

import static org.assertj.core.api.Assertions.assertThat;

import org.interledger.connector.accounts.event.AccountCredentialsUpdatedEvent;
import org.interledger.connector.persistence.config.ConnectorPersistenceConfig;
import org.interledger.connector.persistence.converters.AccessTokenEntityConverter;
import org.interledger.connector.persistence.repositories.AccessTokensRepository;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.eventbus.EventBus;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.convert.support.ConfigurableConversionService;
import org.springframework.core.convert.support.DefaultConversionService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
  ConnectorPersistenceConfig.class, DefaultAccessTokenManagerTest.TestPersistenceConfig.class
})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
public class DefaultAccessTokenManagerTest {

  @Rule
  public MockitoRule mockitoRule = MockitoJUnit.rule();

  @Autowired
  private AccessTokensRepository accessTokensRepository;

  @Mock
  private EventBus eventBus;

  private PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();
  private DefaultAccessTokenManager manager;

  @Before
  public void setUp() {
    manager = new DefaultAccessTokenManager(passwordEncoder, accessTokensRepository, eventBus);
  }

  @Test
  public void createToken() {
    AccountId accountId = AccountId.of("test");
    AccessToken accessToken = manager.createToken(accountId);
    assertThat(accessToken.rawToken()).isNotEmpty();
    assertThat(passwordEncoder.matches(accessToken.rawToken().get(), accessToken.encryptedToken())).isTrue();
  }

  @Test
  public void testCRUDWithMultipleAccounts() {
    AccountId hugh = AccountId.of("hugh");
    AccountId vic = AccountId.of("vic");
    AccessToken hughToken1 = manager.createToken(hugh);
    AccessToken hughToken2 = manager.createToken(hugh);
    AccessToken vicToken1 = manager.createToken(vic);
    AccessToken vicToken2 = manager.createToken(vic);

    assertThat(manager.findTokensByAccountId(hugh)).hasSize(2)
      .containsExactlyInAnyOrder(hughToken1, hughToken2);

    assertThat(manager.findTokensByAccountId(vic)).hasSize(2)
      .containsExactlyInAnyOrder(vicToken1, vicToken2);

    assertThat(manager.findByAccountIdAndRawToken(hugh, hughToken1.rawToken().get()))
      .isNotEmpty()
      .get().extracting(AccessToken::id).isEqualTo(hughToken1.id());

    manager.deleteByAccountId(hugh);

    assertThat(manager.findTokensByAccountId(hugh)).isEmpty();
    assertThat(manager.findByAccountIdAndRawToken(hugh, hughToken1.rawToken().get())).isEmpty();
    assertThat(manager.findTokensByAccountId(vic)).hasSize(2)
      .containsExactlyInAnyOrder(vicToken1, vicToken2);
  }

  @Test
  public void deletePostsEvent() {
    AccountId hugh = AccountId.of("hugh");
    manager.createToken(hugh);
    manager.deleteByAccountId(hugh);
    Mockito.verify(eventBus).post(AccountCredentialsUpdatedEvent.builder().accountId(hugh).build());
    Mockito.verifyNoMoreInteractions(eventBus);
  }

  @Configuration("application.yml")
  public static class TestPersistenceConfig {

    ////////////////////////
    // SpringConverters
    ////////////////////////

    @Autowired
    private AccessTokenEntityConverter accessTokensEntityConverter;

    @Bean
    public ObjectMapper objectMapper() {
      return new ObjectMapper();
    }

    @Bean
    public ConfigurableConversionService conversionService() {
      ConfigurableConversionService conversionService = new DefaultConversionService();
      conversionService.addConverter(accessTokensEntityConverter);
      return conversionService;
    }
  }
}