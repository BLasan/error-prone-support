package tech.picnic.errorprone.refasterrules;

import com.google.common.io.ByteStreams;
import com.google.errorprone.refaster.annotation.AfterTemplate;
import com.google.errorprone.refaster.annotation.BeforeTemplate;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import tech.picnic.errorprone.refaster.annotation.OnlineDocumentation;

/** Refaster rules related to expressions dealing with {@link InputStream}s. */
// XXX: Add a rule for `ByteStreams.skipFully(in, n)` -> `in.skipNBytes(n)` once we have a way to
// target JDK 12+ APIs.
@OnlineDocumentation
final class InputStreamRules {
  private InputStreamRules() {}

  static final class InputStreamTransferTo {
    @BeforeTemplate
    long before(InputStream in, OutputStream out) throws IOException {
      return ByteStreams.copy(in, out);
    }

    @AfterTemplate
    long after(InputStream in, OutputStream out) throws IOException {
      return in.transferTo(out);
    }
  }

  static final class InputStreamReadAllBytes {
    @BeforeTemplate
    byte[] before(InputStream in) throws IOException {
      return ByteStreams.toByteArray(in);
    }

    @AfterTemplate
    byte[] after(InputStream in) throws IOException {
      return in.readAllBytes();
    }
  }
}
