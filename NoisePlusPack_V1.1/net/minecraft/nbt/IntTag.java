package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public record IntTag(int value) implements NumericTag {
   private static final int SELF_SIZE_IN_BYTES = 12;
   public static final TagType<IntTag> TYPE = new TagType.StaticSize<IntTag>() {
      public IntTag load(DataInput p_128708_, NbtAccounter p_128710_) throws IOException {
         return IntTag.valueOf(readAccounted(p_128708_, p_128710_));
      }

      @Override
      public StreamTagVisitor.ValueResult parse(DataInput p_197483_, StreamTagVisitor p_197484_, NbtAccounter p_301753_) throws IOException {
         return p_197484_.visit(readAccounted(p_197483_, p_301753_));
      }

      private static int readAccounted(DataInput p_301711_, NbtAccounter p_301714_) throws IOException {
         p_301714_.accountBytes(12L);
         return p_301711_.readInt();
      }

      @Override
      public int size() {
         return 4;
      }

      @Override
      public String getName() {
         return "INT";
      }

      @Override
      public String getPrettyName() {
         return "TAG_Int";
      }
   };

   public static IntTag valueOf(int p_128680_) {
      return p_128680_ >= -128 && p_128680_ <= 1024 ? IntTag.Cache.cache[p_128680_ - -128] : new IntTag(p_128680_);
   }

   @Override
   public void write(DataOutput p_128682_) throws IOException {
      p_128682_.writeInt(this.value);
   }

   @Override
   public int sizeInBytes() {
      return 12;
   }

   @Override
   public byte getId() {
      return 3;
   }

   @Override
   public TagType<IntTag> getType() {
      return TYPE;
   }

   public IntTag copy() {
      return this;
   }

   @Override
   public void accept(TagVisitor p_177984_) {
      p_177984_.visitInt(this);
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
      return (short)(this.value & 65535);
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
   public StreamTagVisitor.ValueResult accept(StreamTagVisitor p_197481_) {
      return p_197481_.visit(this.value);
   }

   @Override
   public String toString() {
      StringTagVisitor stringtagvisitor = new StringTagVisitor();
      stringtagvisitor.visitInt(this);
      return stringtagvisitor.build();
   }

   static class Cache {
      private static final int HIGH = 1024;
      private static final int LOW = -128;
      static final IntTag[] cache = new IntTag[1153];

      private Cache() {
      }

      static {
         for (int i = 0; i < cache.length; i++) {
            cache[i] = new IntTag(-128 + i);
         }
      }
   }
}
