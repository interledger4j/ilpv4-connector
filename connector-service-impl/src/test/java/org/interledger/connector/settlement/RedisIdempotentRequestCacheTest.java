package org.interledger.connector.settlement;

import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.test.annotation.DirtiesContext;

/**
 * Unit tests for {@link RedisIdempotentRequestCache}.
 */
//@ContextConfiguration(classes = {RedisIdempotentRequestCacheTest.Config.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
@DataJpaTest
public class RedisIdempotentRequestCacheTest {

  // TODO: Remove if unused!

  //  @ClassRule
  //  public static final SpringClassRule SPRING_CLASS_RULE = new SpringClassRule();
  //
  //  protected static final int REDIS_PORT = 6379;
  //
  //  private static redis.embedded.RedisServer redisServer;
  //
  //  @Rule
  //  public final SpringMethodRule springMethodRule = new SpringMethodRule();
  //
  //  @Autowired
  //  protected IdempotentRequestCache idempotentRequestCache;
  //
  //  @BeforeClass
  //  public static void startRedisServer() {
  //    redisServer = new RedisServerBuilder().port(REDIS_PORT).build();
  //    redisServer.start();
  //  }
  //
  //  @AfterClass
  //  public static void stopRedisServer() {
  //    redisServer.stop();
  //  }
  //
  //  @Test
  //  public void updateIdempotenceRecordWhenNoExistingRecord() {
  //    boolean result = idempotentRequestCache.updateHttpResponseInfo(
  //      HttpResponseInfo.builder()
  //        .requestId(UUID.randomUUID())
  //        .responseStatus(HttpStatus.OK)
  //        .responseHeaders(new HttpHeaders())
  //        .responseBody(ImmutableSettlementQuantity.builder().amount(BigInteger.ONE).scale(2).build())
  //        .build()
  //    );
  //
  //    assertThat(result, is(false));
  //  }
  //
  //  @Test
  //  public void updateIdempotenceRecordWhenOneExists() {
  //    final UUID requestId = UUID.randomUUID();
  //    assertThat(idempotentRequestCache.reserveRequestId(requestId), is(true));
  //
  //    HttpHeaders headers = new HttpHeaders();
  //    headers.setBasicAuth("user", "password");
  //    headers.setAccept(Lists.newArrayList(MediaType.APPLICATION_JSON));
  //    headers.setContentType(MediaType.APPLICATION_JSON);
  //
  //    final HttpResponseInfo data =
  //      HttpResponseInfo.builder()
  //        .requestId(requestId)
  //        .responseStatus(HttpStatus.OK)
  //        .responseHeaders(headers)
  //        .responseBody(ImmutableSettlementQuantity.builder().amount(BigInteger.ONE).scale(2).build())
  //        .build();
  //
  //    assertThat(idempotentRequestCache.updateHttpResponseInfo(data), is(true));
  //    assertThat(idempotentRequestCache.updateHttpResponseInfo(data), is(true));
  //  }
  //
  //  @Test
  //  public void getIdempotenceRecordWhenNoExistingRecord() {
  //    assertThat(idempotentRequestCache.getHttpResponseInfo(UUID.randomUUID()).isPresent(), is(false));
  //  }
  //
  //  @Test
  //  public void getIdempotenceRecordWhenRecordExists() {
  //    final UUID requestId = UUID.randomUUID();
  //    assertThat(idempotentRequestCache.reserveRequestId(requestId), is(true));
  //
  //    boolean result = idempotentRequestCache.updateHttpResponseInfo(
  //      HttpResponseInfo.builder()
  //        .requestId(requestId)
  //        .responseStatus(HttpStatus.OK)
  //        .responseHeaders(new HttpHeaders())
  //        .responseBody(ImmutableSettlementQuantity.builder().amount(BigInteger.ONE).scale(2).build())
  //        .build()
  //    );
  //    assertThat(result, is(true));
  //    assertThat(idempotentRequestCache.getHttpResponseInfo(requestId).isPresent(), is(true));
  //  }
  //
  //  @Configuration
  //  @Import({ConnectorPersistenceConfig.class, RedisConfig.class, SettlementConfig.class, BalanceTrackerConfig.class})
  //  static class Config {
  //
  //    // For testing purposes, Redis is not secured with a password, so this implementation can be a no-op.
  //    @Bean
  //    protected Decryptor decryptor() {
  //      return (keyMetadata, encryptionAlgorithm, cipherMessage) -> new byte[0];
  //    }
  //
  //    @Bean
  //    protected ObjectMapper objectMapper() {
  //      return ObjectMapperFactory.create();
  //    }
  //
  //  }
}
