package org.interledger.connector.server.spring.settings.javamoney;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.google.common.collect.Sets;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;

import java.util.HashSet;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Supplier;

public class ApplicationContextProviderTest {

  @Test
  public void getAllApplicationContexts() {
    assertThat(ApplicationContextProvider.findActiveContexts(
        Sets.newHashSet(mockContext(() -> false), mockContext(() -> false)))).hasSize(0);

    assertThat(ApplicationContextProvider.findActiveContexts(
        Sets.newHashSet(mockContext(() -> true), mockContext(() -> false)))).hasSize(1);

    assertThat(ApplicationContextProvider.findActiveContexts(
        Sets.newHashSet(mockContext(() -> true), mockContext(() -> true)))).hasSize(2);
  }

  @Test
  public void getAllApplicationContextsWithChangingValue() {
    AtomicBoolean active = new AtomicBoolean(false);
    HashSet<ApplicationContext> contexts = Sets.newHashSet(mockContext(() -> active.get()), mockContext(() -> false));
    assertThat(ApplicationContextProvider.findActiveContexts(contexts)).hasSize(0);

    active.set(true);
    assertThat(ApplicationContextProvider.findActiveContexts(contexts)).hasSize(1);

    active.set(false);
    assertThat(ApplicationContextProvider.findActiveContexts(contexts)).hasSize(0);
  }

  private ApplicationContext mockContext(Supplier<Boolean> isActive) {
    ConfigurableApplicationContext mock = mock(ConfigurableApplicationContext.class);
    when(mock.isActive()).thenAnswer((args) -> isActive.get());
    return mock;
  }


}
