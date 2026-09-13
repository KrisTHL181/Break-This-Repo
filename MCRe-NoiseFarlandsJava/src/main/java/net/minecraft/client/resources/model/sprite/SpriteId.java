package net.minecraft.client.resources.model.sprite;

import java.util.function.Function;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record SpriteId(Identifier atlasLocation, Identifier texture) {
    public RenderType renderType(final Function<Identifier, RenderType> renderType) {
        return renderType.apply(this.atlasLocation);
    }
}