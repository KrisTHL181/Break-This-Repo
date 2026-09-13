package net.minecraft.client.renderer.texture;

import java.io.IOException;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class SimpleTexture extends ReloadableTexture {
    public SimpleTexture(final Identifier location) {
        super(location);
    }

    @Override
    public TextureContents loadContents(final ResourceManager resourceManager) throws IOException {
        return TextureContents.load(resourceManager, this.resourceId());
    }
}