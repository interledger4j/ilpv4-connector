package org.interledger.connector.opa.model;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import okhttp3.HttpUrl;
import org.immutables.value.Value;

@JsonSerialize(as = ImmutableNewCharge.class)
@JsonDeserialize(as = ImmutableNewCharge.class)
public interface NewCharge {

  HttpUrl invoice();

  @Value.Immutable
  abstract class AbstractNewCharge implements NewCharge {

  }

}
