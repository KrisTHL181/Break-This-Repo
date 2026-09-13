package net.minecraft.gametest.framework;

import java.nio.file.Path;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.stream.Stream;
import net.minecraft.commands.arguments.blocks.BlockInput;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Vec3i;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.ai.village.poi.PoiManager;
import net.minecraft.world.entity.ai.village.poi.PoiTypes;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTypes;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class StructureUtils {
   public static final int DEFAULT_Y_SEARCH_RADIUS = 10;
   public static @Nullable Path testStructuresTargetDir;
   public static @Nullable Path testStructuresSourceDir;

   public static Rotation getRotationForRotationSteps(final int rotationSteps) {
      return switch (rotationSteps) {
         case 0 -> Rotation.NONE;
         case 1 -> Rotation.CLOCKWISE_90;
         case 2 -> Rotation.CLOCKWISE_180;
         case 3 -> Rotation.COUNTERCLOCKWISE_90;
         default -> throw new IllegalArgumentException("rotationSteps must be a value from 0-3. Got value " + rotationSteps);
      };
   }

   public static int getRotationStepsForRotation(final Rotation rotation) {
      return switch (rotation) {
         case NONE -> 0;
         case CLOCKWISE_90 -> 1;
         case CLOCKWISE_180 -> 2;
         case COUNTERCLOCKWISE_90 -> 3;
         default -> throw new IllegalArgumentException("Unknown rotation value, don't know how many steps it represents: " + rotation);
      };
   }

   public static TestInstanceBlockEntity createNewEmptyTest(
      final Identifier id, final BlockPos structurePos, final Vec3i size, final Rotation rotation, final ServerLevel level
   ) {
      BoundingBox structureBoundingBox = getStructureBoundingBox(TestInstanceBlockEntity.getStructurePos(structurePos), size, rotation);
      clearSpaceForStructure(structureBoundingBox, level);
      level.setBlockAndUpdate(structurePos, Blocks.TEST_INSTANCE_BLOCK.defaultBlockState());
      TestInstanceBlockEntity test = (TestInstanceBlockEntity)level.getBlockEntity(structurePos);
      ResourceKey<GameTestInstance> key = ResourceKey.create(Registries.TEST_INSTANCE, id);
      test.set(new TestInstanceBlockEntity.Data(Optional.of(key), size, rotation, false, TestInstanceBlockEntity.Status.CLEARED, Optional.empty()));
      return test;
   }

   public static void clearSpaceForStructure(final BoundingBox structureBoundingBox, final ServerLevel level) {
      int groundHeight = structureBoundingBox.minY() - 1;
      BlockPos.betweenClosedStream(structureBoundingBox).forEach(pos -> clearBlock(groundHeight, pos, level));
      level.getBlockTicks().clearArea(structureBoundingBox);
      level.clearBlockEvents(structureBoundingBox);
      AABB bounds = AABB.of(structureBoundingBox);
      List<Entity> livingEntities = level.getEntitiesOfClass(Entity.class, bounds, mob -> !(mob instanceof Player));
      livingEntities.forEach(Entity::discard);
   }

   public static BlockPos getTransformedFarCorner(final BlockPos structurePosition, final Vec3i size, final Rotation rotation) {
      BlockPos farCornerBeforeTransform = structurePosition.offset(size).offset(-1, -1, -1);
      return StructureTemplate.transform(farCornerBeforeTransform, Mirror.NONE, rotation, structurePosition);
   }

   public static BoundingBox getStructureBoundingBox(final BlockPos northWestCorner, final Vec3i size, final Rotation rotation) {
      BlockPos farCorner = getTransformedFarCorner(northWestCorner, size, rotation);
      BoundingBox boundingBox = BoundingBox.fromCorners(northWestCorner, farCorner);
      int currentNorthWestCornerX = Math.min(boundingBox.minX(), boundingBox.maxX());
      int currentNorthWestCornerZ = Math.min(boundingBox.minZ(), boundingBox.maxZ());
      return boundingBox.move(northWestCorner.getX() - currentNorthWestCornerX, 0, northWestCorner.getZ() - currentNorthWestCornerZ);
   }

   public static Optional<BlockPos> findTestContainingPos(final BlockPos pos, final int searchRadius, final ServerLevel level) {
      return findTestBlocks(pos, searchRadius, level).filter(testBlockPosToCheck -> doesStructureContain(testBlockPosToCheck, pos, level)).findFirst();
   }

   public static Optional<BlockPos> findNearestTest(final BlockPos relativeToPos, final int searchRadius, final ServerLevel level) {
      Comparator<BlockPos> distanceToPlayer = Comparator.comparingInt(pos -> pos.distManhattan(relativeToPos));
      return findTestBlocks(relativeToPos, searchRadius, level).min(distanceToPlayer);
   }

   public static Stream<BlockPos> findTestBlocks(final BlockPos centerPos, final int searchRadius, final ServerLevel level) {
      return level.getPoiManager()
         .findAll(p -> p.is(PoiTypes.TEST_INSTANCE), p -> true, centerPos, searchRadius, PoiManager.Occupancy.ANY)
         .map(BlockPos::immutable);
   }

   public static Stream<BlockPos> lookedAtTestPos(final BlockPos pos, final Entity camera, final ServerLevel level) {
      int radius = 250;
      Vec3 start = camera.getEyePosition();
      Vec3 end = start.add(camera.getLookAngle().scale(250.0));
      return findTestBlocks(pos, 250, level)
         .map(blockPos -> level.getBlockEntity(blockPos, BlockEntityTypes.TEST_INSTANCE_BLOCK))
         .flatMap(Optional::stream)
         .filter(blockEntity -> blockEntity.getStructureBounds().clip(start, end).isPresent())
         .map(BlockEntity::getBlockPos)
         .sorted(Comparator.comparing(pos::distSqr))
         .limit(1L);
   }

   private static void clearBlock(final int airIfAboveThisY, final BlockPos pos, final ServerLevel level) {
      BlockState blockState;
      if (pos.getY() < airIfAboveThisY) {
         blockState = Blocks.STONE.defaultBlockState();
      } else {
         blockState = Blocks.AIR.defaultBlockState();
      }

      BlockInput blockInput = new BlockInput(blockState, Collections.emptySet(), null);
      blockInput.place(level, pos, 818);
      level.updateNeighborsAt(pos, blockState.getBlock());
   }

   private static boolean doesStructureContain(final BlockPos testInstanceBlockPos, final BlockPos pos, final ServerLevel level) {
      return level.getBlockEntity(testInstanceBlockPos) instanceof TestInstanceBlockEntity blockEntity
         ? blockEntity.getStructureBoundingBox().isInside(pos)
         : false;
   }
}
