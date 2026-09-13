package net.minecraft.nbt;

import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import net.minecraft.util.Util;

public class SnbtPrinterTagVisitor implements TagVisitor {
   private static final Map<String, List<String>> KEY_ORDER = Util.make(Maps.newHashMap(), map -> {
      map.put("{}", Lists.newArrayList(new String[]{"DataVersion", "author", "size", "data", "entities", "palette", "palettes"}));
      map.put("{}.data.[].{}", Lists.newArrayList(new String[]{"pos", "state", "nbt"}));
      map.put("{}.entities.[].{}", Lists.newArrayList(new String[]{"blockPos", "pos"}));
   });
   private static final Set<String> NO_INDENTATION = Sets.newHashSet(new String[]{"{}.size.[]", "{}.data.[].{}", "{}.palette.[].{}", "{}.entities.[].{}"});
   private static final Pattern SIMPLE_VALUE = Pattern.compile("[A-Za-z0-9._+-]+");
   private static final String NAME_VALUE_SEPARATOR = String.valueOf(':');
   private static final String ELEMENT_SEPARATOR = String.valueOf(',');
   private static final String LIST_OPEN = "[";
   private static final String LIST_CLOSE = "]";
   private static final String LIST_TYPE_SEPARATOR = ";";
   private static final String ELEMENT_SPACING = " ";
   private static final String STRUCT_OPEN = "{";
   private static final String STRUCT_CLOSE = "}";
   private static final String NEWLINE = "\n";
   private final String indentation;
   private final int depth;
   private final List<String> path;
   private String result = "";

   public SnbtPrinterTagVisitor() {
      this("    ", 0, Lists.newArrayList());
   }

   public SnbtPrinterTagVisitor(final String indentation, final int depth, final List<String> path) {
      this.indentation = indentation;
      this.depth = depth;
      this.path = path;
   }

   public String visit(final Tag tag) {
      tag.accept(this);
      return this.result;
   }

   @Override
   public void visitString(final StringTag tag) {
      this.result = StringTag.quoteAndEscape(tag.value());
   }

   @Override
   public void visitByte(final ByteTag tag) {
      this.result = tag.value() + "b";
   }

   @Override
   public void visitShort(final ShortTag tag) {
      this.result = tag.value() + "s";
   }

   @Override
   public void visitInt(final IntTag tag) {
      this.result = String.valueOf(tag.value());
   }

   @Override
   public void visitLong(final LongTag tag) {
      this.result = tag.value() + "L";
   }

   @Override
   public void visitFloat(final FloatTag tag) {
      this.result = tag.value() + "f";
   }

   @Override
   public void visitDouble(final DoubleTag tag) {
      this.result = tag.value() + "d";
   }

   @Override
   public void visitByteArray(final ByteArrayTag tag) {
      StringBuilder builder = new StringBuilder("[").append("B").append(";");
      byte[] data = tag.getAsByteArray();

      for (int i = 0; i < data.length; i++) {
         builder.append(" ").append(data[i]).append("B");
         if (i != data.length - 1) {
            builder.append(ELEMENT_SEPARATOR);
         }
      }

      builder.append("]");
      this.result = builder.toString();
   }

   @Override
   public void visitIntArray(final IntArrayTag tag) {
      StringBuilder builder = new StringBuilder("[").append("I").append(";");
      int[] data = tag.getAsIntArray();

      for (int i = 0; i < data.length; i++) {
         builder.append(" ").append(data[i]);
         if (i != data.length - 1) {
            builder.append(ELEMENT_SEPARATOR);
         }
      }

      builder.append("]");
      this.result = builder.toString();
   }

   @Override
   public void visitLongArray(final LongArrayTag tag) {
      String type = "L";
      StringBuilder builder = new StringBuilder("[").append("L").append(";");
      long[] data = tag.getAsLongArray();

      for (int i = 0; i < data.length; i++) {
         builder.append(" ").append(data[i]).append("L");
         if (i != data.length - 1) {
            builder.append(ELEMENT_SEPARATOR);
         }
      }

      builder.append("]");
      this.result = builder.toString();
   }

   @Override
   public void visitList(final ListTag tag) {
      if (tag.isEmpty()) {
         this.result = "[]";
      } else {
         StringBuilder builder = new StringBuilder("[");
         this.pushPath("[]");
         String indentation = NO_INDENTATION.contains(this.pathString()) ? "" : this.indentation;
         if (!indentation.isEmpty()) {
            builder.append("\n");
         }

         for (int i = 0; i < tag.size(); i++) {
            builder.append(Strings.repeat(indentation, this.depth + 1));
            builder.append(new SnbtPrinterTagVisitor(indentation, this.depth + 1, this.path).visit(tag.get(i)));
            if (i != tag.size() - 1) {
               builder.append(ELEMENT_SEPARATOR).append(indentation.isEmpty() ? " " : "\n");
            }
         }

         if (!indentation.isEmpty()) {
            builder.append("\n").append(Strings.repeat(indentation, this.depth));
         }

         builder.append("]");
         this.result = builder.toString();
         this.popPath();
      }
   }

   @Override
   public void visitCompound(final CompoundTag tag) {
      if (tag.isEmpty()) {
         this.result = "{}";
      } else {
         StringBuilder builder = new StringBuilder("{");
         this.pushPath("{}");
         String indentation = NO_INDENTATION.contains(this.pathString()) ? "" : this.indentation;
         if (!indentation.isEmpty()) {
            builder.append("\n");
         }

         Collection<String> keys = this.getKeys(tag);
         Iterator<String> iterator = keys.iterator();

         while (iterator.hasNext()) {
            String key = iterator.next();
            Tag value = tag.get(key);
            this.pushPath(key);
            builder.append(Strings.repeat(indentation, this.depth + 1))
               .append(handleEscapePretty(key))
               .append(NAME_VALUE_SEPARATOR)
               .append(" ")
               .append(new SnbtPrinterTagVisitor(indentation, this.depth + 1, this.path).visit(value));
            this.popPath();
            if (iterator.hasNext()) {
               builder.append(ELEMENT_SEPARATOR).append(indentation.isEmpty() ? " " : "\n");
            }
         }

         if (!indentation.isEmpty()) {
            builder.append("\n").append(Strings.repeat(indentation, this.depth));
         }

         builder.append("}");
         this.result = builder.toString();
         this.popPath();
      }
   }

   private void popPath() {
      this.path.remove(this.path.size() - 1);
   }

   private void pushPath(final String e) {
      this.path.add(e);
   }

   protected List<String> getKeys(final CompoundTag tag) {
      Set<String> keys = Sets.newHashSet(tag.keySet());
      List<String> strings = Lists.newArrayList();
      List<String> order = KEY_ORDER.get(this.pathString());
      if (order != null) {
         for (String key : order) {
            if (keys.remove(key)) {
               strings.add(key);
            }
         }

         if (!keys.isEmpty()) {
            keys.stream().sorted().forEach(strings::add);
         }
      } else {
         strings.addAll(keys);
         Collections.sort(strings);
      }

      return strings;
   }

   public String pathString() {
      return String.join(".", this.path);
   }

   protected static String handleEscapePretty(final String input) {
      return SIMPLE_VALUE.matcher(input).matches() ? input : StringTag.quoteAndEscape(input);
   }

   @Override
   public void visitEnd(final EndTag tag) {
   }
}
