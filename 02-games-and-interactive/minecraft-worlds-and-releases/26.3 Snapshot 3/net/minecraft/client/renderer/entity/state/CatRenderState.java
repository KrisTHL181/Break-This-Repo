package net.minecraft.client.renderer.entity.state;

import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import org.jspecify.annotations.Nullable;

public class CatRenderState extends FelineRenderState {
   private static final Identifier DEFAULT_TEXTURE = Identifier.withDefaultNamespace("textures/entity/cat/cat_tabby.png");
   public Identifier texture = DEFAULT_TEXTURE;
   public boolean isLyingOnTopOfSleepingPlayer;
   public @Nullable DyeColor collarColor;
}
