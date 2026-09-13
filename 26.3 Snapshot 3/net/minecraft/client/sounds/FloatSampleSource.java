package net.minecraft.client.sounds;

import it.unimi.dsi.fastutil.floats.FloatConsumer;
import java.io.IOException;
import java.nio.ByteBuffer;

public interface FloatSampleSource extends FiniteAudioStream {
   int EXPECTED_MAX_FRAME_SIZE = 8192;

   boolean readChunk(FloatConsumer output) throws IOException;

   @Override
   default ByteBuffer read(final int expectedSize) throws IOException {
      ChunkedSampleByteBuf output = new ChunkedSampleByteBuf(expectedSize + 8192);

      while (this.readChunk(output) && output.size() < expectedSize) {
      }

      return output.get();
   }

   @Override
   default ByteBuffer readAll() throws IOException {
      ChunkedSampleByteBuf output = new ChunkedSampleByteBuf(16384);

      while (this.readChunk(output)) {
      }

      return output.get();
   }
}
