package net.minecraft.client.data.models.blockstates;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import net.minecraft.client.data.models.MultiVariant;
import net.minecraft.client.renderer.block.model.BlockModelDefinition;
import net.minecraft.client.renderer.block.model.multipart.Condition;
import net.minecraft.client.renderer.block.model.multipart.Selector;
import net.minecraft.world.level.block.Block;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MultiPartGenerator implements BlockModelDefinitionGenerator {
   private final Block block;
   private final List<MultiPartGenerator.Entry> parts = new ArrayList<>();

   private MultiPartGenerator(Block p_376910_) {
      this.block = p_376910_;
   }

   @Override
   public Block block() {
      return this.block;
   }

   public static MultiPartGenerator multiPart(Block p_376179_) {
      return new MultiPartGenerator(p_376179_);
   }

   public MultiPartGenerator with(MultiVariant p_396276_) {
      this.parts.add(new MultiPartGenerator.Entry(Optional.empty(), p_396276_));
      return this;
   }

   private void validateCondition(Condition p_392697_) {
      p_392697_.instantiate(this.block.getStateDefinition());
   }

   public MultiPartGenerator with(Condition p_395503_, MultiVariant p_394665_) {
      this.validateCondition(p_395503_);
      this.parts.add(new MultiPartGenerator.Entry(Optional.of(p_395503_), p_394665_));
      return this;
   }

   public MultiPartGenerator with(ConditionBuilder p_396571_, MultiVariant p_392855_) {
      return this.with(p_396571_.build(), p_392855_);
   }

   @Override
   public BlockModelDefinition create() {
      return new BlockModelDefinition(
         Optional.empty(), Optional.of(new BlockModelDefinition.MultiPartDefinition(this.parts.stream().map(MultiPartGenerator.Entry::toUnbaked).toList()))
      );
   }

   @OnlyIn(Dist.CLIENT)
   record Entry(Optional<Condition> condition, MultiVariant variants) {
      public Selector toUnbaked() {
         return new Selector(this.condition, this.variants.toUnbaked());
      }
   }
}
