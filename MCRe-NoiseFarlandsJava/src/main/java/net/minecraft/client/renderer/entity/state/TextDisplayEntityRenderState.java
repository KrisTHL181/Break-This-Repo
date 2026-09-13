package net.minecraft.client.renderer.entity.state;

import net.minecraft.world.entity.Display;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class TextDisplayEntityRenderState extends DisplayEntityRenderState {
    public Display.TextDisplay.@Nullable TextRenderState textRenderState;
    public Display.TextDisplay.@Nullable CachedInfo cachedInfo;

    @Override
    public boolean hasSubState() {
        return this.textRenderState != null;
    }
}