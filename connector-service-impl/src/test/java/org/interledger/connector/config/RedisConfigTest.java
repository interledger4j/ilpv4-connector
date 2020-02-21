package org.interledger.connector.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.interledger.crypto.Decryptor;
import org.interledger.crypto.EncryptedSecret;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.data.redis.connection.RedisPassword;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = { RedisConfig.class, RedisConfigTest.TestMockConfig.class })
@TestPropertySource(
  properties = {
    "redis.host=1.2.3.4",
    "redis.password=" + RedisConfigTest.ENCRYPTED_SHH,
    "redis.port=9999"
  }
)
@EnableConfigurationProperties
public class RedisConfigTest {

  static final String ENCRYPTED_SHH
    = "enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADKZPmASojt1iayb2bPy4D-Toq7TGLTN95HzCQAeJtz0=";

  @Autowired
  private LettuceConnectionFactory lettuceConnectionFactory;

  @Test
  public void testConfigProps() {
    assertThat(lettuceConnectionFactory.getStandaloneConfiguration().getHostName()).isEqualTo("1.2.3.4");
    assertThat(lettuceConnectionFactory.getStandaloneConfiguration().getPassword()).isEqualTo(RedisPassword.of("shh"));
    assertThat(lettuceConnectionFactory.getStandaloneConfiguration().getPort()).isEqualTo(9999);
  }

  @Component
  static class TestMockConfig {
    @Bean
    public Decryptor decryptor() {
      Decryptor mock = mock(Decryptor.class);
      when(mock.decrypt(EncryptedSecret.fromEncodedValue(ENCRYPTED_SHH))).thenReturn("shh".getBytes());
      return mock;
    }
  }

}