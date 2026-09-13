package net.minecraft.client.renderer.state;

import net.minecraft.client.renderer.state.gui.GuiRenderState;
import net.minecraft.client.renderer.state.level.LevelRenderState;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GameRenderState {
    public final LevelRenderState levelRenderState = new LevelRenderState();
    public final LightmapRenderState lightmapRenderState = new LightmapRenderState();
    public final GuiRenderState guiRenderState = new GuiRenderState();
    public final OptionsRenderState optionsRenderState = new OptionsRenderState();
    public final WindowRenderState windowRenderState = new WindowRenderState();
    public int framerateLimit;

    public boolean useShaderTransparency() {
        return !this.levelRenderState.cameraRenderState.isPanoramicMode && this.optionsRenderState.improvedTransparency;
    }
}