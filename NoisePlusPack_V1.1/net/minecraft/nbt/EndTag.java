package net.minecraft.nbt;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;

public final class EndTag implements Tag {
   private static final int SELF_SIZE_IN_BYTES = 8;
   public static final TagType<EndTag> TYPE = new TagType<EndTag>() {
      public EndTag load(DataInput p_128550_, NbtAccounter p_128552_) {
         p_128552_.accountBytes(8L);
         return EndTag.INSTANCE;
      }

      @Override
      public StreamTagVisitor.ValueResult parse(DataInput p_197465_, StreamTagVisitor p_197466_, NbtAccounter p_301715_) {
         p_301715_.accountBytes(8L);
         return p_197466_.visitEnd();
      }

      @Override
      public void skip(DataInput p_197460_, int p_301764_, NbtAccounter p_301761_) {
      }

      @Override
      public void skip(DataInput p_197462_, NbtAccounter p_301747_) {
      }

      @Override
      public String getName() {
         return "END";
      }

      @Override
      public String getPrettyName() {
         return "TAG_End";
      }
   };
   public static final EndTag INSTANCE = new EndTag();

   private EndTag() {
   }

   @Override
   public void write(DataOutput p_128539_) throws IOException {
   }

   @Override
   public int sizeInBytes() {
      return 8;
   }

   @Override
   public byte getId() {
      return 0;
   }

   @Override
   public TagType<EndTag> getType() {
      return TYPE;
   }

   @Override
   public String toString() {
      StringTagVisitor stringtagvisitor = new StringTagVisitor();
      stringtagvisitor.visitEnd(this);
      return stringtagvisitor.build();
   }

   public EndTag copy() {
      return this;
   }

   @Override
   public void accept(TagVisitor p_177863_) {
      p_177863_.visitEnd(this);
   }

   @Override
   public StreamTagVisitor.ValueResult accept(StreamTagVisitor p_197458_) {
      return p_197458_.visitEnd();
   }
}
