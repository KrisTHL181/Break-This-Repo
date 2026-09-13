package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.model.object.skull.SkullModelBase;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SkullBlockRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class SkullSpecialRenderer implements NoDataSpecialModelRenderer {
   private final SkullModelBase model;
   private final float animation;
   private final RenderType renderType;

   public SkullSpecialRenderer(SkullModelBase p_456592_, float p_377202_, RenderType p_454638_) {
      this.model = p_456592_;
      this.animation = p_377202_;
      this.renderType = p_454638_;
   }

   @Override
   public void submit(
      ItemDisplayContext p_426011_, PoseStack p_426457_, SubmitNodeCollector p_425711_, int p_424696_, int p_426768_, boolean p_431503_, int p_431880_
   ) {
      SkullBlockRenderer.submitSkull(null, 180.0F, this.animation, p_426457_, p_425711_, p_424696_, this.model, this.renderType, p_431880_, null);
   }

   @Override
   public void getExtents(Consumer<Vector3fc> p_452230_) {
      PoseStack posestack = new PoseStack();
      posestack.translate(0.5F, 0.0F, 0.5F);
      posestack.scale(-1.0F, -1.0F, 1.0F);
      SkullModelBase.State skullmodelbase$state = new SkullModelBase.State();
      skullmodelbase$state.animationPos = this.animation;
      skullmodelbase$state.yRot = 180.0F;
      this.model.setupAnim(skullmodelbase$state);
      this.model.root().getExtentsForGui(posestack, p_452230_);
   }

   @OnlyIn(Dist.CLIENT)
   public record Unbaked(SkullBlock.Type kind, Optional<Identifier> textureOverride, float animation) implements SpecialModelRenderer.Unbaked {
      public static final MapCodec<SkullSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_448371_ -> p_448371_.group(
               SkullBlock.Type.CODEC.fieldOf("kind").forGetter(SkullSpecialRenderer.Unbaked::kind),
               Identifier.CODEC.optionalFieldOf("texture").forGetter(SkullSpecialRenderer.Unbaked::textureOverride),
               Codec.FLOAT.optionalFieldOf("animation", 0.0F).forGetter(SkullSpecialRenderer.Unbaked::animation)
            )
            .apply(p_448371_, SkullSpecialRenderer.Unbaked::new)
      );

      public Unbaked(SkullBlock.Type p_376549_) {
         this(p_376549_, Optional.empty(), 0.0F);
      }

      @Override
      public MapCodec<SkullSpecialRenderer.Unbaked> type() {
         return MAP_CODEC;
      }

      @Override
      public @Nullable SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext p_423590_) {
         SkullModelBase skullmodelbase = SkullBlockRenderer.createModel(p_423590_.entityModelSet(), this.kind);
         Identifier identifier = this.textureOverride
            .<Identifier>map(p_448372_ -> p_448372_.withPath(p_377715_ -> "textures/entity/" + p_377715_ + ".png"))
            .orElse(null);
         if (skullmodelbase == null) {
            return null;
         }

         RenderType rendertype = SkullBlockRenderer.getSkullRenderType(this.kind, identifier);
         return new SkullSpecialRenderer(skullmodelbase, this.animation, rendertype);
      }
   }
}
