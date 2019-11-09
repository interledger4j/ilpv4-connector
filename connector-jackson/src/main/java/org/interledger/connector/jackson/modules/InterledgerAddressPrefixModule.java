package org.interledger.connector.jackson.modules;

import org.interledger.core.InterledgerAddressPrefix;

import com.fasterxml.jackson.core.Version;
import com.fasterxml.jackson.databind.module.SimpleModule;

public class InterledgerAddressPrefixModule extends SimpleModule {

  private static final String NAME = "InterledgerAddressPrefixModule";

  public InterledgerAddressPrefixModule() {
    super(
        NAME,
        new Version(1, 0, 0, null, "org.interledger.connector",
            "interledger-address-prefix")
    );

    addSerializer(InterledgerAddressPrefix.class, new InterledgerAddressPrefixSerializer());
    addDeserializer(InterledgerAddressPrefix.class, new InterledgerAddressPrefixDeserializer());
  }
}
