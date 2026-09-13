package net.minecraft.world.level.chunk;

import com.google.common.annotations.VisibleForTesting;
import com.mojang.serialization.Codec;
import com.mojang.serialization.DataResult;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import it.unimi.dsi.fastutil.ints.Int2IntOpenHashMap;
import it.unimi.dsi.fastutil.ints.IntArraySet;
import it.unimi.dsi.fastutil.ints.IntSet;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.LongStream;
import net.minecraft.core.IdMap;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.util.BitStorage;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.SimpleBitStorage;
import net.minecraft.util.ThreadingDetector;
import net.minecraft.util.ZeroBitStorage;
import org.jspecify.annotations.Nullable;

public class PalettedContainer<T> implements PaletteResize<T>, PalettedContainerRO<T> {
   private static final int MIN_PALETTE_BITS = 0;
   private volatile PalettedContainer.Data<T> data;
   private final Strategy<T> strategy;
   private final ThreadingDetector threadingDetector = new ThreadingDetector("PalettedContainer");

   public void acquire() {
      this.threadingDetector.checkAndLock();
   }

   public void release() {
      this.threadingDetector.checkAndUnlock();
   }

   public static <T> Codec<PalettedContainer<T>> codecRW(Codec<T> p_238373_, Strategy<T> p_424106_, T p_238375_) {
      PalettedContainerRO.Unpacker<T, PalettedContainer<T>> unpacker = PalettedContainer::unpack;
      return codec(p_238373_, p_424106_, p_238375_, unpacker);
   }

   public static <T> Codec<PalettedContainerRO<T>> codecRO(Codec<T> p_238420_, Strategy<T> p_423493_, T p_238422_) {
      PalettedContainerRO.Unpacker<T, PalettedContainerRO<T>> unpacker = (p_422204_, p_422205_) -> unpack(p_422204_, p_422205_).map(p_238264_ -> p_238264_);
      return codec(p_238420_, p_423493_, p_238422_, unpacker);
   }

   private static <T, C extends PalettedContainerRO<T>> Codec<C> codec(
      Codec<T> p_238429_, Strategy<T> p_430459_, T p_238431_, PalettedContainerRO.Unpacker<T, C> p_238432_
   ) {
      return RecordCodecBuilder.create(
            p_327417_ -> p_327417_.group(
                  p_238429_.mapResult(ExtraCodecs.orElsePartial(p_238431_))
                     .listOf()
                     .fieldOf("palette")
                     .forGetter(PalettedContainerRO.PackedData::paletteEntries),
                  Codec.LONG_STREAM.lenientOptionalFieldOf("data").forGetter(PalettedContainerRO.PackedData::storage)
               )
               .apply(p_327417_, PalettedContainerRO.PackedData::new)
         )
         .comapFlatMap(p_422199_ -> p_238432_.read(p_430459_, p_422199_), p_422201_ -> p_422201_.pack(p_430459_));
   }

   private PalettedContainer(Strategy<T> p_430627_, Configuration p_431112_, BitStorage p_422488_, Palette<T> p_429989_) {
      this.strategy = p_430627_;
      this.data = new PalettedContainer.Data<>(p_431112_, p_422488_, p_429989_);
   }

   private PalettedContainer(PalettedContainer<T> p_368978_) {
      this.strategy = p_368978_.strategy;
      this.data = p_368978_.data.copy();
   }

   public PalettedContainer(T p_422786_, Strategy<T> p_427822_) {
      this.strategy = p_427822_;
      this.data = this.createOrReuseData(null, 0);
      this.data.palette.idFor(p_422786_, this);
   }

   private PalettedContainer.Data<T> createOrReuseData(PalettedContainer.@Nullable Data<T> p_188052_, int p_188053_) {
      Configuration configuration = this.strategy.getConfigurationForBitCount(p_188053_);
      if (p_188052_ != null && configuration.equals(p_188052_.configuration())) {
         return p_188052_;
      }

      BitStorage bitstorage = configuration.bitsInMemory() == 0
         ? new ZeroBitStorage(this.strategy.entryCount())
         : new SimpleBitStorage(configuration.bitsInMemory(), this.strategy.entryCount());
      Palette<T> palette = configuration.createPalette(this.strategy, List.of());
      return new PalettedContainer.Data<>(configuration, bitstorage, palette);
   }

   @Override
   public int onResize(int p_63142_, T p_63143_) {
      PalettedContainer.Data<T> data = this.data;
      PalettedContainer.Data<T> data1 = this.createOrReuseData(data, p_63142_);
      data1.copyFrom(data.palette, data.storage);
      this.data = data1;
      return data1.palette.idFor(p_63143_, PaletteResize.noResizeExpected());
   }

   public T getAndSet(int p_63092_, int p_63093_, int p_63094_, T p_63095_) {
      this.acquire();

      Object object;
      try {
         object = this.getAndSet(this.strategy.getIndex(p_63092_, p_63093_, p_63094_), p_63095_);
      } finally {
         this.release();
      }

      return (T)object;
   }

   public T getAndSetUnchecked(int p_63128_, int p_63129_, int p_63130_, T p_63131_) {
      return this.getAndSet(this.strategy.getIndex(p_63128_, p_63129_, p_63130_), p_63131_);
   }

   private T getAndSet(int p_63097_, T p_63098_) {
      int i = this.data.palette.idFor(p_63098_, this);
      int j = this.data.storage.getAndSet(p_63097_, i);
      return this.data.palette.valueFor(j);
   }

   public void set(int p_156471_, int p_156472_, int p_156473_, T p_156474_) {
      this.acquire();

      try {
         this.set(this.strategy.getIndex(p_156471_, p_156472_, p_156473_), p_156474_);
      } finally {
         this.release();
      }
   }

   private void set(int p_63133_, T p_63134_) {
      int i = this.data.palette.idFor(p_63134_, this);
      this.data.storage.set(p_63133_, i);
   }

   @Override
   public T get(int p_63088_, int p_63089_, int p_63090_) {
      return this.get(this.strategy.getIndex(p_63088_, p_63089_, p_63090_));
   }

   protected T get(int p_63086_) {
      PalettedContainer.Data<T> data = this.data;
      return data.palette.valueFor(data.storage.get(p_63086_));
   }

   @Override
   public void getAll(Consumer<T> p_196880_) {
      Palette<T> palette = this.data.palette();
      IntSet intset = new IntArraySet();
      this.data.storage.getAll(intset::add);
      intset.forEach(p_238274_ -> p_196880_.accept(palette.valueFor(p_238274_)));
   }

   public void read(FriendlyByteBuf p_63119_) {
      this.acquire();

      try {
         int i = p_63119_.readByte();
         PalettedContainer.Data<T> data = this.createOrReuseData(this.data, i);
         data.palette.read(p_63119_, this.strategy.globalMap());
         p_63119_.readFixedSizeLongArray(data.storage.getRaw());
         this.data = data;
      } finally {
         this.release();
      }
   }

   @Override
   public void write(FriendlyByteBuf p_63136_) {
      this.acquire();

      try {
         this.data.write(p_63136_, this.strategy.globalMap());
      } finally {
         this.release();
      }
   }

   @VisibleForTesting
   public static <T> DataResult<PalettedContainer<T>> unpack(Strategy<T> p_425228_, PalettedContainerRO.PackedData<T> p_238258_) {
      List<T> list = p_238258_.paletteEntries();
      int i = p_425228_.entryCount();
      Configuration configuration = p_425228_.getConfigurationForPaletteSize(list.size());
      int j = configuration.bitsInStorage();
      if (p_238258_.bitsPerEntry() != -1 && j != p_238258_.bitsPerEntry()) {
         return DataResult.error(() -> "Invalid bit count, calculated " + j + ", but container declared " + p_238258_.bitsPerEntry());
      }

      BitStorage bitstorage;
      Palette<T> palette;
      if (configuration.bitsInMemory() == 0) {
         palette = configuration.createPalette(p_425228_, list);
         bitstorage = new ZeroBitStorage(i);
      } else {
         Optional<LongStream> optional = p_238258_.storage();
         if (optional.isEmpty()) {
            return DataResult.error(() -> "Missing values for non-zero storage");
         }

         long[] along = optional.get().toArray();

         try {
            if (!configuration.alwaysRepack() && configuration.bitsInMemory() == j) {
               palette = configuration.createPalette(p_425228_, list);
               bitstorage = new SimpleBitStorage(configuration.bitsInMemory(), i, along);
            } else {
               Palette<T> palette1 = new HashMapPalette<>(j, list);
               SimpleBitStorage simplebitstorage = new SimpleBitStorage(j, i, along);
               Palette<T> palette2 = configuration.createPalette(p_425228_, list);
               int[] aint = reencodeContents(simplebitstorage, palette1, palette2);
               palette = palette2;
               bitstorage = new SimpleBitStorage(configuration.bitsInMemory(), i, aint);
            }
         } catch (SimpleBitStorage.InitializationException simplebitstorage$initializationexception) {
            return DataResult.error(() -> "Failed to read PalettedContainer: " + simplebitstorage$initializationexception.getMessage());
         }
      }

      return DataResult.success(new PalettedContainer<>(p_425228_, configuration, bitstorage, palette));
   }

   @Override
   public PalettedContainerRO.PackedData<T> pack(Strategy<T> p_422772_) {
      this.acquire();

      PalettedContainerRO.PackedData palettedcontainerro$packeddata;
      try {
         BitStorage bitstorage = this.data.storage;
         Palette<T> palette = this.data.palette;
         HashMapPalette<T> hashmappalette = new HashMapPalette<>(bitstorage.getBits());
         int i = p_422772_.entryCount();
         int[] aint = reencodeContents(bitstorage, palette, hashmappalette);
         Configuration configuration = p_422772_.getConfigurationForPaletteSize(hashmappalette.getSize());
         int j = configuration.bitsInStorage();
         Optional<LongStream> optional;
         if (j != 0) {
            SimpleBitStorage simplebitstorage = new SimpleBitStorage(j, i, aint);
            optional = Optional.of(Arrays.stream(simplebitstorage.getRaw()));
         } else {
            optional = Optional.empty();
         }

         palettedcontainerro$packeddata = new PalettedContainerRO.PackedData<>(hashmappalette.getEntries(), optional, j);
      } finally {
         this.release();
      }

      return palettedcontainerro$packeddata;
   }

   private static <T> int[] reencodeContents(BitStorage p_424383_, Palette<T> p_430775_, Palette<T> p_428228_) {
      int[] aint = new int[p_424383_.getSize()];
      p_424383_.unpack(aint);
      PaletteResize<T> paletteresize = PaletteResize.noResizeExpected();
      int i = -1;
      int j = -1;

      for (int k = 0; k < aint.length; k++) {
         int l = aint[k];
         if (l != i) {
            i = l;
            j = p_428228_.idFor(p_430775_.valueFor(l), paletteresize);
         }

         aint[k] = j;
      }

      return aint;
   }

   @Override
   public int getSerializedSize() {
      return this.data.getSerializedSize(this.strategy.globalMap());
   }

   @Override
   public int bitsPerEntry() {
      return this.data.storage().getBits();
   }

   @Override
   public boolean maybeHas(Predicate<T> p_63110_) {
      return this.data.palette.maybeHas(p_63110_);
   }

   @Override
   public PalettedContainer<T> copy() {
      return new PalettedContainer<>(this);
   }

   @Override
   public PalettedContainer<T> recreate() {
      return new PalettedContainer<>(this.data.palette.valueFor(0), this.strategy);
   }

   @Override
   public void count(PalettedContainer.CountConsumer<T> p_63100_) {
      if (this.data.palette.getSize() == 1) {
         p_63100_.accept(this.data.palette.valueFor(0), this.data.storage.getSize());
      } else {
         Int2IntOpenHashMap int2intopenhashmap = new Int2IntOpenHashMap();
         this.data.storage.getAll(p_238269_ -> int2intopenhashmap.addTo(p_238269_, 1));
         int2intopenhashmap.int2IntEntrySet().forEach(p_238271_ -> p_63100_.accept(this.data.palette.valueFor(p_238271_.getIntKey()), p_238271_.getIntValue()));
      }
   }

   @FunctionalInterface
   public interface CountConsumer<T> {
      void accept(T var1, int var2);
   }

   record Data<T>(Configuration configuration, BitStorage storage, Palette<T> palette) {
      public void copyFrom(Palette<T> p_188112_, BitStorage p_188113_) {
         PaletteResize<T> paletteresize = PaletteResize.noResizeExpected();

         for (int i = 0; i < p_188113_.getSize(); i++) {
            T t = p_188112_.valueFor(p_188113_.get(i));
            this.storage.set(i, this.palette.idFor(t, paletteresize));
         }
      }

      public int getSerializedSize(IdMap<T> p_424384_) {
         return 1 + this.palette.getSerializedSize(p_424384_) + this.storage.getRaw().length * 8;
      }

      public void write(FriendlyByteBuf p_188115_, IdMap<T> p_423327_) {
         p_188115_.writeByte(this.storage.getBits());
         this.palette.write(p_188115_, p_423327_);
         p_188115_.writeFixedSizeLongArray(this.storage.getRaw());
      }

      public PalettedContainer.Data<T> copy() {
         return new PalettedContainer.Data<>(this.configuration, this.storage.copy(), this.palette.copy());
      }
   }
}
