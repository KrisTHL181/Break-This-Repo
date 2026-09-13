package net.minecraft.world.level.storage.loot.functions;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.entity.BannerPattern;
import net.minecraft.world.level.block.entity.BannerPatternLayers;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

public class SetBannerPatternFunction extends LootItemConditionalFunction {
   public static final MapCodec<SetBannerPatternFunction> MAP_CODEC = RecordCodecBuilder.mapCodec(
      i -> commonFields(i)
         .and(i.group(BannerPatternLayers.CODEC.fieldOf("patterns").forGetter(f -> f.patterns), Codec.BOOL.fieldOf("append").forGetter(f -> f.append)))
         .apply(i, SetBannerPatternFunction::new)
   );
   private final BannerPatternLayers patterns;
   private final boolean append;

   private SetBannerPatternFunction(final List<LootItemCondition> predicates, final BannerPatternLayers patterns, final boolean append) {
      super(predicates);
      this.patterns = patterns;
      this.append = append;
   }

   @Override
   protected ItemStack run(final ItemStack itemStack, final LootContext context) {
      if (this.append) {
         itemStack.update(
            DataComponents.BANNER_PATTERNS,
            BannerPatternLayers.EMPTY,
            this.patterns,
            (base, appended) -> new BannerPatternLayers.Builder().addAll(base).addAll(appended).build()
         );
      } else {
         itemStack.set(DataComponents.BANNER_PATTERNS, this.patterns);
      }

      return itemStack;
   }

   @Override
   public MapCodec<SetBannerPatternFunction> codec() {
      return MAP_CODEC;
   }

   public static SetBannerPatternFunction.Builder setBannerPattern(final boolean append) {
      return new SetBannerPatternFunction.Builder(append);
   }

   public static class Builder extends LootItemConditionalFunction.Builder<SetBannerPatternFunction.Builder> {
      private final BannerPatternLayers.Builder patterns = new BannerPatternLayers.Builder();
      private final boolean append;

      private Builder(final boolean append) {
         this.append = append;
      }

      protected SetBannerPatternFunction.Builder getThis() {
         return this;
      }

      @Override
      public LootItemFunction build() {
         return new SetBannerPatternFunction(this.getConditions(), this.patterns.build(), this.append);
      }

      public SetBannerPatternFunction.Builder addPattern(final Holder<BannerPattern> pattern, final DyeColor color) {
         this.patterns.add(pattern, color);
         return this;
      }
   }
}
