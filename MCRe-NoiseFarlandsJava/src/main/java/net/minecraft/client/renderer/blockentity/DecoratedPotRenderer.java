package net.minecraft.client.renderer.blockentity;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import java.util.EnumSet;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.geom.PartPose;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.CubeListBuilder;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.model.geom.builders.MeshDefinition;
import net.minecraft.client.model.geom.builders.PartDefinition;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.DecoratedPotRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.Direction;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.entity.DecoratedPotBlockEntity;
import net.minecraft.world.level.block.entity.DecoratedPotPattern;
import net.minecraft.world.level.block.entity.DecoratedPotPatterns;
import net.minecraft.world.level.block.entity.PotDecorations;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class DecoratedPotRenderer implements BlockEntityRenderer<DecoratedPotBlockEntity, DecoratedPotRenderState> {
    private static final Map<Direction, Transformation> TRANSFORMATIONS = Util.makeEnumMap(Direction.class, DecoratedPotRenderer::createModelTransformation);
    private static final Map<ResourceKey<Item>, SpriteId> DECORATED_POT_SPRITES = Util.make(() -> {
        Builder<ResourceKey<Item>, SpriteId> builder = ImmutableMap.builder();
        DecoratedPotPatterns.itemToPatternMappings((itemId, patternId) -> {
            Holder.Reference<DecoratedPotPattern> pattern = BuiltInRegistries.DECORATED_POT_PATTERN.getOrThrow(patternId);
            builder.put(itemId, Sheets.DECORATED_POT_MAPPER.apply(pattern.value().assetId()));
        });
        return builder.buildOrThrow();
    });
    private final SpriteGetter sprites;
    private static final String NECK = "neck";
    private static final String FRONT = "front";
    private static final String BACK = "back";
    private static final String LEFT = "left";
    private static final String RIGHT = "right";
    private static final String TOP = "top";
    private static final String BOTTOM = "bottom";
    private final ModelPart neck;
    private final ModelPart frontSide;
    private final ModelPart backSide;
    private final ModelPart leftSide;
    private final ModelPart rightSide;
    private final ModelPart top;
    private final ModelPart bottom;
    private static final float WOBBLE_AMPLITUDE = 0.125F;

    public DecoratedPotRenderer(final BlockEntityRendererProvider.Context context) {
        this(context.entityModelSet(), context.sprites());
    }

    public DecoratedPotRenderer(final SpecialModelRenderer.BakingContext context) {
        this(context.entityModelSet(), context.sprites());
    }

    public DecoratedPotRenderer(final EntityModelSet entityModelSet, final SpriteGetter sprites) {
        this.sprites = sprites;
        ModelPart baseRoot = entityModelSet.bakeLayer(ModelLayers.DECORATED_POT_BASE);
        this.neck = baseRoot.getChild("neck");
        this.top = baseRoot.getChild("top");
        this.bottom = baseRoot.getChild("bottom");
        ModelPart sidesRoot = entityModelSet.bakeLayer(ModelLayers.DECORATED_POT_SIDES);
        this.frontSide = sidesRoot.getChild("front");
        this.backSide = sidesRoot.getChild("back");
        this.leftSide = sidesRoot.getChild("left");
        this.rightSide = sidesRoot.getChild("right");
    }

    public static LayerDefinition createBaseLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeDeformation inflate = new CubeDeformation(0.2F);
        CubeDeformation deflate = new CubeDeformation(-0.1F);
        root.addOrReplaceChild(
            "neck",
            CubeListBuilder.create()
                .texOffs(0, 0)
                .addBox(4.0F, 17.0F, 4.0F, 8.0F, 3.0F, 8.0F, deflate)
                .texOffs(0, 5)
                .addBox(5.0F, 20.0F, 5.0F, 6.0F, 1.0F, 6.0F, inflate),
            PartPose.offsetAndRotation(0.0F, 37.0F, 16.0F, (float) Math.PI, 0.0F, 0.0F)
        );
        CubeListBuilder topBottomPlane = CubeListBuilder.create().texOffs(-14, 13).addBox(0.0F, 0.0F, 0.0F, 14.0F, 0.0F, 14.0F);
        root.addOrReplaceChild("top", topBottomPlane, PartPose.offsetAndRotation(1.0F, 16.0F, 1.0F, 0.0F, 0.0F, 0.0F));
        root.addOrReplaceChild("bottom", topBottomPlane, PartPose.offsetAndRotation(1.0F, 0.0F, 1.0F, 0.0F, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 32, 32);
    }

    public static LayerDefinition createSidesLayer() {
        MeshDefinition mesh = new MeshDefinition();
        PartDefinition root = mesh.getRoot();
        CubeListBuilder sidePlane = CubeListBuilder.create().texOffs(1, 0).addBox(0.0F, 0.0F, 0.0F, 14.0F, 16.0F, 0.0F, EnumSet.of(Direction.NORTH));
        root.addOrReplaceChild("back", sidePlane, PartPose.offsetAndRotation(15.0F, 16.0F, 1.0F, 0.0F, 0.0F, (float) Math.PI));
        root.addOrReplaceChild("left", sidePlane, PartPose.offsetAndRotation(1.0F, 16.0F, 1.0F, 0.0F, (float) (-Math.PI / 2), (float) Math.PI));
        root.addOrReplaceChild("right", sidePlane, PartPose.offsetAndRotation(15.0F, 16.0F, 15.0F, 0.0F, (float) (Math.PI / 2), (float) Math.PI));
        root.addOrReplaceChild("front", sidePlane, PartPose.offsetAndRotation(1.0F, 16.0F, 15.0F, (float) Math.PI, 0.0F, 0.0F));
        return LayerDefinition.create(mesh, 16, 16);
    }

    private static SpriteId getSideSprite(final Optional<Item> item) {
        if (item.isPresent()) {
            SpriteId result = DECORATED_POT_SPRITES.get(item.get().builtInRegistryHolder().key());
            if (result != null) {
                return result;
            }
        }

        return Sheets.DECORATED_POT_SIDE;
    }

    public DecoratedPotRenderState createRenderState() {
        return new DecoratedPotRenderState();
    }

    public void extractRenderState(
        final DecoratedPotBlockEntity blockEntity,
        final DecoratedPotRenderState state,
        final float partialTicks,
        final Vec3 cameraPosition,
        final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.decorations = blockEntity.getDecorations();
        state.direction = blockEntity.getDirection();
        DecoratedPotBlockEntity.WobbleStyle wobbleStyle = blockEntity.lastWobbleStyle;
        if (wobbleStyle != null && blockEntity.getLevel() != null) {
            state.wobbleProgress = ((float)(blockEntity.getLevel().getGameTime() - blockEntity.wobbleStartedAtTick) + partialTicks) / wobbleStyle.duration;
        } else {
            state.wobbleProgress = 0.0F;
        }
    }

    public void submit(
        final DecoratedPotRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera
    ) {
        poseStack.pushPose();
        poseStack.mulPose(modelTransformation(state.direction));
        if (state.wobbleProgress >= 0.0F && state.wobbleProgress <= 1.0F) {
            if (state.wobbleStyle == DecoratedPotBlockEntity.WobbleStyle.POSITIVE) {
                float amplitude = 0.015625F;
                float deltaTime = state.wobbleProgress * (float) (Math.PI * 2);
                float tiltX = -1.5F * (Mth.cos(deltaTime) + 0.5F) * Mth.sin(deltaTime / 2.0F);
                poseStack.rotateAround(Axis.XP.rotation(tiltX * 0.015625F), 0.5F, 0.0F, 0.5F);
                float tiltZ = Mth.sin(deltaTime);
                poseStack.rotateAround(Axis.ZP.rotation(tiltZ * 0.015625F), 0.5F, 0.0F, 0.5F);
            } else {
                float turnAngle = Mth.sin(-state.wobbleProgress * 3.0F * (float) Math.PI) * 0.125F;
                float linearDecayFactor = 1.0F - state.wobbleProgress;
                poseStack.rotateAround(Axis.YP.rotation(turnAngle * linearDecayFactor), 0.5F, 0.0F, 0.5F);
            }
        }

        this.submit(poseStack, submitNodeCollector, state.lightCoords, OverlayTexture.NO_OVERLAY, state.decorations, 0);
        poseStack.popPose();
    }

    public static Transformation modelTransformation(final Direction facing) {
        return TRANSFORMATIONS.get(facing);
    }

    private static Transformation createModelTransformation(final Direction entityDirection) {
        return new Transformation(new Matrix4f().rotateAround(Axis.YP.rotationDegrees(180.0F - entityDirection.toYRot()), 0.5F, 0.5F, 0.5F));
    }

    public void submit(
        final PoseStack poseStack,
        final SubmitNodeCollector submitNodeCollector,
        final int lightCoords,
        final int overlayCoords,
        final PotDecorations decorations,
        final int outlineColor
    ) {
        RenderType renderType = Sheets.DECORATED_POT_BASE.renderType(RenderTypes::entitySolid);
        TextureAtlasSprite sprite = this.sprites.get(Sheets.DECORATED_POT_BASE);
        submitNodeCollector.submitModelPart(this.neck, poseStack, renderType, lightCoords, overlayCoords, sprite, -1, null, outlineColor);
        submitNodeCollector.submitModelPart(this.top, poseStack, renderType, lightCoords, overlayCoords, sprite, -1, null, outlineColor);
        submitNodeCollector.submitModelPart(this.bottom, poseStack, renderType, lightCoords, overlayCoords, sprite, -1, null, outlineColor);
        SpriteId frontSprite = getSideSprite(decorations.front());
        submitNodeCollector.submitModelPart(
            this.frontSide,
            poseStack,
            frontSprite.renderType(RenderTypes::entitySolid),
            lightCoords,
            overlayCoords,
            this.sprites.get(frontSprite),
            -1,
            null,
            outlineColor
        );
        SpriteId backSprite = getSideSprite(decorations.back());
        submitNodeCollector.submitModelPart(
            this.backSide,
            poseStack,
            backSprite.renderType(RenderTypes::entitySolid),
            lightCoords,
            overlayCoords,
            this.sprites.get(backSprite),
            -1,
            null,
            outlineColor
        );
        SpriteId leftSprite = getSideSprite(decorations.left());
        submitNodeCollector.submitModelPart(
            this.leftSide,
            poseStack,
            leftSprite.renderType(RenderTypes::entitySolid),
            lightCoords,
            overlayCoords,
            this.sprites.get(leftSprite),
            -1,
            null,
            outlineColor
        );
        SpriteId rightSprite = getSideSprite(decorations.right());
        submitNodeCollector.submitModelPart(
            this.rightSide,
            poseStack,
            rightSprite.renderType(RenderTypes::entitySolid),
            lightCoords,
            overlayCoords,
            this.sprites.get(rightSprite),
            -1,
            null,
            outlineColor
        );
    }

    public void getExtents(final Consumer<Vector3fc> output) {
        PoseStack poseStack = new PoseStack();
        this.neck.getExtentsForGui(poseStack, output);
        this.top.getExtentsForGui(poseStack, output);
        this.bottom.getExtentsForGui(poseStack, output);
    }
}