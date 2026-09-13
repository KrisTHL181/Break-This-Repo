package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.model.Model;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.SignRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.level.block.state.properties.WoodType;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class StandingSignSpecialRenderer implements NoDataSpecialModelRenderer {
   private final MaterialSet materials;
   private final Model.Simple model;
   private final Material material;

   public StandingSignSpecialRenderer(MaterialSet p_425998_, Model.Simple p_431018_, Material p_378123_) {
      this.materials = p_425998_;
      this.model = p_431018_;
      this.material = p_378123_;
   }

   @Override
   public void submit(
      ItemDisplayContext p_430424_, PoseStack p_429659_, SubmitNodeCollector p_424620_, int p_430942_, int p_429400_, boolean p_423124_, int p_431903_
   ) {
      SignRenderer.submitSpecial(this.materials, p_429659_, p_424620_, p_430942_, p_429400_, this.model, this.material);
   }

   @Override
   public void getExtents(Consumer<Vector3fc> p_457709_) {
      PoseStack posestack = new PoseStack();
      SignRenderer.applyInHandTransforms(posestack);
      this.model.root().getExtentsForGui(posestack, p_457709_);
   }

   @OnlyIn(Dist.CLIENT)
   public record Unbaked(WoodType woodType, Optional<Identifier> texture) implements SpecialModelRenderer.Unbaked {
      public static final MapCodec<StandingSignSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_448373_ -> p_448373_.group(
               WoodType.CODEC.fieldOf("wood_type").forGetter(StandingSignSpecialRenderer.Unbaked::woodType),
               Identifier.CODEC.optionalFieldOf("texture").forGetter(StandingSignSpecialRenderer.Unbaked::texture)
            )
            .apply(p_448373_, StandingSignSpecialRenderer.Unbaked::new)
      );

      public Unbaked(WoodType p_376460_) {
         this(p_376460_, Optional.empty());
      }

      @Override
      public MapCodec<StandingSignSpecialRenderer.Unbaked> type() {
         return MAP_CODEC;
      }

      @Override
      public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext p_428993_) {
         Model.Simple model$simple = SignRenderer.createSignModel(p_428993_.entityModelSet(), this.woodType, true);
         Material material = this.texture.map(Sheets.SIGN_MAPPER::apply).orElseGet(() -> Sheets.getSignMaterial(this.woodType));
         return new StandingSignSpecialRenderer(p_428993_.materials(), model$simple, material);
      }
   }
}
