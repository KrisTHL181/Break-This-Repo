package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.Passthrough;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.RuleBlockEntityModifier;
import org.jspecify.annotations.Nullable;

public class ProcessorRule {
   public static final Passthrough DEFAULT_BLOCK_ENTITY_MODIFIER = Passthrough.INSTANCE;
   public static final Codec<ProcessorRule> CODEC = RecordCodecBuilder.create(
      p_327493_ -> p_327493_.group(
            RuleTest.CODEC.fieldOf("input_predicate").forGetter(p_163747_ -> p_163747_.inputPredicate),
            RuleTest.CODEC.fieldOf("location_predicate").forGetter(p_163745_ -> p_163745_.locPredicate),
            PosRuleTest.CODEC.lenientOptionalFieldOf("position_predicate", PosAlwaysTrueTest.INSTANCE).forGetter(p_163743_ -> p_163743_.posPredicate),
            BlockState.CODEC.fieldOf("output_state").forGetter(p_163741_ -> p_163741_.outputState),
            RuleBlockEntityModifier.CODEC
               .lenientOptionalFieldOf("block_entity_modifier", DEFAULT_BLOCK_ENTITY_MODIFIER)
               .forGetter(p_277333_ -> p_277333_.blockEntityModifier)
         )
         .apply(p_327493_, ProcessorRule::new)
   );
   private final RuleTest inputPredicate;
   private final RuleTest locPredicate;
   private final PosRuleTest posPredicate;
   private final BlockState outputState;
   private final RuleBlockEntityModifier blockEntityModifier;

   public ProcessorRule(RuleTest p_74223_, RuleTest p_74224_, BlockState p_74225_) {
      this(p_74223_, p_74224_, PosAlwaysTrueTest.INSTANCE, p_74225_);
   }

   public ProcessorRule(RuleTest p_74227_, RuleTest p_74228_, PosRuleTest p_74229_, BlockState p_74230_) {
      this(p_74227_, p_74228_, p_74229_, p_74230_, DEFAULT_BLOCK_ENTITY_MODIFIER);
   }

   public ProcessorRule(RuleTest p_277678_, RuleTest p_277379_, PosRuleTest p_278018_, BlockState p_277412_, RuleBlockEntityModifier p_277808_) {
      this.inputPredicate = p_277678_;
      this.locPredicate = p_277379_;
      this.posPredicate = p_278018_;
      this.outputState = p_277412_;
      this.blockEntityModifier = p_277808_;
   }

   public boolean test(BlockState p_230310_, BlockState p_230311_, BlockPos p_230312_, BlockPos p_230313_, BlockPos p_230314_, RandomSource p_230315_) {
      return this.inputPredicate.test(p_230310_, p_230315_)
         && this.locPredicate.test(p_230311_, p_230315_)
         && this.posPredicate.test(p_230312_, p_230313_, p_230314_, p_230315_);
   }

   public BlockState getOutputState() {
      return this.outputState;
   }

   public @Nullable CompoundTag getOutputTag(RandomSource p_277551_, @Nullable CompoundTag p_277867_) {
      return this.blockEntityModifier.apply(p_277551_, p_277867_);
   }
}
