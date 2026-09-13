package net.minecraft.client.player.inventory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.logging.LogUtils;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.DynamicOps;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.RegistryAccess;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.util.Util;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.slf4j.Logger;

@OnlyIn(Dist.CLIENT)
public class Hotbar {
   private static final Logger LOGGER = LogUtils.getLogger();
   private static final int SIZE = Inventory.getSelectionSize();
   public static final Codec<Hotbar> CODEC = Codec.PASSTHROUGH
      .listOf()
      .validate(p_448147_ -> Util.fixedSize(p_448147_, SIZE))
      .xmap(Hotbar::new, p_334316_ -> p_334316_.items);
   private static final DynamicOps<Tag> DEFAULT_OPS = NbtOps.INSTANCE;
   private static final Dynamic<?> EMPTY_STACK = new Dynamic(DEFAULT_OPS, (Tag)ItemStack.OPTIONAL_CODEC.encodeStart(DEFAULT_OPS, ItemStack.EMPTY).getOrThrow());
   private List<Dynamic<?>> items;

   private Hotbar(List<Dynamic<?>> p_336192_) {
      this.items = p_336192_;
   }

   public Hotbar() {
      this(Collections.nCopies(SIZE, EMPTY_STACK));
   }

   public List<ItemStack> load(HolderLookup.Provider p_331400_) {
      return this.items
         .stream()
         .map(
            p_334847_ -> ItemStack.OPTIONAL_CODEC
               .parse(RegistryOps.injectRegistryContext(p_334847_, p_331400_))
               .resultOrPartial(p_332209_ -> LOGGER.warn("Could not parse hotbar item: {}", p_332209_))
               .orElse(ItemStack.EMPTY)
         )
         .toList();
   }

   public void storeFrom(Inventory p_335728_, RegistryAccess p_328533_) {
      RegistryOps<Tag> registryops = p_328533_.createSerializationContext(DEFAULT_OPS);
      Builder<Dynamic<?>> builder = ImmutableList.builderWithExpectedSize(SIZE);

      for (int i = 0; i < SIZE; i++) {
         ItemStack itemstack = p_335728_.getItem(i);
         Optional<Dynamic<?>> optional = ItemStack.OPTIONAL_CODEC
            .encodeStart(registryops, itemstack)
            .resultOrPartial(p_332599_ -> LOGGER.warn("Could not encode hotbar item: {}", p_332599_))
            .map(p_331427_ -> new Dynamic(DEFAULT_OPS, p_331427_));
         builder.add(optional.orElse(EMPTY_STACK));
      }

      this.items = builder.build();
   }

   public boolean isEmpty() {
      for (Dynamic<?> dynamic : this.items) {
         if (!isEmpty(dynamic)) {
            return false;
         }
      }

      return true;
   }

   private static boolean isEmpty(Dynamic<?> p_331706_) {
      return EMPTY_STACK.equals(p_331706_);
   }
}
