package net.minecraft.client.multiplayer.chat;

import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.components.ComponentRenderUtils;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MessageSignature;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public record GuiMessage(int addedTime, Component content, @Nullable MessageSignature signature, GuiMessageSource source, @Nullable GuiMessageTag tag) {
    private static final int MESSAGE_TAG_MARGIN_LEFT = 4;

    public List<FormattedCharSequence> splitLines(final Font font, int maxWidth) {
        if (this.tag != null && this.tag.icon() != null) {
            maxWidth -= this.tag.icon().width + 4 + 2;
        }

        return ComponentRenderUtils.wrapComponents(this.content, maxWidth, font);
    }

    @OnlyIn(Dist.CLIENT)
    public record Line(GuiMessage parent, FormattedCharSequence content, boolean endOfEntry) {
        public int getTagIconLeft(final Font font) {
            return font.width(this.content) + 4;
        }

        public @Nullable GuiMessageTag tag() {
            return this.parent.tag;
        }

        public int addedTime() {
            return this.parent.addedTime;
        }
    }
}