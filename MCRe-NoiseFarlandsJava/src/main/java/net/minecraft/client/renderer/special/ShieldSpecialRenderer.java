package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Transformation;
import com.mojang.serialization.MapCodec;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.equipment.ShieldModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.util.Unit;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class ShieldSpecialRenderer implements SpecialModelRenderer<DataComponentMap> {
    public static final Transformation DEFAULT_TRANSFORMATION = new Transformation(null, null, new Vector3f(1.0F, -1.0F, -1.0F), null);
    private final SpriteGetter sprites;
    private final ShieldModel model;

    public ShieldSpecialRenderer(final SpriteGetter sprites, final ShieldModel model) {
        this.sprites = sprites;
        this.model = model;
    }

    public @Nullable DataComponentMap extractArgument(final ItemStack stack) {
        return stack.immutableComponents();
    }

    public void submit(
        final @Nullable DataComponentMap components,
        final PoseStack poseStack,
        final SubmitNodeCollector submitNodeCollector,
        final int lightCoords,
        final int overlayCoords,
        final boolean hasFoil,
        final int outlineColor
    ) {
        BannerPatternLayers patterns = components != null
            ? components.getOrDefault(DataComponents.BANNER_PATTERNS, BannerPatternLayers.EMPTY)
            : BannerPatternLayers.EMPTY;
        DyeColor baseColor = components != null ? components.get(DataComponents.BASE_COLOR) : null;
        boolean hasPatterns = !patterns.layers().isEmpty() || baseColor != null;
        SpriteId base = hasPatterns ? Sheets.SHIELD_BASE : Sheets.SHIELD_BASE_NO_PATTERN;
        submitNodeCollector.submitModel(this.model, Unit.INSTANCE, poseStack, lightCoords, overlayCoords, -1, base, this.sprites, outlineColor, null);
        if (hasPatterns) {
            BannerRenderer.submitPatterns(
                this.sprites,
                poseStack,
                submitNodeCollector,
                lightCoords,
                overlayCoords,
                this.model,
                Unit.INSTANCE,
                false,
                Objects.requireNonNullElse(baseColor, DyeColor.WHITE),
                patterns,
                null
            );
        }

        if (hasFoil) {
            submitNodeCollector.order(patterns.layers().size() + 1)
                .submitModel(this.model, Unit.INSTANCE, poseStack, RenderTypes.entityGlint(), lightCoords, overlayCoords, -1, this.sprites.get(base), 0, null);
        }
    }

    @Override
    public void getExtents(final Consumer<Vector3fc> output) {
        PoseStack poseStack = new PoseStack();
        this.model.root().getExtentsForGui(poseStack, output);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked() implements SpecialModelRenderer.Unbaked<DataComponentMap> {
        public static final ShieldSpecialRenderer.Unbaked INSTANCE = new ShieldSpecialRenderer.Unbaked();
        public static final MapCodec<ShieldSpecialRenderer.Unbaked> MAP_CODEC = MapCodec.unit(INSTANCE);

        @Override
        public MapCodec<ShieldSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        public ShieldSpecialRenderer bake(final SpecialModelRenderer.BakingContext context) {
            return new ShieldSpecialRenderer(context.sprites(), new ShieldModel(context.entityModelSet().bakeLayer(ModelLayers.SHIELD)));
        }
    }
}