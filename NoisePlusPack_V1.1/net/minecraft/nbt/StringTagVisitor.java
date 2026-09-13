package net.minecraft.nbt;

import java.util.ArrayList;
import java.util.List;
import java.util.Map.Entry;
import java.util.regex.Pattern;

public class StringTagVisitor implements TagVisitor {
   private static final Pattern UNQUOTED_KEY_MATCH = Pattern.compile("[A-Za-z._]+[A-Za-z0-9._+-]*");
   private final StringBuilder builder = new StringBuilder();

   public String build() {
      return this.builder.toString();
   }

   @Override
   public void visitString(StringTag p_178186_) {
      this.builder.append(StringTag.quoteAndEscape(p_178186_.value()));
   }

   @Override
   public void visitByte(ByteTag p_178164_) {
      this.builder.append(p_178164_.value()).append('b');
   }

   @Override
   public void visitShort(ShortTag p_178184_) {
      this.builder.append(p_178184_.value()).append('s');
   }

   @Override
   public void visitInt(IntTag p_178176_) {
      this.builder.append(p_178176_.value());
   }

   @Override
   public void visitLong(LongTag p_178182_) {
      this.builder.append(p_178182_.value()).append('L');
   }

   @Override
   public void visitFloat(FloatTag p_178172_) {
      this.builder.append(p_178172_.value()).append('f');
   }

   @Override
   public void visitDouble(DoubleTag p_178168_) {
      this.builder.append(p_178168_.value()).append('d');
   }

   @Override
   public void visitByteArray(ByteArrayTag p_178162_) {
      this.builder.append("[B;");
      byte[] abyte = p_178162_.getAsByteArray();

      for (int i = 0; i < abyte.length; i++) {
         if (i != 0) {
            this.builder.append(',');
         }

         this.builder.append(abyte[i]).append('B');
      }

      this.builder.append(']');
   }

   @Override
   public void visitIntArray(IntArrayTag p_178174_) {
      this.builder.append("[I;");
      int[] aint = p_178174_.getAsIntArray();

      for (int i = 0; i < aint.length; i++) {
         if (i != 0) {
            this.builder.append(',');
         }

         this.builder.append(aint[i]);
      }

      this.builder.append(']');
   }

   @Override
   public void visitLongArray(LongArrayTag p_178180_) {
      this.builder.append("[L;");
      long[] along = p_178180_.getAsLongArray();

      for (int i = 0; i < along.length; i++) {
         if (i != 0) {
            this.builder.append(',');
         }

         this.builder.append(along[i]).append('L');
      }

      this.builder.append(']');
   }

   @Override
   public void visitList(ListTag p_178178_) {
      this.builder.append('[');

      for (int i = 0; i < p_178178_.size(); i++) {
         if (i != 0) {
            this.builder.append(',');
         }

         p_178178_.get(i).accept(this);
      }

      this.builder.append(']');
   }

   @Override
   public void visitCompound(CompoundTag p_178166_) {
      this.builder.append('{');
      List<Entry<String, Tag>> list = new ArrayList<>(p_178166_.entrySet());
      list.sort(Entry.comparingByKey());

      for (int i = 0; i < list.size(); i++) {
         Entry<String, Tag> entry = list.get(i);
         if (i != 0) {
            this.builder.append(',');
         }

         this.handleKeyEscape(entry.getKey());
         this.builder.append(':');
         entry.getValue().accept(this);
      }

      this.builder.append('}');
   }

   private void handleKeyEscape(String p_398005_) {
      if (!p_398005_.equalsIgnoreCase("true") && !p_398005_.equalsIgnoreCase("false") && UNQUOTED_KEY_MATCH.matcher(p_398005_).matches()) {
         this.builder.append(p_398005_);
      } else {
         StringTag.quoteAndEscape(p_398005_, this.builder);
      }
   }

   @Override
   public void visitEnd(EndTag p_178170_) {
      this.builder.append("END");
   }
}
