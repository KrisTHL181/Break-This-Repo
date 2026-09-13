package net.minecraft.client.renderer.special;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Objects;
import java.util.function.Consumer;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BannerRenderer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class BannerSpecialRenderer implements SpecialModelRenderer<BannerPatternLayers> {
   private final BannerRenderer bannerRenderer;
   private final DyeColor baseColor;

   public BannerSpecialRenderer(DyeColor p_377632_, BannerRenderer p_376830_) {
      this.bannerRenderer = p_376830_;
      this.baseColor = p_377632_;
   }

   public @Nullable BannerPatternLayers extractArgument(ItemStack p_376998_) {
      return p_376998_.get(DataComponents.BANNER_PATTERNS);
   }

   public void submit(
      @Nullable BannerPatternLayers p_422473_,
      ItemDisplayContext p_423122_,
      PoseStack p_422445_,
      SubmitNodeCollector p_430183_,
      int p_431182_,
      int p_428848_,
      boolean p_426372_,
      int p_431892_
   ) {
      this.bannerRenderer
         .submitSpecial(p_422445_, p_430183_, p_431182_, p_428848_, this.baseColor, Objects.requireNonNullElse(p_422473_, BannerPatternLayers.EMPTY), p_431892_);
   }

   @Override
   public void getExtents(Consumer<Vector3fc> p_456720_) {
      this.bannerRenderer.getExtents(p_456720_);
   }

   @OnlyIn(Dist.CLIENT)
   public record Unbaked(DyeColor baseColor) implements SpecialModelRenderer.Unbaked {
      public static final MapCodec<BannerSpecialRenderer.Unbaked> MAP_CODEC = RecordCodecBuilder.mapCodec(
         p_376470_ -> p_376470_.group(DyeColor.CODEC.fieldOf("color").forGetter(BannerSpecialRenderer.Unbaked::baseColor))
            .apply(p_376470_, BannerSpecialRenderer.Unbaked::new)
      );

      @Override
      public MapCodec<BannerSpecialRenderer.Unbaked> type() {
         return MAP_CODEC;
      }

      @Override
      public SpecialModelRenderer<?> bake(SpecialModelRenderer.BakingContext p_425611_) {
         return new BannerSpecialRenderer(this.baseColor, new BannerRenderer(p_425611_));
      }
   }
}
