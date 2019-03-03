package com.sappenin.interledger.ilpv4.connector.events;

import java.util.Objects;

public class GenericIlpNodeEvent<T> {

  protected boolean success;
  private T what;

  public GenericIlpNodeEvent(T what, boolean success) {
    this.what = Objects.requireNonNull(what);
    this.success = success;
  }


}
