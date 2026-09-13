package net.minecraft.core.particles;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

public class ItemParticleOption implements ParticleOptions {
   private static final Codec<ItemStack> ITEM_CODEC = Codec.withAlternative(ItemStack.SINGLE_ITEM_CODEC, Item.CODEC, ItemStack::new);
   private final ParticleType<ItemParticleOption> type;
   private final ItemStack itemStack;

   public static MapCodec<ItemParticleOption> codec(ParticleType<ItemParticleOption> p_123711_) {
      return ITEM_CODEC.xmap(p_123714_ -> new ItemParticleOption(p_123711_, p_123714_), p_123709_ -> p_123709_.itemStack).fieldOf("item");
   }

   public static StreamCodec<? super RegistryFriendlyByteBuf, ItemParticleOption> streamCodec(ParticleType<ItemParticleOption> p_332819_) {
      return ItemStack.STREAM_CODEC.map(p_325801_ -> new ItemParticleOption(p_332819_, p_325801_), p_325802_ -> p_325802_.itemStack);
   }

   public ItemParticleOption(ParticleType<ItemParticleOption> p_123705_, ItemStack p_123706_) {
      if (p_123706_.isEmpty()) {
         throw new IllegalArgumentException("Empty stacks are not allowed");
      }

      this.type = p_123705_;
      this.itemStack = p_123706_;
   }

   @Override
   public ParticleType<ItemParticleOption> getType() {
      return this.type;
   }

   public ItemStack getItem() {
      return this.itemStack;
   }
}
