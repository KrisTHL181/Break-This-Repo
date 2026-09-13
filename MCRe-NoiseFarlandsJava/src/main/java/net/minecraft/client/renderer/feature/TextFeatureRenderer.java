package net.minecraft.client.renderer.feature;

import com.mojang.blaze3d.vertex.VertexConsumer;
import java.util.List;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.font.TextRenderable;
import net.minecraft.client.renderer.feature.submit.SubmitNode;
import net.minecraft.util.FormattedCharSequence;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;

@OnlyIn(Dist.CLIENT)
public class TextFeatureRenderer extends RenderTypeFeatureRenderer<TextFeatureRenderer.Submit> {
    public static final FeatureRendererType<TextFeatureRenderer.Submit> TYPE = FeatureRendererType.create("Text");

    @Override
    protected void buildGroup(final FeatureFrameContext context, final List<TextFeatureRenderer.Submit> submits) {
        Font font = context.font();
        TextFeatureRenderer.GlyphRenderer glyphRenderer = new TextFeatureRenderer.GlyphRenderer();

        for (TextFeatureRenderer.Submit submit : submits) {
            glyphRenderer.pose.set(submit.pose());
            glyphRenderer.lightCoords = submit.lightCoords();
            glyphRenderer.displayMode = submit.displayMode();
            if (submit.outlineColor() == 0) {
                Font.PreparedText text = font.prepareText(
                    submit.string(), submit.x(), submit.y(), submit.color(), submit.dropShadow(), false, submit.backgroundColor()
                );
                text.visit(glyphRenderer);
            } else {
                Font.PreparedText outline = font.prepare8xTextOutline(submit.string(), submit.x(), submit.y(), submit.outlineColor());
                Font.PreparedText text = font.prepareText(submit.string(), submit.x(), submit.y(), submit.color(), false, false, 0);
                glyphRenderer.displayMode = Font.DisplayMode.NORMAL;
                outline.visit(glyphRenderer);
                glyphRenderer.displayMode = Font.DisplayMode.POLYGON_OFFSET;
                text.visit(glyphRenderer);
            }
        }
    }

    @OnlyIn(Dist.CLIENT)
    private class GlyphRenderer implements Font.GlyphVisitor {
        private final Matrix4f pose = new Matrix4f();
        private int lightCoords = 15728880;
        private Font.DisplayMode displayMode = Font.DisplayMode.NORMAL;

        @Override
        public void acceptRenderable(final TextRenderable renderable) {
            VertexConsumer builder = TextFeatureRenderer.this.getVertexBuilder(renderable.renderType(this.displayMode));
            renderable.render(this.pose, builder, this.lightCoords, false);
        }
    }

    @OnlyIn(Dist.CLIENT)
    public record Submit(
        Matrix4fc pose,
        float x,
        float y,
        FormattedCharSequence string,
        boolean dropShadow,
        Font.DisplayMode displayMode,
        int lightCoords,
        int color,
        int backgroundColor,
        int outlineColor
    ) implements SubmitNode {
        @Override
        public FeatureRendererType<TextFeatureRenderer.Submit> featureType() {
            return TextFeatureRenderer.TYPE;
        }
    }
}