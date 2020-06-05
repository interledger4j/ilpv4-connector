package org.interledger.connector.opa.model;

import java.util.Optional;

public interface AuthorizablePayment {

  Optional<String> userAuthorizationUrl();

}
