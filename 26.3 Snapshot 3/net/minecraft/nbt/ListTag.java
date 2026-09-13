package net.minecraft.nbt;

import com.google.common.annotations.VisibleForTesting;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.AbstractList;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Stream;
import org.jspecify.annotations.Nullable;

public final class ListTag extends AbstractList<Tag> implements CollectionTag {
   private static final String WRAPPER_MARKER = "";
   private static final int SELF_SIZE_IN_BYTES = 36;
   public static final TagType<ListTag> TYPE = new TagType.VariableSize<ListTag>() {
      public ListTag load(final DataInput input, final NbtAccounter accounter) throws IOException {
         accounter.pushDepth();

         try {
            return loadList(input, accounter);
         } finally {
            accounter.popDepth();
         }
      }

      private static ListTag loadList(final DataInput input, final NbtAccounter accounter) throws IOException {
         accounter.accountBytes(36L);
         byte typeId = input.readByte();
         int count = readListCount(input);
         if (typeId == 0 && count > 0) {
            throw new NbtFormatException("Missing type on ListTag");
         }

         accounter.accountBytes(4L, count);
         TagType<?> type = TagTypes.getType(typeId);
         ListTag list = new ListTag(new ArrayList<>(count));

         for (int i = 0; i < count; i++) {
            list.addAndUnwrap(type.load(input, accounter));
         }

         return list;
      }

      @Override
      public StreamTagVisitor.ValueResult parse(final DataInput input, final StreamTagVisitor output, final NbtAccounter accounter) throws IOException {
         accounter.pushDepth();

         try {
            return parseList(input, output, accounter);
         } finally {
            accounter.popDepth();
         }
      }

      private static StreamTagVisitor.ValueResult parseList(final DataInput input, final StreamTagVisitor output, final NbtAccounter accounter) throws IOException {
         accounter.accountBytes(36L);
         TagType<?> elementType = TagTypes.getType(input.readByte());
         int count = readListCount(input);
         switch (output.visitList(elementType, count)) {
            case HALT:
               return StreamTagVisitor.ValueResult.HALT;
            case BREAK:
               elementType.skip(input, count, accounter);
               return output.visitContainerEnd();
            default:
               accounter.accountBytes(4L, count);
               int i = 0;

               while (true) {
                  label41: {
                     if (i < count) {
                        switch (output.visitElement(elementType, i)) {
                           case HALT:
                              return StreamTagVisitor.ValueResult.HALT;
                           case BREAK:
                              elementType.skip(input, accounter);
                              break;
                           case SKIP:
                              elementType.skip(input, accounter);
                              break label41;
                           default:
                              switch (elementType.parse(input, output, accounter)) {
                                 case HALT:
                                    return StreamTagVisitor.ValueResult.HALT;
                                 case BREAK:
                                    break;
                                 default:
                                    break label41;
                              }
                        }
                     }

                     int amountToSkip = count - 1 - i;
                     if (amountToSkip > 0) {
                        elementType.skip(input, amountToSkip, accounter);
                     }

                     return output.visitContainerEnd();
                  }

                  i++;
               }
         }
      }

      private static int readListCount(final DataInput input) throws IOException {
         int count = input.readInt();
         if (count < 0) {
            throw new NbtFormatException("ListTag length cannot be negative: " + count);
         } else {
            return count;
         }
      }

      @Override
      public void skip(final DataInput input, final NbtAccounter accounter) throws IOException {
         accounter.pushDepth();

         try {
            TagType<?> type = TagTypes.getType(input.readByte());
            int count = input.readInt();
            type.skip(input, count, accounter);
         } finally {
            accounter.popDepth();
         }
      }

      @Override
      public String getName() {
         return "LIST";
      }

      @Override
      public String getPrettyName() {
         return "TAG_List";
      }
   };
   private final List<Tag> list;

   public ListTag() {
      this(new ArrayList<>());
   }

   ListTag(final List<Tag> list) {
      this.list = list;
   }

   private static Tag tryUnwrap(final CompoundTag tag) {
      if (tag.size() == 1) {
         Tag value = tag.get("");
         if (value != null) {
            return value;
         }
      }

      return tag;
   }

   private static boolean isWrapper(final CompoundTag tag) {
      return tag.size() == 1 && tag.contains("");
   }

   private static Tag wrapIfNeeded(final byte elementType, final Tag tag) {
      if (elementType != 10) {
         return tag;
      } else {
         return tag instanceof CompoundTag compoundTag && !isWrapper(compoundTag) ? compoundTag : wrapElement(tag);
      }
   }

   private static CompoundTag wrapElement(final Tag tag) {
      return new CompoundTag(Map.of("", tag));
   }

   @Override
   public void write(final DataOutput output) throws IOException {
      byte elementType = this.identifyRawElementType();
      output.writeByte(elementType);
      output.writeInt(this.list.size());

      for (Tag element : this.list) {
         wrapIfNeeded(elementType, element).write(output);
      }
   }

   @VisibleForTesting
   byte identifyRawElementType() {
      byte homogenousType = 0;

      for (Tag element : this.list) {
         byte elementType = element.getId();
         if (homogenousType == 0) {
            homogenousType = elementType;
         } else if (homogenousType != elementType) {
            return 10;
         }
      }

      return homogenousType;
   }

   public void addAndUnwrap(final Tag tag) {
      if (tag instanceof CompoundTag compound) {
         this.add(tryUnwrap(compound));
      } else {
         this.add(tag);
      }
   }

   @Override
   public int sizeInBytes() {
      int size = 36;
      size += 4 * this.list.size();

      for (Tag child : this.list) {
         size += child.sizeInBytes();
      }

      return size;
   }

   @Override
   public byte getId() {
      return 9;
   }

   @Override
   public TagType<ListTag> getType() {
      return TYPE;
   }

   @Override
   public String toString() {
      StringTagVisitor visitor = new StringTagVisitor();
      visitor.visitList(this);
      return visitor.build();
   }

   @Override
   public Tag remove(final int index) {
      return this.list.remove(index);
   }

   @Override
   public boolean isEmpty() {
      return this.list.isEmpty();
   }

   public Optional<CompoundTag> getCompound(final int index) {
      return this.getNullable(index) instanceof CompoundTag tag ? Optional.of(tag) : Optional.empty();
   }

   public CompoundTag getCompoundOrEmpty(final int index) {
      return this.getCompound(index).orElseGet(CompoundTag::new);
   }

   public Optional<ListTag> getList(final int index) {
      return this.getNullable(index) instanceof ListTag tag ? Optional.of(tag) : Optional.empty();
   }

   public ListTag getListOrEmpty(final int index) {
      return this.getList(index).orElseGet(ListTag::new);
   }

   public Optional<Short> getShort(final int index) {
      return this.getOptional(index).flatMap(Tag::asShort);
   }

   public short getShortOr(final int index, final short defaultValue) {
      return this.getNullable(index) instanceof NumericTag tag ? tag.shortValue() : defaultValue;
   }

   public Optional<Integer> getInt(final int index) {
      return this.getOptional(index).flatMap(Tag::asInt);
   }

   public int getIntOr(final int index, final int defaultValue) {
      return this.getNullable(index) instanceof NumericTag tag ? tag.intValue() : defaultValue;
   }

   public Optional<int[]> getIntArray(final int index) {
      return this.getNullable(index) instanceof IntArrayTag tag ? Optional.of(tag.getAsIntArray()) : Optional.empty();
   }

   public Optional<long[]> getLongArray(final int index) {
      return this.getNullable(index) instanceof LongArrayTag tag ? Optional.of(tag.getAsLongArray()) : Optional.empty();
   }

   public Optional<Double> getDouble(final int index) {
      return this.getOptional(index).flatMap(Tag::asDouble);
   }

   public double getDoubleOr(final int index, final double defaultValue) {
      return this.getNullable(index) instanceof NumericTag tag ? tag.doubleValue() : defaultValue;
   }

   public Optional<Float> getFloat(final int index) {
      return this.getOptional(index).flatMap(Tag::asFloat);
   }

   public float getFloatOr(final int index, final float defaultValue) {
      return this.getNullable(index) instanceof NumericTag tag ? tag.floatValue() : defaultValue;
   }

   public Optional<String> getString(final int index) {
      return this.getOptional(index).flatMap(Tag::asString);
   }

   public String getStringOr(final int index, final String defaultValue) {
      return this.getNullable(index) instanceof StringTag(String var8) ? var8 : defaultValue;
   }

   private @Nullable Tag getNullable(final int index) {
      return index >= 0 && index < this.list.size() ? this.list.get(index) : null;
   }

   private Optional<Tag> getOptional(final int index) {
      return Optional.ofNullable(this.getNullable(index));
   }

   @Override
   public int size() {
      return this.list.size();
   }

   @Override
   public Tag get(final int index) {
      return this.list.get(index);
   }

   public Tag set(final int index, final Tag tag) {
      return this.list.set(index, tag);
   }

   public void add(final int index, final Tag tag) {
      this.list.add(index, tag);
   }

   @Override
   public boolean setTag(final int index, final Tag tag) {
      this.list.set(index, tag);
      return true;
   }

   @Override
   public boolean addTag(final int index, final Tag tag) {
      this.list.add(index, tag);
      return true;
   }

   public ListTag copy() {
      List<Tag> copy = new ArrayList<>(this.list.size());

      for (Tag tag : this.list) {
         copy.add(tag.copy());
      }

      return new ListTag(copy);
   }

   @Override
   public Optional<ListTag> asList() {
      return Optional.of(this);
   }

   @Override
   public boolean equals(final Object obj) {
      return this == obj ? true : obj instanceof ListTag listTag && Objects.equals(this.list, listTag.list);
   }

   @Override
   public int hashCode() {
      return this.list.hashCode();
   }

   @Override
   public Stream<Tag> stream() {
      return super.stream();
   }

   public Stream<CompoundTag> compoundStream() {
      return this.stream().mapMulti((tag, output) -> {
         if (tag instanceof CompoundTag compound) {
            output.accept(compound);
         }
      });
   }

   @Override
   public void accept(final TagVisitor visitor) {
      visitor.visitList(this);
   }

   @Override
   public void clear() {
      this.list.clear();
   }

   @Override
   public StreamTagVisitor.ValueResult accept(final StreamTagVisitor visitor) {
      byte elementType = this.identifyRawElementType();
      switch (visitor.visitList(TagTypes.getType(elementType), this.list.size())) {
         case HALT:
            return StreamTagVisitor.ValueResult.HALT;
         case BREAK:
            return visitor.visitContainerEnd();
         default:
            int i = 0;

            while (i < this.list.size()) {
               Tag tag = wrapIfNeeded(elementType, this.list.get(i));
               switch (visitor.visitElement(tag.getType(), i)) {
                  case HALT:
                     return StreamTagVisitor.ValueResult.HALT;
                  case BREAK:
                     return visitor.visitContainerEnd();
                  default:
                     switch (tag.accept(visitor)) {
                        case HALT:
                           return StreamTagVisitor.ValueResult.HALT;
                        case BREAK:
                           return visitor.visitContainerEnd();
                     }
                  case SKIP:
                     i++;
               }
            }

            return visitor.visitContainerEnd();
      }
   }
}
