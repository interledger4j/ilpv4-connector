package org.interledger.connector.pubsub;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

public class CoordinationProxyGeneratorTest {

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  private CoordinationProxyGenerator generator;

  @Before
  public void setup() {
    generator = new CoordinationProxyGeneratorImpl();
  }

  @Test
  public void concreteClassFails() {
    expectedException.expect(IllegalArgumentException.class);
    generator.createCoordinatedProxy(new NoInterface());
  }

  @Test
  public void implOfInterface() {
    FooImpl instance = new FooImpl();
    Foo proxy = (Foo) generator.createCoordinatedProxy(instance);
    assertThat(proxy.value()).isEqualTo(instance.value());
    assertThat(proxy).isInstanceOf(Coordinated.class);
  }

  @Test
  public void finalImplOfInterface() {
    FinalFooImpl instance = new FinalFooImpl();
    Foo proxy = (Foo) generator.createCoordinatedProxy(instance);
    assertThat(proxy.value()).isEqualTo(instance.value());
    assertThat(proxy).isInstanceOf(Coordinated.class);
  }

  @Test
  public void immutablesInstance() {
    ImmutablesSampleMessage sampleMessage = ImmutablesSampleMessage.builder().build();
    ImmutablesSampleMessage proxy = (ImmutablesSampleMessage) generator.createCoordinatedProxy(sampleMessage);
    assertThat(proxy.value()).isEqualTo(sampleMessage.value());
    assertThat(proxy).isInstanceOf(Coordinated.class);
  }

  interface Foo {
    String value();
  }

  private static class FooImpl implements Foo {

    @Override
    public String value() {
      return "what a story mark";
    }
  }

  private static final class FinalFooImpl implements Foo {

    @Override
    public String value() {
      return "leave your stupid comments in your pocket!";
    }
  }

  private static class NoInterface {

    public String value() {
      return "i will record everything";
    }
  }

}
