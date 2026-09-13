package net.minecraft.world.level.storage;

import com.mojang.serialization.Codec;
import com.mojang.serialization.DynamicOps;
import com.mojang.serialization.MapCodec;
import java.util.Collections;
import java.util.Iterator;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.Tag;

public class ValueInputContextHelper {
   final HolderLookup.Provider lookup;
   private final DynamicOps<Tag> ops;
   final ValueInput.ValueInputList emptyChildList = new ValueInput.ValueInputList() {
      @Override
      public boolean isEmpty() {
         return true;
      }

      @Override
      public Stream<ValueInput> stream() {
         return Stream.empty();
      }

      @Override
      public Iterator<ValueInput> iterator() {
         return Collections.emptyIterator();
      }
   };
   private final ValueInput.TypedInputList<Object> emptyTypedList = new ValueInput.TypedInputList<Object>() {
      @Override
      public boolean isEmpty() {
         return true;
      }

      @Override
      public Stream<Object> stream() {
         return Stream.empty();
      }

      @Override
      public Iterator<Object> iterator() {
         return Collections.emptyIterator();
      }
   };
   private final ValueInput empty = new ValueInput() {
      @Override
      public <T> Optional<T> read(String p_409633_, Codec<T> p_409097_) {
         return Optional.empty();
      }

      @Override
      public <T> Optional<T> read(MapCodec<T> p_406513_) {
         return Optional.empty();
      }

      @Override
      public Optional<ValueInput> child(String p_407548_) {
         return Optional.empty();
      }

      @Override
      public ValueInput childOrEmpty(String p_405886_) {
         return this;
      }

      @Override
      public Optional<ValueInput.ValueInputList> childrenList(String p_410211_) {
         return Optional.empty();
      }

      @Override
      public ValueInput.ValueInputList childrenListOrEmpty(String p_407655_) {
         return ValueInputContextHelper.this.emptyChildList;
      }

      @Override
      public <T> Optional<ValueInput.TypedInputList<T>> list(String p_407586_, Codec<T> p_405984_) {
         return Optional.empty();
      }

      @Override
      public <T> ValueInput.TypedInputList<T> listOrEmpty(String p_407683_, Codec<T> p_406255_) {
         return ValueInputContextHelper.this.emptyTypedList();
      }

      @Override
      public boolean getBooleanOr(String p_410083_, boolean p_408304_) {
         return p_408304_;
      }

      @Override
      public byte getByteOr(String p_410345_, byte p_410503_) {
         return p_410503_;
      }

      @Override
      public int getShortOr(String p_407606_, short p_409191_) {
         return p_409191_;
      }

      @Override
      public Optional<Integer> getInt(String p_409158_) {
         return Optional.empty();
      }

      @Override
      public int getIntOr(String p_408323_, int p_409619_) {
         return p_409619_;
      }

      @Override
      public long getLongOr(String p_407063_, long p_407312_) {
         return p_407312_;
      }

      @Override
      public Optional<Long> getLong(String p_406389_) {
         return Optional.empty();
      }

      @Override
      public float getFloatOr(String p_407538_, float p_408700_) {
         return p_408700_;
      }

      @Override
      public double getDoubleOr(String p_410446_, double p_409165_) {
         return p_409165_;
      }

      @Override
      public Optional<String> getString(String p_409405_) {
         return Optional.empty();
      }

      @Override
      public String getStringOr(String p_409852_, String p_408310_) {
         return p_408310_;
      }

      @Override
      public HolderLookup.Provider lookup() {
         return ValueInputContextHelper.this.lookup;
      }

      @Override
      public Optional<int[]> getIntArray(String p_410316_) {
         return Optional.empty();
      }
   };

   public ValueInputContextHelper(HolderLookup.Provider p_407381_, DynamicOps<Tag> p_406716_) {
      this.lookup = p_407381_;
      this.ops = p_407381_.createSerializationContext(p_406716_);
   }

   public DynamicOps<Tag> ops() {
      return this.ops;
   }

   public HolderLookup.Provider lookup() {
      return this.lookup;
   }

   public ValueInput empty() {
      return this.empty;
   }

   public ValueInput.ValueInputList emptyList() {
      return this.emptyChildList;
   }

   public <T> ValueInput.TypedInputList<T> emptyTypedList() {
      return (ValueInput.TypedInputList<T>)this.emptyTypedList;
   }
}
