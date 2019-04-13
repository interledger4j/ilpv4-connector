package org.interledger.ilpv4.connector.persistence.repository;

import org.interledger.ilpv4.connector.persistence.model.AccountEntity;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.connection.RedisStandaloneConfiguration;
import org.springframework.data.redis.connection.jedis.JedisConnectionFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

/**
 * Unit tests for {@link AccountRepository}.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = AccountRepositoryTest.RedisConnectorPersistenceConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class AccountRepositoryTest {

  private static redis.embedded.RedisServer redisServer;

  @Autowired
  private AccountRepository accountRepository;

  @BeforeClass
  public static void startRedisServer() throws IOException {
    redisServer = new redis.embedded.RedisServer(6380);
    redisServer.start();
  }

  @AfterClass
  public static void stopRedisServer() {
    redisServer.stop();
  }

  @Test
  public void testSaveBob() {
    final AccountEntity accountEntity = new AccountEntity();
    accountEntity.setId("foo");
    accountEntity.setName("Bob");
    accountRepository.save(accountEntity);

    final AccountEntity retrievedAccountEntity = accountRepository.findById("foo").get();

    assertThat(retrievedAccountEntity, is(accountEntity));
  }

  @Configuration
  @EnableRedisRepositories(basePackages = "org.interledger.ilpv4.connector.persistence.repository")
  public static class RedisConnectorPersistenceConfig {

    @Bean
    JedisConnectionFactory jedisConnectionFactory() {
      RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("localhost", 6380);
      JedisConnectionFactory jedisConFactory = new JedisConnectionFactory(config);
      return jedisConFactory;
    }

    @Bean
    public RedisTemplate<String, Object> redisTemplate() {
      RedisTemplate<String, Object> template = new RedisTemplate<>();
      template.setConnectionFactory(jedisConnectionFactory());
      return template;
    }

  }
}