package org.interledger.openpayments;

import java.util.Optional;

public interface AuthorizablePayment {

  Optional<String> userAuthorizationUrl();

}
