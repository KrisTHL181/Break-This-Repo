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
   private static final ClassTreeIdRegistry ID_REGISTRY = new ClassTreeIdRegistry();
   private final SyncedDataHolder entity;
   private final SynchedEntityData.DataItem<?>[] itemsById;
   private boolean isDirty;

   private SynchedEntityData(final SyncedDataHolder entity, final SynchedEntityData.DataItem<?>[] itemsById) {
      this.entity = entity;
      this.itemsById = itemsById;
   }

   public static <T> EntityDataAccessor<T> defineId(final Class<? extends SyncedDataHolder> clazz, final EntityDataSerializer<T> type) {
      if (LOGGER.isDebugEnabled()) {
         try {
            Class<?> aClass = Class.forName(Thread.currentThread().getStackTrace()[2].getClassName());
            if (!aClass.equals(clazz)) {
               LOGGER.debug("defineId called for: {} from {}", new Object[]{clazz, aClass, new RuntimeException()});
            }
         } catch (ClassNotFoundException var3) {
         }
      }

      int id = ID_REGISTRY.define(clazz);
      if (id > 254) {
         throw new IllegalArgumentException("Data value id is too big with " + id + "! (Max is 254)");
      } else {
         return type.createAccessor(id);
      }
   }

   private <T> SynchedEntityData.DataItem<T> getItem(final EntityDataAccessor<T> accessor) {
      return (SynchedEntityData.DataItem<T>)this.itemsById[accessor.id()];
   }

   public <T> T get(final EntityDataAccessor<T> accessor) {
      return this.getItem(accessor).getValue();
   }

   public <T> void set(final EntityDataAccessor<T> accessor, final T value) {
      this.set(accessor, value, false);
   }

   public <T> void set(final EntityDataAccessor<T> accessor, final T value, final boolean forceDirty) {
      SynchedEntityData.DataItem<T> dataItem = this.getItem(accessor);
      if (forceDirty || ObjectUtils.notEqual(value, dataItem.getValue())) {
         dataItem.setValue(value);
         this.entity.onSyncedDataUpdated(accessor);
         dataItem.setDirty(true);
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
      List<SynchedEntityData.DataValue<?>> result = new ArrayList<>();

      for (SynchedEntityData.DataItem<?> dataItem : this.itemsById) {
         if (dataItem.isDirty()) {
            dataItem.setDirty(false);
            result.add(dataItem.value());
         }
      }

      return result;
   }

   public @Nullable List<SynchedEntityData.DataValue<?>> getNonDefaultValues() {
      List<SynchedEntityData.DataValue<?>> result = null;

      for (SynchedEntityData.DataItem<?> dataItem : this.itemsById) {
         if (!dataItem.isSetToDefault()) {
            if (result == null) {
               result = new ArrayList<>();
            }

            result.add(dataItem.value());
         }
      }

      return result;
   }

   public void assignValues(final List<SynchedEntityData.DataValue<?>> items) {
      for (SynchedEntityData.DataValue<?> item : items) {
         SynchedEntityData.DataItem<?> dataItem = this.itemsById[item.id];
         this.assignValue(dataItem, item);
         this.entity.onSyncedDataUpdated(dataItem.getAccessor());
      }

      this.entity.onSyncedDataUpdated(items);
   }

   private <T> void assignValue(final SynchedEntityData.DataItem<T> dataItem, final SynchedEntityData.DataValue<?> item) {
      if (!Objects.equals(item.serializer(), dataItem.accessor.serializer())) {
         throw new IllegalStateException(
            String.format(
               Locale.ROOT,
               "Invalid entity data item type for field %d on entity %s: old=%s(%s), new=%s(%s)",
               dataItem.accessor.id(),
               this.entity,
               dataItem.value,
               dataItem.value.getClass(),
               item.value,
               item.value.getClass()
            )
         );
      }

      dataItem.setValue((T)item.value);
   }

   public static class Builder {
      private final SyncedDataHolder entity;
      private final SynchedEntityData.@Nullable DataItem<?>[] itemsById;

      public Builder(final SyncedDataHolder entity) {
         this.entity = entity;
         this.itemsById = new SynchedEntityData.DataItem[SynchedEntityData.ID_REGISTRY.getCount(entity.getClass())];
      }

      public <T> SynchedEntityData.Builder define(final EntityDataAccessor<T> accessor, final T value) {
         int id = accessor.id();
         if (id > this.itemsById.length) {
            throw new IllegalArgumentException("Data value id is too big with " + id + "! (Max is " + this.itemsById.length + ")");
         }

         if (this.itemsById[id] != null) {
            throw new IllegalArgumentException("Duplicate id value for " + id + "!");
         }

         if (EntityDataSerializers.getSerializedId(accessor.serializer()) < 0) {
            throw new IllegalArgumentException("Unregistered serializer " + accessor.serializer() + " for " + id + "!");
         }

         this.itemsById[accessor.id()] = new SynchedEntityData.DataItem<>(accessor, value);
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
      private final EntityDataAccessor<T> accessor;
      private T value;
      private final T initialValue;
      private boolean dirty;

      public DataItem(final EntityDataAccessor<T> accessor, final T initialValue) {
         this.accessor = accessor;
         this.initialValue = initialValue;
         this.value = initialValue;
      }

      public EntityDataAccessor<T> getAccessor() {
         return this.accessor;
      }

      public void setValue(final T value) {
         this.value = value;
      }

      public T getValue() {
         return this.value;
      }

      public boolean isDirty() {
         return this.dirty;
      }

      public void setDirty(final boolean dirty) {
         this.dirty = dirty;
      }

      public boolean isSetToDefault() {
         return this.initialValue.equals(this.value);
      }

      public SynchedEntityData.DataValue<T> value() {
         return SynchedEntityData.DataValue.create(this.accessor, this.value);
      }
   }

   public record DataValue<T>(int id, EntityDataSerializer<T> serializer, T value) {
      public static <T> SynchedEntityData.DataValue<T> create(final EntityDataAccessor<T> accessor, final T value) {
         EntityDataSerializer<T> serializer = accessor.serializer();
         return new SynchedEntityData.DataValue<>(accessor.id(), serializer, serializer.copy(value));
      }

      public void write(final RegistryFriendlyByteBuf output) {
         int serializerId = EntityDataSerializers.getSerializedId(this.serializer);
         if (serializerId < 0) {
            throw new EncoderException("Unknown serializer type " + this.serializer);
         }

         output.writeByte(this.id);
         output.writeVarInt(serializerId);
         this.serializer.codec().encode(output, this.value);
      }

      public static SynchedEntityData.DataValue<?> read(final RegistryFriendlyByteBuf input, final int id) {
         int type = input.readVarInt();
         EntityDataSerializer<?> serializer = EntityDataSerializers.getSerializer(type);
         if (serializer == null) {
            throw new DecoderException("Unknown serializer type " + type);
         } else {
            return read(input, id, serializer);
         }
      }

      private static <T> SynchedEntityData.DataValue<T> read(final RegistryFriendlyByteBuf input, final int id, final EntityDataSerializer<T> serializer) {
         return new SynchedEntityData.DataValue<>(id, serializer, serializer.codec().decode(input));
      }
   }
}
