package net.minecraft.network.syncher;

import com.mojang.logging.LogUtils;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.util.ClassTreeIdRegistry;
import org.apache.commons.lang3.ObjectUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;

public class SynchedEntityData {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int MAX_ID_VALUE = 254;
   static final ClassTreeIdRegistry ID_REGISTRY = new ClassTreeIdRegistry();
   private final SyncedDataHolder entity;
   private final SynchedEntityData.DataItem<?>[] itemsById;
   private boolean isDirty;

   SynchedEntityData(SyncedDataHolder p_334075_, SynchedEntityData.DataItem<?>[] p_331536_) {
      this.entity = p_334075_;
      this.itemsById = p_331536_;
   }

   public static <T> EntityDataAccessor<T> defineId(Class<? extends SyncedDataHolder> p_135354_, EntityDataSerializer<T> p_135355_) {
      if (LOGGER.isDebugEnabled()) {
         try {
            Class<?> oclass = Class.forName(Thread.currentThread().getStackTrace()[2].getClassName());
            if (!oclass.equals(p_135354_)) {
               LOGGER.debug("defineId called for: {} from {}", new Object[]{p_135354_, oclass, new RuntimeException()});
            }
         } catch (ClassNotFoundException var3) {
         }
      }

      int i = ID_REGISTRY.define(p_135354_);
      if (i > 254) {
         throw new IllegalArgumentException("Data value id is too big with " + i + "! (Max is 254)");
      } else {
         return p_135355_.createAccessor(i);
      }
   }

   private <T> SynchedEntityData.DataItem<T> getItem(EntityDataAccessor<T> p_135380_) {
      return (SynchedEntityData.DataItem<T>)this.itemsById[p_135380_.id()];
   }

   public <T> T get(EntityDataAccessor<T> p_135371_) {
      return this.getItem(p_135371_).getValue();
   }

   public <T> void set(EntityDataAccessor<T> p_135382_, T p_135383_) {
      this.set(p_135382_, p_135383_, false);
   }

   public <T> void set(EntityDataAccessor<T> p_276368_, T p_276363_, boolean p_276370_) {
      SynchedEntityData.DataItem<T> dataitem = this.getItem(p_276368_);
      if (p_276370_ || ObjectUtils.notEqual(p_276363_, dataitem.getValue())) {
         dataitem.setValue(p_276363_);
         this.entity.onSyncedDataUpdated(p_276368_);
         dataitem.setDirty(true);
         this.isDirty = true;
      }
   }

   public boolean isDirty() {
      return this.isDirty;
   }

   public @Nullable List<SynchedEntityData.DataValue<?>> packDirty() {
      if (!this.isDirty) {
         return null;
      }

      this.isDirty = false;
      List<SynchedEntityData.DataValue<?>> list = new ArrayList<>();

      for (SynchedEntityData.DataItem<?> dataitem : this.itemsById) {
         if (dataitem.isDirty()) {
            dataitem.setDirty(false);
            list.add(dataitem.value());
         }
      }

      return list;
   }

   public @Nullable List<SynchedEntityData.DataValue<?>> getNonDefaultValues() {
      List<SynchedEntityData.DataValue<?>> list = null;

      for (SynchedEntityData.DataItem<?> dataitem : this.itemsById) {
         if (!dataitem.isSetToDefault()) {
            if (list == null) {
               list = new ArrayList<>();
            }

            list.add(dataitem.value());
         }
      }

      return list;
   }

   public void assignValues(List<SynchedEntityData.DataValue<?>> p_135357_) {
      for (SynchedEntityData.DataValue<?> datavalue : p_135357_) {
         SynchedEntityData.DataItem<?> dataitem = this.itemsById[datavalue.id];
         this.assignValue(dataitem, datavalue);
         this.entity.onSyncedDataUpdated(dataitem.getAccessor());
      }

      this.entity.onSyncedDataUpdated(p_135357_);
   }

   private <T> void assignValue(SynchedEntityData.DataItem<T> p_135376_, SynchedEntityData.DataValue<?> p_254484_) {
      if (!Objects.equals(p_254484_.serializer(), p_135376_.accessor.serializer())) {
         throw new IllegalStateException(
            String.format(
               Locale.ROOT,
               "Invalid entity data item type for field %d on entity %s: old=%s(%s), new=%s(%s)",
               p_135376_.accessor.id(),
               this.entity,
               p_135376_.value,
               p_135376_.value.getClass(),
               p_254484_.value,
               p_254484_.value.getClass()
            )
         );
      }

      p_135376_.setValue((T)p_254484_.value);
   }

   public static class Builder {
      private final SyncedDataHolder entity;
      private final SynchedEntityData.@Nullable DataItem<?>[] itemsById;

      public Builder(SyncedDataHolder p_334752_) {
         this.entity = p_334752_;
         this.itemsById = new SynchedEntityData.DataItem[SynchedEntityData.ID_REGISTRY.getCount(p_334752_.getClass())];
      }

      public <T> SynchedEntityData.Builder define(EntityDataAccessor<T> p_329741_, T p_330016_) {
         int i = p_329741_.id();
         if (i > this.itemsById.length) {
            throw new IllegalArgumentException("Data value id is too big with " + i + "! (Max is " + this.itemsById.length + ")");
         }

         if (this.itemsById[i] != null) {
            throw new IllegalArgumentException("Duplicate id value for " + i + "!");
         }

         if (EntityDataSerializers.getSerializedId(p_329741_.serializer()) < 0) {
            throw new IllegalArgumentException("Unregistered serializer " + p_329741_.serializer() + " for " + i + "!");
         }

         this.itemsById[p_329741_.id()] = new SynchedEntityData.DataItem<>(p_329741_, p_330016_);
         return this;
      }

      public SynchedEntityData build() {
         for (int i = 0; i < this.itemsById.length; i++) {
            if (this.itemsById[i] == null) {
               throw new IllegalStateException("Entity " + this.entity.getClass() + " has not defined synched data value " + i);
            }
         }

         return new SynchedEntityData(this.entity, this.itemsById);
      }
   }

   public static class DataItem<T> {
      final EntityDataAccessor<T> accessor;
      T value;
      private final T initialValue;
      private boolean dirty;

      public DataItem(EntityDataAccessor<T> p_135394_, T p_135395_) {
         this.accessor = p_135394_;
         this.initialValue = p_135395_;
         this.value = p_135395_;
      }

      public EntityDataAccessor<T> getAccessor() {
         return this.accessor;
      }

      public void setValue(T p_135398_) {
         this.value = p_135398_;
      }

      public T getValue() {
         return this.value;
      }

      public boolean isDirty() {
         return this.dirty;
      }

      public void setDirty(boolean p_135402_) {
         this.dirty = p_135402_;
      }

      public boolean isSetToDefault() {
         return this.initialValue.equals(this.value);
      }

      public SynchedEntityData.DataValue<T> value() {
         return SynchedEntityData.DataValue.create(this.accessor, this.value);
      }
   }

   public record DataValue<T>(int id, EntityDataSerializer<T> serializer, T value) {
      public static <T> SynchedEntityData.DataValue<T> create(EntityDataAccessor<T> p_254543_, T p_254138_) {
         EntityDataSerializer<T> entitydataserializer = p_254543_.serializer();
         return new SynchedEntityData.DataValue<>(p_254543_.id(), entitydataserializer, entitydataserializer.copy(p_254138_));
      }

      public void write(RegistryFriendlyByteBuf p_328126_) {
         int i = EntityDataSerializers.getSerializedId(this.serializer);
         if (i < 0) {
            throw new EncoderException("Unknown serializer type " + this.serializer);
         }

         p_328126_.writeByte(this.id);
         p_328126_.writeVarInt(i);
         this.serializer.codec().encode(p_328126_, this.value);
      }

      public static SynchedEntityData.DataValue<?> read(RegistryFriendlyByteBuf p_335154_, int p_254356_) {
         int i = p_335154_.readVarInt();
         EntityDataSerializer<?> entitydataserializer = EntityDataSerializers.getSerializer(i);
         if (entitydataserializer == null) {
            throw new DecoderException("Unknown serializer type " + i);
         } else {
            return read(p_335154_, p_254356_, entitydataserializer);
         }
      }

      private static <T> SynchedEntityData.DataValue<T> read(RegistryFriendlyByteBuf p_333448_, int p_253899_, EntityDataSerializer<T> p_254222_) {
         return new SynchedEntityData.DataValue<>(p_253899_, p_254222_, p_254222_.codec().decode(p_333448_));
      }
   }
}
