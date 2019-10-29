package org.interledger.connector.persistence;

import org.interledger.link.LinkType;

import java.util.Map;

import javax.persistence.AttributeConverter;

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
