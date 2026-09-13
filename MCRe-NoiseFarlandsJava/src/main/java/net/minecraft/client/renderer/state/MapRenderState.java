package net.minecraft.client.renderer.state;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class MapRenderState {
    public @Nullable Identifier texture;
    public final List<MapRenderState.MapDecorationRenderState> decorations = new ArrayList<>();

    @OnlyIn(Dist.CLIENT)
    public static class MapDecorationRenderState {
        public @Nullable TextureAtlasSprite atlasSprite;
        public byte x;
        public byte y;
        public byte rot;
        public boolean renderOnFrame;
        public @Nullable Component name;
    }
}