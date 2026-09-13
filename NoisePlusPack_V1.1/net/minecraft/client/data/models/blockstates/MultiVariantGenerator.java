package net.minecraft.client.data.models.blockstates;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.renderer.block.model.BlockStateModel;
import net.minecraft.client.renderer.block.model.VariantMutator;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MultiVariantGenerator implements BlockModelDefinitionGenerator {
   private final Block block;
   private final List<MultiVariantGenerator.Entry> entries;
   private final Set<Property<?>> seenProperties;

   MultiVariantGenerator(Block p_378449_, List<MultiVariantGenerator.Entry> p_378181_, Set<Property<?>> p_393628_) {
      this.block = p_378449_;
      this.entries = p_378181_;
      this.seenProperties = p_393628_;
   }

   static Set<Property<?>> validateAndExpandProperties(Set<Property<?>> p_394085_, Block p_394620_, PropertyDispatch<?> p_394046_) {
      List<Property<?>> list = p_394046_.getDefinedProperties();
      list.forEach(p_389280_ -> {
         if (p_394620_.getStateDefinition().getProperty(p_389280_.getName()) != p_389280_) {
            throw new IllegalStateException("Property " + p_389280_ + " is not defined for block " + p_394620_);
         }

         if (p_394085_.contains(p_389280_)) {
            throw new IllegalStateException("Values of property " + p_389280_ + " already defined for block " + p_394620_);
         }
      });
      Set<Property<?>> set = new HashSet<>(p_394085_);
      set.addAll(list);
      return set;
   }

   public MultiVariantGenerator with(PropertyDispatch<VariantMutator> p_376800_) {
      Set<Property<?>> set = validateAndExpandProperties(this.seenProperties, this.block, p_376800_);
      List<MultiVariantGenerator.Entry> list = this.entries.stream().flatMap(p_389282_ -> p_389282_.apply(p_376800_)).toList();
      return new MultiVariantGenerator(this.block, list, set);
   }

   public MultiVariantGenerator with(VariantMutator p_393117_) {
      List<MultiVariantGenerator.Entry> list = this.entries.stream().flatMap(p_389284_ -> p_389284_.apply(p_393117_)).toList();
      return new MultiVariantGenerator(this.block, list, this.seenProperties);
   }

   @Override
   public BlockModelDefinition create() {
      Map<String, BlockStateModel.Unbaked> map = new HashMap<>();

      for (MultiVariantGenerator.Entry multivariantgenerator$entry : this.entries) {
         map.put(multivariantgenerator$entry.properties.getKey(), multivariantgenerator$entry.variant.toUnbaked());
      }

      return new BlockModelDefinition(Optional.of(new BlockModelDefinition.SimpleModelSelectors(map)), Optional.empty());
   }

   @Override
   public Block block() {
      return this.block;
   }

   public static MultiVariantGenerator.Empty dispatch(Block p_394689_) {
      return new MultiVariantGenerator.Empty(p_394689_);
   }

   public static MultiVariantGenerator dispatch(Block p_396861_, MultiVariant p_392410_) {
      return new MultiVariantGenerator(p_396861_, List.of(new MultiVariantGenerator.Entry(PropertyValueList.EMPTY, p_392410_)), Set.of());
   }

   @OnlyIn(Dist.CLIENT)
   public static class Empty {
      private final Block block;

      public Empty(Block p_396303_) {
         this.block = p_396303_;
      }

      public MultiVariantGenerator with(PropertyDispatch<MultiVariant> p_396976_) {
         Set<Property<?>> set = MultiVariantGenerator.validateAndExpandProperties(Set.of(), this.block, p_396976_);
         List<MultiVariantGenerator.Entry> list = p_396976_.getEntries()
            .entrySet()
            .stream()
            .map(p_396238_ -> new MultiVariantGenerator.Entry(p_396238_.getKey(), p_396238_.getValue()))
            .toList();
         return new MultiVariantGenerator(this.block, list, set);
      }
   }

   @OnlyIn(Dist.CLIENT)
   record Entry(PropertyValueList properties, MultiVariant variant) {
      public Stream<MultiVariantGenerator.Entry> apply(PropertyDispatch<VariantMutator> p_392121_) {
         return p_392121_.getEntries().entrySet().stream().map(p_393452_ -> {
            PropertyValueList propertyvaluelist = this.properties.extend(p_393452_.getKey());
            MultiVariant multivariant = this.variant.with(p_393452_.getValue());
            return new MultiVariantGenerator.Entry(propertyvaluelist, multivariant);
         });
      }

      public Stream<MultiVariantGenerator.Entry> apply(VariantMutator p_391514_) {
         return Stream.of(new MultiVariantGenerator.Entry(this.properties, this.variant.with(p_391514_)));
      }
   }
}
