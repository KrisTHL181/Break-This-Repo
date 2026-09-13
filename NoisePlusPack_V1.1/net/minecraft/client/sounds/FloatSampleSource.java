package net.minecraft.client.sounds;

import it.unimi.dsi.fastutil.floats.FloatConsumer;
import java.io.IOException;
import java.nio.ByteBuffer;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public interface FloatSampleSource extends FiniteAudioStream {
   int EXPECTED_MAX_FRAME_SIZE = 8192;

   boolean readChunk(FloatConsumer var1) throws IOException;

   @Override
   default ByteBuffer read(int p_332929_) throws IOException {
      ChunkedSampleByteBuf chunkedsamplebytebuf = new ChunkedSampleByteBuf(p_332929_ + 8192);

      while (this.readChunk(chunkedsamplebytebuf) && chunkedsamplebytebuf.size() < p_332929_) {
      }

      return chunkedsamplebytebuf.get();
   }

   @Override
   default ByteBuffer readAll() throws IOException {
      ChunkedSampleByteBuf chunkedsamplebytebuf = new ChunkedSampleByteBuf(16384);

      while (this.readChunk(chunkedsamplebytebuf)) {
      }

      return chunkedsamplebytebuf.get();
   }
}
