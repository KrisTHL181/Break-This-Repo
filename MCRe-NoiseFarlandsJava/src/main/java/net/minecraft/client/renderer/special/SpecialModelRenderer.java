package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import java.util.function.Consumer;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public interface SpecialModelRenderer<T> {
    void submit(
        @Nullable T argument,
        PoseStack poseStack,
        SubmitNodeCollector submitNodeCollector,
        int lightCoords,
        int overlayCoords,
        boolean hasFoil,
        final int outlineColor
    );

    void getExtents(Consumer<Vector3fc> output);

    @Nullable T extractArgument(ItemStack stack);

    @OnlyIn(Dist.CLIENT)
    interface BakingContext {
        EntityModelSet entityModelSet();

        SpriteGetter sprites();

        PlayerSkinRenderCache playerSkinRenderCache();
    }

    @OnlyIn(Dist.CLIENT)
    interface Unbaked<T> {
        @Nullable SpecialModelRenderer<T> bake(SpecialModelRenderer.BakingContext context);

        MapCodec<? extends SpecialModelRenderer.Unbaked<T>> type();
    }
}