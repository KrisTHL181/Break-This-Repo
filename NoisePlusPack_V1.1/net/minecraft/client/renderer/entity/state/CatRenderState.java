package net.minecraft.client.renderer.entity.state;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class CatRenderState extends FelineRenderState {
   private static final Identifier DEFAULT_TEXTURE = Identifier.withDefaultNamespace("textures/entity/cat/tabby.png");
   public Identifier texture = DEFAULT_TEXTURE;
   public boolean isLyingOnTopOfSleepingPlayer;
   public @Nullable DyeColor collarColor;
}
