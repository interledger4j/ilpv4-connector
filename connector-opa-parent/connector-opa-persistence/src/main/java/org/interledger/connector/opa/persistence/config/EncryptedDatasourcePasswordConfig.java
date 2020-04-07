package org.interledger.connector.opa.persistence.config;

import org.interledger.crypto.EncryptedSecret;
import org.interledger.crypto.EncryptionService;

import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.jdbc.DataSourceBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

@Configuration
public class EncryptedDatasourcePasswordConfig {

  /**
   * Overrides default {@link DataSource} to support using "spring.datasource.password" with an {@link EncryptedSecret}.
   * Plain-text passwords are also supported if the password is not in the encrypted format.
   *
   * @param dataSourceProperties
   * @param encryptionService
   * @return datasource
   */
  @Bean
  @Primary
  public DataSource datasource(DataSourceProperties dataSourceProperties, EncryptionService encryptionService) {
    return createDataSourceBuilder(dataSourceProperties, encryptionService).build();
  }

  protected DataSourceBuilder createDataSourceBuilder(DataSourceProperties dataSourceProperties,
                                                      EncryptionService encryptionService) {
    DataSourceBuilder dataSourceBuilder = DataSourceBuilder.create();
    dataSourceBuilder.url(dataSourceProperties.determineUrl());
    dataSourceBuilder.username(dataSourceProperties.getUsername());
    if (dataSourceProperties.getPassword() != null && dataSourceProperties.getPassword().startsWith("enc")) {
      encryptionService.withDecrypted(EncryptedSecret.fromEncodedValue(dataSourceProperties.getPassword()),
        (decrypted) -> dataSourceBuilder.password(new String(decrypted)));
    } else {
      dataSourceBuilder.password(dataSourceProperties.getPassword());
    }
    return dataSourceBuilder;
  }

}
