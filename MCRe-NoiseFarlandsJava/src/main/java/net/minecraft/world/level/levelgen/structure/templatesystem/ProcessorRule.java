package net.minecraft.world.level.levelgen.structure.templatesystem;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.Passthrough;
import net.minecraft.world.level.levelgen.structure.templatesystem.rule.blockentity.RuleBlockEntityModifier;
import org.jspecify.annotations.Nullable;

public class ProcessorRule {
    public static final Passthrough DEFAULT_BLOCK_ENTITY_MODIFIER = Passthrough.INSTANCE;
    public static final Codec<ProcessorRule> CODEC = RecordCodecBuilder.create(
        i -> i.group(
                RuleTest.CODEC.fieldOf("input_predicate").forGetter(r -> r.inputPredicate),
                RuleTest.CODEC.fieldOf("location_predicate").forGetter(r -> r.locPredicate),
                PosRuleTest.CODEC.lenientOptionalFieldOf("position_predicate", PosAlwaysTrueTest.INSTANCE).forGetter(r -> r.posPredicate),
                BlockState.CODEC.fieldOf("output_state").forGetter(r -> r.outputState),
                RuleBlockEntityModifier.CODEC
                    .lenientOptionalFieldOf("block_entity_modifier", DEFAULT_BLOCK_ENTITY_MODIFIER)
                    .forGetter(r -> r.blockEntityModifier)
            )
            .apply(i, ProcessorRule::new)
    );
    private final RuleTest inputPredicate;
    private final RuleTest locPredicate;
    private final PosRuleTest posPredicate;
    private final BlockState outputState;
    private final RuleBlockEntityModifier blockEntityModifier;

    public ProcessorRule(final RuleTest inputPredicate, final RuleTest locPredicate, final BlockState outputState) {
        this(inputPredicate, locPredicate, PosAlwaysTrueTest.INSTANCE, outputState);
    }

    public ProcessorRule(final RuleTest inputPredicate, final RuleTest locPredicate, final PosRuleTest posPredicate, final BlockState outputState) {
        this(inputPredicate, locPredicate, posPredicate, outputState, DEFAULT_BLOCK_ENTITY_MODIFIER);
    }

    public ProcessorRule(
        final RuleTest inputPredicate,
        final RuleTest locPredicate,
        final PosRuleTest posPredicate,
        final BlockState outputState,
        final RuleBlockEntityModifier blockEntityModifier
    ) {
        this.inputPredicate = inputPredicate;
        this.locPredicate = locPredicate;
        this.posPredicate = posPredicate;
        this.outputState = outputState;
        this.blockEntityModifier = blockEntityModifier;
    }

    public boolean test(
        final LevelReader level,
        final BlockState inputState,
        final BlockPos inTemplatePos,
        final BlockPos worldPos,
        final BlockPos reference,
        final RandomSource random
    ) {
        return this.inputPredicate.test(inputState, random)
            && this.locPredicate.testAgainstWorldState(level, worldPos, random)
            && this.posPredicate.test(inTemplatePos, worldPos, reference, random);
    }

    public BlockState getOutputState() {
        return this.outputState;
    }

    public @Nullable CompoundTag getOutputTag(final RandomSource random, final @Nullable CompoundTag existingTag) {
        return this.blockEntityModifier.apply(random, existingTag);
    }
}