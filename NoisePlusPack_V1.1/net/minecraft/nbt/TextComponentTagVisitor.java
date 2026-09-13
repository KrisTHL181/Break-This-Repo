package net.minecraft.nbt;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.mojang.logging.LogUtils;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import org.slf4j.Logger;

public class TextComponentTagVisitor implements TagVisitor {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int INLINE_LIST_THRESHOLD = 8;
   private static final int MAX_DEPTH = 64;
   private static final int MAX_LENGTH = 128;
   private static final ChatFormatting SYNTAX_HIGHLIGHTING_KEY = ChatFormatting.AQUA;
   private static final ChatFormatting SYNTAX_HIGHLIGHTING_STRING = ChatFormatting.GREEN;
   private static final ChatFormatting SYNTAX_HIGHLIGHTING_NUMBER = ChatFormatting.GOLD;
   private static final ChatFormatting SYNTAX_HIGHLIGHTING_NUMBER_TYPE = ChatFormatting.RED;
   private static final Pattern SIMPLE_VALUE = Pattern.compile("[A-Za-z0-9._+-]+");
   private static final String LIST_OPEN = "[";
   private static final String LIST_CLOSE = "]";
   private static final String LIST_TYPE_SEPARATOR = ";";
   private static final String ELEMENT_SPACING = " ";
   private static final String STRUCT_OPEN = "{";
   private static final String STRUCT_CLOSE = "}";
   private static final String NEWLINE = "\n";
   private static final String NAME_VALUE_SEPARATOR = ": ";
   private static final String ELEMENT_SEPARATOR = String.valueOf(',');
   private static final String WRAPPED_ELEMENT_SEPARATOR = ELEMENT_SEPARATOR + "\n";
   private static final String SPACED_ELEMENT_SEPARATOR = ELEMENT_SEPARATOR + " ";
   private static final Component FOLDED = Component.literal("<...>").withStyle(ChatFormatting.GRAY);
   private static final Component BYTE_TYPE = Component.literal("b").withStyle(SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
   private static final Component SHORT_TYPE = Component.literal("s").withStyle(SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
   private static final Component INT_TYPE = Component.literal("I").withStyle(SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
   private static final Component LONG_TYPE = Component.literal("L").withStyle(SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
   private static final Component FLOAT_TYPE = Component.literal("f").withStyle(SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
   private static final Component DOUBLE_TYPE = Component.literal("d").withStyle(SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
   private static final Component BYTE_ARRAY_TYPE = Component.literal("B").withStyle(SYNTAX_HIGHLIGHTING_NUMBER_TYPE);
   private final String indentation;
   private int indentDepth;
   private int depth;
   private final MutableComponent result = Component.empty();

   public TextComponentTagVisitor(String p_178251_) {
      this.indentation = p_178251_;
   }

   public Component visit(Tag p_178282_) {
      p_178282_.accept(this);
      return this.result;
   }

   @Override
   public void visitString(StringTag p_178280_) {
      String s = StringTag.quoteAndEscape(p_178280_.value());
      String s1 = s.substring(0, 1);
      Component component = Component.literal(s.substring(1, s.length() - 1)).withStyle(SYNTAX_HIGHLIGHTING_STRING);
      this.result.append(s1).append(component).append(s1);
   }

   @Override
   public void visitByte(ByteTag p_178258_) {
      this.result.append(Component.literal(String.valueOf(p_178258_.value())).withStyle(SYNTAX_HIGHLIGHTING_NUMBER)).append(BYTE_TYPE);
   }

   @Override
   public void visitShort(ShortTag p_178278_) {
      this.result.append(Component.literal(String.valueOf(p_178278_.value())).withStyle(SYNTAX_HIGHLIGHTING_NUMBER)).append(SHORT_TYPE);
   }

   @Override
   public void visitInt(IntTag p_178270_) {
      this.result.append(Component.literal(String.valueOf(p_178270_.value())).withStyle(SYNTAX_HIGHLIGHTING_NUMBER));
   }

   @Override
   public void visitLong(LongTag p_178276_) {
      this.result.append(Component.literal(String.valueOf(p_178276_.value())).withStyle(SYNTAX_HIGHLIGHTING_NUMBER)).append(LONG_TYPE);
   }

   @Override
   public void visitFloat(FloatTag p_178266_) {
      this.result.append(Component.literal(String.valueOf(p_178266_.value())).withStyle(SYNTAX_HIGHLIGHTING_NUMBER)).append(FLOAT_TYPE);
   }

   @Override
   public void visitDouble(DoubleTag p_178262_) {
      this.result.append(Component.literal(String.valueOf(p_178262_.value())).withStyle(SYNTAX_HIGHLIGHTING_NUMBER)).append(DOUBLE_TYPE);
   }

   @Override
   public void visitByteArray(ByteArrayTag p_178256_) {
      this.result.append("[").append(BYTE_ARRAY_TYPE).append(";");
      byte[] abyte = p_178256_.getAsByteArray();

      for (int i = 0; i < abyte.length && i < 128; i++) {
         MutableComponent mutablecomponent = Component.literal(String.valueOf(abyte[i])).withStyle(SYNTAX_HIGHLIGHTING_NUMBER);
         this.result.append(" ").append(mutablecomponent).append(BYTE_ARRAY_TYPE);
         if (i != abyte.length - 1) {
            this.result.append(ELEMENT_SEPARATOR);
         }
      }

      if (abyte.length > 128) {
         this.result.append(FOLDED);
      }

      this.result.append("]");
   }

   @Override
   public void visitIntArray(IntArrayTag p_178268_) {
      this.result.append("[").append(INT_TYPE).append(";");
      int[] aint = p_178268_.getAsIntArray();

      for (int i = 0; i < aint.length && i < 128; i++) {
         this.result.append(" ").append(Component.literal(String.valueOf(aint[i])).withStyle(SYNTAX_HIGHLIGHTING_NUMBER));
         if (i != aint.length - 1) {
            this.result.append(ELEMENT_SEPARATOR);
         }
      }

      if (aint.length > 128) {
         this.result.append(FOLDED);
      }

      this.result.append("]");
   }

   @Override
   public void visitLongArray(LongArrayTag p_178274_) {
      this.result.append("[").append(LONG_TYPE).append(";");
      long[] along = p_178274_.getAsLongArray();

      for (int i = 0; i < along.length && i < 128; i++) {
         Component component = Component.literal(String.valueOf(along[i])).withStyle(SYNTAX_HIGHLIGHTING_NUMBER);
         this.result.append(" ").append(component).append(LONG_TYPE);
         if (i != along.length - 1) {
            this.result.append(ELEMENT_SEPARATOR);
         }
      }

      if (along.length > 128) {
         this.result.append(FOLDED);
      }

      this.result.append("]");
   }

   private static boolean shouldWrapListElements(ListTag p_397147_) {
      if (p_397147_.size() >= 8) {
         return false;
      }

      for (Tag tag : p_397147_) {
         if (!(tag instanceof NumericTag)) {
            return true;
         }
      }

      return false;
   }

   @Override
   public void visitList(ListTag p_178272_) {
      if (p_178272_.isEmpty()) {
         this.result.append("[]");
      } else if (this.depth >= 64) {
         this.result.append("[").append(FOLDED).append("]");
      } else if (!shouldWrapListElements(p_178272_)) {
         this.result.append("[");

         for (int j = 0; j < p_178272_.size(); j++) {
            if (j != 0) {
               this.result.append(SPACED_ELEMENT_SEPARATOR);
            }

            this.appendSubTag(p_178272_.get(j), false);
         }

         this.result.append("]");
      } else {
         this.result.append("[");
         if (!this.indentation.isEmpty()) {
            this.result.append("\n");
         }

         String s = Strings.repeat(this.indentation, this.indentDepth + 1);

         for (int i = 0; i < p_178272_.size() && i < 128; i++) {
            this.result.append(s);
            this.appendSubTag(p_178272_.get(i), true);
            if (i != p_178272_.size() - 1) {
               this.result.append(this.indentation.isEmpty() ? SPACED_ELEMENT_SEPARATOR : WRAPPED_ELEMENT_SEPARATOR);
            }
         }

         if (p_178272_.size() > 128) {
            this.result.append(s).append(FOLDED);
         }

         if (!this.indentation.isEmpty()) {
            this.result.append("\n" + Strings.repeat(this.indentation, this.indentDepth));
         }

         this.result.append("]");
      }
   }

   @Override
   public void visitCompound(CompoundTag p_178260_) {
      if (p_178260_.isEmpty()) {
         this.result.append("{}");
      } else if (this.depth >= 64) {
         this.result.append("{").append(FOLDED).append("}");
      } else {
         this.result.append("{");
         Collection<String> collection = p_178260_.keySet();
         if (LOGGER.isDebugEnabled()) {
            List<String> list = Lists.newArrayList(p_178260_.keySet());
            Collections.sort(list);
            collection = list;
         }

         if (!this.indentation.isEmpty()) {
            this.result.append("\n");
         }

         String s1 = Strings.repeat(this.indentation, this.indentDepth + 1);
         Iterator<String> iterator = collection.iterator();

         while (iterator.hasNext()) {
            String s = iterator.next();
            this.result.append(s1).append(handleEscapePretty(s)).append(": ");
            this.appendSubTag(p_178260_.get(s), true);
            if (iterator.hasNext()) {
               this.result.append(this.indentation.isEmpty() ? SPACED_ELEMENT_SEPARATOR : WRAPPED_ELEMENT_SEPARATOR);
            }
         }

         if (!this.indentation.isEmpty()) {
            this.result.append("\n" + Strings.repeat(this.indentation, this.indentDepth));
         }

         this.result.append("}");
      }
   }

   private void appendSubTag(Tag p_345236_, boolean p_344785_) {
      if (p_344785_) {
         this.indentDepth++;
      }

      this.depth++;

      try {
         p_345236_.accept(this);
      } finally {
         if (p_344785_) {
            this.indentDepth--;
         }

         this.depth--;
      }
   }

   protected static Component handleEscapePretty(String p_178254_) {
      if (SIMPLE_VALUE.matcher(p_178254_).matches()) {
         return Component.literal(p_178254_).withStyle(SYNTAX_HIGHLIGHTING_KEY);
      }

      String s = StringTag.quoteAndEscape(p_178254_);
      String s1 = s.substring(0, 1);
      Component component = Component.literal(s.substring(1, s.length() - 1)).withStyle(SYNTAX_HIGHLIGHTING_KEY);
      return Component.literal(s1).append(component).append(s1);
   }

   @Override
   public void visitEnd(EndTag p_178264_) {
   }
}
