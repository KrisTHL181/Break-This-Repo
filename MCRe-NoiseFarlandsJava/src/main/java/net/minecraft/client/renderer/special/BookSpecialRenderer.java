package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.book.BookModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.EnchantTableRenderer;
import net.minecraft.client.resources.model.sprite.SpriteGetter;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class BookSpecialRenderer implements NoDataSpecialModelRenderer {
    private final SpriteGetter sprites;
    private final BookModel model;
    private final BookModel.State state;

    public BookSpecialRenderer(final SpriteGetter sprites, final BookModel model, final BookModel.State state) {
        this.sprites = sprites;
        this.model = model;
        this.state = state;
    }

    @Override
    public void submit(
        final PoseStack poseStack,
        final SubmitNodeCollector submitNodeCollector,
        final int lightCoords,
        final int overlayCoords,
        final boolean hasFoil,
        final int outlineColor
    ) {
        submitNodeCollector.submitModel(
            this.model, this.state, poseStack, lightCoords, overlayCoords, -1, EnchantTableRenderer.BOOK_TEXTURE, this.sprites, outlineColor, null
        );
    }

    @Override
    public void getExtents(final Consumer<Vector3fc> output) {
        PoseStack poseStack = new PoseStack();
        this.model.setupAnim(this.state);
        this.model.root().getExtentsForGui(poseStack, output);
    }

    @OnlyIn(Dist.CLIENT)
    public record Unbaked(float openAngle, float page1, float page2) implements NoDataSpecialModelRenderer.Unbaked {
        public static final MapCodec<BookSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
            i -> i.group(
                    Codec.FLOAT.fieldOf("open_angle").forGetter(BookSpecialRenderer.Unbaked::openAngle),
                    Codec.FLOAT.fieldOf("page1").forGetter(BookSpecialRenderer.Unbaked::page1),
                    Codec.FLOAT.fieldOf("page2").forGetter(BookSpecialRenderer.Unbaked::page2)
                )
                .apply(i, BookSpecialRenderer.Unbaked::new)
        );

        @Override
        public MapCodec<BookSpecialRenderer.Unbaked> type() {
            return MAP_CODEC;
        }

        public BookSpecialRenderer bake(final SpecialModelRenderer.BakingContext context) {
            return new BookSpecialRenderer(
                context.sprites(),
                new BookModel(context.entityModelSet().bakeLayer(ModelLayers.BOOK)),
                new BookModel.State(this.openAngle * (float) (Math.PI / 180.0), this.page1, this.page2)
            );
        }
    }
}