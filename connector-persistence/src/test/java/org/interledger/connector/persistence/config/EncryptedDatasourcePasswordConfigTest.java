package org.interledger.connector.persistence.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.interledger.crypto.EncryptedSecret;
import org.interledger.crypto.EncryptionService;

import com.zaxxer.hikari.HikariDataSource;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.jdbc.DataSourceAutoConfiguration;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import java.util.function.Function;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {
  EncryptedDatasourcePasswordConfig.class,
  DataSourceAutoConfiguration.class,
  EncryptedDatasourcePasswordConfigTest.TestMockConfig.class })
@TestPropertySource(
  properties = {
    "spring.datasource.url=" + EncryptedDatasourcePasswordConfigTest.URL,
    "spring.datasource.password=" + EncryptedDatasourcePasswordConfigTest.ENCRYPTED_SHH,
    "spring.datasource.username=" + EncryptedDatasourcePasswordConfigTest.USERNAME,
    "spring.datasource.platform=postgresql"
  }
)
@EnableConfigurationProperties
public class EncryptedDatasourcePasswordConfigTest {

  protected static final String URL = "jdbc:postgresql://host.docker.internal:5432/connector";

  protected static final String USERNAME = "foo";

  protected static final String ENCRYPTED_SHH
    = "enc:JKS:crypto.p12:secret0:1:aes_gcm:AAAADKZPmASojt1iayb2bPy4D-Toq7TGLTN95HzCQAeJtz0=";

  @Autowired
  private HikariDataSource dataSource;

  @Autowired
  private EncryptionService encryptionService;

  @Autowired
  private EncryptedDatasourcePasswordConfig config;

  /**
   * Test to validate decryption is applied when Datasource is built and autowired by Spring
   */
  @Test
  public void testInjectedDatasourceGetsDecrypted() {
    assertThat(dataSource.getJdbcUrl()).isEqualTo(URL);
    assertThat(dataSource.getUsername()).isEqualTo(USERNAME);
    assertThat(dataSource.getPassword()).isEqualTo("shh");
  }

  @Test
  public void testEncryptedIsDecrypted() {
    DataSourceProperties original = new DataSourceProperties();
    original.setUrl(URL);
    original.setUsername(USERNAME);
    original.setPassword(ENCRYPTED_SHH);
    assertThat(dataSource.getJdbcUrl()).isEqualTo(URL);
    assertThat(dataSource.getUsername()).isEqualTo(USERNAME);
    assertThat(dataSource.getPassword()).isEqualTo("shh");
  }

  @Test
  public void testPlainTextNotDecrypted() {
    String plainText = "not encrypted";
    DataSourceProperties original = new DataSourceProperties();
    original.setUrl(URL);
    original.setUsername(USERNAME);
    original.setPassword(plainText);
    HikariDataSource dataSource = getDataSource(original);
    assertThat(dataSource.getJdbcUrl()).isEqualTo(URL);
    assertThat(dataSource.getUsername()).isEqualTo(USERNAME);
    assertThat(dataSource.getPassword()).isEqualTo(plainText);
  }

  private HikariDataSource getDataSource(DataSourceProperties original) {
    return (HikariDataSource) config.createDataSourceBuilder(original, encryptionService).build();
  }

  @Component
  public static class TestMockConfig {
    @Bean
    public EncryptionService decryptor() {
      EncryptionService mock = mock(EncryptionService.class);
      when(mock.withDecrypted(eq(EncryptedSecret.fromEncodedValue(ENCRYPTED_SHH)), Mockito.any(Function.class)))
        .thenAnswer((args) -> args.getArgument(1, Function.class).apply("shh".getBytes()));
      return mock;
    }
  }

}