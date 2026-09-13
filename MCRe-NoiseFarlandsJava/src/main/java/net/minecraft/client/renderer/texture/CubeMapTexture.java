package net.minecraft.client.renderer.texture;

import com.mojang.blaze3d.GpuFormat;
import com.mojang.blaze3d.buffers.GpuBufferSlice;
import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.systems.GpuDevice;
import com.mojang.blaze3d.systems.RenderSystem;
import java.io.IOException;
import net.minecraft.client.resources.metadata.texture.TextureMetadataSection;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.resources.ResourceManager;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class CubeMapTexture extends ReloadableTexture {
    private static final String[] SUFFIXES = new String[]{"_1.png", "_3.png", "_5.png", "_4.png", "_0.png", "_2.png"};

    public CubeMapTexture(final Identifier resourceId) {
        super(resourceId);
    }

    @Override
    public TextureContents loadContents(final ResourceManager resourceManager) throws IOException {
        Identifier location = this.resourceId();

        try (TextureContents first = TextureContents.load(resourceManager, location.withSuffix(SUFFIXES[0]))) {
            int width = first.image().getWidth();
            int height = first.image().getHeight();
            NativeImage stackedImage = new NativeImage(width, height * 6, false);
            first.image().copyRect(stackedImage, 0, 0, 0, 0, width, height, false, true);

            for (int i = 1; i < 6; i++) {
                try (TextureContents part = TextureContents.load(resourceManager, location.withSuffix(SUFFIXES[i]))) {
                    if (part.image().getWidth() != width || part.image().getHeight() != height) {
                        throw new IOException(
                            "Image dimensions of cubemap '"
                                + location
                                + "' sides do not match: part 0 is "
                                + width
                                + "x"
                                + height
                                + ", but part "
                                + i
                                + " is "
                                + part.image().getWidth()
                                + "x"
                                + part.image().getHeight()
                        );
                    }

                    part.image().copyRect(stackedImage, 0, 0, 0, i * height, width, height, false, true);
                }
            }

            return new TextureContents(stackedImage, new TextureMetadataSection(true, false, MipmapStrategy.MEAN, 0.0F));
        }
    }

    @Override
    protected void doLoad(final NativeImage image) {
        GpuDevice device = RenderSystem.getDevice();
        int width = image.getWidth();
        int height = image.getHeight() / 6;
        this.close();
        this.texture = device.createTexture(this.resourceId()::toString, 21, GpuFormat.RGBA8_UNORM, width, height, 6, 1);
        this.textureView = device.createTextureView(this.texture);
        GpuBufferSlice stagingBuffer = device.createCommandEncoder().transientMemory().uploadStaging(image.getPixelBytes(), 1L, 16);

        for (int i = 0; i < 6; i++) {
            device.createCommandEncoder()
                .copyBufferToTexture(stagingBuffer, 0, height * i, image.getWidth(), image.getHeight(), this.texture, 0, 0, width, height, 0, i);
        }
    }
}