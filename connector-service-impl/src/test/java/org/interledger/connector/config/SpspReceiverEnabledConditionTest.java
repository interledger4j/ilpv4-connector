package org.interledger.connector.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.interledger.connector.core.ConfigConstants.DOT;
import static org.interledger.connector.core.ConfigConstants.ENABLED_FEATURES;
import static org.interledger.connector.core.ConfigConstants.ENABLED_PROTOCOLS;
import static org.interledger.connector.core.ConfigConstants.LOCAL_SPSP_FULFILLMENT_ENABLED;
import static org.interledger.connector.core.ConfigConstants.SPSP_ENABLED;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.env.Environment;
import org.springframework.core.type.AnnotatedTypeMetadata;

/**
 * Unit tests for {@link SpspReceiverEnabledCondition}.
 */
public class SpspReceiverEnabledConditionTest {

  private static final String FALSE = "false";
  private static final String TRUE = "true";
  private static final String PRECONDITION_MESSAGE
    = "Local SPSP fulfillment may not be disabled if `interledger.connector.enabledProtocols.spspEnabled` is enabled";

  @Rule
  public ExpectedException expectedException = ExpectedException.none();

  @Mock
  private Environment environmentMock;

  @Mock
  private AnnotatedTypeMetadata metadataMock;

  @Mock
  private ConditionContext contextMock;

  private SpspReceiverEnabledCondition condition;

  @Before
  public void setUp() {
    MockitoAnnotations.initMocks(this);
    when(contextMock.getEnvironment()).thenReturn(environmentMock);

    this.condition = new SpspReceiverEnabledCondition();
  }

  @Test
  public void exceptionWhenMissingAndMissing() {
    when(environmentMock.getProperty(ENABLED_PROTOCOLS + DOT + SPSP_ENABLED)).thenReturn(null);
    when(environmentMock.getProperty(ENABLED_FEATURES + DOT + LOCAL_SPSP_FULFILLMENT_ENABLED)).thenReturn(null);
    assertThat(condition.matches(contextMock, metadataMock)).isFalse();
  }

  @Test
  public void matchesWhenMissingAndTrue() {
    when(environmentMock.getProperty(ENABLED_PROTOCOLS + DOT + SPSP_ENABLED)).thenReturn(null);
    when(environmentMock.getProperty(ENABLED_FEATURES + DOT + LOCAL_SPSP_FULFILLMENT_ENABLED)).thenReturn(TRUE);
    assertThat(condition.matches(contextMock, metadataMock)).isTrue();
  }

  @Test
  public void matchesWhenMissingAndFalse() {
    when(environmentMock.getProperty(ENABLED_PROTOCOLS + DOT + SPSP_ENABLED)).thenReturn(null);
    when(environmentMock.getProperty(ENABLED_FEATURES + DOT + LOCAL_SPSP_FULFILLMENT_ENABLED)).thenReturn(FALSE);
    assertThat(condition.matches(contextMock, metadataMock)).isFalse();
  }

  @Test
  public void exceptionWhenTrueAndMissing() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(PRECONDITION_MESSAGE);
    when(environmentMock.getProperty(ENABLED_PROTOCOLS + DOT + SPSP_ENABLED)).thenReturn(TRUE);
    when(environmentMock.getProperty(ENABLED_FEATURES + DOT + LOCAL_SPSP_FULFILLMENT_ENABLED)).thenReturn(null);
    condition.matches(contextMock, metadataMock);
  }

  @Test
  public void matchesWhenTrueAndFalse() {
    expectedException.expect(IllegalArgumentException.class);
    expectedException.expectMessage(PRECONDITION_MESSAGE);
    when(environmentMock.getProperty(ENABLED_PROTOCOLS + DOT + SPSP_ENABLED)).thenReturn(TRUE);
    when(environmentMock.getProperty(ENABLED_FEATURES + DOT + LOCAL_SPSP_FULFILLMENT_ENABLED)).thenReturn(FALSE);

    condition.matches(contextMock, metadataMock);
  }

  @Test
  public void matchesWhenTrueAndTrue() {
    when(environmentMock.getProperty(ENABLED_PROTOCOLS + DOT + SPSP_ENABLED)).thenReturn(TRUE);
    when(environmentMock.getProperty(ENABLED_FEATURES + DOT + LOCAL_SPSP_FULFILLMENT_ENABLED)).thenReturn(TRUE);
    assertThat(condition.matches(contextMock, metadataMock)).isTrue();
  }

  @Test
  public void exceptionWhenFalseAndMissing() {
    when(environmentMock.getProperty(ENABLED_PROTOCOLS + DOT + SPSP_ENABLED)).thenReturn(FALSE);
    when(environmentMock.getProperty(ENABLED_FEATURES + DOT + LOCAL_SPSP_FULFILLMENT_ENABLED)).thenReturn(null);
    assertThat(condition.matches(contextMock, metadataMock)).isFalse();
  }

  @Test
  public void exceptionWhenFalseAndFalse() {
    when(environmentMock.getProperty(ENABLED_PROTOCOLS + DOT + SPSP_ENABLED)).thenReturn(FALSE);
    when(environmentMock.getProperty(ENABLED_FEATURES + DOT + LOCAL_SPSP_FULFILLMENT_ENABLED)).thenReturn(FALSE);
    assertThat(condition.matches(contextMock, metadataMock)).isFalse();
  }

  @Test
  public void exceptionWhenFalseAndTrue() {
    when(environmentMock.getProperty(ENABLED_PROTOCOLS + DOT + SPSP_ENABLED)).thenReturn(FALSE);
    when(environmentMock.getProperty(ENABLED_FEATURES + DOT + LOCAL_SPSP_FULFILLMENT_ENABLED)).thenReturn(TRUE);
    assertThat(condition.matches(contextMock, metadataMock)).isTrue();
  }

}
