package com.sappenin.ilpv4.plugins.btp;

import org.immutables.value.Value;

@Value.Immutable
public interface BtpSessionCredentials {

  String getName();

  //String token();

}
