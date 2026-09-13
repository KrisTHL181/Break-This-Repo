package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.ShulkerBoxRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class ShulkerBoxSpecialRenderer implements NoDataSpecialModelRenderer {
   private final ShulkerBoxRenderer shulkerBoxRenderer;
   private final float openness;
   private final Direction orientation;
   private final Material material;

   public ShulkerBoxSpecialRenderer(ShulkerBoxRenderer p_378569_, float p_376947_, Direction p_375769_, Material p_377130_) {
      this.shulkerBoxRenderer = p_378569_;
      this.openness = p_376947_;
      this.orientation = p_375769_;
      this.material = p_377130_;
   }

   @Override
   public void submit(
      ItemDisplayContext p_424781_, PoseStack p_422603_, SubmitNodeCollector p_423492_, int p_427245_, int p_424244_, boolean p_429241_, int p_431897_
   ) {
      this.shulkerBoxRenderer.submit(p_422603_, p_423492_, p_427245_, p_424244_, this.orientation, this.openness, null, this.material, p_431897_);
   }

   @Override
   public void getExtents(Consumer<Vector3fc> p_457929_) {
      this.shulkerBoxRenderer.getExtents(this.orientation, this.openness, p_457929_);
   }

   @OnlyIn(Dist.CLIENT)
   public record Unbaked(Identifier texture, float openness, Direction orientation) implements SpecialModelRenderer.Unbaked {
      public static final MapCodec<ShulkerBoxSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_448370_ -> p_448370_.group(
               Identifier.CODEC.fieldOf("texture").forGetter(ShulkerBoxSpecialRenderer.Unbaked::texture),
               Codec.FLOAT.optionalFieldOf("openness", 0.0F).forGetter(ShulkerBoxSpecialRenderer.Unbaked::openness),
               Direction.CODEC.optionalFieldOf("orientation", Direction.UP).forGetter(ShulkerBoxSpecialRenderer.Unbaked::orientation)
            )
            .apply(p_448370_, ShulkerBoxSpecialRenderer.Unbaked::new)
      );

      public Unbaked() {
         this(Identifier.withDefaultNamespace("shulker"), 0.0F, Direction.UP);
      }

      public Unbaked(DyeColor p_375531_) {
         this(Sheets.colorToShulkerMaterial(p_375531_), 0.0F, Direction.UP);
      }

      @Override
      public MapCodec<ShulkerBoxSpecialRenderer.Unbaked> type() {
         return MAP_CODEC;
      }

      @Override
      public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext p_422919_) {
         return new ShulkerBoxSpecialRenderer(new ShulkerBoxRenderer(p_422919_), this.openness, this.orientation, Sheets.SHULKER_MAPPER.apply(this.texture));
      }
   }
}
