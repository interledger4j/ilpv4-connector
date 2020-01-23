package org.interledger.connector.server.spring.gcp;

import ch.qos.logback.core.OutputStreamAppender;
import ch.qos.logback.core.joran.spi.ConsoleTarget;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Console appender that buffers output and periodically flushes. By default, output will be flushed
 * every 100KB or 1000ms (whichever comes first). These can be overridden via {@link #setBufferSize(int)}
 * and {@link #setFlushRateMillis(int)}
 *
 * @param <E>
 */
public class BufferedConsoleAppender<E> extends OutputStreamAppender<E> {

  private final ScheduledExecutorService scheduledFlusher;
  private OutputStream consoleStream = ConsoleTarget.SystemOut.getStream();

  private int bufferSize = 100 * 1024;

  private int flushRateMillis = 1000;
  private BufferedOutputStream targetStream;

  public BufferedConsoleAppender() {
    scheduledFlusher = Executors.newScheduledThreadPool(1);
  }

  /**
   * Set the number of bytes that are buffered before writing to the underlying console stream
   *
   * @param bufferSize buffer size in bytes
   */
  public void setBufferSize(int bufferSize) {
    this.bufferSize = bufferSize;
  }

  /**
   * Sets how frequently the underlying console stream is flushed.
   * @param flushRateMillis flush rate in millseconds
   */
  public void setFlushRateMillis(int flushRateMillis) {
    this.flushRateMillis = flushRateMillis;
  }

  protected void setConsoleStream(OutputStream consoleStream) {
    this.consoleStream = consoleStream;
  }

  @Override
  public void start() {
    super.setImmediateFlush(false);
    targetStream = new BufferedOutputStream(consoleStream, bufferSize);
    setOutputStream(targetStream);
    super.start();
    startFlushing();
  }

  @Override
  public void stop() {
    scheduledFlusher.shutdown();
    super.stop();
  }

  private void startFlushing() {
    scheduledFlusher.scheduleAtFixedRate(this::flushStream,
      flushRateMillis,
      flushRateMillis,
      TimeUnit.MILLISECONDS);
    Runtime.getRuntime().addShutdownHook(new Thread(() -> flushStream()));
  }

  private void flushStream() {
    try {
      targetStream.flush();
    } catch (IOException e) {
      e.printStackTrace(); // this should never happen with System.out
    }
  }
}
