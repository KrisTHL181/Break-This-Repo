package net.minecraft.client.sounds;

import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.floats.FloatConsumer;
import java.nio.ByteBuffer;
import java.util.List;
import net.minecraft.util.Mth;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.lwjgl.BufferUtils;

@OnlyIn(Dist.CLIENT)
public class ChunkedSampleByteBuf implements FloatConsumer {
   private final List<ByteBuffer> buffers = Lists.newArrayList();
   private final int bufferSize;
   private int byteCount;
   private ByteBuffer currentBuffer;

   public ChunkedSampleByteBuf(int p_330452_) {
      this.bufferSize = p_330452_ + 1 & -2;
      this.currentBuffer = BufferUtils.createByteBuffer(p_330452_);
   }

   public void accept(float p_332948_) {
      if (this.currentBuffer.remaining() == 0) {
         this.currentBuffer.flip();
         this.buffers.add(this.currentBuffer);
         this.currentBuffer = BufferUtils.createByteBuffer(this.bufferSize);
      }

      int i = Mth.clamp((int)(p_332948_ * 32767.5F - 0.5F), -32768, 32767);
      this.currentBuffer.putShort((short)i);
      this.byteCount += 2;
   }

   public ByteBuffer get() {
      this.currentBuffer.flip();
      if (this.buffers.isEmpty()) {
         return this.currentBuffer;
      }

      ByteBuffer bytebuffer = BufferUtils.createByteBuffer(this.byteCount);
      this.buffers.forEach(bytebuffer::put);
      bytebuffer.put(this.currentBuffer);
      bytebuffer.flip();
      return bytebuffer;
   }

   public int size() {
      return this.byteCount;
   }
}
