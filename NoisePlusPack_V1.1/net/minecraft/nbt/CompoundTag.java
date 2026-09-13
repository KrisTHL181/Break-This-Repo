package net.minecraft.nbt;

import com.google.common.collect.Maps;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.MapLike;
import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import java.util.function.BiConsumer;
import net.minecraft.CrashReport;
import net.minecraft.CrashReportCategory;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public final class CompoundTag implements Tag {
   private static final Logger LOGGER = LogUtils.getLogger();
   public static final Codec<CompoundTag> CODEC = Codec.PASSTHROUGH
      .comapFlatMap(
         p_308555_ -> {
            Tag tag = (Tag)p_308555_.convert(NbtOps.INSTANCE).getValue();
            return tag instanceof CompoundTag compoundtag
               ? DataResult.success(compoundtag == p_308555_.getValue() ? compoundtag.copy() : compoundtag)
               : DataResult.error(() -> "Not a compound tag: " + tag);
         },
         p_308554_ -> new Dynamic(NbtOps.INSTANCE, p_308554_.copy())
      );
   private static final int SELF_SIZE_IN_BYTES = 48;
   private static final int MAP_ENTRY_SIZE_IN_BYTES = 32;
   public static final TagType<CompoundTag> TYPE = new TagType.VariableSize<CompoundTag>() {
      public CompoundTag load(DataInput p_128485_, NbtAccounter p_128487_) throws IOException {
         p_128487_.pushDepth();

         CompoundTag compoundtag;
         try {
            compoundtag = loadCompound(p_128485_, p_128487_);
         } finally {
            p_128487_.popDepth();
         }

         return compoundtag;
      }

      private static CompoundTag loadCompound(DataInput p_301703_, NbtAccounter p_301763_) throws IOException {
         p_301763_.accountBytes(48L);
         Map<String, Tag> map = Maps.newHashMap();

         byte b0;
         while ((b0 = p_301703_.readByte()) != 0) {
            String s = readString(p_301703_, p_301763_);
            Tag tag = CompoundTag.readNamedTagData(TagTypes.getType(b0), s, p_301703_, p_301763_);
            if (map.put(s, tag) == null) {
               p_301763_.accountBytes(36L);
            }
         }

         return new CompoundTag(map);
      }

      @Override
      public StreamTagVisitor.ValueResult parse(DataInput p_197446_, StreamTagVisitor p_197447_, NbtAccounter p_301769_) throws IOException {
         p_301769_.pushDepth();

         StreamTagVisitor.ValueResult streamtagvisitor$valueresult;
         try {
            streamtagvisitor$valueresult = parseCompound(p_197446_, p_197447_, p_301769_);
         } finally {
            p_301769_.popDepth();
         }

         return streamtagvisitor$valueresult;
      }

      private static StreamTagVisitor.ValueResult parseCompound(DataInput p_301721_, StreamTagVisitor p_301777_, NbtAccounter p_301778_) throws IOException {
         p_301778_.accountBytes(48L);

         byte b0;
         label35:
         while ((b0 = p_301721_.readByte()) != 0) {
            TagType<?> tagtype = TagTypes.getType(b0);
            switch (p_301777_.visitEntry(tagtype)) {
               case HALT:
                  return StreamTagVisitor.ValueResult.HALT;
               case BREAK:
                  StringTag.skipString(p_301721_);
                  tagtype.skip(p_301721_, p_301778_);
                  break label35;
               case SKIP:
                  StringTag.skipString(p_301721_);
                  tagtype.skip(p_301721_, p_301778_);
                  break;
               default:
                  String s = readString(p_301721_, p_301778_);
                  switch (p_301777_.visitEntry(tagtype, s)) {
                     case HALT:
                        return StreamTagVisitor.ValueResult.HALT;
                     case BREAK:
                        tagtype.skip(p_301721_, p_301778_);
                        break label35;
                     case SKIP:
                        tagtype.skip(p_301721_, p_301778_);
                        break;
                     default:
                        p_301778_.accountBytes(36L);
                        switch (tagtype.parse(p_301721_, p_301777_, p_301778_)) {
                           case HALT:
                              return StreamTagVisitor.ValueResult.HALT;
                           case BREAK:
                        }
                  }
            }
         }

         if (b0 != 0) {
            while ((b0 = p_301721_.readByte()) != 0) {
               StringTag.skipString(p_301721_);
               TagTypes.getType(b0).skip(p_301721_, p_301778_);
            }
         }

         return p_301777_.visitContainerEnd();
      }

      private static String readString(DataInput p_301867_, NbtAccounter p_301863_) throws IOException {
         String s = p_301867_.readUTF();
         p_301863_.accountBytes(28L);
         p_301863_.accountBytes(2L, s.length());
         return s;
      }

      @Override
      public void skip(DataInput p_197444_, NbtAccounter p_301720_) throws IOException {
         p_301720_.pushDepth();

         byte b0;
         try {
            while ((b0 = p_197444_.readByte()) != 0) {
               StringTag.skipString(p_197444_);
               TagTypes.getType(b0).skip(p_197444_, p_301720_);
            }
         } finally {
            p_301720_.popDepth();
         }
      }

      @Override
      public String getName() {
         return "COMPOUND";
      }

      @Override
      public String getPrettyName() {
         return "TAG_Compound";
      }
   };
   private final Map<String, Tag> tags;

   CompoundTag(Map<String, Tag> p_128333_) {
      this.tags = p_128333_;
   }

   public CompoundTag() {
      this(new HashMap<>());
   }

   @Override
   public void write(DataOutput p_128341_) throws IOException {
      for (String s : this.tags.keySet()) {
         Tag tag = this.tags.get(s);
         writeNamedTag(s, tag, p_128341_);
      }

      p_128341_.writeByte(0);
   }

   @Override
   public int sizeInBytes() {
      int i = 48;

      for (Entry<String, Tag> entry : this.tags.entrySet()) {
         i += 28 + 2 * entry.getKey().length();
         i += 36;
         i += entry.getValue().sizeInBytes();
      }

      return i;
   }

   public Set<String> keySet() {
      return this.tags.keySet();
   }

   public Set<Entry<String, Tag>> entrySet() {
      return this.tags.entrySet();
   }

   public Collection<Tag> values() {
      return this.tags.values();
   }

   public void forEach(BiConsumer<String, Tag> p_393594_) {
      this.tags.forEach(p_393594_);
   }

   @Override
   public byte getId() {
      return 10;
   }

   @Override
   public TagType<CompoundTag> getType() {
      return TYPE;
   }

   public int size() {
      return this.tags.size();
   }

   public @Nullable Tag put(String p_128366_, Tag p_128367_) {
      return this.tags.put(p_128366_, p_128367_);
   }

   public void putByte(String p_128345_, byte p_128346_) {
      this.tags.put(p_128345_, ByteTag.valueOf(p_128346_));
   }

   public void putShort(String p_128377_, short p_128378_) {
      this.tags.put(p_128377_, ShortTag.valueOf(p_128378_));
   }

   public void putInt(String p_128406_, int p_128407_) {
      this.tags.put(p_128406_, IntTag.valueOf(p_128407_));
   }

   public void putLong(String p_128357_, long p_128358_) {
      this.tags.put(p_128357_, LongTag.valueOf(p_128358_));
   }

   public void putFloat(String p_128351_, float p_128352_) {
      this.tags.put(p_128351_, FloatTag.valueOf(p_128352_));
   }

   public void putDouble(String p_128348_, double p_128349_) {
      this.tags.put(p_128348_, DoubleTag.valueOf(p_128349_));
   }

   public void putString(String p_128360_, String p_128361_) {
      this.tags.put(p_128360_, StringTag.valueOf(p_128361_));
   }

   public void putByteArray(String p_128383_, byte[] p_128384_) {
      this.tags.put(p_128383_, new ByteArrayTag(p_128384_));
   }

   public void putIntArray(String p_128386_, int[] p_128387_) {
      this.tags.put(p_128386_, new IntArrayTag(p_128387_));
   }

   public void putLongArray(String p_128389_, long[] p_128390_) {
      this.tags.put(p_128389_, new LongArrayTag(p_128390_));
   }

   public void putBoolean(String p_128380_, boolean p_128381_) {
      this.tags.put(p_128380_, ByteTag.valueOf(p_128381_));
   }

   public @Nullable Tag get(String p_128424_) {
      return this.tags.get(p_128424_);
   }

   public boolean contains(String p_128442_) {
      return this.tags.containsKey(p_128442_);
   }

   private Optional<Tag> getOptional(String p_392464_) {
      return Optional.ofNullable(this.tags.get(p_392464_));
   }

   public Optional<Byte> getByte(String p_128446_) {
      return this.getOptional(p_128446_).flatMap(Tag::asByte);
   }

   public byte getByteOr(String p_394532_, byte p_393625_) {
      return this.tags.get(p_394532_) instanceof NumericTag numerictag ? numerictag.byteValue() : p_393625_;
   }

   public Optional<Short> getShort(String p_128449_) {
      return this.getOptional(p_128449_).flatMap(Tag::asShort);
   }

   public short getShortOr(String p_392496_, short p_393242_) {
      return this.tags.get(p_392496_) instanceof NumericTag numerictag ? numerictag.shortValue() : p_393242_;
   }

   public Optional<Integer> getInt(String p_128452_) {
      return this.getOptional(p_128452_).flatMap(Tag::asInt);
   }

   public int getIntOr(String p_393175_, int p_392894_) {
      return this.tags.get(p_393175_) instanceof NumericTag numerictag ? numerictag.intValue() : p_392894_;
   }

   public Optional<Long> getLong(String p_128455_) {
      return this.getOptional(p_128455_).flatMap(Tag::asLong);
   }

   public long getLongOr(String p_392953_, long p_394069_) {
      return this.tags.get(p_392953_) instanceof NumericTag numerictag ? numerictag.longValue() : p_394069_;
   }

   public Optional<Float> getFloat(String p_128458_) {
      return this.getOptional(p_128458_).flatMap(Tag::asFloat);
   }

   public float getFloatOr(String p_395832_, float p_391811_) {
      return this.tags.get(p_395832_) instanceof NumericTag numerictag ? numerictag.floatValue() : p_391811_;
   }

   public Optional<Double> getDouble(String p_128460_) {
      return this.getOptional(p_128460_).flatMap(Tag::asDouble);
   }

   public double getDoubleOr(String p_392581_, double p_395699_) {
      return this.tags.get(p_392581_) instanceof NumericTag numerictag ? numerictag.doubleValue() : p_395699_;
   }

   public Optional<String> getString(String p_128462_) {
      return this.getOptional(p_128462_).flatMap(Tag::asString);
   }

   public String getStringOr(String p_392515_, String p_391983_) {
      return this.tags.get(p_392515_) instanceof StringTag(String s) ? s : p_391983_;
   }

   public Optional<byte[]> getByteArray(String p_128464_) {
      return this.tags.get(p_128464_) instanceof ByteArrayTag bytearraytag ? Optional.of(bytearraytag.getAsByteArray()) : Optional.empty();
   }

   public Optional<int[]> getIntArray(String p_128466_) {
      return this.tags.get(p_128466_) instanceof IntArrayTag intarraytag ? Optional.of(intarraytag.getAsIntArray()) : Optional.empty();
   }

   public Optional<long[]> getLongArray(String p_128468_) {
      return this.tags.get(p_128468_) instanceof LongArrayTag longarraytag ? Optional.of(longarraytag.getAsLongArray()) : Optional.empty();
   }

   public Optional<CompoundTag> getCompound(String p_128470_) {
      return this.tags.get(p_128470_) instanceof CompoundTag compoundtag ? Optional.of(compoundtag) : Optional.empty();
   }

   public CompoundTag getCompoundOrEmpty(String p_394014_) {
      return this.getCompound(p_394014_).orElseGet(CompoundTag::new);
   }

   public Optional<ListTag> getList(String p_128438_) {
      return this.tags.get(p_128438_) instanceof ListTag listtag ? Optional.of(listtag) : Optional.empty();
   }

   public ListTag getListOrEmpty(String p_393038_) {
      return this.getList(p_393038_).orElseGet(ListTag::new);
   }

   public Optional<Boolean> getBoolean(String p_128472_) {
      return this.getOptional(p_128472_).flatMap(Tag::asBoolean);
   }

   public boolean getBooleanOr(String p_392625_, boolean p_394254_) {
      return this.getByteOr(p_392625_, (byte)(p_394254_ ? 1 : 0)) != 0;
   }

   public @Nullable Tag remove(String p_128474_) {
      return this.tags.remove(p_128474_);
   }

   @Override
   public String toString() {
      StringTagVisitor stringtagvisitor = new StringTagVisitor();
      stringtagvisitor.visitCompound(this);
      return stringtagvisitor.build();
   }

   public boolean isEmpty() {
      return this.tags.isEmpty();
   }

   protected CompoundTag shallowCopy() {
      return new CompoundTag(new HashMap<>(this.tags));
   }

   public CompoundTag copy() {
      HashMap<String, Tag> hashmap = new HashMap<>();
      this.tags.forEach((p_389877_, p_389878_) -> hashmap.put(p_389877_, p_389878_.copy()));
      return new CompoundTag(hashmap);
   }

   @Override
   public Optional<CompoundTag> asCompound() {
      return Optional.of(this);
   }

   @Override
   public boolean equals(Object p_128444_) {
      return this == p_128444_ ? true : p_128444_ instanceof CompoundTag && Objects.equals(this.tags, ((CompoundTag)p_128444_).tags);
   }

   @Override
   public int hashCode() {
      return this.tags.hashCode();
   }

   private static void writeNamedTag(String p_128369_, Tag p_128370_, DataOutput p_128371_) throws IOException {
      p_128371_.writeByte(p_128370_.getId());
      if (p_128370_.getId() != 0) {
         p_128371_.writeUTF(p_128369_);
         p_128370_.write(p_128371_);
      }
   }

   static Tag readNamedTagData(TagType<?> p_128414_, String p_128415_, DataInput p_128416_, NbtAccounter p_128418_) {
      try {
         return p_128414_.load(p_128416_, p_128418_);
      } catch (IOException ioexception) {
         CrashReport crashreport = CrashReport.forThrowable(ioexception, "Loading NBT data");
         CrashReportCategory crashreportcategory = crashreport.addCategory("NBT Tag");
         crashreportcategory.setDetail("Tag name", p_128415_);
         crashreportcategory.setDetail("Tag type", p_128414_.getName());
         throw new ReportedNbtException(crashreport);
      }
   }

   public CompoundTag merge(CompoundTag p_128392_) {
      for (String s : p_128392_.tags.keySet()) {
         Tag tag = p_128392_.tags.get(s);
         if (tag instanceof CompoundTag compoundtag && this.tags.get(s) instanceof CompoundTag compoundtag1) {
            compoundtag1.merge(compoundtag);
         } else {
            this.put(s, tag.copy());
         }
      }

      return this;
   }

   @Override
   public void accept(TagVisitor p_177857_) {
      p_177857_.visitCompound(this);
   }

   @Override
   public StreamTagVisitor.ValueResult accept(StreamTagVisitor p_197442_) {
      for (Entry<String, Tag> entry : this.tags.entrySet()) {
         Tag tag = entry.getValue();
         TagType<?> tagtype = tag.getType();
         StreamTagVisitor.EntryResult streamtagvisitor$entryresult = p_197442_.visitEntry(tagtype);
         switch (streamtagvisitor$entryresult) {
            case HALT:
               return StreamTagVisitor.ValueResult.HALT;
            case BREAK:
               return p_197442_.visitContainerEnd();
            case SKIP:
               break;
            default:
               streamtagvisitor$entryresult = p_197442_.visitEntry(tagtype, entry.getKey());
               switch (streamtagvisitor$entryresult) {
                  case HALT:
                     return StreamTagVisitor.ValueResult.HALT;
                  case BREAK:
                     return p_197442_.visitContainerEnd();
                  case SKIP:
                     break;
                  default:
                     StreamTagVisitor.ValueResult streamtagvisitor$valueresult = tag.accept(p_197442_);
                     switch (streamtagvisitor$valueresult) {
                        case HALT:
                           return StreamTagVisitor.ValueResult.HALT;
                        case BREAK:
                           return p_197442_.visitContainerEnd();
                     }
               }
         }
      }

      return p_197442_.visitContainerEnd();
   }

   public <T> void store(String p_396702_, Codec<T> p_393338_, T p_397135_) {
      this.store(p_396702_, p_393338_, NbtOps.INSTANCE, p_397135_);
   }

   public <T> void storeNullable(String p_395458_, Codec<T> p_397353_, @Nullable T p_391376_) {
      if (p_391376_ != null) {
         this.store(p_395458_, p_397353_, p_391376_);
      }
   }

   public <T> void store(String p_395188_, Codec<T> p_394724_, DynamicOps<Tag> p_391366_, T p_396055_) {
      this.put(p_395188_, (Tag)p_394724_.encodeStart(p_391366_, p_396055_).getOrThrow());
   }

   public <T> void storeNullable(String p_391195_, Codec<T> p_397981_, DynamicOps<Tag> p_392476_, @Nullable T p_397098_) {
      if (p_397098_ != null) {
         this.store(p_391195_, p_397981_, p_392476_, p_397098_);
      }
   }

   public <T> void store(MapCodec<T> p_394864_, T p_392157_) {
      this.store(p_394864_, NbtOps.INSTANCE, p_392157_);
   }

   public <T> void store(MapCodec<T> p_396427_, DynamicOps<Tag> p_394678_, T p_397219_) {
      this.merge((CompoundTag)p_396427_.encoder().encodeStart(p_394678_, p_397219_).getOrThrow());
   }

   public <T> Optional<T> read(String p_396657_, Codec<T> p_394504_) {
      return this.read(p_396657_, p_394504_, NbtOps.INSTANCE);
   }

   public <T> Optional<T> read(String p_392287_, Codec<T> p_397476_, DynamicOps<Tag> p_395097_) {
      Tag tag = this.get(p_392287_);
      return tag == null
         ? Optional.empty()
         : p_397476_.parse(p_395097_, tag)
            .resultOrPartial(p_389874_ -> LOGGER.error("Failed to read field ({}={}): {}", new Object[]{p_392287_, tag, p_389874_}));
   }

   public <T> Optional<T> read(MapCodec<T> p_393650_) {
      return this.read(p_393650_, NbtOps.INSTANCE);
   }

   public <T> Optional<T> read(MapCodec<T> p_396302_, DynamicOps<Tag> p_396015_) {
      return p_396302_.decode(p_396015_, (MapLike)p_396015_.getMap(this).getOrThrow())
         .resultOrPartial(p_389875_ -> LOGGER.error("Failed to read value ({}): {}", this, p_389875_));
   }
}
