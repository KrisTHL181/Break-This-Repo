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
      public ListTag load(DataInput p_128792_, NbtAccounter p_128794_) throws IOException {
         p_128794_.pushDepth();

         ListTag listtag;
         try {
            listtag = loadList(p_128792_, p_128794_);
         } finally {
            p_128794_.popDepth();
         }

         return listtag;
      }

      private static ListTag loadList(DataInput p_301758_, NbtAccounter p_301694_) throws IOException {
         p_301694_.accountBytes(36L);
         byte b0 = p_301758_.readByte();
         int i = readListCount(p_301758_);
         if (b0 == 0 && i > 0) {
            throw new NbtFormatException("Missing type on ListTag");
         }

         p_301694_.accountBytes(4L, i);
         TagType<?> tagtype = TagTypes.getType(b0);
         ListTag listtag = new ListTag(new ArrayList<>(i));

         for (int j = 0; j < i; j++) {
            listtag.addAndUnwrap(tagtype.load(p_301758_, p_301694_));
         }

         return listtag;
      }

      @Override
      public StreamTagVisitor.ValueResult parse(DataInput p_197491_, StreamTagVisitor p_197492_, NbtAccounter p_301731_) throws IOException {
         p_301731_.pushDepth();

         StreamTagVisitor.ValueResult streamtagvisitor$valueresult;
         try {
            streamtagvisitor$valueresult = parseList(p_197491_, p_197492_, p_301731_);
         } finally {
            p_301731_.popDepth();
         }

         return streamtagvisitor$valueresult;
      }

      private static StreamTagVisitor.ValueResult parseList(DataInput p_301745_, StreamTagVisitor p_301695_, NbtAccounter p_301734_) throws IOException {
         p_301734_.accountBytes(36L);
         TagType<?> tagtype = TagTypes.getType(p_301745_.readByte());
         int i = readListCount(p_301745_);
         switch (p_301695_.visitList(tagtype, i)) {
            case HALT:
               return StreamTagVisitor.ValueResult.HALT;
            case BREAK:
               tagtype.skip(p_301745_, i, p_301734_);
               return p_301695_.visitContainerEnd();
            default:
               p_301734_.accountBytes(4L, i);
               int j = 0;

               while (true) {
                  label42: {
                     if (j < i) {
                        switch (p_301695_.visitElement(tagtype, j)) {
                           case HALT:
                              return StreamTagVisitor.ValueResult.HALT;
                           case BREAK:
                              tagtype.skip(p_301745_, p_301734_);
                              break;
                           case SKIP:
                              tagtype.skip(p_301745_, p_301734_);
                              break label42;
                           default:
                              switch (tagtype.parse(p_301745_, p_301695_, p_301734_)) {
                                 case HALT:
                                    return StreamTagVisitor.ValueResult.HALT;
                                 case BREAK:
                                    break;
                                 default:
                                    break label42;
                              }
                        }
                     }

                     int k = i - 1 - j;
                     if (k > 0) {
                        tagtype.skip(p_301745_, k, p_301734_);
                     }

                     return p_301695_.visitContainerEnd();
                  }

                  j++;
               }
         }
      }

      private static int readListCount(DataInput p_406196_) throws IOException {
         int i = p_406196_.readInt();
         if (i < 0) {
            throw new NbtFormatException("ListTag length cannot be negative: " + i);
         } else {
            return i;
         }
      }

      @Override
      public void skip(DataInput p_301743_, NbtAccounter p_301728_) throws IOException {
         p_301728_.pushDepth();

         try {
            TagType<?> tagtype = TagTypes.getType(p_301743_.readByte());
            int i = p_301743_.readInt();
            tagtype.skip(p_301743_, i, p_301728_);
         } finally {
            p_301728_.popDepth();
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

   ListTag(List<Tag> p_128721_) {
      this.list = p_128721_;
   }

   private static Tag tryUnwrap(CompoundTag p_395168_) {
      if (p_395168_.size() == 1) {
         Tag tag = p_395168_.get("");
         if (tag != null) {
            return tag;
         }
      }

      return p_395168_;
   }

   private static boolean isWrapper(CompoundTag p_392202_) {
      return p_392202_.size() == 1 && p_392202_.contains("");
   }

   private static Tag wrapIfNeeded(byte p_396867_, Tag p_395814_) {
      if (p_396867_ != 10) {
         return p_395814_;
      } else {
         return p_395814_ instanceof CompoundTag compoundtag && !isWrapper(compoundtag) ? compoundtag : wrapElement(p_395814_);
      }
   }

   private static CompoundTag wrapElement(Tag p_394364_) {
      return new CompoundTag(Map.of("", p_394364_));
   }

   @Override
   public void write(DataOutput p_128734_) throws IOException {
      byte b0 = this.identifyRawElementType();
      p_128734_.writeByte(b0);
      p_128734_.writeInt(this.list.size());

      for (Tag tag : this.list) {
         wrapIfNeeded(b0, tag).write(p_128734_);
      }
   }

   @VisibleForTesting
   byte identifyRawElementType() {
      byte b0 = 0;

      for (Tag tag : this.list) {
         byte b1 = tag.getId();
         if (b0 == 0) {
            b0 = b1;
         } else if (b0 != b1) {
            return 10;
         }
      }

      return b0;
   }

   public void addAndUnwrap(Tag p_397373_) {
      if (p_397373_ instanceof CompoundTag compoundtag) {
         this.add(tryUnwrap(compoundtag));
      } else {
         this.add(p_397373_);
      }
   }

   @Override
   public int sizeInBytes() {
      int i = 36;
      i += 4 * this.list.size();

      for (Tag tag : this.list) {
         i += tag.sizeInBytes();
      }

      return i;
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
      StringTagVisitor stringtagvisitor = new StringTagVisitor();
      stringtagvisitor.visitList(this);
      return stringtagvisitor.build();
   }

   @Override
   public Tag remove(int p_128751_) {
      return this.list.remove(p_128751_);
   }

   @Override
   public boolean isEmpty() {
      return this.list.isEmpty();
   }

   public Optional<CompoundTag> getCompound(int p_128729_) {
      return this.getNullable(p_128729_) instanceof CompoundTag compoundtag ? Optional.of(compoundtag) : Optional.empty();
   }

   public CompoundTag getCompoundOrEmpty(int p_392090_) {
      return this.getCompound(p_392090_).orElseGet(CompoundTag::new);
   }

   public Optional<ListTag> getList(int p_128745_) {
      return this.getNullable(p_128745_) instanceof ListTag listtag ? Optional.of(listtag) : Optional.empty();
   }

   public ListTag getListOrEmpty(int p_393040_) {
      return this.getList(p_393040_).orElseGet(ListTag::new);
   }

   public Optional<Short> getShort(int p_128758_) {
      return this.getOptional(p_128758_).flatMap(Tag::asShort);
   }

   public short getShortOr(int p_396605_, short p_395292_) {
      return this.getNullable(p_396605_) instanceof NumericTag numerictag ? numerictag.shortValue() : p_395292_;
   }

   public Optional<Integer> getInt(int p_128764_) {
      return this.getOptional(p_128764_).flatMap(Tag::asInt);
   }

   public int getIntOr(int p_397695_, int p_392783_) {
      return this.getNullable(p_397695_) instanceof NumericTag numerictag ? numerictag.intValue() : p_392783_;
   }

   public Optional<int[]> getIntArray(int p_128768_) {
      return this.getNullable(p_128768_) instanceof IntArrayTag intarraytag ? Optional.of(intarraytag.getAsIntArray()) : Optional.empty();
   }

   public Optional<long[]> getLongArray(int p_177992_) {
      return this.getNullable(p_177992_) instanceof LongArrayTag longarraytag ? Optional.of(longarraytag.getAsLongArray()) : Optional.empty();
   }

   public Optional<Double> getDouble(int p_128773_) {
      return this.getOptional(p_128773_).flatMap(Tag::asDouble);
   }

   public double getDoubleOr(int p_395161_, double p_393731_) {
      return this.getNullable(p_395161_) instanceof NumericTag numerictag ? numerictag.doubleValue() : p_393731_;
   }

   public Optional<Float> getFloat(int p_128776_) {
      return this.getOptional(p_128776_).flatMap(Tag::asFloat);
   }

   public float getFloatOr(int p_397883_, float p_391690_) {
      return this.getNullable(p_397883_) instanceof NumericTag numerictag ? numerictag.floatValue() : p_391690_;
   }

   public Optional<String> getString(int p_128779_) {
      return this.getOptional(p_128779_).flatMap(Tag::asString);
   }

   public String getStringOr(int p_391635_, String p_396554_) {
      return this.getNullable(p_391635_) instanceof StringTag(String s) ? s : p_396554_;
   }

   private @Nullable Tag getNullable(int p_397686_) {
      return p_397686_ >= 0 && p_397686_ < this.list.size() ? this.list.get(p_397686_) : null;
   }

   private Optional<Tag> getOptional(int p_396323_) {
      return Optional.ofNullable(this.getNullable(p_396323_));
   }

   @Override
   public int size() {
      return this.list.size();
   }

   @Override
   public Tag get(int p_128781_) {
      return this.list.get(p_128781_);
   }

   public Tag set(int p_128760_, Tag p_128761_) {
      return this.list.set(p_128760_, p_128761_);
   }

   public void add(int p_128753_, Tag p_128754_) {
      this.list.add(p_128753_, p_128754_);
   }

   @Override
   public boolean setTag(int p_128731_, Tag p_128732_) {
      this.list.set(p_128731_, p_128732_);
      return true;
   }

   @Override
   public boolean addTag(int p_128747_, Tag p_128748_) {
      this.list.add(p_128747_, p_128748_);
      return true;
   }

   public ListTag copy() {
      List<Tag> list = new ArrayList<>(this.list.size());

      for (Tag tag : this.list) {
         list.add(tag.copy());
      }

      return new ListTag(list);
   }

   @Override
   public Optional<ListTag> asList() {
      return Optional.of(this);
   }

   @Override
   public boolean equals(Object p_128766_) {
      return this == p_128766_ ? true : p_128766_ instanceof ListTag && Objects.equals(this.list, ((ListTag)p_128766_).list);
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
      return this.stream().mapMulti((p_396018_, p_392733_) -> {
         if (p_396018_ instanceof CompoundTag compoundtag) {
            p_392733_.accept(compoundtag);
         }
      });
   }

   @Override
   public void accept(TagVisitor p_177990_) {
      p_177990_.visitList(this);
   }

   @Override
   public void clear() {
      this.list.clear();
   }

   @Override
   public StreamTagVisitor.ValueResult accept(StreamTagVisitor p_197487_) {
      byte b0 = this.identifyRawElementType();
      switch (p_197487_.visitList(TagTypes.getType(b0), this.list.size())) {
         case HALT:
            return StreamTagVisitor.ValueResult.HALT;
         case BREAK:
            return p_197487_.visitContainerEnd();
         default:
            int i = 0;

            while (i < this.list.size()) {
               Tag tag = wrapIfNeeded(b0, this.list.get(i));
               switch (p_197487_.visitElement(tag.getType(), i)) {
                  case HALT:
                     return StreamTagVisitor.ValueResult.HALT;
                  case BREAK:
                     return p_197487_.visitContainerEnd();
                  default:
                     switch (tag.accept(p_197487_)) {
                        case HALT:
                           return StreamTagVisitor.ValueResult.HALT;
                        case BREAK:
                           return p_197487_.visitContainerEnd();
                     }
                  case SKIP:
                     i++;
               }
            }

            return p_197487_.visitContainerEnd();
      }
   }
}
