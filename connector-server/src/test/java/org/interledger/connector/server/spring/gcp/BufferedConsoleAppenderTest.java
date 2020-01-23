package org.interledger.connector.server.spring.gcp;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import ch.qos.logback.core.encoder.EchoEncoder;
import com.google.common.base.Strings;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.OutputStream;

public class BufferedConsoleAppenderTest {

  private OutputStream mockStream;
  private BufferedConsoleAppender consoleAppender;

  @Before
  public void setUp() {
    mockStream = mock(OutputStream.class);
    consoleAppender = new BufferedConsoleAppender();
    consoleAppender.setConsoleStream(mockStream);
    consoleAppender.setEncoder(new EchoEncoder());
  }

  @After
  public void tearDown() {
    if (consoleAppender.isStarted()) {
      consoleAppender.stop();
    }
  }

  @Test
  public void lessThanBufferGetsWrittenOnTimer() throws Exception {
    int flushRateMillis = 1000;
    int bufferSize = 100;
    consoleAppender.setFlushRateMillis(flushRateMillis);
    consoleAppender.setBufferSize(bufferSize);
    consoleAppender.start();
    verifyNoInteractions(mockStream);

    consoleAppender.doAppend(Strings.repeat("a", bufferSize - 2));
    verifyNoInteractions(mockStream);

    Thread.sleep(flushRateMillis + 100);
    verify(mockStream, times(1)).write(any(), anyInt(), anyInt());
    verify(mockStream, times(1)).flush();
  }

  @Test
  public void longerThanBufferGetsWrittenImmediately() throws Exception {
    int flushRateMillis = 10000;
    int bufferSize = 100;

    consoleAppender.setFlushRateMillis(flushRateMillis);
    consoleAppender.setBufferSize(bufferSize);
    consoleAppender.start();
    verifyNoInteractions(mockStream);

    consoleAppender.doAppend(Strings.repeat("b", bufferSize - 1));
    verify(mockStream, times(1)).write(any(), anyInt(), anyInt());
  }

  @Test
  public void bufferedOutputGetsWrittenOnStop() throws Exception {
    int flushRateMillis = 1000;
    int bufferSize = 100;
    consoleAppender.setFlushRateMillis(flushRateMillis);
    consoleAppender.setBufferSize(bufferSize);
    consoleAppender.start();
    verifyNoInteractions(mockStream);

    consoleAppender.doAppend(Strings.repeat("a", bufferSize - 2));
    verifyNoInteractions(mockStream);

    consoleAppender.stop();
    verify(mockStream, times(1)).write(any(), anyInt(), anyInt());
  }

}