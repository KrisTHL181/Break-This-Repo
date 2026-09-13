package net.minecraft.network;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.ReferenceCounted;

public record HiddenByteBuf(ByteBuf contents) implements ReferenceCounted {
   public HiddenByteBuf(final ByteBuf contents) {
      this.contents = ByteBufUtil.ensureAccessible(contents);
   }

   public static Object pack(Object p_376336_) {
      return p_376336_ instanceof ByteBuf bytebuf ? new HiddenByteBuf(bytebuf) : p_376336_;
   }

   public static Object unpack(Object p_376438_) {
      return p_376438_ instanceof HiddenByteBuf hiddenbytebuf ? ByteBufUtil.ensureAccessible(hiddenbytebuf.contents) : p_376438_;
   }

   public int refCnt() {
      return this.contents.refCnt();
   }

   public HiddenByteBuf retain() {
      this.contents.retain();
      return this;
   }

   public HiddenByteBuf retain(int p_377803_) {
      this.contents.retain(p_377803_);
      return this;
   }

   public HiddenByteBuf touch() {
      this.contents.touch();
      return this;
   }

   public HiddenByteBuf touch(Object p_376790_) {
      this.contents.touch(p_376790_);
      return this;
   }

   public boolean release() {
      return this.contents.release();
   }

   public boolean release(int p_377565_) {
      return this.contents.release(p_377565_);
   }
}
