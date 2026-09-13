package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.function.Consumer;
import net.minecraft.client.renderer.Sheets;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BedRenderer;
import net.minecraft.client.resources.model.Material;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;

@OnlyIn(Dist.CLIENT)
public class BedSpecialRenderer implements NoDataSpecialModelRenderer {
   private final BedRenderer bedRenderer;
   private final Material material;

   public BedSpecialRenderer(BedRenderer p_376795_, Material p_375687_) {
      this.bedRenderer = p_376795_;
      this.material = p_375687_;
   }

   @Override
   public void submit(
      ItemDisplayContext p_431627_, PoseStack p_429759_, SubmitNodeCollector p_430985_, int p_429825_, int p_424543_, boolean p_426202_, int p_431911_
   ) {
      this.bedRenderer.submitSpecial(p_429759_, p_430985_, p_429825_, p_424543_, this.material, p_431911_);
   }

   @Override
   public void getExtents(Consumer<Vector3fc> p_454410_) {
      this.bedRenderer.getExtents(p_454410_);
   }

   @OnlyIn(Dist.CLIENT)
   public record Unbaked(Identifier texture) implements SpecialModelRenderer.Unbaked {
      public static final MapCodec<BedSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_448366_ -> p_448366_.group(Identifier.CODEC.fieldOf("texture").forGetter(BedSpecialRenderer.Unbaked::texture))
            .apply(p_448366_, BedSpecialRenderer.Unbaked::new)
      );

      public Unbaked(DyeColor p_378179_) {
         this(Sheets.colorToResourceMaterial(p_378179_));
      }

      @Override
      public MapCodec<BedSpecialRenderer.Unbaked> type() {
         return MAP_CODEC;
      }

      @Override
      public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext p_425891_) {
         return new BedSpecialRenderer(new BedRenderer(p_425891_), Sheets.BED_MAPPER.apply(this.texture));
      }
   }
}
