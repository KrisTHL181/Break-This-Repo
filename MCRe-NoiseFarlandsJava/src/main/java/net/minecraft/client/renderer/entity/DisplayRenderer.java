package net.minecraft.client.renderer.entity;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.math.Transformation;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Camera;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.OrderedSubmitNodeCollector;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockModelResolver;
import net.minecraft.client.renderer.block.model.BlockDisplayContext;
import net.minecraft.client.renderer.entity.state.BlockDisplayEntityRenderState;
import net.minecraft.client.renderer.entity.state.DisplayEntityRenderState;
import net.minecraft.client.renderer.entity.state.ItemDisplayEntityRenderState;
import net.minecraft.client.renderer.entity.state.TextDisplayEntityRenderState;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.Display;
import net.minecraft.world.phys.AABB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Quaternionf;

@OnlyIn(Dist.CLIENT)
public abstract class DisplayRenderer<T extends Display, S, ST extends DisplayEntityRenderState> extends EntityRenderer<T, ST> {
    public static final BlockDisplayContext BLOCK_DISPLAY_CONTEXT = BlockDisplayContext.create();
    private final EntityRenderDispatcher entityRenderDispatcher;
    protected final BlockModelResolver blockModelResolver;

    protected DisplayRenderer(final EntityRendererProvider.Context context) {
        super(context);
        this.entityRenderDispatcher = context.getEntityRenderDispatcher();
        this.blockModelResolver = context.getBlockModelResolver();
    }

    protected AABB getBoundingBoxForCulling(final T entity) {
        return entity.getBoundingBoxForCulling();
    }

    protected boolean affectedByCulling(final T entity) {
        return entity.affectedByCulling();
    }

    private static int getBrightnessOverride(final Display entity) {
        Display.RenderState renderState = entity.renderState();
        return renderState != null ? renderState.brightnessOverride() : -1;
    }

    protected int getSkyLightLevel(final T entity, final BlockPos blockPos) {
        int packedBrightnessOverride = getBrightnessOverride(entity);
        return packedBrightnessOverride != -1 ? LightCoordsUtil.sky(packedBrightnessOverride) : super.getSkyLightLevel(entity, blockPos);
    }

    protected int getBlockLightLevel(final T entity, final BlockPos blockPos) {
        int packedBrightnessOverride = getBrightnessOverride(entity);
        return packedBrightnessOverride != -1 ? LightCoordsUtil.block(packedBrightnessOverride) : super.getBlockLightLevel(entity, blockPos);
    }

    protected float getShadowRadius(final ST state) {
        Display.RenderState renderState = state.renderState;
        return renderState == null ? 0.0F : renderState.shadowRadius().get(state.interpolationProgress);
    }

    protected float getShadowStrength(final ST state) {
        Display.RenderState renderState = state.renderState;
        return renderState == null ? 0.0F : renderState.shadowStrength().get(state.interpolationProgress);
    }

    public void submit(final ST state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final CameraRenderState camera) {
        Display.RenderState renderState = state.renderState;
        if (renderState != null && state.hasSubState()) {
            float interpolationProgress = state.interpolationProgress;
            super.submit(state, poseStack, submitNodeCollector, camera);
            poseStack.pushPose();
            poseStack.mulPose(this.calculateOrientation(renderState, state, new Quaternionf()));
            Transformation transformation = renderState.transformation().get(interpolationProgress);
            poseStack.mulPose(transformation);
            this.submitInner(state, poseStack, submitNodeCollector, state.lightCoords, interpolationProgress);
            poseStack.popPose();
        }
    }

    private Quaternionf calculateOrientation(final Display.RenderState renderState, final ST state, final Quaternionf output) {
        return switch (renderState.billboardConstraints()) {
            case FIXED -> output.rotationYXZ((float) (-Math.PI / 180.0) * state.entityYRot, (float) (Math.PI / 180.0) * state.entityXRot, 0.0F);
            case HORIZONTAL -> output.rotationYXZ(
                (float) (-Math.PI / 180.0) * state.entityYRot, (float) (Math.PI / 180.0) * transformXRot(state.cameraXRot), 0.0F
            );
            case VERTICAL -> output.rotationYXZ(
                (float) (-Math.PI / 180.0) * transformYRot(state.cameraYRot), (float) (Math.PI / 180.0) * state.entityXRot, 0.0F
            );
            case CENTER -> output.rotationYXZ(
                (float) (-Math.PI / 180.0) * transformYRot(state.cameraYRot), (float) (Math.PI / 180.0) * transformXRot(state.cameraXRot), 0.0F
            );
        };
    }

    private static float transformYRot(final float cameraYRot) {
        return cameraYRot - 180.0F;
    }

    private static float transformXRot(final float cameraXRot) {
        return -cameraXRot;
    }

    private static <T extends Display> float entityYRot(final T entity, final float partialTicks) {
        return entity.getYRot(partialTicks);
    }

    private static <T extends Display> float entityXRot(final T entity, final float partialTicks) {
        return entity.getXRot(partialTicks);
    }

    protected abstract void submitInner(
        final ST state, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector, final int lightCoords, final float interpolationProgress
    );

    public void extractRenderState(final T entity, final ST state, final float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.renderState = entity.renderState();
        state.interpolationProgress = entity.calculateInterpolationProgress(partialTicks);
        state.entityYRot = entityYRot(entity, partialTicks);
        state.entityXRot = entityXRot(entity, partialTicks);
        Camera camera = this.entityRenderDispatcher.camera;
        state.cameraXRot = camera.xRot();
        state.cameraYRot = camera.yRot();
    }

    @OnlyIn(Dist.CLIENT)
    public static class BlockDisplayRenderer
        extends DisplayRenderer<Display.BlockDisplay, Display.BlockDisplay.BlockRenderState, BlockDisplayEntityRenderState> {
        protected BlockDisplayRenderer(final EntityRendererProvider.Context context) {
            super(context);
        }

        public BlockDisplayEntityRenderState createRenderState() {
            return new BlockDisplayEntityRenderState();
        }

        public void extractRenderState(final Display.BlockDisplay entity, final BlockDisplayEntityRenderState state, final float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            Display.BlockDisplay.BlockRenderState blockRenderState = entity.blockRenderState();
            if (blockRenderState != null) {
                this.blockModelResolver.update(state.blockModel, blockRenderState.blockState(), BLOCK_DISPLAY_CONTEXT);
            } else {
                state.blockModel.clear();
            }
        }

        public void submitInner(
            final BlockDisplayEntityRenderState state,
            final PoseStack poseStack,
            final SubmitNodeCollector submitNodeCollector,
            final int lightCoords,
            final float interpolationProgress
        ) {
            state.blockModel.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class ItemDisplayRenderer extends DisplayRenderer<Display.ItemDisplay, Display.ItemDisplay.ItemRenderState, ItemDisplayEntityRenderState> {
        private final ItemModelResolver itemModelResolver;

        protected ItemDisplayRenderer(final EntityRendererProvider.Context context) {
            super(context);
            this.itemModelResolver = context.getItemModelResolver();
        }

        public ItemDisplayEntityRenderState createRenderState() {
            return new ItemDisplayEntityRenderState();
        }

        public void extractRenderState(final Display.ItemDisplay entity, final ItemDisplayEntityRenderState state, final float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            Display.ItemDisplay.ItemRenderState itemRenderState = entity.itemRenderState();
            if (itemRenderState != null) {
                this.itemModelResolver.updateForNonLiving(state.item, itemRenderState.itemStack(), itemRenderState.itemTransform(), entity);
            } else {
                state.item.clear();
            }
        }

        public void submitInner(
            final ItemDisplayEntityRenderState state,
            final PoseStack poseStack,
            final SubmitNodeCollector submitNodeCollector,
            final int lightCoords,
            final float interpolationProgress
        ) {
            if (!state.item.isEmpty()) {
                poseStack.mulPose(Axis.YP.rotation((float) Math.PI));
                state.item.submit(poseStack, submitNodeCollector, lightCoords, OverlayTexture.NO_OVERLAY, state.outlineColor);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    public static class TextDisplayRenderer extends DisplayRenderer<Display.TextDisplay, Display.TextDisplay.TextRenderState, TextDisplayEntityRenderState> {
        private static final float TEXT_BACKGROUND_OFFSET = -0.01F;
        private final Font font;

        protected TextDisplayRenderer(final EntityRendererProvider.Context context) {
            super(context);
            this.font = context.getFont();
        }

        public TextDisplayEntityRenderState createRenderState() {
            return new TextDisplayEntityRenderState();
        }

        public void extractRenderState(final Display.TextDisplay entity, final TextDisplayEntityRenderState state, final float partialTicks) {
            super.extractRenderState(entity, state, partialTicks);
            state.textRenderState = entity.textRenderState();
            state.cachedInfo = entity.cacheDisplay(this::splitLines);
        }

        private Display.TextDisplay.CachedInfo splitLines(final Component input, final int width) {
            List<FormattedCharSequence> lines = this.font.split(input, width);
            List<Display.TextDisplay.CachedLine> result = new ArrayList<>(lines.size());
            int maxLineWidth = 0;

            for (FormattedCharSequence line : lines) {
                int lineWidth = this.font.width(line);
                maxLineWidth = Math.max(maxLineWidth, lineWidth);
                result.add(new Display.TextDisplay.CachedLine(line, lineWidth));
            }

            return new Display.TextDisplay.CachedInfo(result, maxLineWidth);
        }

        public void submitInner(
            final TextDisplayEntityRenderState state,
            final PoseStack poseStack,
            final SubmitNodeCollector submitNodeCollector,
            final int lightCoords,
            final float interpolationProgress
        ) {
            Display.TextDisplay.TextRenderState renderState = state.textRenderState;
            byte flags = renderState.flags();
            boolean seeThrough = (flags & 2) != 0;
            boolean useDefaultBackground = (flags & 4) != 0;
            boolean shadow = (flags & 1) != 0;
            Display.TextDisplay.Align alignment = Display.TextDisplay.getAlign(flags);
            byte textOpacity = (byte)renderState.textOpacity().get(interpolationProgress);
            int backgroundColor;
            if (useDefaultBackground) {
                float backgroundAlpha = Minecraft.getInstance().gameRenderer.gameRenderState().optionsRenderState.getBackgroundOpacity(0.25F);
                backgroundColor = (int)(backgroundAlpha * 255.0F) << 24;
            } else {
                backgroundColor = renderState.backgroundColor().get(interpolationProgress);
            }

            float y = 0.0F;
            Matrix4f pose = poseStack.last().pose();
            pose.rotate((float) Math.PI, 0.0F, 1.0F, 0.0F);
            pose.scale(-0.025F, -0.025F, -0.025F);
            Display.TextDisplay.CachedInfo cachedInfo = state.cachedInfo;
            int lineSpacing = 1;
            int lineHeight = 9 + 1;
            int width = cachedInfo.width();
            int height = cachedInfo.lines().size() * lineHeight - 1;
            pose.translate(1.0F - width / 2.0F, -height, 0.0F);
            if (backgroundColor != 0) {
                submitNodeCollector.submitCustomGeometry(
                    poseStack, seeThrough ? RenderTypes.textBackgroundSeeThrough() : RenderTypes.textBackground(), (lambdaPose, buffer) -> {
                        buffer.addVertex(lambdaPose, -1.0F, -1.0F, -0.01F).setColor(backgroundColor).setLight(lightCoords);
                        buffer.addVertex(lambdaPose, -1.0F, height, -0.01F).setColor(backgroundColor).setLight(lightCoords);
                        buffer.addVertex(lambdaPose, width, height, -0.01F).setColor(backgroundColor).setLight(lightCoords);
                        buffer.addVertex(lambdaPose, width, -1.0F, -0.01F).setColor(backgroundColor).setLight(lightCoords);
                    }
                );
            }

            OrderedSubmitNodeCollector textCollector = submitNodeCollector.order(backgroundColor != 0 ? 1 : 0);

            for (Display.TextDisplay.CachedLine line : cachedInfo.lines()) {
                float offset = switch (alignment) {
                    case LEFT -> 0.0F;
                    case RIGHT -> width - line.width();
                    case CENTER -> width / 2.0F - line.width() / 2.0F;
                };
                textCollector.submitText(
                    poseStack,
                    offset,
                    y,
                    line.contents(),
                    shadow,
                    seeThrough ? Font.DisplayMode.SEE_THROUGH : Font.DisplayMode.POLYGON_OFFSET,
                    lightCoords,
                    textOpacity << 24 | 16777215,
                    0,
                    0
                );
                y += lineHeight;
            }
        }
    }
}