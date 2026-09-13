package net.minecraft.client.gui.components;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.PlayerSkinRenderCache;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.item.component.ResolvableProfile;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class PlayerFaceExtractor {
    public static final int SKIN_HEAD_U = 8;
    public static final int SKIN_HEAD_V = 8;
    public static final int SKIN_HEAD_WIDTH = 8;
    public static final int SKIN_HEAD_HEIGHT = 8;
    public static final int SKIN_HAT_U = 40;
    public static final int SKIN_HAT_V = 8;
    public static final int SKIN_HAT_WIDTH = 8;
    public static final int SKIN_HAT_HEIGHT = 8;
    public static final int SKIN_TEX_WIDTH = 64;
    public static final int SKIN_TEX_HEIGHT = 64;

    public static void extractRenderState(final GuiGraphicsExtractor graphics, final ResolvableProfile skinProfile, final int x, final int y, final int size) {
        PlayerSkinRenderCache skinCache = Minecraft.getInstance().playerSkinRenderCache();
        PlayerSkinRenderCache.RenderInfo renderInfo = skinCache.getOrDefault(skinProfile);
        extractRenderState(graphics, renderInfo.playerSkin(), x, y, size);
    }

    public static void extractRenderState(final GuiGraphicsExtractor graphics, final PlayerSkin skin, final int x, final int y, final int size) {
        extractRenderState(graphics, skin, x, y, size, -1);
    }

    public static void extractRenderState(final GuiGraphicsExtractor graphics, final PlayerSkin skin, final int x, final int y, final int size, final int color) {
        extractRenderState(graphics, skin.body().texturePath(), x, y, size, true, false, color);
    }

    public static void extractRenderState(
        final GuiGraphicsExtractor graphics,
        final Identifier texture,
        final int x,
        final int y,
        final int size,
        final boolean hat,
        final boolean flip,
        final int color
    ) {
        int skinHeadV = 8 + (flip ? 8 : 0);
        int skinHeadHeight = 8 * (flip ? -1 : 1);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 8.0F, skinHeadV, size, size, 8, skinHeadHeight, 64, 64, color);
        if (hat) {
            extractHat(graphics, texture, x, y, size, flip, color);
        }
    }

    private static void extractHat(
        final GuiGraphicsExtractor graphics, final Identifier texture, final int x, final int y, final int size, final boolean flip, final int color
    ) {
        int skinHatV = 8 + (flip ? 8 : 0);
        int skinHatHeight = 8 * (flip ? -1 : 1);
        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x, y, 40.0F, skinHatV, size, size, 8, skinHatHeight, 64, 64, color);
    }
}