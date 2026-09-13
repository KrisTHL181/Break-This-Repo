package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.object.chest.ChestModel;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.resources.model.Material;
import net.minecraft.client.resources.model.MaterialSet;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class ChestSpecialRenderer implements NoDataSpecialModelRenderer {
   public static final Identifier GIFT_CHEST_TEXTURE = Identifier.withDefaultNamespace("christmas");
   public static final Identifier NORMAL_CHEST_TEXTURE = Identifier.withDefaultNamespace("normal");
   public static final Identifier TRAPPED_CHEST_TEXTURE = Identifier.withDefaultNamespace("trapped");
   public static final Identifier ENDER_CHEST_TEXTURE = Identifier.withDefaultNamespace("ender");
   public static final Identifier COPPER_CHEST_TEXTURE = Identifier.withDefaultNamespace("copper");
   public static final Identifier EXPOSED_COPPER_CHEST_TEXTURE = Identifier.withDefaultNamespace("copper_exposed");
   public static final Identifier WEATHERED_COPPER_CHEST_TEXTURE = Identifier.withDefaultNamespace("copper_weathered");
   public static final Identifier OXIDIZED_COPPER_CHEST_TEXTURE = Identifier.withDefaultNamespace("copper_oxidized");
   private final MaterialSet materials;
   private final ChestModel model;
   private final Material material;
   private final float openness;

   public ChestSpecialRenderer(MaterialSet p_423388_, ChestModel p_458513_, Material p_377410_, float p_378366_) {
      this.materials = p_423388_;
      this.model = p_458513_;
      this.material = p_377410_;
      this.openness = p_378366_;
   }

   @Override
   public void submit(
      ItemDisplayContext p_430007_, PoseStack p_423292_, SubmitNodeCollector p_422433_, int p_428808_, int p_429897_, boolean p_422441_, int p_431900_
   ) {
      p_422433_.submitModel(
         this.model,
         this.openness,
         p_423292_,
         this.material.renderType(RenderTypes::entitySolid),
         p_428808_,
         p_429897_,
         -1,
         this.materials.get(this.material),
         p_431900_,
         null
      );
   }

   @Override
   public void getExtents(Consumer<Vector3fc> p_459464_) {
      PoseStack posestack = new PoseStack();
      this.model.setupAnim(this.openness);
      this.model.root().getExtentsForGui(posestack, p_459464_);
   }

   @OnlyIn(Dist.CLIENT)
   public record Unbaked(Identifier texture, float openness) implements SpecialModelRenderer.Unbaked {
      public static final MapCodec<ChestSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_448367_ -> p_448367_.group(
               Identifier.CODEC.fieldOf("texture").forGetter(ChestSpecialRenderer.Unbaked::texture),
               Codec.FLOAT.optionalFieldOf("openness", 0.0F).forGetter(ChestSpecialRenderer.Unbaked::openness)
            )
            .apply(p_448367_, ChestSpecialRenderer.Unbaked::new)
      );

      public Unbaked(Identifier p_453972_) {
         this(p_453972_, 0.0F);
      }

      @Override
      public MapCodec<ChestSpecialRenderer.Unbaked> type() {
         return MAP_CODEC;
      }

      @Override
      public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext p_423340_) {
         ChestModel chestmodel = new ChestModel(p_423340_.entityModelSet().bakeLayer(ModelLayers.CHEST));
         Material material = Sheets.CHEST_MAPPER.apply(this.texture);
         return new ChestSpecialRenderer(p_423340_.materials(), chestmodel, material, this.openness);
      }
   }
}
