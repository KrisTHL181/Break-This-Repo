package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.HangingSignRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class HangingSignSpecialRenderer implements NoDataSpecialModelRenderer {
   private final MaterialSet materials;
   private final Model.Simple model;
   private final Material material;

   public HangingSignSpecialRenderer(MaterialSet p_428711_, Model.Simple p_426863_, Material p_378581_) {
      this.materials = p_428711_;
      this.model = p_426863_;
      this.material = p_378581_;
   }

   @Override
   public void submit(
      ItemDisplayContext p_431249_, PoseStack p_427874_, SubmitNodeCollector p_424201_, int p_424535_, int p_430218_, boolean p_428642_, int p_431886_
   ) {
      HangingSignRenderer.submitSpecial(this.materials, p_427874_, p_424201_, p_424535_, p_430218_, this.model, this.material);
   }

   @Override
   public void getExtents(Consumer<Vector3fc> p_455122_) {
      PoseStack posestack = new PoseStack();
      HangingSignRenderer.translateBase(posestack, 0.0F);
      posestack.scale(1.0F, -1.0F, -1.0F);
      this.model.root().getExtentsForGui(posestack, p_455122_);
   }

   @OnlyIn(Dist.CLIENT)
   public record Unbaked(WoodType woodType, Optional<Identifier> texture) implements SpecialModelRenderer.Unbaked {
      public static final MapCodec<HangingSignSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_448369_ -> p_448369_.group(
               WoodType.CODEC.fieldOf("wood_type").forGetter(HangingSignSpecialRenderer.Unbaked::woodType),
               Identifier.CODEC.optionalFieldOf("texture").forGetter(HangingSignSpecialRenderer.Unbaked::texture)
            )
            .apply(p_448369_, HangingSignSpecialRenderer.Unbaked::new)
      );

      public Unbaked(WoodType p_375515_) {
         this(p_375515_, Optional.empty());
      }

      @Override
      public MapCodec<HangingSignSpecialRenderer.Unbaked> type() {
         return MAP_CODEC;
      }

      @Override
      public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext p_427627_) {
         Model.Simple model$simple = HangingSignRenderer.createSignModel(
            p_427627_.entityModelSet(), this.woodType, HangingSignRenderer.AttachmentType.CEILING_MIDDLE
         );
         Material material = this.texture.map(Sheets.HANGING_SIGN_MAPPER::apply).orElseGet(() -> Sheets.getHangingSignMaterial(this.woodType));
         return new HangingSignSpecialRenderer(p_427627_.materials(), model$simple, material);
      }
   }
}
