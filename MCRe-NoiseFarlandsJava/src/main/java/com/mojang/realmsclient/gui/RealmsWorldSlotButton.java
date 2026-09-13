package com.mojang.realmsclient.gui;

import com.mojang.realmsclient.RealmsMainScreen;
import com.mojang.realmsclient.dto.RealmsServer;
import com.mojang.realmsclient.dto.RealmsSlot;
import com.mojang.realmsclient.gui.screens.configuration.RealmsConfigureWorldScreen;
import com.mojang.realmsclient.util.RealmsTextureManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.StringWidget;
import net.minecraft.client.gui.components.Tooltip;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class RealmsWorldSlotButton extends Button {
    private static final Identifier SLOT_FRAME_SPRITE = Identifier.withDefaultNamespace("widget/slot_frame");
    public static final Identifier EMPTY_SLOT_LOCATION = Identifier.withDefaultNamespace("textures/gui/realms/empty_frame.png");
    public static final Identifier DEFAULT_WORLD_SLOT_1 = Identifier.withDefaultNamespace("textures/gui/title/background/panorama_0.png");
    public static final Identifier DEFAULT_WORLD_SLOT_2 = Identifier.withDefaultNamespace("textures/gui/title/background/panorama_2.png");
    public static final Identifier DEFAULT_WORLD_SLOT_3 = Identifier.withDefaultNamespace("textures/gui/title/background/panorama_3.png");
    private static final Component SWITCH_TO_MINIGAME_SLOT_TOOLTIP = Component.translatable("mco.configure.world.slot.tooltip.minigame");
    private static final Component SWITCH_TO_WORLD_SLOT_TOOLTIP = Component.translatable("mco.configure.world.slot.tooltip");
    private static final Component MINIGAME = Component.translatable("mco.worldSlot.minigame");
    private final RealmsConfigureWorldScreen configureWorldScreen;
    private final int slotIndex;
    private final StringWidget slotNameWidget;
    private RealmsWorldSlotButton.State state;

    public RealmsWorldSlotButton(
        final RealmsConfigureWorldScreen configureWorldScreen,
        final int x,
        final int y,
        final int width,
        final int height,
        final int slotIndex,
        final RealmsServer serverData,
        final Button.OnPress onPress
    ) {
        super(x, y, width, height, CommonComponents.EMPTY, onPress, DEFAULT_NARRATION);
        this.configureWorldScreen = configureWorldScreen;
        this.slotIndex = slotIndex;
        this.state = this.setServerData(serverData);
        this.slotNameWidget = new StringWidget(Component.literal(this.state.slotName), Minecraft.getInstance().font);
    }

    public RealmsWorldSlotButton.State getState() {
        return this.state;
    }

    public RealmsWorldSlotButton.State setServerData(final RealmsServer serverData) {
        this.state = new RealmsWorldSlotButton.State(serverData, this.slotIndex);
        this.setTooltipAndNarration(this.state, serverData.minigameName);
        return this.state;
    }

    private void setTooltipAndNarration(final RealmsWorldSlotButton.State state, final @Nullable String minigameName) {
        Component tooltipComponent = switch (state.action) {
            case SWITCH_SLOT -> state.minigame ? SWITCH_TO_MINIGAME_SLOT_TOOLTIP : SWITCH_TO_WORLD_SLOT_TOOLTIP;
            default -> null;
        };
        if (tooltipComponent != null) {
            this.setTooltip(Tooltip.create(tooltipComponent));
        }

        MutableComponent slotContents = Component.literal(state.slotName);
        if (state.minigame && minigameName != null) {
            slotContents = slotContents.append(CommonComponents.SPACE).append(minigameName);
        }

        this.setMessage(slotContents);
    }

    private static RealmsWorldSlotButton.Action getAction(final boolean activeSlot, final boolean empty, final boolean expired) {
        return activeSlot || empty && expired ? RealmsWorldSlotButton.Action.NOTHING : RealmsWorldSlotButton.Action.SWITCH_SLOT;
    }

    @Override
    public boolean isActive() {
        return this.state.action != RealmsWorldSlotButton.Action.NOTHING && super.isActive();
    }

    @Override
    public void extractContents(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY, final float a) {
        int x = this.getX();
        int y = this.getY();
        boolean hoveredOrFocused = this.isHoveredOrFocused();
        Identifier texture;
        if (this.state.minigame) {
            texture = RealmsTextureManager.worldTemplate(String.valueOf(this.state.imageId), this.state.image);
        } else if (this.state.empty) {
            texture = EMPTY_SLOT_LOCATION;
        } else if (this.state.image != null && this.state.imageId != -1L) {
            texture = RealmsTextureManager.worldTemplate(String.valueOf(this.state.imageId), this.state.image);
        } else if (this.slotIndex == 1) {
            texture = DEFAULT_WORLD_SLOT_1;
        } else if (this.slotIndex == 2) {
            texture = DEFAULT_WORLD_SLOT_2;
        } else if (this.slotIndex == 3) {
            texture = DEFAULT_WORLD_SLOT_3;
        } else {
            texture = EMPTY_SLOT_LOCATION;
        }

        int color = -1;
        if (!this.state.activeSlot) {
            color = ARGB.colorFromFloat(1.0F, 0.56F, 0.56F, 0.56F);
        }

        graphics.blit(RenderPipelines.GUI_TEXTURED, texture, x + 1, y + 1, 0.0F, 0.0F, this.width - 2, this.height - 2, 74, 74, 74, 74, color);
        if (hoveredOrFocused && this.state.action != RealmsWorldSlotButton.Action.NOTHING) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_FRAME_SPRITE, x, y, this.width, this.height);
        } else if (this.state.activeSlot) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_FRAME_SPRITE, x, y, this.width, this.height, ARGB.colorFromFloat(1.0F, 0.8F, 0.8F, 0.8F));
        } else {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, SLOT_FRAME_SPRITE, x, y, this.width, this.height, ARGB.colorFromFloat(1.0F, 0.56F, 0.56F, 0.56F));
        }

        if (this.state.hardcore) {
            graphics.blitSprite(RenderPipelines.GUI_TEXTURED, RealmsMainScreen.HARDCORE_MODE_SPRITE, x + 3, y + 4, 9, 8);
        }

        this.slotNameWidget.setMaxWidth(this.getWidth() - (this.state.activeSlot ? 2 : 0), StringWidget.TextOverflow.SCROLLING);
        this.slotNameWidget.setPosition(this.getX() + this.getWidth() / 2 - this.slotNameWidget.getWidth() / 2, y + this.height - 14);
        this.slotNameWidget.extractRenderState(graphics, mouseX, mouseY, a);
        if (this.state.activeSlot) {
            graphics.centeredText(
                Minecraft.getInstance().font,
                RealmsMainScreen.getVersionComponent(this.state.slotVersion, this.state.compatibility.isCompatible()),
                x + this.width / 2,
                y + this.height + 2,
                -1
            );
        }
    }

    @Override
    protected void extractTooltipForNextRenderPass(final GuiGraphicsExtractor graphics, final int mouseX, final int mouseY) {
        if (Minecraft.getInstance().gui.screen() == this.configureWorldScreen) {
            super.extractTooltipForNextRenderPass(graphics, mouseX, mouseY);
        }
    }

    public void updateSlotState(final RealmsServer serverData) {
        this.state = this.setServerData(serverData);
        this.slotNameWidget.setMessage(Component.literal(this.state.slotName));
    }

    @OnlyIn(Dist.CLIENT)
    public enum Action {
        NOTHING,
        SWITCH_SLOT;
    }

    @OnlyIn(Dist.CLIENT)
    public static class State {
        private final String slotName;
        private final String slotVersion;
        private final RealmsServer.Compatibility compatibility;
        private final long imageId;
        private final @Nullable String image;
        public final boolean empty;
        public final boolean minigame;
        public final RealmsWorldSlotButton.Action action;
        public final boolean hardcore;
        public final boolean activeSlot;

        public State(final RealmsServer serverData, final int slotIndex) {
            this.minigame = slotIndex == 4;
            if (this.minigame) {
                this.slotName = RealmsWorldSlotButton.MINIGAME.getString();
                this.imageId = serverData.minigameId;
                this.image = serverData.minigameImage;
                this.empty = serverData.minigameId == -1;
                this.slotVersion = "";
                this.compatibility = RealmsServer.Compatibility.UNVERIFIABLE;
                this.hardcore = false;
                this.activeSlot = serverData.isMinigameActive();
            } else {
                RealmsSlot slot = serverData.slots.get(slotIndex);
                this.slotName = slot.options.getSlotName(slotIndex);
                this.imageId = slot.options.templateId;
                this.image = slot.options.templateImage;
                this.empty = slot.options.empty;
                this.slotVersion = slot.options.version;
                this.compatibility = slot.options.compatibility;
                this.hardcore = slot.isHardcore();
                this.activeSlot = serverData.activeSlot == slotIndex && !serverData.isMinigameActive();
            }

            this.action = RealmsWorldSlotButton.getAction(this.activeSlot, this.empty, serverData.expired);
        }
    }
}