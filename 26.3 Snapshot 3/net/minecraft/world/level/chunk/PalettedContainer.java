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

   public static <T> Codec<PalettedContainer<T>> codecRW(final Codec<T> elementCodec, final Strategy<T> strategy, final T defaultValue) {
      PalettedContainerRO.Unpacker<T, PalettedContainer<T>> unpacker = PalettedContainer::unpack;
      return codec(elementCodec, strategy, defaultValue, unpacker);
   }

   public static <T> Codec<PalettedContainerRO<T>> codecRO(final Codec<T> elementCodec, final Strategy<T> strategy, final T defaultValue) {
      PalettedContainerRO.Unpacker<T, PalettedContainerRO<T>> unpacker = (s, data) -> unpack(s, data).map(e -> e);
      return codec(elementCodec, strategy, defaultValue, unpacker);
   }

   // ===== 修改：显式指定 RecordCodecBuilder 类型，为 comapFlatMap 的 lambda 显式参数类型 =====
   private static <T, C extends PalettedContainerRO<T>> Codec<C> codec(
      final Codec<T> elementCodec, final Strategy<T> strategy, final T defaultValue, final PalettedContainerRO.Unpacker<T, C> unpacker
   ) {
      return RecordCodecBuilder.<PalettedContainerRO.PackedData<T>>create(
            i -> i.group(
                  elementCodec.mapResult(ExtraCodecs.orElsePartial(defaultValue))
                     .listOf()
                     .fieldOf("palette")
                     .forGetter(PalettedContainerRO.PackedData::paletteEntries),
                  Codec.LONG_STREAM.lenientOptionalFieldOf("data").forGetter(PalettedContainerRO.PackedData::storage)
               )
               .apply(i, PalettedContainerRO.PackedData::new)
         )
         .comapFlatMap(
            (PalettedContainerRO.PackedData<T> discData) -> unpacker.read(strategy, discData),
            (C palettedContainer) -> palettedContainer.pack(strategy)
         );
   }

   private PalettedContainer(final Strategy<T> strategy, final Configuration dataConfiguration, final BitStorage storage, final Palette<T> palette) {
      this.strategy = strategy;
      this.data = new PalettedContainer.Data<>(dataConfiguration, storage, palette);
   }

   private PalettedContainer(final PalettedContainer<T> source) {
      this.strategy = source.strategy;
      this.data = source.data.copy();
   }

   public PalettedContainer(final T initialValue, final Strategy<T> strategy) {
      this.strategy = strategy;
      this.data = this.createOrReuseData(null, 0);
      this.data.palette.idFor(initialValue, this);
   }

   private PalettedContainer.Data<T> createOrReuseData(final PalettedContainer.@Nullable Data<T> oldData, final int targetBits) {
      Configuration dataConfiguration = this.strategy.getConfigurationForBitCount(targetBits);
      if (oldData != null && dataConfiguration.equals(oldData.configuration())) {
         return oldData;
      }

      BitStorage storage = dataConfiguration.bitsInMemory() == 0
         ? new ZeroBitStorage(this.strategy.entryCount())
         : new SimpleBitStorage(dataConfiguration.bitsInMemory(), this.strategy.entryCount());
      Palette<T> palette = dataConfiguration.createPalette(this.strategy, List.of());
      return new PalettedContainer.Data<>(dataConfiguration, storage, palette);
   }

   @Override
   public int onResize(final int bits, final T lastAddedValue) {
      PalettedContainer.Data<T> oldData = this.data;
      PalettedContainer.Data<T> newData = this.createOrReuseData(oldData, bits);
      newData.copyFrom(oldData.palette, oldData.storage);
      this.data = newData;
      return newData.palette.idFor(lastAddedValue, PaletteResize.noResizeExpected());
   }

   public T getAndSet(final int x, final int y, final int z, final T value) {
      this.acquire();

      try {
         return this.getAndSet(this.strategy.getIndex(x, y, z), value);
      } finally {
         this.release();
      }
   }

   public T getAndSetUnchecked(final int x, final int y, final int z, final T value) {
      return this.getAndSet(this.strategy.getIndex(x, y, z), value);
   }

   private T getAndSet(final int index, final T value) {
      int id = this.data.palette.idFor(value, this);
      int oldId = this.data.storage.getAndSet(index, id);
      return this.data.palette.valueFor(oldId);
   }

   public void set(final int x, final int y, final int z, final T value) {
      this.acquire();

      try {
         this.set(this.strategy.getIndex(x, y, z), value);
      } finally {
         this.release();
      }
   }

   private void set(final int index, final T value) {
      int id = this.data.palette.idFor(value, this);
      this.data.storage.set(index, id);
   }

   @Override
   public T get(final int x, final int y, final int z) {
      return this.get(this.strategy.getIndex(x, y, z));
   }

   protected T get(final int index) {
      PalettedContainer.Data<T> data = this.data;
      return data.palette.valueFor(data.storage.get(index));
   }

   @Override
   public void getAll(final Consumer<T> consumer) {
      Palette<T> palette = this.data.palette();
      IntSet allExistingEntries = new IntArraySet();
      this.data.storage.getAll(allExistingEntries::add);
      allExistingEntries.forEach(state -> consumer.accept(palette.valueFor(state)));
   }

   public void read(final FriendlyByteBuf buffer) {
      this.acquire();

      try {
         int newBits = buffer.readByte();
         PalettedContainer.Data<T> newData = this.createOrReuseData(this.data, newBits);
         newData.palette.read(buffer, this.strategy.globalMap());
         buffer.readFixedSizeLongArray(newData.storage.getRaw());
         this.data = newData;
      } finally {
         this.release();
      }
   }

   @Override
   public void write(final FriendlyByteBuf buffer) {
      this.acquire();

      try {
         this.data.write(buffer, this.strategy.globalMap());
      } finally {
         this.release();
      }
   }

   @VisibleForTesting
   public static <T> DataResult<PalettedContainer<T>> unpack(final Strategy<T> strategy, final PalettedContainerRO.PackedData<T> discData) {
      List<T> paletteEntries = discData.paletteEntries();
      int entryCount = strategy.entryCount();
      Configuration storedConfiguration = strategy.getConfigurationForPaletteSize(paletteEntries.size());
      int bitsOnDisc = storedConfiguration.bitsInStorage();
      if (discData.bitsPerEntry() != -1 && bitsOnDisc != discData.bitsPerEntry()) {
         return DataResult.error(() -> "Invalid bit count, calculated " + bitsOnDisc + ", but container declared " + discData.bitsPerEntry());
      }

      BitStorage storage;
      Palette<T> palette;
      if (storedConfiguration.bitsInMemory() == 0) {
         palette = storedConfiguration.createPalette(strategy, paletteEntries);
         storage = new ZeroBitStorage(entryCount);
      } else {
         Optional<LongStream> dataOpt = discData.storage();
         if (dataOpt.isEmpty()) {
            return DataResult.error(() -> "Missing values for non-zero storage");
         }

         long[] data = dataOpt.get().toArray();

         try {
            if (!storedConfiguration.alwaysRepack() && storedConfiguration.bitsInMemory() == bitsOnDisc) {
               palette = storedConfiguration.createPalette(strategy, paletteEntries);
               storage = new SimpleBitStorage(storedConfiguration.bitsInMemory(), entryCount, data);
            } else {
               Palette<T> oldPalette = new HashMapPalette<>(bitsOnDisc, paletteEntries);
               SimpleBitStorage oldStorage = new SimpleBitStorage(bitsOnDisc, entryCount, data);
               Palette<T> newPalette = storedConfiguration.createPalette(strategy, paletteEntries);
               int[] newContents = reencodeContents(oldStorage, oldPalette, newPalette);
               palette = newPalette;
               storage = new SimpleBitStorage(storedConfiguration.bitsInMemory(), entryCount, newContents);
            }
         } catch (SimpleBitStorage.InitializationException exception) {
            return DataResult.error(() -> "Failed to read PalettedContainer: " + exception.getMessage());
         }
      }

      return DataResult.success(new PalettedContainer<>(strategy, storedConfiguration, storage, palette));
   }

   @Override
   public PalettedContainerRO.PackedData<T> pack(final Strategy<T> strategy) {
      this.acquire();

      try {
         BitStorage currentStorage = this.data.storage;
         Palette<T> currentPalette = this.data.palette;
         HashMapPalette<T> newPalette = new HashMapPalette<>(currentStorage.getBits());
         int entryCount = strategy.entryCount();
         int[] newContents = reencodeContents(currentStorage, currentPalette, newPalette);
         Configuration storedConfiguration = strategy.getConfigurationForPaletteSize(newPalette.getSize());
         int bitsOnDisc = storedConfiguration.bitsInStorage();
         Optional<LongStream> values;
         if (bitsOnDisc != 0) {
            SimpleBitStorage storage = new SimpleBitStorage(bitsOnDisc, entryCount, newContents);
            values = Optional.of(Arrays.stream(storage.getRaw()));
         } else {
            values = Optional.empty();
         }

         return new PalettedContainerRO.PackedData<>(newPalette.getEntries(), values, bitsOnDisc);
      } finally {
         this.release();
      }
   }

   private static <T> int[] reencodeContents(final BitStorage storage, final Palette<T> oldPalette, final Palette<T> newPalette) {
      int[] buffer = new int[storage.getSize()];
      storage.unpack(buffer);
      PaletteResize<T> dummyResizer = PaletteResize.noResizeExpected();
      int lastReadId = -1;
      int lastWrittenId = -1;

      for (int index = 0; index < buffer.length; index++) {
         int id = buffer[index];
         if (id != lastReadId) {
            lastReadId = id;
            lastWrittenId = newPalette.idFor(oldPalette.valueFor(id), dummyResizer);
         }

         buffer[index] = lastWrittenId;
      }

      return buffer;
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
   public boolean maybeHas(final Predicate<T> predicate) {
      return this.data.palette.maybeHas(predicate);
   }

   @Override
   public void forEachInPalette(final Consumer<T> consumer) {
      for (int i = 0; i < this.data.palette.getSize(); i++) {
         consumer.accept(this.data.palette.valueFor(i));
      }
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
   public void count(final PalettedContainer.CountConsumer<T> output) {
      if (this.data.palette.getSize() == 1) {
         output.accept(this.data.palette.valueFor(0), this.data.storage.getSize());
      } else {
         Int2IntOpenHashMap counts = new Int2IntOpenHashMap();
         this.data.storage.getAll(state -> counts.addTo(state, 1));
         counts.int2IntEntrySet().forEach(entry -> output.accept(this.data.palette.valueFor(entry.getIntKey()), entry.getIntValue()));
      }
   }

   @FunctionalInterface
   public interface CountConsumer<T> {
      void accept(final T entry, final int count);
   }

   private record Data<T>(Configuration configuration, BitStorage storage, Palette<T> palette) {
      public void copyFrom(final Palette<T> oldPalette, final BitStorage oldStorage) {
         PaletteResize<T> dummyResizer = PaletteResize.noResizeExpected();

         for (int i = 0; i < oldStorage.getSize(); i++) {
            T value = oldPalette.valueFor(oldStorage.get(i));
            this.storage.set(i, this.palette.idFor(value, dummyResizer));
         }
      }

      public int getSerializedSize(final IdMap<T> globalMap) {
         return 1 + this.palette.getSerializedSize(globalMap) + this.storage.getRaw().length * 8;
      }

      public void write(final FriendlyByteBuf buffer, final IdMap<T> globalMap) {
         buffer.writeByte(this.storage.getBits());
         this.palette.write(buffer, globalMap);
         buffer.writeFixedSizeLongArray(this.storage.getRaw());
      }

      public PalettedContainer.Data<T> copy() {
         return new PalettedContainer.Data<>(this.configuration, this.storage.copy(), this.palette.copy());
      }
   }
}