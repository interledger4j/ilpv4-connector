package org.interledger.crypto;

import static org.assertj.core.api.Assertions.assertThat;

import com.google.common.base.Stopwatch;
import org.assertj.core.data.Percentage;
import org.junit.Test;

import java.util.Arrays;

public class ByteArrayUtilsTest {

  @Test
  public void isEqualUsingConstantTime() {
    byte [] bytes = "some bytes".getBytes();
    byte [] sameBytes = "some bytes".getBytes();
    byte [] differentBytes = "different bytes".getBytes();

    assertThat(ByteArrayUtils.isEqualUsingConstantTime(bytes, sameBytes)).isTrue();
    assertThat(ByteArrayUtils.isEqualUsingConstantTime(sameBytes, bytes)).isTrue();
    assertThat(ByteArrayUtils.isEqualUsingConstantTime(new byte[0], new byte[0])).isTrue();

    assertThat(ByteArrayUtils.isEqualUsingConstantTime(bytes, differentBytes)).isFalse();
    assertThat(ByteArrayUtils.isEqualUsingConstantTime(differentBytes, bytes)).isFalse();
  }

  /**
   * Test {@link ByteArrayUtils#isEqualUsingConstantTime} actually runs in constant time.
   */
  @Test
  public void isEqualRunsInConstantTime() {
    // calculate a baseline by comparing 2 byte arrays with identical values (which requires comparing every byte)
    // calculate comparing 2 byte arrays with different values (which does not require comparing every byte but should)
    // if the 2 timings are roughly the same, then the algorithm is constant time

    int arraySize = 250000;
    byte [] lotsOfBytes = new byte[arraySize];
    Arrays.fill(lotsOfBytes, (byte) 0);

    byte [] sameLotsOfBytes = new byte[arraySize];
    Arrays.fill(sameLotsOfBytes, (byte) 0);

    byte [] lotsMoreDifferentBytes = new byte[arraySize];
    Arrays.fill(lotsMoreDifferentBytes, (byte) 1);
    int iterations = 10000;

    // prime the JIT before benchmarking
    for (int i = 0; i < 1000; i++) {
      assertThat(ByteArrayUtils.isEqualUsingConstantTime(lotsOfBytes, lotsMoreDifferentBytes)).isFalse();
    }

    Stopwatch baseline = Stopwatch.createStarted();
    for (int i = 0; i < iterations; i++) {
      assertThat(ByteArrayUtils.isEqualUsingConstantTime(lotsOfBytes, sameLotsOfBytes)).isTrue();
    }
    baseline.stop();

    Stopwatch test = Stopwatch.createStarted();
    for (int i = 0; i < iterations; i++) {
      assertThat(ByteArrayUtils.isEqualUsingConstantTime(lotsOfBytes, lotsMoreDifferentBytes)).isFalse();
    }
    test.stop();

    // timings have lots of noise since benchmark doesnt run for a long time.
    // 75% seems like a reasonable variance but may need to be widened if we find more jitter in the results
    assertThat(test.elapsed().toMillis()).isCloseTo(baseline.elapsed().toMillis(), Percentage.withPercentage(75.0));
  }

  @Test
  public void generateRandom32Bytes() {
    assertThat(ByteArrayUtils.generate32RandomBytes())
        .hasSize(32)
        .isNotEqualTo(ByteArrayUtils.generate32RandomBytes());
  }

}
