package net.minecraft.client.gui.font;

import com.mojang.blaze3d.pipeline.RenderPipeline;
import com.mojang.blaze3d.textures.GpuTextureView;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.gui.Font;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4fc;

@OnlyIn(Dist.CLIENT)
public interface TextRenderable {
    void render(Matrix4fc pose, VertexConsumer buffer, int packedLightCoords, boolean flat);

    RenderType renderType(Font.DisplayMode displayMode);

    GpuTextureView textureView();

    RenderPipeline guiPipeline();

    float left();

    float top();

    float right();

    float bottom();

    @OnlyIn(Dist.CLIENT)
    interface Styled extends TextRenderable, ActiveArea {
        @Override
        default float activeLeft() {
            return this.left();
        }

        @Override
        default float activeTop() {
            return this.top();
        }

        @Override
        default float activeRight() {
            return this.right();
        }

        @Override
        default float activeBottom() {
            return this.bottom();
        }
    }
}