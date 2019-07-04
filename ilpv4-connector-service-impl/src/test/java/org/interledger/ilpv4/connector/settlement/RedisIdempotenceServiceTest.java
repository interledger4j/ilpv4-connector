package org.interledger.ilpv4.connector.settlement;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.common.collect.Lists;
import com.sappenin.interledger.ilpv4.connector.settlement.IdempotenceService;
import com.sappenin.interledger.ilpv4.connector.settlement.IdempotentResponseInfo;
import org.interledger.crypto.Decryptor;
import org.interledger.ilpv4.connector.balances.BalanceTrackerConfig;
import org.interledger.ilpv4.connector.config.RedisConfig;
import org.interledger.ilpv4.connector.core.settlement.ImmutableQuantity;
import org.interledger.ilpv4.connector.jackson.ObjectMapperFactory;
import org.interledger.ilpv4.connector.persistence.config.ConnectorPersistenceConfig;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.rules.SpringClassRule;
import org.springframework.test.context.junit4.rules.SpringMethodRule;
import redis.embedded.RedisServerBuilder;

import java.math.BigInteger;
import java.util.UUID;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Unit tests for {@link RedisIdempotenceService}.
 */
@ContextConfiguration(classes = {RedisIdempotenceServiceTest.Config.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
public class RedisIdempotenceServiceTest {

  @ClassRule
  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();

  protected static final int REDIS_PORT = 6379;

  private static redis.embedded.RedisServer redisServer;

  @Rule
  public final SpringMethodRule springMethodRule = new SpringMethodRule();

  @Autowired
  protected IdempotenceService idempotenceService;

  @BeforeClass
  public static void startRedisServer() {
    redisServer = new RedisServerBuilder().port(REDIS_PORT).build();
    redisServer.start();
  }

  @AfterClass
  public static void stopRedisServer() {
    redisServer.stop();
  }

  @Test
  public void updateIdempotenceRecordWhenNoExistingRecord() {
    boolean result = idempotenceService.updateIdempotenceRecord(
      IdempotentResponseInfo.builder()
        .requestId(UUID.randomUUID())
        .responseStatus(HttpStatus.OK)
        .responseHeaders(new HttpHeaders())
        .responseBody(ImmutableQuantity.builder().amount(BigInteger.ONE).scale(2).build())
        .build()
    );

    assertThat(result, is(false));
  }

  @Test
  public void updateIdempotenceRecordWhenOneExists() {
    final UUID requestId = UUID.randomUUID();
    assertThat(idempotenceService.reserveRequestId(requestId), is(true));

    HttpHeaders headers = new HttpHeaders();
    headers.setBasicAuth("user", "password");
    headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));
    headers.setContentType(MediaType.APPLICATION_JSON);

    final IdempotentResponseInfo data =
      IdempotentResponseInfo.builder()
        .requestId(requestId)
        .responseStatus(HttpStatus.OK)
        .responseHeaders(headers)
        .responseBody(ImmutableQuantity.builder().amount(BigInteger.ONE).scale(2).build())
        .build();

    assertThat(idempotenceService.updateIdempotenceRecord(data), is(true));
    assertThat(idempotenceService.updateIdempotenceRecord(data), is(true));
  }

  @Test
  public void getIdempotenceRecordWhenNoExistingRecord() {
    assertThat(idempotenceService.getIdempotenceRecord(UUID.randomUUID()).isPresent(), is(false));
  }

  @Test
  public void getIdempotenceRecordWhenRecordExists() {
    final UUID requestId = UUID.randomUUID();
    assertThat(idempotenceService.reserveRequestId(requestId), is(true));

    boolean result = idempotenceService.updateIdempotenceRecord(
      IdempotentResponseInfo.builder()
        .requestId(requestId)
        .responseStatus(HttpStatus.OK)
        .responseHeaders(new HttpHeaders())
        .responseBody(ImmutableQuantity.builder().amount(BigInteger.ONE).scale(2).build())
        .build()
    );
    assertThat(result, is(true));
    assertThat(idempotenceService.getIdempotenceRecord(requestId).isPresent(), is(true));
  }

  @Configuration
  @Import({ConnectorPersistenceConfig.class, RedisConfig.class, SettlementConfig.class, BalanceTrackerConfig.class})
  static class Config {

    // For testing purposes, Redis is not secured with a password, so this implementation can be a no-op.
    @Bean
    protected Decryptor decryptor() {
      return (keyMetadata, encryptionAlgorithm, cipherMessage) -> new byte[0];
    }

    @Bean
    protected ObjectMapper objectMapper() {
      return ObjectMapperFactory.create();
    }

  }
}
