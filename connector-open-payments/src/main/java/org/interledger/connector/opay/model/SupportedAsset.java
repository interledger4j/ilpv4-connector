package org.interledger.connector.opay.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

@Value.Immutable
@JsonSerialize(as = ImmutableSupportedAsset.class)
@JsonDeserialize(as = ImmutableSupportedAsset.class)
public interface SupportedAsset {

  static ImmutableSupportedAsset.Builder builder(String assetCode, short assetScale) {
    return ImmutableSupportedAsset
      .builder()
      .assetCode(assetCode)
      .assetScale(assetScale);
  }

  @JsonProperty("code")
  String assetCode();

  @JsonProperty("scale")
  short assetScale();
}
