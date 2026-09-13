package net.minecraft.world.entity;

import com.mojang.serialization.Codec;
import java.util.EnumMap;
import java.util.Map;
import java.util.Objects;
import java.util.Map.Entry;
import net.minecraft.world.item.ItemStack;

public class EntityEquipment {
   public static final Codec<EntityEquipment> CODEC = Codec.unboundedMap(EquipmentSlot.CODEC, ItemStack.CODEC).xmap(p_395484_ -> {
      EnumMap<EquipmentSlot, ItemStack> enummap = new EnumMap<>(EquipmentSlot.class);
      enummap.putAll(p_395484_);
      return new EntityEquipment(enummap);
   }, p_392815_ -> {
      Map<EquipmentSlot, ItemStack> map = new EnumMap<>(p_392815_.items);
      map.values().removeIf(ItemStack::isEmpty);
      return map;
   });
   private final EnumMap<EquipmentSlot, ItemStack> items;

   private EntityEquipment(EnumMap<EquipmentSlot, ItemStack> p_397671_) {
      this.items = p_397671_;
   }

   public EntityEquipment() {
      this(new EnumMap<>(EquipmentSlot.class));
   }

   public ItemStack set(EquipmentSlot p_397868_, ItemStack p_394019_) {
      return Objects.requireNonNullElse(this.items.put(p_397868_, p_394019_), ItemStack.EMPTY);
   }

   public ItemStack get(EquipmentSlot p_392964_) {
      return this.items.getOrDefault(p_392964_, ItemStack.EMPTY);
   }

   public boolean isEmpty() {
      for (ItemStack itemstack : this.items.values()) {
         if (!itemstack.isEmpty()) {
            return false;
         }
      }

      return true;
   }

   public void tick(Entity p_394224_) {
      for (Entry<EquipmentSlot, ItemStack> entry : this.items.entrySet()) {
         ItemStack itemstack = entry.getValue();
         if (!itemstack.isEmpty()) {
            itemstack.inventoryTick(p_394224_.level(), p_394224_, entry.getKey());
         }
      }
   }

   public void setAll(EntityEquipment p_397344_) {
      this.items.clear();
      this.items.putAll(p_397344_.items);
   }

   public void dropAll(LivingEntity p_394536_) {
      for (ItemStack itemstack : this.items.values()) {
         p_394536_.drop(itemstack, true, false);
      }

      this.clear();
   }

   public void clear() {
      this.items.replaceAll((p_393205_, p_394162_) -> ItemStack.EMPTY);
   }
}
