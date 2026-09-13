package net.minecraft.world.item.enchantment;

import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectIterator;
import it.unimi.dsi.fastutil.objects.Object2IntMap.Entry;
import java.util.Collections;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Predicate;
import net.minecraft.core.Holder;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.HolderSet;
import net.minecraft.core.Registry;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.EnchantmentTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipProvider;
import org.jspecify.annotations.Nullable;

public class ItemEnchantments implements TooltipProvider {
   public static final ItemEnchantments EMPTY = new ItemEnchantments(new Object2IntOpenHashMap());
   private static final Codec<Integer> LEVEL_CODEC = Codec.intRange(1, 255);
   public static final Codec<ItemEnchantments> CODEC = Codec.unboundedMap(Enchantment.CODEC, LEVEL_CODEC)
      .xmap(map -> new ItemEnchantments(new Object2IntOpenHashMap(map)), enchantments -> enchantments.enchantments);
   public static final StreamCodec<RegistryFriendlyByteBuf, ItemEnchantments> STREAM_CODEC = StreamCodec.composite(
      ByteBufCodecs.map(Object2IntOpenHashMap::new, Enchantment.STREAM_CODEC, ByteBufCodecs.VAR_INT), c -> c.enchantments, ItemEnchantments::new
   );
   private final Object2IntOpenHashMap<Holder<Enchantment>> enchantments;

   private ItemEnchantments(final Object2IntOpenHashMap<Holder<Enchantment>> enchantments) {
      this.enchantments = enchantments;
      ObjectIterator var2 = enchantments.object2IntEntrySet().iterator();

      while (var2.hasNext()) {
         Entry<Holder<Enchantment>> entry = (Entry<Holder<Enchantment>>)var2.next();
         int level = entry.getIntValue();
         if (level < 0 || level > 255) {
            throw new IllegalArgumentException("Enchantment " + entry.getKey() + " has invalid level " + level);
         }
      }
   }

   public int getLevel(final Holder<Enchantment> enchantment) {
      return this.enchantments.getInt(enchantment);
   }

   @Override
   public void addToTooltip(final Item.TooltipContext context, final Consumer<Component> consumer, final TooltipFlag flag, final DataComponentGetter components) {
      HolderLookup.Provider registries = context.registries();
      HolderSet<Enchantment> order = getTagOrEmpty(registries, Registries.ENCHANTMENT, EnchantmentTags.TOOLTIP_ORDER);

      for (Holder<Enchantment> enchantment : order) {
         int level = this.enchantments.getInt(enchantment);
         if (level > 0) {
            consumer.accept(Enchantment.getFullname(enchantment, level));
         }
      }

      ObjectIterator var10 = this.enchantments.object2IntEntrySet().iterator();

      while (var10.hasNext()) {
         Entry<Holder<Enchantment>> entry = (Entry<Holder<Enchantment>>)var10.next();
         Holder<Enchantment> enchantment = (Holder<Enchantment>)entry.getKey();
         if (!order.contains(enchantment)) {
            consumer.accept(Enchantment.getFullname((Holder<Enchantment>)entry.getKey(), entry.getIntValue()));
         }
      }
   }

   private static <T> HolderSet<T> getTagOrEmpty(final HolderLookup.@Nullable Provider registries, final ResourceKey<Registry<T>> registry, final TagKey<T> tag) {
      if (registries != null) {
         Optional<HolderSet.Named<T>> maybeOrder = registries.lookupOrThrow(registry).get(tag);
         if (maybeOrder.isPresent()) {
            return maybeOrder.get();
         }
      }

      return HolderSet.empty();
   }

   public Set<Holder<Enchantment>> keySet() {
      return Collections.unmodifiableSet(this.enchantments.keySet());
   }

   public Set<Entry<Holder<Enchantment>>> entrySet() {
      return Collections.unmodifiableSet(this.enchantments.object2IntEntrySet());
   }

   public int size() {
      return this.enchantments.size();
   }

   public boolean isEmpty() {
      return this.enchantments.isEmpty();
   }

   @Override
   public boolean equals(final Object obj) {
      if (this == obj) {
         return true;
      } else {
         return obj instanceof ItemEnchantments that ? this.enchantments.equals(that.enchantments) : false;
      }
   }

   @Override
   public int hashCode() {
      return this.enchantments.hashCode();
   }

   @Override
   public String toString() {
      return "ItemEnchantments{enchantments=" + this.enchantments + "}";
   }

   public static class Mutable {
      private final Object2IntOpenHashMap<Holder<Enchantment>> enchantments = new Object2IntOpenHashMap();

      public Mutable(final ItemEnchantments enchantments) {
         this.enchantments.putAll(enchantments.enchantments);
      }

      public void set(final Holder<Enchantment> enchantment, final int level) {
         if (level <= 0) {
            this.enchantments.removeInt(enchantment);
         } else {
            this.enchantments.put(enchantment, Math.min(level, 255));
         }
      }

      public void upgrade(final Holder<Enchantment> enchantment, final int level) {
         if (level > 0) {
            this.enchantments.merge(enchantment, Math.min(level, 255), Integer::max);
         }
      }

      public void removeIf(final Predicate<Holder<Enchantment>> predicate) {
         this.enchantments.keySet().removeIf(predicate);
      }

      public int getLevel(final Holder<Enchantment> enchantment) {
         return this.enchantments.getOrDefault(enchantment, 0);
      }

      public Set<Holder<Enchantment>> keySet() {
         return this.enchantments.keySet();
      }

      public ItemEnchantments toImmutable() {
         return new ItemEnchantments(this.enchantments);
      }
   }
}
