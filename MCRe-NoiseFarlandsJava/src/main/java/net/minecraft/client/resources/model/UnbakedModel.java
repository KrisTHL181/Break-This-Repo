package net.minecraft.client.resources.model;

import net.minecraft.client.resources.model.cuboid.ItemTransforms;
import net.minecraft.client.resources.model.geometry.UnbakedGeometry;
import net.minecraft.client.resources.model.sprite.TextureSlots;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface UnbakedModel {
    String PARTICLE_TEXTURE_REFERENCE = "particle";

    default @Nullable Boolean ambientOcclusion() {
        return null;
    }

    default UnbakedModel.@Nullable GuiLight guiLight() {
        return null;
    }

    default @Nullable ItemTransforms transforms() {
        return null;
    }

    default TextureSlots.Data textureSlots() {
        return TextureSlots.Data.EMPTY;
    }

    default @Nullable UnbakedGeometry geometry() {
        return null;
    }

    default @Nullable Identifier parent() {
        return null;
    }

    @OnlyIn(Dist.CLIENT)
    enum GuiLight {
        FRONT("front"),
        SIDE("side");

        private final String name;

        GuiLight(final String name) {
            this.name = name;
        }

        public static UnbakedModel.GuiLight getByName(final String name) {
            for (UnbakedModel.GuiLight target : values()) {
                if (target.name.equals(name)) {
                    return target;
                }
            }

            throw new IllegalArgumentException("Invalid gui light: " + name);
        }

        public boolean lightLikeBlock() {
            return this == SIDE;
        }
    }
}