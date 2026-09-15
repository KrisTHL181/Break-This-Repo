package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class EndermanRenderState extends HumanoidRenderState {
   public boolean isCreepy;
   public @Nullable BlockState carriedBlock;
}
