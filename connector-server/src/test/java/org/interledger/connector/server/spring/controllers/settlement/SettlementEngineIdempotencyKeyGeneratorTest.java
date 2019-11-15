package org.interledger.connector.server.spring.controllers.settlement;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.UUID;

/**
 * Unit tests for {@link SettlementEngineIdempotencyKeyGenerator}.
 */
public class SettlementEngineIdempotencyKeyGeneratorTest {

  @Mock
  private Object objectMock;

  @Mock
  private SettlementController settlementControllerMock;

  private SettlementEngineIdempotencyKeyGenerator generator;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    this.generator = new SettlementEngineIdempotencyKeyGenerator();
  }

  @Test(expected = IllegalArgumentException.class)
  public void generateWithTooFewParams() {
    try {
      generator.generate(settlementControllerMock, null, new Object[0]);
    } catch (IllegalArgumentException e) {
      assertThat(e.getMessage()).isEqualTo("params is expected to have at least 1 value");
      throw e;
    }
  }

  @Test
  public void generateWithNonSettlementControllerTarget() {
    assertThat(generator.generate(objectMock, null, new Object[0])).isNull();
  }

  @Test
  public void generateWithOneParam() {
    final String idempotencyId = UUID.randomUUID().toString();
    assertThat(generator.generate(settlementControllerMock, null, new Object[] {idempotencyId})).isEqualTo(idempotencyId);
  }

  @Test
  public void generateWithMultipleParams() {
    final String idempotencyId = UUID.randomUUID().toString();
    assertThat(generator.generate(settlementControllerMock, null, new Object[] {idempotencyId, "foo", "bar"})).isEqualTo(idempotencyId);
  }
}
