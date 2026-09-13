package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public record ShortTag(short value) implements NumericTag {
   private static final int SELF_SIZE_IN_BYTES = 10;
   public static final TagType<ShortTag> TYPE = new TagType.StaticSize<ShortTag>() {
      public ShortTag load(DataInput p_129282_, NbtAccounter p_129284_) throws IOException {
         return ShortTag.valueOf(readAccounted(p_129282_, p_129284_));
      }

      @Override
      public StreamTagVisitor.ValueResult parse(DataInput p_197517_, StreamTagVisitor p_197518_, NbtAccounter p_301775_) throws IOException {
         return p_197518_.visit(readAccounted(p_197517_, p_301775_));
      }

      private static short readAccounted(DataInput p_301710_, NbtAccounter p_301704_) throws IOException {
         p_301704_.accountBytes(10L);
         return p_301710_.readShort();
      }

      @Override
      public int size() {
         return 2;
      }

      @Override
      public String getName() {
         return "SHORT";
      }

      @Override
      public String getPrettyName() {
         return "TAG_Short";
      }
   };

   public static ShortTag valueOf(short p_129259_) {
      return p_129259_ >= -128 && p_129259_ <= 1024 ? ShortTag.Cache.cache[p_129259_ - -128] : new ShortTag(p_129259_);
   }

   @Override
   public void write(DataOutput p_129254_) throws IOException {
      p_129254_.writeShort(this.value);
   }

   @Override
   public int sizeInBytes() {
      return 10;
   }

   @Override
   public byte getId() {
      return 2;
   }

   @Override
   public TagType<ShortTag> getType() {
      return TYPE;
   }

   public ShortTag copy() {
      return this;
   }

   @Override
   public void accept(TagVisitor p_178084_) {
      p_178084_.visitShort(this);
   }

   @Override
   public long longValue() {
      return this.value;
   }

   @Override
   public int intValue() {
      return this.value;
   }

   @Override
   public short shortValue() {
      return this.value;
   }

   @Override
   public byte byteValue() {
      return (byte)(this.value & 0xFF);
   }

   @Override
   public double doubleValue() {
      return this.value;
   }

   @Override
   public float floatValue() {
      return this.value;
   }

   @Override
   public Number box() {
      return this.value;
   }

   @Override
   public StreamTagVisitor.ValueResult accept(StreamTagVisitor p_197515_) {
      return p_197515_.visit(this.value);
   }

   @Override
   public String toString() {
      StringTagVisitor stringtagvisitor = new StringTagVisitor();
      stringtagvisitor.visitShort(this);
      return stringtagvisitor.build();
   }

   static class Cache {
      private static final int HIGH = 1024;
      private static final int LOW = -128;
      static final ShortTag[] cache = new ShortTag[1153];

      private Cache() {
      }

      static {
         for (int i = 0; i < cache.length; i++) {
            cache[i] = new ShortTag((short)(-128 + i));
         }
      }
   }
}
