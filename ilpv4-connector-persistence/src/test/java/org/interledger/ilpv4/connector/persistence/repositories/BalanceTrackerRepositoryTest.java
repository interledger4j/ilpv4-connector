package org.interledger.ilpv4.connector.persistence.repositories;

/**
 * Unit tests for {@link AccountSettingsRepository}.
 */
//@RunWith(SpringJUnit4ClassRunner.class)
//@ContextConfiguration(classes = BalanceTrackerRepositoryTest.PersistenceConfig.class)
//@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
public class BalanceTrackerRepositoryTest {

  //  private static final int REDIS_PORT = 6380;
  //
  //  private static redis.embedded.RedisServer redisServer;
  //
  //  @Autowired
  //  private AccountSettingsRepository accountSettingsRepository;
  //
  //  @BeforeClass
  //  public static void startRedisServer() throws IOException {
  //    //redisServer = new redis.embedded.RedisServer(REDIS_PORT);
  //    //redisServer.start();
  //  }
  //
  //  @AfterClass
  //  public static void stopRedisServer() {
  //    redisServer.stop();
  //  }
  //
  //  @Test
  //  public void testSaveBob() {
  //    final AccountSettingsEntity accountSettingsEntity = new AccountSettingsEntity();
  //
  //    accountSettingsEntity.setRelationship(AccountRelationship.PEER);
  //    accountSettingsRepository.save(accountSettingsEntity);
  //
  //    final AccountSettingsEntity retrievedAccountSettingsEntity = accountSettingsRepository.findById("foo").get();
  //
  //    assertThat(retrievedAccountSettingsEntity, is(accountSettingsEntity));
  //  }
  //
  //  @Test
  //  public void whenStoringAJsonColumn_thenDeserializedVersionMatches() {
  //    AccountSettingsEntity customer = new AccountSettingsEntity();
  //    //customer.setFirstName("first name");
  //    //customer.setLastName("last name");
  //
  //    Map<String, Object> attributes = new HashMap<>();
  //    attributes.put("address", "123 Main Street");
  //    attributes.put("zipcode", 12345);
  //
  //    //customer.setCustomerAttributes(attributes);
  //    //customer.serializeCustomerAttributes();
  //
  //    //String serialized = customer.getCustomerAttributeJSON();
  //
  //    //customer.setCustomerAttributeJSON(serialized);
  //    //customer.deserializeCustomerAttributes();
  //
  //    //assertEquals(attributes, customer.getCustomerAttributes());
  //  }
  //
  //  @Configuration
  //  @EnableRedisRepositories(basePackages = "org.interledger.ilpv4.connector.persistence.repository")
  //  public static class PersistenceConfig {
  //
  //    @Bean
  //    JedisConnectionFactory jedisConnectionFactory() {
  //      RedisStandaloneConfiguration config = new RedisStandaloneConfiguration("localhost", REDIS_PORT);
  //      JedisConnectionFactory jedisConFactory = new JedisConnectionFactory(config);
  //      return jedisConFactory;
  //    }
  //
  //    @Bean
  //    public RedisTemplate<String, Object> redisTemplate() {
  //      RedisTemplate<String, Object> template = new RedisTemplate<>();
  //      template.setConnectionFactory(jedisConnectionFactory());
  //      return template;
  //    }
  //
  //
  //  }
}