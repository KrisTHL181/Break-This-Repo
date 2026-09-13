package net.minecraft.client.data.models.blockstates;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelDispatcher;
import net.minecraft.client.renderer.block.dispatch.multipart.Condition;
import net.minecraft.client.renderer.block.dispatch.multipart.Selector;
import net.minecraft.world.level.block.Block;

public class MultiPartGenerator implements BlockModelDefinitionGenerator {
   private final Block block;
   private final List<MultiPartGenerator.Entry> parts = new ArrayList<>();

   private MultiPartGenerator(final Block block) {
      this.block = block;
   }

   @Override
   public Block block() {
      return this.block;
   }

   public static MultiPartGenerator multiPart(final Block block) {
      return new MultiPartGenerator(block);
   }

   public MultiPartGenerator with(final MultiVariant variants) {
      this.parts.add(new MultiPartGenerator.Entry(Optional.empty(), variants));
      return this;
   }

   private void validateCondition(final Condition condition) {
      condition.instantiate(this.block.getStateDefinition());
   }

   public MultiPartGenerator with(final Condition condition, final MultiVariant variants) {
      this.validateCondition(condition);
      this.parts.add(new MultiPartGenerator.Entry(Optional.of(condition), variants));
      return this;
   }

   public MultiPartGenerator with(final ConditionBuilder condition, final MultiVariant variants) {
      return this.with(condition.build(), variants);
   }

   @Override
   public BlockStateModelDispatcher create() {
      return new BlockStateModelDispatcher(
         Optional.empty(),
         Optional.of(new BlockStateModelDispatcher.MultiPartDefinition(this.parts.stream().map(MultiPartGenerator.Entry::toUnbaked).toList()))
      );
   }

   private record Entry(Optional<Condition> condition, MultiVariant variants) {
      public Selector toUnbaked() {
         return new Selector(this.condition, this.variants.toUnbaked());
      }
   }
}
