package net.minecraft.world.level.levelgen.feature;

import com.mojang.serialization.Codec;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Vec3i;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.levelgen.feature.configurations.TemplateFeatureConfiguration;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;

public class TemplateFeature extends Feature<TemplateFeatureConfiguration> {
    public TemplateFeature(final Codec<TemplateFeatureConfiguration> codec) {
        super(codec);
    }

    @Override
    public boolean place(final FeaturePlaceContext<TemplateFeatureConfiguration> context) {
        RandomSource random = context.random();
        WorldGenLevel level = context.level();
        TemplateFeatureConfiguration config = context.config();
        TemplateFeatureConfiguration.TemplateEntry templateEntry = config.templates().getRandomOrThrow(random);
        Rotation rotation = Util.getRandom(templateEntry.rotations(), random);
        StructureTemplateManager structureTemplateManager = level.getLevel().getServer().getStructureManager();
        StructureTemplate template = structureTemplateManager.getOrCreate(templateEntry.template());
        Vec3i offsetX = this.getRotatedOffset(rotation, Direction.Axis.X, template);
        Vec3i offsetZ = this.getRotatedOffset(rotation, Direction.Axis.Z, template);
        BlockPos pos = context.origin().offset(offsetX).offset(offsetZ);
        StructurePlaceSettings settings = new StructurePlaceSettings().setRotation(rotation).setRandom(random);
        return template.placeInWorld(level, pos, pos, settings, random, 3);
    }

    private Vec3i getRotatedOffset(final Rotation rotation, final Direction.Axis axis, final StructureTemplate template) {
        return rotation.rotate(axis.getNegative()).getUnitVec3i().multiply(template.getSize().get(axis) / 2);
    }
}