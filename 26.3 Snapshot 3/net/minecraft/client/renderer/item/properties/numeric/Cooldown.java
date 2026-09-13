package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public record Cooldown() implements RangeSelectItemModelProperty {
   public static final MapCodec<Cooldown> MAP_CODEC = MapCodec.unit(new Cooldown());

   @Override
   public float get(final ItemStack itemStack, final @Nullable ClientLevel level, final @Nullable ItemOwner owner, final int seed) {
      return owner != null && owner.asLivingEntity() instanceof Player player ? player.getCooldowns().getCooldownPercent(itemStack, 0.0F) : 0.0F;
   }

   @Override
   public MapCodec<Cooldown> type() {
      return MAP_CODEC;
   }
}
