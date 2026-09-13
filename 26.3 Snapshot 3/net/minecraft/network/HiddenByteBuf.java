package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.ReferenceCounted;

public record HiddenByteBuf(ByteBuf contents) implements ReferenceCounted {
   public HiddenByteBuf(final ByteBuf contents) {
      this.contents = ByteBufUtil.ensureAccessible(contents);
   }

   public static Object pack(final Object msg) {
      return msg instanceof ByteBuf buf ? new HiddenByteBuf(buf) : msg;
   }

   public static Object unpack(final Object msg) {
      return msg instanceof HiddenByteBuf buf ? ByteBufUtil.ensureAccessible(buf.contents) : msg;
   }

   public int refCnt() {
      return this.contents.refCnt();
   }

   public HiddenByteBuf retain() {
      this.contents.retain();
      return this;
   }

   public HiddenByteBuf retain(final int increment) {
      this.contents.retain(increment);
      return this;
   }

   public HiddenByteBuf touch() {
      this.contents.touch();
      return this;
   }

   public HiddenByteBuf touch(final Object hint) {
      this.contents.touch(hint);
      return this;
   }

   public boolean release() {
      return this.contents.release();
   }

   public boolean release(final int decrement) {
      return this.contents.release(decrement);
   }
}
