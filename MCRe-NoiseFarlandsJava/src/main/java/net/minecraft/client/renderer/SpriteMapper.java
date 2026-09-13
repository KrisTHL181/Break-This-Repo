package net.minecraft.client.renderer;

import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public record SpriteMapper(Identifier sheet, String prefix) {
    public SpriteId apply(final Identifier path) {
        return new SpriteId(this.sheet, path.withPrefix(this.prefix + "/"));
    }

    public SpriteId defaultNamespaceApply(final String path) {
        return this.apply(Identifier.withDefaultNamespace(path));
    }
}