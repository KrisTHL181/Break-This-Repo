package net.minecraft.world.level.levelgen.feature.stateproviders;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.Collection;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.IntProvider;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.level.block.state.properties.Property;
import org.jspecify.annotations.Nullable;

public class RandomizedIntStateProvider extends BlockStateProvider {
   public static final MapCodec<RandomizedIntStateProvider> CODEC = RecordCodecBuilder.mapCodec(
      p_161576_ -> p_161576_.group(
            BlockStateProvider.CODEC.fieldOf("source").forGetter(p_161592_ -> p_161592_.source),
            Codec.STRING.fieldOf("property").forGetter(p_161590_ -> p_161590_.propertyName),
            IntProvider.CODEC.fieldOf("values").forGetter(p_161578_ -> p_161578_.values)
         )
         .apply(p_161576_, RandomizedIntStateProvider::new)
   );
   private final BlockStateProvider source;
   private final String propertyName;
   private @Nullable IntegerProperty property;
   private final IntProvider values;

   public RandomizedIntStateProvider(BlockStateProvider p_161562_, IntegerProperty p_161563_, IntProvider p_161564_) {
      this.source = p_161562_;
      this.property = p_161563_;
      this.propertyName = p_161563_.getName();
      this.values = p_161564_;
      Collection<Integer> collection = p_161563_.getPossibleValues();

      for (int i = p_161564_.getMinValue(); i <= p_161564_.getMaxValue(); i++) {
         if (!collection.contains(i)) {
            throw new IllegalArgumentException("Property value out of range: " + p_161563_.getName() + ": " + i);
         }
      }
   }

   public RandomizedIntStateProvider(BlockStateProvider p_161566_, String p_161567_, IntProvider p_161568_) {
      this.source = p_161566_;
      this.propertyName = p_161567_;
      this.values = p_161568_;
   }

   @Override
   protected BlockStateProviderType<?> type() {
      return BlockStateProviderType.RANDOMIZED_INT_STATE_PROVIDER;
   }

   @Override
   public BlockState getState(RandomSource p_225919_, BlockPos p_225920_) {
      BlockState blockstate = this.source.getState(p_225919_, p_225920_);
      if (this.property == null || !blockstate.hasProperty(this.property)) {
         IntegerProperty integerproperty = findProperty(blockstate, this.propertyName);
         if (integerproperty == null) {
            return blockstate;
         }

         this.property = integerproperty;
      }

      return blockstate.setValue(this.property, this.values.sample(p_225919_));
   }

   private static @Nullable IntegerProperty findProperty(BlockState p_161571_, String p_161572_) {
      Collection<Property<?>> collection = p_161571_.getProperties();
      Optional<IntegerProperty> optional = collection.stream()
         .filter(p_161583_ -> p_161583_.getName().equals(p_161572_))
         .filter(p_161588_ -> p_161588_ instanceof IntegerProperty)
         .map(p_161574_ -> (IntegerProperty)p_161574_)
         .findAny();
      return optional.orElse(null);
   }
}
