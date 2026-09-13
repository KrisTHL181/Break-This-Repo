package net.minecraft.gametest.framework;

import java.nio.file.Path;
import java.nio.file.Paths;
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
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.entity.TestInstanceBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

public class StructureUtils {
   public static final int DEFAULT_Y_SEARCH_RADIUS = 10;
   public static final String DEFAULT_TEST_STRUCTURES_DIR = "Minecraft.Server/src/test/convertables/data";
   public static Path testStructuresDir = Paths.get("Minecraft.Server/src/test/convertables/data");

   public static Rotation getRotationForRotationSteps(int p_127836_) {
      switch (p_127836_) {
         case 0:
            return Rotation.NONE;
         case 1:
            return Rotation.CLOCKWISE_90;
         case 2:
            return Rotation.CLOCKWISE_180;
         case 3:
            return Rotation.COUNTERCLOCKWISE_90;
         default:
            throw new IllegalArgumentException("rotationSteps must be a value from 0-3. Got value " + p_127836_);
      }
   }

   public static int getRotationStepsForRotation(Rotation p_177752_) {
      switch (p_177752_) {
         case NONE:
            return 0;
         case CLOCKWISE_90:
            return 1;
         case CLOCKWISE_180:
            return 2;
         case COUNTERCLOCKWISE_90:
            return 3;
         default:
            throw new IllegalArgumentException("Unknown rotation value, don't know how many steps it represents: " + p_177752_);
      }
   }

   public static TestInstanceBlockEntity createNewEmptyTest(
      Identifier p_458884_, BlockPos p_391956_, Vec3i p_394926_, Rotation p_395782_, ServerLevel p_396943_
   ) {
      BoundingBox boundingbox = getStructureBoundingBox(TestInstanceBlockEntity.getStructurePos(p_391956_), p_394926_, p_395782_);
      clearSpaceForStructure(boundingbox, p_396943_);
      p_396943_.setBlockAndUpdate(p_391956_, Blocks.TEST_INSTANCE_BLOCK.defaultBlockState());
      TestInstanceBlockEntity testinstanceblockentity = (TestInstanceBlockEntity)p_396943_.getBlockEntity(p_391956_);
      ResourceKey<GameTestInstance> resourcekey = ResourceKey.create(Registries.TEST_INSTANCE, p_458884_);
      testinstanceblockentity.set(
         new TestInstanceBlockEntity.Data(Optional.of(resourcekey), p_394926_, p_395782_, false, TestInstanceBlockEntity.Status.CLEARED, Optional.empty())
      );
      return testinstanceblockentity;
   }

   public static void clearSpaceForStructure(BoundingBox p_127850_, ServerLevel p_127852_) {
      int i = p_127850_.minY() - 1;
      BlockPos.betweenClosedStream(p_127850_).forEach(p_177748_ -> clearBlock(i, p_177748_, p_127852_));
      p_127852_.getBlockTicks().clearArea(p_127850_);
      p_127852_.clearBlockEvents(p_127850_);
      AABB aabb = AABB.of(p_127850_);
      List<Entity> list = p_127852_.getEntitiesOfClass(Entity.class, aabb, p_177750_ -> !(p_177750_ instanceof Player));
      list.forEach(Entity::discard);
   }

   public static BlockPos getTransformedFarCorner(BlockPos p_310098_, Vec3i p_312132_, Rotation p_309587_) {
      BlockPos blockpos = p_310098_.offset(p_312132_).offset(-1, -1, -1);
      return StructureTemplate.transform(blockpos, Mirror.NONE, p_309587_, p_310098_);
   }

   public static BoundingBox getStructureBoundingBox(BlockPos p_177761_, Vec3i p_177762_, Rotation p_177763_) {
      BlockPos blockpos = getTransformedFarCorner(p_177761_, p_177762_, p_177763_);
      BoundingBox boundingbox = BoundingBox.fromCorners(p_177761_, blockpos);
      int i = Math.min(boundingbox.minX(), boundingbox.maxX());
      int j = Math.min(boundingbox.minZ(), boundingbox.maxZ());
      return boundingbox.move(p_177761_.getX() - i, 0, p_177761_.getZ() - j);
   }

   public static Optional<BlockPos> findTestContainingPos(BlockPos p_396694_, int p_391597_, ServerLevel p_393804_) {
      return findTestBlocks(p_396694_, p_391597_, p_393804_).filter(p_177756_ -> doesStructureContain(p_177756_, p_396694_, p_393804_)).findFirst();
   }

   public static Optional<BlockPos> findNearestTest(BlockPos p_397473_, int p_391969_, ServerLevel p_395973_) {
      Comparator<BlockPos> comparator = Comparator.comparingInt(p_177759_ -> p_177759_.distManhattan(p_397473_));
      return findTestBlocks(p_397473_, p_391969_, p_395973_).min(comparator);
   }

   public static Stream<BlockPos> findTestBlocks(BlockPos p_127911_, int p_127912_, ServerLevel p_127913_) {
      return p_127913_.getPoiManager()
         .findAll(p_405074_ -> p_405074_.is(PoiTypes.TEST_INSTANCE), p_405075_ -> true, p_127911_, p_127912_, PoiManager.Occupancy.ANY)
         .map(BlockPos::immutable);
   }

   public static Stream<BlockPos> lookedAtTestPos(BlockPos p_393627_, Entity p_395264_, ServerLevel p_392963_) {
      int i = 250;
      Vec3 vec3 = p_395264_.getEyePosition();
      Vec3 vec31 = vec3.add(p_395264_.getLookAngle().scale(250.0));
      return findTestBlocks(p_393627_, 250, p_392963_)
         .map(p_389787_ -> p_392963_.getBlockEntity(p_389787_, BlockEntityType.TEST_INSTANCE_BLOCK))
         .flatMap(Optional::stream)
         .filter(p_389792_ -> p_389792_.getStructureBounds().clip(vec3, vec31).isPresent())
         .map(BlockEntity::getBlockPos)
         .sorted(Comparator.comparing(p_393627_::distSqr))
         .limit(1L);
   }

   private static void clearBlock(int p_127842_, BlockPos p_127843_, ServerLevel p_127844_) {
      BlockState blockstate;
      if (p_127843_.getY() < p_127842_) {
         blockstate = Blocks.STONE.defaultBlockState();
      } else {
         blockstate = Blocks.AIR.defaultBlockState();
      }

      BlockInput blockinput = new BlockInput(blockstate, Collections.emptySet(), null);
      blockinput.place(p_127844_, p_127843_, 818);
      p_127844_.updateNeighborsAt(p_127843_, blockstate.getBlock());
   }

   private static boolean doesStructureContain(BlockPos p_127868_, BlockPos p_127869_, ServerLevel p_127870_) {
      return p_127870_.getBlockEntity(p_127868_) instanceof TestInstanceBlockEntity testinstanceblockentity
         ? testinstanceblockentity.getStructureBoundingBox().isInside(p_127869_)
         : false;
   }
}
