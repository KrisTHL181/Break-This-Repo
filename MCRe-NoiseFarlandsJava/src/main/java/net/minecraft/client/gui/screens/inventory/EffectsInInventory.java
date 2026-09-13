package net.minecraft.client.gui.screens.inventory;

import com.google.common.collect.Ordering;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.Hud;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffectUtil;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class EffectsInInventory {
    private static final Identifier EFFECT_BACKGROUND_SPRITE = Identifier.withDefaultNamespace("container/inventory/effect_background");
    private static final Identifier EFFECT_BACKGROUND_AMBIENT_SPRITE = Identifier.withDefaultNamespace("container/inventory/effect_background_ambient");
    private static final int ICON_SIZE = 18;
    public static final int SPACING = 7;
    private static final int TEXT_X_OFFSET = 32;
    public static final int SPRITE_SQUARE_SIZE = 32;
    private final AbstractContainerScreen<?> screen;
    private final Minecraft minecraft;

    public EffectsInInventory(final AbstractContainerScreen<?> screen) {
        this.screen = screen;
        this.minecraft = Minecraft.getInstance();
    }

    public boolean canSeeEffects() {
        int xo = this.screen.leftPos + this.screen.imageWidth + 2;
        int availableWidth = this.screen.width - xo;
        return availableWidth >= 32;
    }

    public void extractRenderState(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        int xo = this.screen.leftPos + this.screen.imageWidth + 2;
        int availableWidth = this.screen.width - xo;
        Collection<MobEffectInstance> activeEffects = this.minecraft.player.getActiveEffects();
        if (!activeEffects.isEmpty() && availableWidth >= 32) {
            int maxWidth = availableWidth >= 120 ? availableWidth - 7 : 32;
            int yStep = 33;
            if (activeEffects.size() > 5) {
                yStep = 132 / (activeEffects.size() - 1);
            }

            this.extractEffects(graphics, activeEffects, xo, yStep, mouseX, mouseY, maxWidth);
        }
    }

    private void extractEffects(
        final GuiGraphicsExtractor graphics,
        final Collection<MobEffectInstance> activeEffects,
        final int x0,
        final int yStep,
        final int mouseX,
        final int mouseY,
        final int maxWidth
    ) {
        Iterable<MobEffectInstance> sortedEffects = Ordering.natural().sortedCopy(activeEffects);
        int y0 = this.screen.topPos;
        Font font = this.screen.getFont();

        for (MobEffectInstance effect : sortedEffects) {
            boolean isAmbient = effect.isAmbient();
            Component effectText = this.getEffectName(effect);
            Component duration = MobEffectUtil.formatDuration(effect, 1.0F, this.minecraft.level.tickRateManager().tickrate());
            int textureWidth = this.extractBackground(graphics, font, effectText, duration, x0, y0, isAmbient, maxWidth);
            this.extractText(graphics, effectText, duration, font, x0, y0, textureWidth, yStep, mouseX, mouseY);
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, Hud.getMobEffectSprite(effect.getEffect()), x0 + 7, y0 + 7, 18, 18);
            y0 += yStep;
        }
    }

    private int extractBackground(
        final GuiGraphicsExtractor graphics,
        final Font font,
        final Component effectName,
        final Component duration,
        final int x0,
        final int y0,
        final boolean isAmbient,
        final int maxTextureWidth
    ) {
        int nameWidth = 32 + font.width(effectName) + 7;
        int durationWidth = 32 + font.width(duration) + 7;
        int textureWidth = Math.min(maxTextureWidth, Math.max(nameWidth, durationWidth));
        graphics.blitSprite(RenderPipelines.GUI_TEXTURED, isAmbient ? EFFECT_BACKGROUND_AMBIENT_SPRITE : EFFECT_BACKGROUND_SPRITE, x0, y0, textureWidth, 32);
        return textureWidth;
    }

    private void extractText(
        final GuiGraphicsExtractor graphics,
        final Component effectText,
        final Component duration,
        final Font font,
        final int x0,
        final int y0,
        final int textureWidth,
        final int yStep,
        final int mouseX,
        final int mouseY
    ) {
        int textX = x0 + 32;
        int textY = y0 + 7;
        int maxTextWidth = textureWidth - 32 - 7;
        boolean isCompact;
        if (maxTextWidth > 0) {
            boolean shouldClip = font.width(effectText) > maxTextWidth;
            FormattedCharSequence clippedText = shouldClip ? ComponentRenderUtils.clipText(effectText, font, maxTextWidth) : effectText.getVisualOrderText();
            graphics.text(font, clippedText, textX, textY, -1);
            graphics.text(font, duration, textX, textY + 9, -8355712);
            isCompact = shouldClip;
        } else {
            isCompact = true;
        }

        if (isCompact && mouseX >= x0 && mouseX <= x0 + textureWidth && mouseY >= y0 && mouseY <= y0 + yStep) {
            graphics.setTooltipForNextFrame(this.screen.getFont(), List.of(effectText, duration), Optional.empty(), mouseX, mouseY);
        }
    }

    private Component getEffectName(final MobEffectInstance effect) {
        MutableComponent name = effect.getEffect().value().getDisplayName().copy();
        if (effect.getAmplifier() >= 1 && effect.getAmplifier() <= 9) {
            name.append(CommonComponents.SPACE).append(Component.translatable("enchantment.level." + (effect.getAmplifier() + 1)));
        }

        return name;
    }
}