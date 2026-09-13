package net.minecraft.client.renderer.item.properties.numeric;

import com.mojang.serialization.MapCodec;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class CompassAngle implements RangeSelectItemModelProperty {
   public static final MapCodec<CompassAngle> MAP_CODEC = CompassAngleState.MAP_CODEC.xmap(CompassAngle::new, p_375840_ -> p_375840_.state);
   private final CompassAngleState state;

   public CompassAngle(boolean p_378444_, CompassAngleState.CompassTarget p_378609_) {
      this(new CompassAngleState(p_378444_, p_378609_));
   }

   private CompassAngle(CompassAngleState p_377062_) {
      this.state = p_377062_;
   }

   @Override
   public float get(ItemStack p_378698_, @Nullable ClientLevel p_375696_, @Nullable ItemOwner p_431479_, int p_377498_) {
      return this.state.get(p_378698_, p_375696_, p_431479_, p_377498_);
   }

   @Override
   public MapCodec<CompassAngle> type() {
      return MAP_CODEC;
   }
}
