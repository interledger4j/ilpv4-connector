package org.interledger.ilpv4.connector.persistence;

import org.interledger.connector.link.LinkType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.persistence.AttributeConverter;
import java.util.Map;

/**
 * An {@link AttributeConverter} for storing a {@link Map} as a JSON string.
 */
public class LinkTypeConverter implements AttributeConverter<LinkType, String> {

  @Override
  public String convertToDatabaseColumn(LinkType linkType) {
    return linkType == null ? null : linkType.value();
  }

  @Override
  public LinkType convertToEntityAttribute(String linkType) {
    return linkType == null ? null : LinkType.of(linkType);
  }

}