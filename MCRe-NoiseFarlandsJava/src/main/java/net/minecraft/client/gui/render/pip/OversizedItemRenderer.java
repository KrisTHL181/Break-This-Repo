package net.minecraft.client.gui.render.pip;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.vertex.PoseStack;
import java.util.Objects;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.TrackingItemStackRenderState;
import net.minecraft.client.renderer.state.gui.GuiItemRenderState;
import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.gui.pip.OversizedItemRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class OversizedItemRenderer extends PictureInPictureRenderer<OversizedItemRenderState> {
    private boolean usedOnThisFrame;
    private @Nullable Object modelOnTextureIdentity;

    public boolean usedOnThisFrame() {
        return this.usedOnThisFrame;
    }

    public void resetUsedOnThisFrame() {
        this.usedOnThisFrame = false;
    }

    public void invalidateTexture() {
        this.modelOnTextureIdentity = null;
    }

    @Override
    public Class<OversizedItemRenderState> getRenderStateClass() {
        return OversizedItemRenderState.class;
    }

    protected void renderToTexture(final OversizedItemRenderState renderState, final PoseStack poseStack, final SubmitNodeCollector submitNodeCollector) {
        poseStack.scale(1.0F, -1.0F, -1.0F);
        GuiItemRenderState guiItemRenderState = renderState.guiItemRenderState();
        ScreenRectangle itemBounds = guiItemRenderState.oversizedItemBounds();
        Objects.requireNonNull(itemBounds);
        float itemBoundsCenterX = (itemBounds.left() + itemBounds.right()) / 2.0F;
        float itemBoundsCenterY = (itemBounds.top() + itemBounds.bottom()) / 2.0F;
        float slotCenterX = guiItemRenderState.x() + 8.0F;
        float slotCenterY = guiItemRenderState.y() + 8.0F;
        poseStack.translate((slotCenterX - itemBoundsCenterX) / 16.0F, (itemBoundsCenterY - slotCenterY) / 16.0F, 0.0F);
        TrackingItemStackRenderState itemStackRenderState = guiItemRenderState.itemStackRenderState();
        boolean flat = !itemStackRenderState.usesBlockLight();
        if (flat) {
            Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_FLAT);
        } else {
            Minecraft.getInstance().gameRenderer.lighting().setupFor(Lighting.Entry.ITEMS_3D);
        }

        itemStackRenderState.submit(poseStack, submitNodeCollector, 15728880, OverlayTexture.NO_OVERLAY, 0);
        this.modelOnTextureIdentity = itemStackRenderState.getModelIdentity();
    }

    public void blitTexture(final OversizedItemRenderState renderState, final GuiRenderState guiRenderState) {
        super.blitTexture(renderState, guiRenderState);
        this.usedOnThisFrame = true;
    }

    public boolean textureIsReadyToBlit(final OversizedItemRenderState renderState) {
        TrackingItemStackRenderState itemStackRenderState = renderState.guiItemRenderState().itemStackRenderState();
        return !itemStackRenderState.isAnimated() && itemStackRenderState.getModelIdentity().equals(this.modelOnTextureIdentity);
    }

    @Override
    protected float getTranslateY(final int height, final int guiScale) {
        return height / 2.0F;
    }

    @Override
    protected String getTextureLabel() {
        return "oversized_item";
    }
}