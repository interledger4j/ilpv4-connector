package org.interledger.connector.persistence;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.persistence.AttributeConverter;
import javax.persistence.Converter;
import java.io.IOException;
import java.util.Map;
import java.util.Objects;

/**
 * An {@link AttributeConverter} for storing a {@link Map} as a JSON string.
 */
@Component
@Converter
public class HashMapConverter implements AttributeConverter<Map<String, Object>, String> {

  private final Logger logger = LoggerFactory.getLogger(this.getClass());

  private ObjectMapper objectMapper;

  public HashMapConverter() {
  }

  public HashMapConverter(ObjectMapper objectMapper) {
    this.objectMapper = Objects.requireNonNull(objectMapper);
  }

  @Override
  public String convertToDatabaseColumn(Map<String, Object> customerInfo) {

    if (customerInfo == null) {
      return null;
    } else {
      String customInfo = null;
      try {
        customInfo = objectMapper.writeValueAsString(customerInfo);
      } catch (final JsonProcessingException e) {
        logger.error("JSON writing error", e);
      }

      return customInfo;
    }
  }

  @Override
  public Map<String, Object> convertToEntityAttribute(String customInfoJSON) {
    if (customInfoJSON == null) {
      return null;
    } else {
      Map<String, Object> customInfo = null;
      try {
        customInfo = objectMapper.readValue(customInfoJSON, Map.class);
      } catch (final IOException e) {
        logger.error("JSON reading error", e);
      }

      return customInfo;
    }
  }

  @Autowired
  public void setObjectMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
  }

}
