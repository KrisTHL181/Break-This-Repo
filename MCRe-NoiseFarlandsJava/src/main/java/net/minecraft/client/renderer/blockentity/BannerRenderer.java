package net.minecraft.client.renderer.blockentity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import java.util.function.Consumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.model.geom.EntityModelSet;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.banner.BannerFlagModel;
import net.minecraft.client.model.object.banner.BannerModel;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.state.BannerRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraft.client.resources.model.sprite.SpriteId;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.util.Unit;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.WallBannerBlock;
import net.minecraft.world.level.block.entity.BannerBlockEntity;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RotationSegment;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BannerRenderer implements BlockEntityRenderer<BannerBlockEntity, BannerRenderState> {
    private static final int MAX_PATTERNS = 16;
    private static final float SIZE = 0.6666667F;
    private static final Vector3fc MODEL_SCALE = new Vector3f(0.6666667F, -0.6666667F, -0.6666667F);
    private static final Vector3fc MODEL_TRANSLATION = new Vector3f(0.5F, 0.0F, 0.5F);
    public static final WallAndGroundTransformations<Transformation> TRANSFORMATIONS = new WallAndGroundTransformations<>(
        BannerRenderer::createWallTransformation, BannerRenderer::createGroundTransformation, 16
    );
    private final SpriteGetter sprites;
    private final BannerModel standingModel;
    private final BannerModel wallModel;
    private final BannerFlagModel standingFlagModel;
    private final BannerFlagModel wallFlagModel;

    public BannerRenderer(final BlockEntityRendererProvider.Context context) {
        this(context.entityModelSet(), context.sprites());
    }

    public BannerRenderer(final SpecialModelRenderer.BakingContext context) {
        this(context.entityModelSet(), context.sprites());
    }

    public BannerRenderer(final EntityModelSet modelSet, final SpriteGetter sprites) {
        this.sprites = sprites;
        this.standingModel = new BannerModel(modelSet.bakeLayer(ModelLayers.STANDING_BANNER));
        this.wallModel = new BannerModel(modelSet.bakeLayer(ModelLayers.WALL_BANNER));
        this.standingFlagModel = new BannerFlagModel(modelSet.bakeLayer(ModelLayers.STANDING_BANNER_FLAG));
        this.wallFlagModel = new BannerFlagModel(modelSet.bakeLayer(ModelLayers.WALL_BANNER_FLAG));
    }

    public BannerRenderState createRenderState() {
        return new BannerRenderState();
    }

    public void extractRenderState(
        final BannerBlockEntity blockEntity,
        final BannerRenderState state,
        final float partialTicks,
        final Vec3 cameraPosition,
        final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        BlockEntityRenderer.super.extractRenderState(blockEntity, state, partialTicks, cameraPosition, breakProgress);
        state.baseColor = blockEntity.getBaseColor();
        state.patterns = blockEntity.getPatterns();
        BlockState blockState = blockEntity.getBlockState();
        if (blockState.getBlock() instanceof BannerBlock) {
            state.transformation = TRANSFORMATIONS.freeTransformations(blockState.getValue(BannerBlock.ROTATION));
            state.attachmentType = BannerBlock.AttachmentType.GROUND;
        } else {
            state.transformation = TRANSFORMATIONS.wallTransformation(blockState.getValue(WallBannerBlock.FACING));
            state.attachmentType = BannerBlock.AttachmentType.WALL;
        }

        long gameTime = blockEntity.getLevel() != null ? blockEntity.getLevel().getGameTime() : 0L;
        BlockPos blockPos = blockEntity.getBlockPos();
        state.phase = ((float)Math.floorMod(blockPos.getX() * 7 + blockPos.getY() * 9 + blockPos.getZ() * 13 + gameTime, 100L) + partialTicks) / 100.0F;
    }

    private BannerModel bannerModel(final BannerBlock.AttachmentType type) {
        return switch (type) {
            case WALL -> this.wallModel;
            case GROUND -> this.standingModel;
        };
    }

    private BannerFlagModel flagModel(final BannerBlock.AttachmentType type) {
        return switch (type) {
            case WALL -> this.wallFlagModel;
            case GROUND -> this.standingFlagModel;
        };
    }

    public void submit(final BannerRenderState state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(state.transformation);
        submitBanner(
            this.sprites,
            poseStack,
            submitNodeCollector,
            state.lightCoords,
            OverlayTexture.NO_OVERLAY,
            this.bannerModel(state.attachmentType),
            this.flagModel(state.attachmentType),
            state.phase,
            state.baseColor,
            state.patterns,
            state.breakProgress,
            0
        );
        poseStack.popPose();
    }

    public void submitSpecial(
        final BannerBlock.AttachmentType type,
        final PoseStack poseStack,
        final SubmitNodeCollector submitNodeCollector,
        final int lightCoords,
        final int overlayCoords,
        final DyeColor baseColor,
        final BannerPatternLayers patterns,
        final int outlineColor
    ) {
        submitBanner(
            this.sprites,
            poseStack,
            submitNodeCollector,
            lightCoords,
            overlayCoords,
            this.bannerModel(type),
            this.flagModel(type),
            0.0F,
            baseColor,
            patterns,
            null,
            outlineColor
        );
    }

    private static void submitBanner(
        final SpriteGetter sprites,
        final PoseStack poseStack,
        final SubmitNodeCollector submitNodeCollector,
        final int lightCoords,
        final int overlayCoords,
        final BannerModel model,
        final BannerFlagModel flagModel,
        final float phase,
        final DyeColor baseColor,
        final BannerPatternLayers patterns,
        final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress,
        final int outlineColor
    ) {
        SpriteId sprite = Sheets.BANNER_BASE;
        submitNodeCollector.submitModel(model, Unit.INSTANCE, poseStack, lightCoords, overlayCoords, -1, sprite, sprites, outlineColor, breakProgress);
        submitNodeCollector.submitModel(flagModel, phase, poseStack, lightCoords, overlayCoords, -1, sprite, sprites, outlineColor, breakProgress);
        submitPatterns(sprites, poseStack, submitNodeCollector, lightCoords, overlayCoords, flagModel, phase, true, baseColor, patterns, breakProgress);
    }

    public static <S> void submitPatterns(
        final SpriteGetter sprites,
        final PoseStack poseStack,
        final SubmitNodeCollector submitNodeCollector,
        final int lightCoords,
        final int overlayCoords,
        final Model<S> model,
        final S state,
        final boolean banner,
        final DyeColor baseColor,
        final BannerPatternLayers patterns,
        final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        submitPatternLayer(
            sprites,
            poseStack,
            submitNodeCollector,
            lightCoords,
            overlayCoords,
            model,
            state,
            banner ? Sheets.BANNER_PATTERN_BASE : Sheets.SHIELD_PATTERN_BASE,
            baseColor,
            breakProgress
        );

        for (int maskIndex = 0; maskIndex < 16 && maskIndex < patterns.layers().size(); maskIndex++) {
            BannerPatternLayers.Layer layer = patterns.layers().get(maskIndex);
            SpriteId sprite = banner ? Sheets.getBannerSprite(layer.pattern()) : Sheets.getShieldSprite(layer.pattern());
            submitPatternLayer(
                sprites, poseStack, submitNodeCollector.order(maskIndex + 1), lightCoords, overlayCoords, model, state, sprite, layer.color(), null
            );
        }
    }

    private static <S> void submitPatternLayer(
        final SpriteGetter sprites,
        final PoseStack poseStack,
        final OrderedSubmitNodeCollector submitNodeCollector,
        final int lightCoords,
        final int overlayCoords,
        final Model<S> model,
        final S state,
        final SpriteId sprite,
        final DyeColor color,
        final ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress
    ) {
        int diffuseColor = color.getTextureDiffuseColor();
        submitNodeCollector.submitModel(
            model,
            state,
            poseStack,
            sprite.renderType(RenderTypes::bannerPattern),
            lightCoords,
            overlayCoords,
            diffuseColor,
            sprites.get(sprite),
            0,
            breakProgress
        );
    }

    public void getExtents(final Consumer<Vector3fc> output) {
        PoseStack poseStack = new PoseStack();
        this.standingModel.root().getExtentsForGui(poseStack, output);
        this.standingFlagModel.setupAnim(0.0F);
        this.standingFlagModel.root().getExtentsForGui(poseStack, output);
    }

    private static Transformation modelTransformation(final float angle) {
        return new Transformation(MODEL_TRANSLATION, Axis.YP.rotationDegrees(-angle), MODEL_SCALE, null);
    }

    private static Transformation createGroundTransformation(final int segment) {
        return modelTransformation(RotationSegment.convertToDegrees(segment));
    }

    private static Transformation createWallTransformation(final Direction direction) {
        return modelTransformation(direction.toYRot());
    }
}