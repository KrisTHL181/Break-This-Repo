package net.minecraft.client.data.models.blockstates;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Stream;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelDispatcher;
import net.minecraft.client.renderer.block.dispatch.VariantMutator;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.properties.Property;

public class MultiVariantGenerator implements BlockModelDefinitionGenerator {
   private final Block block;
   private final List<MultiVariantGenerator.Entry> entries;
   private final Set<Property<?>> seenProperties;

   private MultiVariantGenerator(final Block block, final List<MultiVariantGenerator.Entry> entries, final Set<Property<?>> seenProperties) {
      this.block = block;
      this.entries = entries;
      this.seenProperties = seenProperties;
   }

   private static Set<Property<?>> validateAndExpandProperties(final Set<Property<?>> seenProperties, final Block block, final PropertyDispatch<?> generator) {
      List<Property<?>> addedProperties = generator.getDefinedProperties();
      addedProperties.forEach(property -> {
         if (block.getStateDefinition().getProperty(property.getName()) != property) {
            throw new IllegalStateException("Property " + property + " is not defined for block " + block);
         }

         if (seenProperties.contains(property)) {
            throw new IllegalStateException("Values of property " + property + " already defined for block " + block);
         }
      });
      Set<Property<?>> newSeenProperties = new HashSet<>(seenProperties);
      newSeenProperties.addAll(addedProperties);
      return newSeenProperties;
   }

   public MultiVariantGenerator with(final PropertyDispatch<VariantMutator> newStage) {
      Set<Property<?>> newSeenProperties = validateAndExpandProperties(this.seenProperties, this.block, newStage);
      List<MultiVariantGenerator.Entry> newEntries = this.entries.stream().flatMap(entry -> entry.apply(newStage)).toList();
      return new MultiVariantGenerator(this.block, newEntries, newSeenProperties);
   }

   public MultiVariantGenerator with(final VariantMutator singleMutator) {
      List<MultiVariantGenerator.Entry> newEntries = this.entries.stream().flatMap(entry -> entry.apply(singleMutator)).toList();
      return new MultiVariantGenerator(this.block, newEntries, this.seenProperties);
   }

   @Override
   public BlockStateModelDispatcher create() {
      Map<String, BlockStateModel.Unbaked> variants = new HashMap<>();

      for (MultiVariantGenerator.Entry entry : this.entries) {
         variants.put(entry.properties.getKey(), entry.variant.toUnbaked());
      }

      return new BlockStateModelDispatcher(Optional.of(new BlockStateModelDispatcher.SimpleModelSelectors(variants)), Optional.empty());
   }

   @Override
   public Block block() {
      return this.block;
   }

   public static MultiVariantGenerator.Empty dispatch(final Block block) {
      return new MultiVariantGenerator.Empty(block);
   }

   public static MultiVariantGenerator dispatch(final Block block, final MultiVariant initialModel) {
      return new MultiVariantGenerator(block, List.of(new MultiVariantGenerator.Entry(PropertyValueList.EMPTY, initialModel)), Set.of());
   }

   public static class Empty {
      private final Block block;

      public Empty(final Block block) {
         this.block = block;
      }

      public MultiVariantGenerator with(final PropertyDispatch<MultiVariant> newStage) {
         Set<Property<?>> newSeenProperties = MultiVariantGenerator.validateAndExpandProperties(Set.of(), this.block, newStage);
         List<MultiVariantGenerator.Entry> newEntries = newStage.getEntries()
            .entrySet()
            .stream()
            .map(e -> new MultiVariantGenerator.Entry(e.getKey(), e.getValue()))
            .toList();
         return new MultiVariantGenerator(this.block, newEntries, newSeenProperties);
      }
   }

   private record Entry(PropertyValueList properties, MultiVariant variant) {
      public Stream<MultiVariantGenerator.Entry> apply(final PropertyDispatch<VariantMutator> stage) {
         return stage.getEntries().entrySet().stream().map(property -> {
            PropertyValueList newSelector = this.properties.extend(property.getKey());
            MultiVariant newVariants = this.variant.with(property.getValue());
            return new MultiVariantGenerator.Entry(newSelector, newVariants);
         });
      }

      public Stream<MultiVariantGenerator.Entry> apply(final VariantMutator mutator) {
         return Stream.of(new MultiVariantGenerator.Entry(this.properties, this.variant.with(mutator)));
      }
   }
}
