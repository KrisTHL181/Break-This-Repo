package net.minecraft.world.item.enchantment.effects;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.enchantment.EnchantedItemInUse;
import net.minecraft.world.item.enchantment.LevelBasedValue;
import net.minecraft.world.phys.Vec3;

public record ChangeItemDamage(LevelBasedValue amount) implements EnchantmentEntityEffect {
   public static final MapCodec<ChangeItemDamage> CODEC = RecordCodecBuilder.mapCodec(
      p_360725_ -> p_360725_.group(LevelBasedValue.CODEC.fieldOf("amount").forGetter(p_361527_ -> p_361527_.amount)).apply(p_360725_, ChangeItemDamage::new)
   );

   @Override
   public void apply(ServerLevel p_367230_, int p_364456_, EnchantedItemInUse p_368500_, Entity p_365057_, Vec3 p_365026_) {
      ItemStack itemstack = p_368500_.itemStack();
      if (itemstack.has(DataComponents.MAX_DAMAGE) && itemstack.has(DataComponents.DAMAGE)) {
         ServerPlayer serverplayer = p_368500_.owner() instanceof ServerPlayer serverplayer1 ? serverplayer1 : null;
         int i = (int)this.amount.calculate(p_364456_);
         itemstack.hurtAndBreak(i, p_367230_, serverplayer, p_368500_.onBreak());
      }
   }

   @Override
   public MapCodec<ChangeItemDamage> codec() {
      return CODEC;
   }
}
