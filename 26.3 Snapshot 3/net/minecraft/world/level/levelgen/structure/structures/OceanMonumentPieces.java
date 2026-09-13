package net.minecraft.world.level.levelgen.structure.structures;

import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Lists;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;
import it.unimi.dsi.fastutil.objects.ObjectListIterator;
import java.util.List;
import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.monster.ElderGuardian;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.StructureManager;
import net.minecraft.world.level.WorldGenLevel;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.chunk.ChunkGenerator;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.StructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;

public class OceanMonumentPieces {
   private OceanMonumentPieces() {
   }

   private static class FitDoubleXRoom implements OceanMonumentPieces.MonumentRoomFitter {
      @Override
      public boolean fits(final OceanMonumentPieces.RoomDefinition definition) {
         return definition.hasOpening[Direction.EAST.get3DDataValue()] && !definition.connections[Direction.EAST.get3DDataValue()].claimed;
      }

      @Override
      public OceanMonumentPieces.OceanMonumentPiece create(
         final Direction orientation, final OceanMonumentPieces.RoomDefinition definition, final RandomSource random
      ) {
         definition.claimed = true;
         definition.connections[Direction.EAST.get3DDataValue()].claimed = true;
         return new OceanMonumentPieces.OceanMonumentDoubleXRoom(orientation, definition);
      }
   }

   private static class FitDoubleXYRoom implements OceanMonumentPieces.MonumentRoomFitter {
      @Override
      public boolean fits(final OceanMonumentPieces.RoomDefinition definition) {
         if (definition.hasOpening[Direction.EAST.get3DDataValue()]
            && !definition.connections[Direction.EAST.get3DDataValue()].claimed
            && definition.hasOpening[Direction.UP.get3DDataValue()]
            && !definition.connections[Direction.UP.get3DDataValue()].claimed) {
            OceanMonumentPieces.RoomDefinition east = definition.connections[Direction.EAST.get3DDataValue()];
            return east.hasOpening[Direction.UP.get3DDataValue()] && !east.connections[Direction.UP.get3DDataValue()].claimed;
         } else {
            return false;
         }
      }

      @Override
      public OceanMonumentPieces.OceanMonumentPiece create(
         final Direction orientation, final OceanMonumentPieces.RoomDefinition definition, final RandomSource random
      ) {
         definition.claimed = true;
         definition.connections[Direction.EAST.get3DDataValue()].claimed = true;
         definition.connections[Direction.UP.get3DDataValue()].claimed = true;
         definition.connections[Direction.EAST.get3DDataValue()].connections[Direction.UP.get3DDataValue()].claimed = true;
         return new OceanMonumentPieces.OceanMonumentDoubleXYRoom(orientation, definition);
      }
   }

   private static class FitDoubleYRoom implements OceanMonumentPieces.MonumentRoomFitter {
      @Override
      public boolean fits(final OceanMonumentPieces.RoomDefinition definition) {
         return definition.hasOpening[Direction.UP.get3DDataValue()] && !definition.connections[Direction.UP.get3DDataValue()].claimed;
      }

      @Override
      public OceanMonumentPieces.OceanMonumentPiece create(
         final Direction orientation, final OceanMonumentPieces.RoomDefinition definition, final RandomSource random
      ) {
         definition.claimed = true;
         definition.connections[Direction.UP.get3DDataValue()].claimed = true;
         return new OceanMonumentPieces.OceanMonumentDoubleYRoom(orientation, definition);
      }
   }

   private static class FitDoubleYZRoom implements OceanMonumentPieces.MonumentRoomFitter {
      @Override
      public boolean fits(final OceanMonumentPieces.RoomDefinition definition) {
         if (definition.hasOpening[Direction.NORTH.get3DDataValue()]
            && !definition.connections[Direction.NORTH.get3DDataValue()].claimed
            && definition.hasOpening[Direction.UP.get3DDataValue()]
            && !definition.connections[Direction.UP.get3DDataValue()].claimed) {
            OceanMonumentPieces.RoomDefinition north = definition.connections[Direction.NORTH.get3DDataValue()];
            return north.hasOpening[Direction.UP.get3DDataValue()] && !north.connections[Direction.UP.get3DDataValue()].claimed;
         } else {
            return false;
         }
      }

      @Override
      public OceanMonumentPieces.OceanMonumentPiece create(
         final Direction orientation, final OceanMonumentPieces.RoomDefinition definition, final RandomSource random
      ) {
         definition.claimed = true;
         definition.connections[Direction.NORTH.get3DDataValue()].claimed = true;
         definition.connections[Direction.UP.get3DDataValue()].claimed = true;
         definition.connections[Direction.NORTH.get3DDataValue()].connections[Direction.UP.get3DDataValue()].claimed = true;
         return new OceanMonumentPieces.OceanMonumentDoubleYZRoom(orientation, definition);
      }
   }

   private static class FitDoubleZRoom implements OceanMonumentPieces.MonumentRoomFitter {
      @Override
      public boolean fits(final OceanMonumentPieces.RoomDefinition definition) {
         return definition.hasOpening[Direction.NORTH.get3DDataValue()] && !definition.connections[Direction.NORTH.get3DDataValue()].claimed;
      }

      @Override
      public OceanMonumentPieces.OceanMonumentPiece create(
         final Direction orientation, final OceanMonumentPieces.RoomDefinition definition, final RandomSource random
      ) {
         OceanMonumentPieces.RoomDefinition source = definition;
         if (!definition.hasOpening[Direction.NORTH.get3DDataValue()] || definition.connections[Direction.NORTH.get3DDataValue()].claimed) {
            source = definition.connections[Direction.SOUTH.get3DDataValue()];
         }

         source.claimed = true;
         source.connections[Direction.NORTH.get3DDataValue()].claimed = true;
         return new OceanMonumentPieces.OceanMonumentDoubleZRoom(orientation, source);
      }
   }

   private static class FitSimpleRoom implements OceanMonumentPieces.MonumentRoomFitter {
      @Override
      public boolean fits(final OceanMonumentPieces.RoomDefinition definition) {
         return true;
      }

      @Override
      public OceanMonumentPieces.OceanMonumentPiece create(
         final Direction orientation, final OceanMonumentPieces.RoomDefinition definition, final RandomSource random
      ) {
         definition.claimed = true;
         return new OceanMonumentPieces.OceanMonumentSimpleRoom(orientation, definition, random);
      }
   }

   private static class FitSimpleTopRoom implements OceanMonumentPieces.MonumentRoomFitter {
      @Override
      public boolean fits(final OceanMonumentPieces.RoomDefinition definition) {
         return !definition.hasOpening[Direction.WEST.get3DDataValue()]
            && !definition.hasOpening[Direction.EAST.get3DDataValue()]
            && !definition.hasOpening[Direction.NORTH.get3DDataValue()]
            && !definition.hasOpening[Direction.SOUTH.get3DDataValue()]
            && !definition.hasOpening[Direction.UP.get3DDataValue()];
      }

      @Override
      public OceanMonumentPieces.OceanMonumentPiece create(
         final Direction orientation, final OceanMonumentPieces.RoomDefinition definition, final RandomSource random
      ) {
         definition.claimed = true;
         return new OceanMonumentPieces.OceanMonumentSimpleTopRoom(orientation, definition);
      }
   }

   public static class MonumentBuilding extends OceanMonumentPieces.OceanMonumentPiece {
      private static final int WIDTH = 58;
      private static final int HEIGHT = 22;
      private static final int DEPTH = 58;
      public static final int BIOME_RANGE_CHECK = 29;
      private static final int TOP_POSITION = 61;
      private OceanMonumentPieces.RoomDefinition sourceRoom;
      private OceanMonumentPieces.RoomDefinition coreRoom;
      private final List<OceanMonumentPieces.OceanMonumentPiece> childPieces = Lists.newArrayList();

      public MonumentBuilding(final RandomSource random, final int west, final int north, final Direction direction) {
         super(StructurePieceType.OCEAN_MONUMENT_BUILDING, direction, 0, makeBoundingBox(west, 39, north, direction, 58, 23, 58));
         this.setOrientation(direction);
         List<OceanMonumentPieces.RoomDefinition> roomDefinitions = this.generateRoomGraph(random);
         this.sourceRoom.claimed = true;
         this.childPieces.add(new OceanMonumentPieces.OceanMonumentEntryRoom(direction, this.sourceRoom));
         this.childPieces.add(new OceanMonumentPieces.OceanMonumentCoreRoom(direction, this.coreRoom));
         List<OceanMonumentPieces.MonumentRoomFitter> fitters = Lists.newArrayList();
         fitters.add(new OceanMonumentPieces.FitDoubleXYRoom());
         fitters.add(new OceanMonumentPieces.FitDoubleYZRoom());
         fitters.add(new OceanMonumentPieces.FitDoubleZRoom());
         fitters.add(new OceanMonumentPieces.FitDoubleXRoom());
         fitters.add(new OceanMonumentPieces.FitDoubleYRoom());
         fitters.add(new OceanMonumentPieces.FitSimpleTopRoom());
         fitters.add(new OceanMonumentPieces.FitSimpleRoom());

         for (OceanMonumentPieces.RoomDefinition definition : roomDefinitions) {
            if (!definition.claimed && !definition.isSpecial()) {
               for (OceanMonumentPieces.MonumentRoomFitter fitter : fitters) {
                  if (fitter.fits(definition)) {
                     this.childPieces.add(fitter.create(direction, definition, random));
                     break;
                  }
               }
            }
         }

         BlockPos offset = this.getWorldPos(9, 0, 22);

         for (OceanMonumentPieces.OceanMonumentPiece child : this.childPieces) {
            child.getBoundingBox().move(offset);
         }

         BoundingBox leftWing = BoundingBox.fromCorners(this.getWorldPos(1, 1, 1), this.getWorldPos(23, 8, 21));
         BoundingBox rightWing = BoundingBox.fromCorners(this.getWorldPos(34, 1, 1), this.getWorldPos(56, 8, 21));
         BoundingBox penthouse = BoundingBox.fromCorners(this.getWorldPos(22, 13, 22), this.getWorldPos(35, 17, 35));
         int wingRandom = random.nextInt();
         this.childPieces.add(new OceanMonumentPieces.OceanMonumentWingRoom(direction, leftWing, wingRandom++));
         this.childPieces.add(new OceanMonumentPieces.OceanMonumentWingRoom(direction, rightWing, wingRandom++));
         this.childPieces.add(new OceanMonumentPieces.OceanMonumentPenthouse(direction, penthouse));
      }

      public MonumentBuilding(final CompoundTag tag) {
         super(StructurePieceType.OCEAN_MONUMENT_BUILDING, tag);
      }

      private List<OceanMonumentPieces.RoomDefinition> generateRoomGraph(final RandomSource random) {
         OceanMonumentPieces.RoomDefinition[] roomGrid = new OceanMonumentPieces.RoomDefinition[75];

         for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 4; z++) {
               int y = 0;
               int pos = getRoomIndex(x, 0, z);
               roomGrid[pos] = new OceanMonumentPieces.RoomDefinition(pos);
            }
         }

         for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 4; z++) {
               int y = 1;
               int pos = getRoomIndex(x, 1, z);
               roomGrid[pos] = new OceanMonumentPieces.RoomDefinition(pos);
            }
         }

         for (int x = 1; x < 4; x++) {
            for (int z = 0; z < 2; z++) {
               int y = 2;
               int pos = getRoomIndex(x, 2, z);
               roomGrid[pos] = new OceanMonumentPieces.RoomDefinition(pos);
            }
         }

         this.sourceRoom = roomGrid[GRIDROOM_SOURCE_INDEX];

         for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
               for (int y = 0; y < 3; y++) {
                  int pos = getRoomIndex(x, y, z);
                  if (roomGrid[pos] != null) {
                     for (Direction direction : Direction.values()) {
                        int neighX = x + direction.getStepX();
                        int neighY = y + direction.getStepY();
                        int neighZ = z + direction.getStepZ();
                        if (neighX >= 0 && neighX < 5 && neighZ >= 0 && neighZ < 5 && neighY >= 0 && neighY < 3) {
                           int neighPos = getRoomIndex(neighX, neighY, neighZ);
                           if (roomGrid[neighPos] != null) {
                              if (neighZ == z) {
                                 roomGrid[pos].setConnection(direction, roomGrid[neighPos]);
                              } else {
                                 roomGrid[pos].setConnection(direction.getOpposite(), roomGrid[neighPos]);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         OceanMonumentPieces.RoomDefinition roofRoom = new OceanMonumentPieces.RoomDefinition(1003);
         OceanMonumentPieces.RoomDefinition leftWing = new OceanMonumentPieces.RoomDefinition(1001);
         OceanMonumentPieces.RoomDefinition rightWing = new OceanMonumentPieces.RoomDefinition(1002);
         roomGrid[GRIDROOM_TOP_CONNECT_INDEX].setConnection(Direction.UP, roofRoom);
         roomGrid[GRIDROOM_LEFTWING_CONNECT_INDEX].setConnection(Direction.SOUTH, leftWing);
         roomGrid[GRIDROOM_RIGHTWING_CONNECT_INDEX].setConnection(Direction.SOUTH, rightWing);
         roofRoom.claimed = true;
         leftWing.claimed = true;
         rightWing.claimed = true;
         this.sourceRoom.isSource = true;
         this.coreRoom = roomGrid[getRoomIndex(random.nextInt(4), 0, 2)];
         this.coreRoom.claimed = true;
         this.coreRoom.connections[Direction.EAST.get3DDataValue()].claimed = true;
         this.coreRoom.connections[Direction.NORTH.get3DDataValue()].claimed = true;
         this.coreRoom.connections[Direction.EAST.get3DDataValue()].connections[Direction.NORTH.get3DDataValue()].claimed = true;
         this.coreRoom.connections[Direction.UP.get3DDataValue()].claimed = true;
         this.coreRoom.connections[Direction.EAST.get3DDataValue()].connections[Direction.UP.get3DDataValue()].claimed = true;
         this.coreRoom.connections[Direction.NORTH.get3DDataValue()].connections[Direction.UP.get3DDataValue()].claimed = true;
         this.coreRoom.connections[Direction.EAST.get3DDataValue()].connections[Direction.NORTH.get3DDataValue()].connections[Direction.UP.get3DDataValue()].claimed = true;
         ObjectArrayList<OceanMonumentPieces.RoomDefinition> roomDefs = new ObjectArrayList();

         for (OceanMonumentPieces.RoomDefinition definition : roomGrid) {
            if (definition != null) {
               definition.updateOpenings();
               roomDefs.add(definition);
            }
         }

         roofRoom.updateOpenings();
         Util.shuffle(roomDefs, random);
         int scanIndex = 1;
         ObjectListIterator var34 = roomDefs.iterator();

         while (var34.hasNext()) {
            OceanMonumentPieces.RoomDefinition definition = (OceanMonumentPieces.RoomDefinition)var34.next();
            int closeCount = 0;
            int attemptCount = 0;

            while (closeCount < 2 && attemptCount < 5) {
               attemptCount++;
               int f = random.nextInt(6);
               if (definition.hasOpening[f]) {
                  int of = Direction.from3DDataValue(f).getOpposite().get3DDataValue();
                  definition.hasOpening[f] = false;
                  definition.connections[f].hasOpening[of] = false;
                  if (definition.findSource(scanIndex++) && definition.connections[f].findSource(scanIndex++)) {
                     closeCount++;
                  } else {
                     definition.hasOpening[f] = true;
                     definition.connections[f].hasOpening[of] = true;
                  }
               }
            }
         }

         roomDefs.add(roofRoom);
         roomDefs.add(leftWing);
         roomDefs.add(rightWing);
         return roomDefs;
      }

      @Override
      public void postProcess(
         final WorldGenLevel level,
         final StructureManager structureManager,
         final ChunkGenerator generator,
         final RandomSource random,
         final BoundingBox chunkBB,
         final ChunkPos chunkPos,
         final BlockPos referencePos
      ) {
         int waterHeight = Math.max(level.getSeaLevel(), 64) - this.boundingBox.minY();
         this.generateWaterBox(level, chunkBB, 0, 0, 0, 58, waterHeight, 58);
         this.generateWing(false, 0, level, random, chunkBB);
         this.generateWing(true, 33, level, random, chunkBB);
         this.generateEntranceArchs(level, random, chunkBB);
         this.generateEntranceWall(level, random, chunkBB);
         this.generateRoofPiece(level, random, chunkBB);
         this.generateLowerWall(level, random, chunkBB);
         this.generateMiddleWall(level, random, chunkBB);
         this.generateUpperWall(level, random, chunkBB);

         for (int pillarX = 0; pillarX < 7; pillarX++) {
            int pillarZ = 0;

            while (pillarZ < 7) {
               if (pillarZ == 0 && pillarX == 3) {
                  pillarZ = 6;
               }

               int bx = pillarX * 9;
               int bz = pillarZ * 9;

               for (int w = 0; w < 4; w++) {
                  for (int d = 0; d < 4; d++) {
                     this.placeBlock(level, BASE_LIGHT, bx + w, 0, bz + d, chunkBB);
                     this.fillColumnDown(level, BASE_LIGHT, bx + w, -1, bz + d, chunkBB);
                  }
               }

               if (pillarX != 0 && pillarX != 6) {
                  pillarZ += 6;
               } else {
                  pillarZ++;
               }
            }
         }

         for (int i = 0; i < 5; i++) {
            this.generateWaterBox(level, chunkBB, -1 - i, 0 + i * 2, -1 - i, -1 - i, 23, 58 + i);
            this.generateWaterBox(level, chunkBB, 58 + i, 0 + i * 2, -1 - i, 58 + i, 23, 58 + i);
            this.generateWaterBox(level, chunkBB, 0 - i, 0 + i * 2, -1 - i, 57 + i, 23, -1 - i);
            this.generateWaterBox(level, chunkBB, 0 - i, 0 + i * 2, 58 + i, 57 + i, 23, 58 + i);
         }

         for (OceanMonumentPieces.OceanMonumentPiece child : this.childPieces) {
            if (child.getBoundingBox().intersects(chunkBB)) {
               child.postProcess(level, structureManager, generator, random, chunkBB, chunkPos, referencePos);
            }
         }
      }

      private void generateWing(final boolean isFlipped, final int xoff, final WorldGenLevel level, final RandomSource random, final BoundingBox chunkBB) {
         int sectionWidth = 24;
         if (this.chunkIntersects(chunkBB, xoff, 0, xoff + 23, 20)) {
            this.generateBox(level, chunkBB, xoff + 0, 0, 0, xoff + 24, 0, 20, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(level, chunkBB, xoff + 0, 1, 0, xoff + 24, 10, 20);

            for (int i = 0; i < 4; i++) {
               this.generateBox(level, chunkBB, xoff + i, i + 1, i, xoff + i, i + 1, 20, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, xoff + i + 7, i + 5, i + 7, xoff + i + 7, i + 5, 20, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, xoff + 17 - i, i + 5, i + 7, xoff + 17 - i, i + 5, 20, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, xoff + 24 - i, i + 1, i, xoff + 24 - i, i + 1, 20, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, xoff + i + 1, i + 1, i, xoff + 23 - i, i + 1, i, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, xoff + i + 8, i + 5, i + 7, xoff + 16 - i, i + 5, i + 7, BASE_LIGHT, BASE_LIGHT, false);
            }

            this.generateBox(level, chunkBB, xoff + 4, 4, 4, xoff + 6, 4, 20, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, xoff + 7, 4, 4, xoff + 17, 4, 6, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, xoff + 18, 4, 4, xoff + 20, 4, 20, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, xoff + 11, 8, 11, xoff + 13, 8, 20, BASE_GRAY, BASE_GRAY, false);
            this.placeBlock(level, DOT_DECO_DATA, xoff + 12, 9, 12, chunkBB);
            this.placeBlock(level, DOT_DECO_DATA, xoff + 12, 9, 15, chunkBB);
            this.placeBlock(level, DOT_DECO_DATA, xoff + 12, 9, 18, chunkBB);
            int leftPos = xoff + (isFlipped ? 19 : 5);
            int rightPos = xoff + (isFlipped ? 5 : 19);

            for (int z = 20; z >= 5; z -= 3) {
               this.placeBlock(level, DOT_DECO_DATA, leftPos, 5, z, chunkBB);
            }

            for (int z = 19; z >= 7; z -= 3) {
               this.placeBlock(level, DOT_DECO_DATA, rightPos, 5, z, chunkBB);
            }

            for (int i = 0; i < 4; i++) {
               int pos = isFlipped ? xoff + 24 - (17 - i * 3) : xoff + 17 - i * 3;
               this.placeBlock(level, DOT_DECO_DATA, pos, 5, 5, chunkBB);
            }

            this.placeBlock(level, DOT_DECO_DATA, rightPos, 5, 5, chunkBB);
            this.generateBox(level, chunkBB, xoff + 11, 1, 12, xoff + 13, 7, 12, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, xoff + 12, 1, 11, xoff + 12, 7, 13, BASE_GRAY, BASE_GRAY, false);
         }
      }

      private void generateEntranceArchs(final WorldGenLevel level, final RandomSource random, final BoundingBox chunkBB) {
         if (this.chunkIntersects(chunkBB, 22, 5, 35, 17)) {
            this.generateWaterBox(level, chunkBB, 25, 0, 0, 32, 8, 20);

            for (int i = 0; i < 4; i++) {
               this.generateBox(level, chunkBB, 24, 2, 5 + i * 4, 24, 4, 5 + i * 4, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, 22, 4, 5 + i * 4, 23, 4, 5 + i * 4, BASE_LIGHT, BASE_LIGHT, false);
               this.placeBlock(level, BASE_LIGHT, 25, 5, 5 + i * 4, chunkBB);
               this.placeBlock(level, BASE_LIGHT, 26, 6, 5 + i * 4, chunkBB);
               this.placeBlock(level, LAMP_BLOCK, 26, 5, 5 + i * 4, chunkBB);
               this.generateBox(level, chunkBB, 33, 2, 5 + i * 4, 33, 4, 5 + i * 4, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, 34, 4, 5 + i * 4, 35, 4, 5 + i * 4, BASE_LIGHT, BASE_LIGHT, false);
               this.placeBlock(level, BASE_LIGHT, 32, 5, 5 + i * 4, chunkBB);
               this.placeBlock(level, BASE_LIGHT, 31, 6, 5 + i * 4, chunkBB);
               this.placeBlock(level, LAMP_BLOCK, 31, 5, 5 + i * 4, chunkBB);
               this.generateBox(level, chunkBB, 27, 6, 5 + i * 4, 30, 6, 5 + i * 4, BASE_GRAY, BASE_GRAY, false);
            }
         }
      }

      private void generateEntranceWall(final WorldGenLevel level, final RandomSource random, final BoundingBox chunkBB) {
         if (this.chunkIntersects(chunkBB, 15, 20, 42, 21)) {
            this.generateBox(level, chunkBB, 15, 0, 21, 42, 0, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(level, chunkBB, 26, 1, 21, 31, 3, 21);
            this.generateBox(level, chunkBB, 21, 12, 21, 36, 12, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, 17, 11, 21, 40, 11, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, 16, 10, 21, 41, 10, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, 15, 7, 21, 42, 9, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, 16, 6, 21, 41, 6, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, 17, 5, 21, 40, 5, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, 21, 4, 21, 36, 4, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, 22, 3, 21, 26, 3, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, 31, 3, 21, 35, 3, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, 23, 2, 21, 25, 2, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, 32, 2, 21, 34, 2, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, 28, 4, 20, 29, 4, 21, BASE_LIGHT, BASE_LIGHT, false);
            this.placeBlock(level, BASE_LIGHT, 27, 3, 21, chunkBB);
            this.placeBlock(level, BASE_LIGHT, 30, 3, 21, chunkBB);
            this.placeBlock(level, BASE_LIGHT, 26, 2, 21, chunkBB);
            this.placeBlock(level, BASE_LIGHT, 31, 2, 21, chunkBB);
            this.placeBlock(level, BASE_LIGHT, 25, 1, 21, chunkBB);
            this.placeBlock(level, BASE_LIGHT, 32, 1, 21, chunkBB);

            for (int i = 0; i < 7; i++) {
               this.placeBlock(level, BASE_BLACK, 28 - i, 6 + i, 21, chunkBB);
               this.placeBlock(level, BASE_BLACK, 29 + i, 6 + i, 21, chunkBB);
            }

            for (int i = 0; i < 4; i++) {
               this.placeBlock(level, BASE_BLACK, 28 - i, 9 + i, 21, chunkBB);
               this.placeBlock(level, BASE_BLACK, 29 + i, 9 + i, 21, chunkBB);
            }

            this.placeBlock(level, BASE_BLACK, 28, 12, 21, chunkBB);
            this.placeBlock(level, BASE_BLACK, 29, 12, 21, chunkBB);

            for (int i = 0; i < 3; i++) {
               this.placeBlock(level, BASE_BLACK, 22 - i * 2, 8, 21, chunkBB);
               this.placeBlock(level, BASE_BLACK, 22 - i * 2, 9, 21, chunkBB);
               this.placeBlock(level, BASE_BLACK, 35 + i * 2, 8, 21, chunkBB);
               this.placeBlock(level, BASE_BLACK, 35 + i * 2, 9, 21, chunkBB);
            }

            this.generateWaterBox(level, chunkBB, 15, 13, 21, 42, 15, 21);
            this.generateWaterBox(level, chunkBB, 15, 1, 21, 15, 6, 21);
            this.generateWaterBox(level, chunkBB, 16, 1, 21, 16, 5, 21);
            this.generateWaterBox(level, chunkBB, 17, 1, 21, 20, 4, 21);
            this.generateWaterBox(level, chunkBB, 21, 1, 21, 21, 3, 21);
            this.generateWaterBox(level, chunkBB, 22, 1, 21, 22, 2, 21);
            this.generateWaterBox(level, chunkBB, 23, 1, 21, 24, 1, 21);
            this.generateWaterBox(level, chunkBB, 42, 1, 21, 42, 6, 21);
            this.generateWaterBox(level, chunkBB, 41, 1, 21, 41, 5, 21);
            this.generateWaterBox(level, chunkBB, 37, 1, 21, 40, 4, 21);
            this.generateWaterBox(level, chunkBB, 36, 1, 21, 36, 3, 21);
            this.generateWaterBox(level, chunkBB, 33, 1, 21, 34, 1, 21);
            this.generateWaterBox(level, chunkBB, 35, 1, 21, 35, 2, 21);
         }
      }

      private void generateRoofPiece(final WorldGenLevel level, final RandomSource random, final BoundingBox chunkBB) {
         if (this.chunkIntersects(chunkBB, 21, 21, 36, 36)) {
            this.generateBox(level, chunkBB, 21, 0, 22, 36, 0, 36, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(level, chunkBB, 21, 1, 22, 36, 23, 36);

            for (int i = 0; i < 4; i++) {
               this.generateBox(level, chunkBB, 21 + i, 13 + i, 21 + i, 36 - i, 13 + i, 21 + i, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, 21 + i, 13 + i, 36 - i, 36 - i, 13 + i, 36 - i, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, 21 + i, 13 + i, 22 + i, 21 + i, 13 + i, 35 - i, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, 36 - i, 13 + i, 22 + i, 36 - i, 13 + i, 35 - i, BASE_LIGHT, BASE_LIGHT, false);
            }

            this.generateBox(level, chunkBB, 25, 16, 25, 32, 16, 32, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, 25, 17, 25, 25, 19, 25, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 32, 17, 25, 32, 19, 25, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 25, 17, 32, 25, 19, 32, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 32, 17, 32, 32, 19, 32, BASE_LIGHT, BASE_LIGHT, false);
            this.placeBlock(level, BASE_LIGHT, 26, 20, 26, chunkBB);
            this.placeBlock(level, BASE_LIGHT, 27, 21, 27, chunkBB);
            this.placeBlock(level, LAMP_BLOCK, 27, 20, 27, chunkBB);
            this.placeBlock(level, BASE_LIGHT, 26, 20, 31, chunkBB);
            this.placeBlock(level, BASE_LIGHT, 27, 21, 30, chunkBB);
            this.placeBlock(level, LAMP_BLOCK, 27, 20, 30, chunkBB);
            this.placeBlock(level, BASE_LIGHT, 31, 20, 31, chunkBB);
            this.placeBlock(level, BASE_LIGHT, 30, 21, 30, chunkBB);
            this.placeBlock(level, LAMP_BLOCK, 30, 20, 30, chunkBB);
            this.placeBlock(level, BASE_LIGHT, 31, 20, 26, chunkBB);
            this.placeBlock(level, BASE_LIGHT, 30, 21, 27, chunkBB);
            this.placeBlock(level, LAMP_BLOCK, 30, 20, 27, chunkBB);
            this.generateBox(level, chunkBB, 28, 21, 27, 29, 21, 27, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, 27, 21, 28, 27, 21, 29, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, 28, 21, 30, 29, 21, 30, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, 30, 21, 28, 30, 21, 29, BASE_GRAY, BASE_GRAY, false);
         }
      }

      private void generateLowerWall(final WorldGenLevel level, final RandomSource random, final BoundingBox chunkBB) {
         if (this.chunkIntersects(chunkBB, 0, 21, 6, 58)) {
            this.generateBox(level, chunkBB, 0, 0, 21, 6, 0, 57, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(level, chunkBB, 0, 1, 21, 6, 7, 57);
            this.generateBox(level, chunkBB, 4, 4, 21, 6, 4, 53, BASE_GRAY, BASE_GRAY, false);

            for (int i = 0; i < 4; i++) {
               this.generateBox(level, chunkBB, i, i + 1, 21, i, i + 1, 57 - i, BASE_LIGHT, BASE_LIGHT, false);
            }

            for (int z = 23; z < 53; z += 3) {
               this.placeBlock(level, DOT_DECO_DATA, 5, 5, z, chunkBB);
            }

            this.placeBlock(level, DOT_DECO_DATA, 5, 5, 52, chunkBB);

            for (int i = 0; i < 4; i++) {
               this.generateBox(level, chunkBB, i, i + 1, 21, i, i + 1, 57 - i, BASE_LIGHT, BASE_LIGHT, false);
            }

            this.generateBox(level, chunkBB, 4, 1, 52, 6, 3, 52, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, 5, 1, 51, 5, 3, 53, BASE_GRAY, BASE_GRAY, false);
         }

         if (this.chunkIntersects(chunkBB, 51, 21, 58, 58)) {
            this.generateBox(level, chunkBB, 51, 0, 21, 57, 0, 57, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(level, chunkBB, 51, 1, 21, 57, 7, 57);
            this.generateBox(level, chunkBB, 51, 4, 21, 53, 4, 53, BASE_GRAY, BASE_GRAY, false);

            for (int i = 0; i < 4; i++) {
               this.generateBox(level, chunkBB, 57 - i, i + 1, 21, 57 - i, i + 1, 57 - i, BASE_LIGHT, BASE_LIGHT, false);
            }

            for (int z = 23; z < 53; z += 3) {
               this.placeBlock(level, DOT_DECO_DATA, 52, 5, z, chunkBB);
            }

            this.placeBlock(level, DOT_DECO_DATA, 52, 5, 52, chunkBB);
            this.generateBox(level, chunkBB, 51, 1, 52, 53, 3, 52, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, 52, 1, 51, 52, 3, 53, BASE_GRAY, BASE_GRAY, false);
         }

         if (this.chunkIntersects(chunkBB, 0, 51, 57, 57)) {
            this.generateBox(level, chunkBB, 7, 0, 51, 50, 0, 57, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(level, chunkBB, 7, 1, 51, 50, 10, 57);

            for (int i = 0; i < 4; i++) {
               this.generateBox(level, chunkBB, i + 1, i + 1, 57 - i, 56 - i, i + 1, 57 - i, BASE_LIGHT, BASE_LIGHT, false);
            }
         }
      }

      private void generateMiddleWall(final WorldGenLevel level, final RandomSource random, final BoundingBox chunkBB) {
         if (this.chunkIntersects(chunkBB, 7, 21, 13, 50)) {
            this.generateBox(level, chunkBB, 7, 0, 21, 13, 0, 50, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(level, chunkBB, 7, 1, 21, 13, 10, 50);
            this.generateBox(level, chunkBB, 11, 8, 21, 13, 8, 53, BASE_GRAY, BASE_GRAY, false);

            for (int i = 0; i < 4; i++) {
               this.generateBox(level, chunkBB, i + 7, i + 5, 21, i + 7, i + 5, 54, BASE_LIGHT, BASE_LIGHT, false);
            }

            for (int z = 21; z <= 45; z += 3) {
               this.placeBlock(level, DOT_DECO_DATA, 12, 9, z, chunkBB);
            }
         }

         if (this.chunkIntersects(chunkBB, 44, 21, 50, 54)) {
            this.generateBox(level, chunkBB, 44, 0, 21, 50, 0, 50, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(level, chunkBB, 44, 1, 21, 50, 10, 50);
            this.generateBox(level, chunkBB, 44, 8, 21, 46, 8, 53, BASE_GRAY, BASE_GRAY, false);

            for (int i = 0; i < 4; i++) {
               this.generateBox(level, chunkBB, 50 - i, i + 5, 21, 50 - i, i + 5, 54, BASE_LIGHT, BASE_LIGHT, false);
            }

            for (int z = 21; z <= 45; z += 3) {
               this.placeBlock(level, DOT_DECO_DATA, 45, 9, z, chunkBB);
            }
         }

         if (this.chunkIntersects(chunkBB, 8, 44, 49, 54)) {
            this.generateBox(level, chunkBB, 14, 0, 44, 43, 0, 50, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(level, chunkBB, 14, 1, 44, 43, 10, 50);

            for (int x = 12; x <= 45; x += 3) {
               this.placeBlock(level, DOT_DECO_DATA, x, 9, 45, chunkBB);
               this.placeBlock(level, DOT_DECO_DATA, x, 9, 52, chunkBB);
               if (x == 12 || x == 18 || x == 24 || x == 33 || x == 39 || x == 45) {
                  this.placeBlock(level, DOT_DECO_DATA, x, 9, 47, chunkBB);
                  this.placeBlock(level, DOT_DECO_DATA, x, 9, 50, chunkBB);
                  this.placeBlock(level, DOT_DECO_DATA, x, 10, 45, chunkBB);
                  this.placeBlock(level, DOT_DECO_DATA, x, 10, 46, chunkBB);
                  this.placeBlock(level, DOT_DECO_DATA, x, 10, 51, chunkBB);
                  this.placeBlock(level, DOT_DECO_DATA, x, 10, 52, chunkBB);
                  this.placeBlock(level, DOT_DECO_DATA, x, 11, 47, chunkBB);
                  this.placeBlock(level, DOT_DECO_DATA, x, 11, 50, chunkBB);
                  this.placeBlock(level, DOT_DECO_DATA, x, 12, 48, chunkBB);
                  this.placeBlock(level, DOT_DECO_DATA, x, 12, 49, chunkBB);
               }
            }

            for (int i = 0; i < 3; i++) {
               this.generateBox(level, chunkBB, 8 + i, 5 + i, 54, 49 - i, 5 + i, 54, BASE_GRAY, BASE_GRAY, false);
            }

            this.generateBox(level, chunkBB, 11, 8, 54, 46, 8, 54, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 14, 8, 44, 43, 8, 53, BASE_GRAY, BASE_GRAY, false);
         }
      }

      private void generateUpperWall(final WorldGenLevel level, final RandomSource random, final BoundingBox chunkBB) {
         if (this.chunkIntersects(chunkBB, 14, 21, 20, 43)) {
            this.generateBox(level, chunkBB, 14, 0, 21, 20, 0, 43, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(level, chunkBB, 14, 1, 22, 20, 14, 43);
            this.generateBox(level, chunkBB, 18, 12, 22, 20, 12, 39, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, 18, 12, 21, 20, 12, 21, BASE_LIGHT, BASE_LIGHT, false);

            for (int i = 0; i < 4; i++) {
               this.generateBox(level, chunkBB, i + 14, i + 9, 21, i + 14, i + 9, 43 - i, BASE_LIGHT, BASE_LIGHT, false);
            }

            for (int z = 23; z <= 39; z += 3) {
               this.placeBlock(level, DOT_DECO_DATA, 19, 13, z, chunkBB);
            }
         }

         if (this.chunkIntersects(chunkBB, 37, 21, 43, 43)) {
            this.generateBox(level, chunkBB, 37, 0, 21, 43, 0, 43, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(level, chunkBB, 37, 1, 22, 43, 14, 43);
            this.generateBox(level, chunkBB, 37, 12, 22, 39, 12, 39, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, 37, 12, 21, 39, 12, 21, BASE_LIGHT, BASE_LIGHT, false);

            for (int i = 0; i < 4; i++) {
               this.generateBox(level, chunkBB, 43 - i, i + 9, 21, 43 - i, i + 9, 43 - i, BASE_LIGHT, BASE_LIGHT, false);
            }

            for (int z = 23; z <= 39; z += 3) {
               this.placeBlock(level, DOT_DECO_DATA, 38, 13, z, chunkBB);
            }
         }

         if (this.chunkIntersects(chunkBB, 15, 37, 42, 43)) {
            this.generateBox(level, chunkBB, 21, 0, 37, 36, 0, 43, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(level, chunkBB, 21, 1, 37, 36, 14, 43);
            this.generateBox(level, chunkBB, 21, 12, 37, 36, 12, 39, BASE_GRAY, BASE_GRAY, false);

            for (int i = 0; i < 4; i++) {
               this.generateBox(level, chunkBB, 15 + i, i + 9, 43 - i, 42 - i, i + 9, 43 - i, BASE_LIGHT, BASE_LIGHT, false);
            }

            for (int x = 21; x <= 36; x += 3) {
               this.placeBlock(level, DOT_DECO_DATA, x, 13, 38, chunkBB);
            }
         }
      }
   }

   private interface MonumentRoomFitter {
      boolean fits(OceanMonumentPieces.RoomDefinition definition);

      OceanMonumentPieces.OceanMonumentPiece create(Direction orientation, OceanMonumentPieces.RoomDefinition definition, RandomSource random);
   }

   public static class OceanMonumentCoreRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentCoreRoom(final Direction orientation, final OceanMonumentPieces.RoomDefinition definition) {
         super(StructurePieceType.OCEAN_MONUMENT_CORE_ROOM, 1, orientation, definition, 2, 2, 2);
      }

      public OceanMonumentCoreRoom(final CompoundTag tag) {
         super(StructurePieceType.OCEAN_MONUMENT_CORE_ROOM, tag);
      }

      @Override
      public void postProcess(
         final WorldGenLevel level,
         final StructureManager structureManager,
         final ChunkGenerator generator,
         final RandomSource random,
         final BoundingBox chunkBB,
         final ChunkPos chunkPos,
         final BlockPos referencePos
      ) {
         this.generateBoxOnFillOnly(level, chunkBB, 1, 8, 0, 14, 8, 14, BASE_GRAY);
         int y = 7;
         BlockState block = BASE_LIGHT;
         this.generateBox(level, chunkBB, 0, 7, 0, 0, 7, 15, block, block, false);
         this.generateBox(level, chunkBB, 15, 7, 0, 15, 7, 15, block, block, false);
         this.generateBox(level, chunkBB, 1, 7, 0, 15, 7, 0, block, block, false);
         this.generateBox(level, chunkBB, 1, 7, 15, 14, 7, 15, block, block, false);

         for (int yx = 1; yx <= 6; yx++) {
            block = BASE_LIGHT;
            if (yx == 2 || yx == 6) {
               block = BASE_GRAY;
            }

            for (int x = 0; x <= 15; x += 15) {
               this.generateBox(level, chunkBB, x, yx, 0, x, yx, 1, block, block, false);
               this.generateBox(level, chunkBB, x, yx, 6, x, yx, 9, block, block, false);
               this.generateBox(level, chunkBB, x, yx, 14, x, yx, 15, block, block, false);
            }

            this.generateBox(level, chunkBB, 1, yx, 0, 1, yx, 0, block, block, false);
            this.generateBox(level, chunkBB, 6, yx, 0, 9, yx, 0, block, block, false);
            this.generateBox(level, chunkBB, 14, yx, 0, 14, yx, 0, block, block, false);
            this.generateBox(level, chunkBB, 1, yx, 15, 14, yx, 15, block, block, false);
         }

         this.generateBox(level, chunkBB, 6, 3, 6, 9, 6, 9, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(level, chunkBB, 7, 4, 7, 8, 5, 8, Blocks.GOLD_BLOCK.defaultBlockState(), Blocks.GOLD_BLOCK.defaultBlockState(), false);

         for (int yx = 3; yx <= 6; yx += 3) {
            for (int x = 6; x <= 9; x += 3) {
               this.placeBlock(level, LAMP_BLOCK, x, yx, 6, chunkBB);
               this.placeBlock(level, LAMP_BLOCK, x, yx, 9, chunkBB);
            }
         }

         this.generateBox(level, chunkBB, 5, 1, 6, 5, 2, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 5, 1, 9, 5, 2, 9, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 10, 1, 6, 10, 2, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 10, 1, 9, 10, 2, 9, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 6, 1, 5, 6, 2, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 9, 1, 5, 9, 2, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 6, 1, 10, 6, 2, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 9, 1, 10, 9, 2, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 5, 2, 5, 5, 6, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 5, 2, 10, 5, 6, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 10, 2, 5, 10, 6, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 10, 2, 10, 10, 6, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 5, 7, 1, 5, 7, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 10, 7, 1, 10, 7, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 5, 7, 9, 5, 7, 14, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 10, 7, 9, 10, 7, 14, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 1, 7, 5, 6, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 1, 7, 10, 6, 7, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 9, 7, 5, 14, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 9, 7, 10, 14, 7, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 2, 1, 2, 2, 1, 3, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 3, 1, 2, 3, 1, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 13, 1, 2, 13, 1, 3, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 12, 1, 2, 12, 1, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 2, 1, 12, 2, 1, 13, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 3, 1, 13, 3, 1, 13, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 13, 1, 12, 13, 1, 13, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 12, 1, 13, 12, 1, 13, BASE_LIGHT, BASE_LIGHT, false);
      }
   }

   public static class OceanMonumentDoubleXRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentDoubleXRoom(final Direction orientation, final OceanMonumentPieces.RoomDefinition definition) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_X_ROOM, 1, orientation, definition, 2, 1, 1);
      }

      public OceanMonumentDoubleXRoom(final CompoundTag tag) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_X_ROOM, tag);
      }

      @Override
      public void postProcess(
         final WorldGenLevel level,
         final StructureManager structureManager,
         final ChunkGenerator generator,
         final RandomSource random,
         final BoundingBox chunkBB,
         final ChunkPos chunkPos,
         final BlockPos referencePos
      ) {
         OceanMonumentPieces.RoomDefinition east = this.roomDefinition.connections[Direction.EAST.get3DDataValue()];
         OceanMonumentPieces.RoomDefinition west = this.roomDefinition;
         if (this.roomDefinition.index / 25 > 0) {
            this.generateDefaultFloor(level, chunkBB, 8, 0, east.hasOpening[Direction.DOWN.get3DDataValue()]);
            this.generateDefaultFloor(level, chunkBB, 0, 0, west.hasOpening[Direction.DOWN.get3DDataValue()]);
         }

         if (west.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(level, chunkBB, 1, 4, 1, 7, 4, 6, BASE_GRAY);
         }

         if (east.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(level, chunkBB, 8, 4, 1, 14, 4, 6, BASE_GRAY);
         }

         this.generateBox(level, chunkBB, 0, 3, 0, 0, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 15, 3, 0, 15, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 1, 3, 0, 15, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 1, 3, 7, 14, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 0, 2, 0, 0, 2, 7, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(level, chunkBB, 15, 2, 0, 15, 2, 7, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(level, chunkBB, 1, 2, 0, 15, 2, 0, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(level, chunkBB, 1, 2, 7, 14, 2, 7, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(level, chunkBB, 0, 1, 0, 0, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 15, 1, 0, 15, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 1, 1, 0, 15, 1, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 1, 1, 7, 14, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 5, 1, 0, 10, 1, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 6, 2, 0, 9, 2, 3, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(level, chunkBB, 5, 3, 0, 10, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.placeBlock(level, LAMP_BLOCK, 6, 2, 3, chunkBB);
         this.placeBlock(level, LAMP_BLOCK, 9, 2, 3, chunkBB);
         if (west.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 3, 1, 0, 4, 2, 0);
         }

         if (west.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 3, 1, 7, 4, 2, 7);
         }

         if (west.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 0, 1, 3, 0, 2, 4);
         }

         if (east.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 11, 1, 0, 12, 2, 0);
         }

         if (east.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 11, 1, 7, 12, 2, 7);
         }

         if (east.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 15, 1, 3, 15, 2, 4);
         }
      }
   }

   public static class OceanMonumentDoubleXYRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentDoubleXYRoom(final Direction orientation, final OceanMonumentPieces.RoomDefinition definition) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_XY_ROOM, 1, orientation, definition, 2, 2, 1);
      }

      public OceanMonumentDoubleXYRoom(final CompoundTag tag) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_XY_ROOM, tag);
      }

      @Override
      public void postProcess(
         final WorldGenLevel level,
         final StructureManager structureManager,
         final ChunkGenerator generator,
         final RandomSource random,
         final BoundingBox chunkBB,
         final ChunkPos chunkPos,
         final BlockPos referencePos
      ) {
         OceanMonumentPieces.RoomDefinition east = this.roomDefinition.connections[Direction.EAST.get3DDataValue()];
         OceanMonumentPieces.RoomDefinition west = this.roomDefinition;
         OceanMonumentPieces.RoomDefinition westUp = west.connections[Direction.UP.get3DDataValue()];
         OceanMonumentPieces.RoomDefinition eastUp = east.connections[Direction.UP.get3DDataValue()];
         if (this.roomDefinition.index / 25 > 0) {
            this.generateDefaultFloor(level, chunkBB, 8, 0, east.hasOpening[Direction.DOWN.get3DDataValue()]);
            this.generateDefaultFloor(level, chunkBB, 0, 0, west.hasOpening[Direction.DOWN.get3DDataValue()]);
         }

         if (westUp.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(level, chunkBB, 1, 8, 1, 7, 8, 6, BASE_GRAY);
         }

         if (eastUp.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(level, chunkBB, 8, 8, 1, 14, 8, 6, BASE_GRAY);
         }

         for (int y = 1; y <= 7; y++) {
            BlockState block = BASE_LIGHT;
            if (y == 2 || y == 6) {
               block = BASE_GRAY;
            }

            this.generateBox(level, chunkBB, 0, y, 0, 0, y, 7, block, block, false);
            this.generateBox(level, chunkBB, 15, y, 0, 15, y, 7, block, block, false);
            this.generateBox(level, chunkBB, 1, y, 0, 15, y, 0, block, block, false);
            this.generateBox(level, chunkBB, 1, y, 7, 14, y, 7, block, block, false);
         }

         this.generateBox(level, chunkBB, 2, 1, 3, 2, 7, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 3, 1, 2, 4, 7, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 3, 1, 5, 4, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 13, 1, 3, 13, 7, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 11, 1, 2, 12, 7, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 11, 1, 5, 12, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 5, 1, 3, 5, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 10, 1, 3, 10, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 5, 7, 2, 10, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 5, 5, 2, 5, 7, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 10, 5, 2, 10, 7, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 5, 5, 5, 5, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 10, 5, 5, 10, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.placeBlock(level, BASE_LIGHT, 6, 6, 2, chunkBB);
         this.placeBlock(level, BASE_LIGHT, 9, 6, 2, chunkBB);
         this.placeBlock(level, BASE_LIGHT, 6, 6, 5, chunkBB);
         this.placeBlock(level, BASE_LIGHT, 9, 6, 5, chunkBB);
         this.generateBox(level, chunkBB, 5, 4, 3, 6, 4, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 9, 4, 3, 10, 4, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.placeBlock(level, LAMP_BLOCK, 5, 4, 2, chunkBB);
         this.placeBlock(level, LAMP_BLOCK, 5, 4, 5, chunkBB);
         this.placeBlock(level, LAMP_BLOCK, 10, 4, 2, chunkBB);
         this.placeBlock(level, LAMP_BLOCK, 10, 4, 5, chunkBB);
         if (west.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 3, 1, 0, 4, 2, 0);
         }

         if (west.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 3, 1, 7, 4, 2, 7);
         }

         if (west.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 0, 1, 3, 0, 2, 4);
         }

         if (east.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 11, 1, 0, 12, 2, 0);
         }

         if (east.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 11, 1, 7, 12, 2, 7);
         }

         if (east.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 15, 1, 3, 15, 2, 4);
         }

         if (westUp.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 3, 5, 0, 4, 6, 0);
         }

         if (westUp.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 3, 5, 7, 4, 6, 7);
         }

         if (westUp.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 0, 5, 3, 0, 6, 4);
         }

         if (eastUp.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 11, 5, 0, 12, 6, 0);
         }

         if (eastUp.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 11, 5, 7, 12, 6, 7);
         }

         if (eastUp.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 15, 5, 3, 15, 6, 4);
         }
      }
   }

   public static class OceanMonumentDoubleYRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentDoubleYRoom(final Direction orientation, final OceanMonumentPieces.RoomDefinition definition) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_Y_ROOM, 1, orientation, definition, 1, 2, 1);
      }

      public OceanMonumentDoubleYRoom(final CompoundTag tag) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_Y_ROOM, tag);
      }

      @Override
      public void postProcess(
         final WorldGenLevel level,
         final StructureManager structureManager,
         final ChunkGenerator generator,
         final RandomSource random,
         final BoundingBox chunkBB,
         final ChunkPos chunkPos,
         final BlockPos referencePos
      ) {
         if (this.roomDefinition.index / 25 > 0) {
            this.generateDefaultFloor(level, chunkBB, 0, 0, this.roomDefinition.hasOpening[Direction.DOWN.get3DDataValue()]);
         }

         OceanMonumentPieces.RoomDefinition above = this.roomDefinition.connections[Direction.UP.get3DDataValue()];
         if (above.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(level, chunkBB, 1, 8, 1, 6, 8, 6, BASE_GRAY);
         }

         this.generateBox(level, chunkBB, 0, 4, 0, 0, 4, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 7, 4, 0, 7, 4, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 1, 4, 0, 6, 4, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 1, 4, 7, 6, 4, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 2, 4, 1, 2, 4, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 1, 4, 2, 1, 4, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 5, 4, 1, 5, 4, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 6, 4, 2, 6, 4, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 2, 4, 5, 2, 4, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 1, 4, 5, 1, 4, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 5, 4, 5, 5, 4, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 6, 4, 5, 6, 4, 5, BASE_LIGHT, BASE_LIGHT, false);
         OceanMonumentPieces.RoomDefinition definition = this.roomDefinition;

         for (int y = 1; y <= 5; y += 4) {
            int z = 0;
            if (definition.hasOpening[Direction.SOUTH.get3DDataValue()]) {
               this.generateBox(level, chunkBB, 2, y, z, 2, y + 2, z, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, 5, y, z, 5, y + 2, z, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, 3, y + 2, z, 4, y + 2, z, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(level, chunkBB, 0, y, z, 7, y + 2, z, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, 0, y + 1, z, 7, y + 1, z, BASE_GRAY, BASE_GRAY, false);
            }

            int var13 = 7;
            if (definition.hasOpening[Direction.NORTH.get3DDataValue()]) {
               this.generateBox(level, chunkBB, 2, y, var13, 2, y + 2, var13, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, 5, y, var13, 5, y + 2, var13, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, 3, y + 2, var13, 4, y + 2, var13, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(level, chunkBB, 0, y, var13, 7, y + 2, var13, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, 0, y + 1, var13, 7, y + 1, var13, BASE_GRAY, BASE_GRAY, false);
            }

            int x = 0;
            if (definition.hasOpening[Direction.WEST.get3DDataValue()]) {
               this.generateBox(level, chunkBB, x, y, 2, x, y + 2, 2, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, x, y, 5, x, y + 2, 5, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, x, y + 2, 3, x, y + 2, 4, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(level, chunkBB, x, y, 0, x, y + 2, 7, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, x, y + 1, 0, x, y + 1, 7, BASE_GRAY, BASE_GRAY, false);
            }

            int var14 = 7;
            if (definition.hasOpening[Direction.EAST.get3DDataValue()]) {
               this.generateBox(level, chunkBB, var14, y, 2, var14, y + 2, 2, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, var14, y, 5, var14, y + 2, 5, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, var14, y + 2, 3, var14, y + 2, 4, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(level, chunkBB, var14, y, 0, var14, y + 2, 7, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, var14, y + 1, 0, var14, y + 1, 7, BASE_GRAY, BASE_GRAY, false);
            }

            definition = above;
         }
      }
   }

   public static class OceanMonumentDoubleYZRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentDoubleYZRoom(final Direction orientation, final OceanMonumentPieces.RoomDefinition definition) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_YZ_ROOM, 1, orientation, definition, 1, 2, 2);
      }

      public OceanMonumentDoubleYZRoom(final CompoundTag tag) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_YZ_ROOM, tag);
      }

      @Override
      public void postProcess(
         final WorldGenLevel level,
         final StructureManager structureManager,
         final ChunkGenerator generator,
         final RandomSource random,
         final BoundingBox chunkBB,
         final ChunkPos chunkPos,
         final BlockPos referencePos
      ) {
         OceanMonumentPieces.RoomDefinition north = this.roomDefinition.connections[Direction.NORTH.get3DDataValue()];
         OceanMonumentPieces.RoomDefinition south = this.roomDefinition;
         OceanMonumentPieces.RoomDefinition northUp = north.connections[Direction.UP.get3DDataValue()];
         OceanMonumentPieces.RoomDefinition southUp = south.connections[Direction.UP.get3DDataValue()];
         if (this.roomDefinition.index / 25 > 0) {
            this.generateDefaultFloor(level, chunkBB, 0, 8, north.hasOpening[Direction.DOWN.get3DDataValue()]);
            this.generateDefaultFloor(level, chunkBB, 0, 0, south.hasOpening[Direction.DOWN.get3DDataValue()]);
         }

         if (southUp.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(level, chunkBB, 1, 8, 1, 6, 8, 7, BASE_GRAY);
         }

         if (northUp.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(level, chunkBB, 1, 8, 8, 6, 8, 14, BASE_GRAY);
         }

         for (int y = 1; y <= 7; y++) {
            BlockState block = BASE_LIGHT;
            if (y == 2 || y == 6) {
               block = BASE_GRAY;
            }

            this.generateBox(level, chunkBB, 0, y, 0, 0, y, 15, block, block, false);
            this.generateBox(level, chunkBB, 7, y, 0, 7, y, 15, block, block, false);
            this.generateBox(level, chunkBB, 1, y, 0, 6, y, 0, block, block, false);
            this.generateBox(level, chunkBB, 1, y, 15, 6, y, 15, block, block, false);
         }

         for (int y = 1; y <= 7; y++) {
            BlockState block = BASE_BLACK;
            if (y == 2 || y == 6) {
               block = LAMP_BLOCK;
            }

            this.generateBox(level, chunkBB, 3, y, 7, 4, y, 8, block, block, false);
         }

         if (south.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 3, 1, 0, 4, 2, 0);
         }

         if (south.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 7, 1, 3, 7, 2, 4);
         }

         if (south.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 0, 1, 3, 0, 2, 4);
         }

         if (north.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 3, 1, 15, 4, 2, 15);
         }

         if (north.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 0, 1, 11, 0, 2, 12);
         }

         if (north.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 7, 1, 11, 7, 2, 12);
         }

         if (southUp.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 3, 5, 0, 4, 6, 0);
         }

         if (southUp.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 7, 5, 3, 7, 6, 4);
            this.generateBox(level, chunkBB, 5, 4, 2, 6, 4, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 6, 1, 2, 6, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 6, 1, 5, 6, 3, 5, BASE_LIGHT, BASE_LIGHT, false);
         }

         if (southUp.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 0, 5, 3, 0, 6, 4);
            this.generateBox(level, chunkBB, 1, 4, 2, 2, 4, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 1, 1, 2, 1, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 1, 1, 5, 1, 3, 5, BASE_LIGHT, BASE_LIGHT, false);
         }

         if (northUp.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 3, 5, 15, 4, 6, 15);
         }

         if (northUp.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 0, 5, 11, 0, 6, 12);
            this.generateBox(level, chunkBB, 1, 4, 10, 2, 4, 13, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 1, 1, 10, 1, 3, 10, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 1, 1, 13, 1, 3, 13, BASE_LIGHT, BASE_LIGHT, false);
         }

         if (northUp.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 7, 5, 11, 7, 6, 12);
            this.generateBox(level, chunkBB, 5, 4, 10, 6, 4, 13, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 6, 1, 10, 6, 3, 10, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 6, 1, 13, 6, 3, 13, BASE_LIGHT, BASE_LIGHT, false);
         }
      }
   }

   public static class OceanMonumentDoubleZRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentDoubleZRoom(final Direction orientation, final OceanMonumentPieces.RoomDefinition definition) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_Z_ROOM, 1, orientation, definition, 1, 1, 2);
      }

      public OceanMonumentDoubleZRoom(final CompoundTag tag) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_Z_ROOM, tag);
      }

      @Override
      public void postProcess(
         final WorldGenLevel level,
         final StructureManager structureManager,
         final ChunkGenerator generator,
         final RandomSource random,
         final BoundingBox chunkBB,
         final ChunkPos chunkPos,
         final BlockPos referencePos
      ) {
         OceanMonumentPieces.RoomDefinition north = this.roomDefinition.connections[Direction.NORTH.get3DDataValue()];
         OceanMonumentPieces.RoomDefinition south = this.roomDefinition;
         if (this.roomDefinition.index / 25 > 0) {
            this.generateDefaultFloor(level, chunkBB, 0, 8, north.hasOpening[Direction.DOWN.get3DDataValue()]);
            this.generateDefaultFloor(level, chunkBB, 0, 0, south.hasOpening[Direction.DOWN.get3DDataValue()]);
         }

         if (south.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(level, chunkBB, 1, 4, 1, 6, 4, 7, BASE_GRAY);
         }

         if (north.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(level, chunkBB, 1, 4, 8, 6, 4, 14, BASE_GRAY);
         }

         this.generateBox(level, chunkBB, 0, 3, 0, 0, 3, 15, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 7, 3, 0, 7, 3, 15, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 1, 3, 0, 7, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 1, 3, 15, 6, 3, 15, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 0, 2, 0, 0, 2, 15, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(level, chunkBB, 7, 2, 0, 7, 2, 15, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(level, chunkBB, 1, 2, 0, 7, 2, 0, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(level, chunkBB, 1, 2, 15, 6, 2, 15, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(level, chunkBB, 0, 1, 0, 0, 1, 15, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 7, 1, 0, 7, 1, 15, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 1, 1, 0, 7, 1, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 1, 1, 15, 6, 1, 15, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 1, 1, 1, 1, 1, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 6, 1, 1, 6, 1, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 1, 3, 1, 1, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 6, 3, 1, 6, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 1, 1, 13, 1, 1, 14, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 6, 1, 13, 6, 1, 14, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 1, 3, 13, 1, 3, 14, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 6, 3, 13, 6, 3, 14, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 2, 1, 6, 2, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 5, 1, 6, 5, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 2, 1, 9, 2, 3, 9, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 5, 1, 9, 5, 3, 9, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 3, 2, 6, 4, 2, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 3, 2, 9, 4, 2, 9, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 2, 2, 7, 2, 2, 8, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 5, 2, 7, 5, 2, 8, BASE_LIGHT, BASE_LIGHT, false);
         this.placeBlock(level, LAMP_BLOCK, 2, 2, 5, chunkBB);
         this.placeBlock(level, LAMP_BLOCK, 5, 2, 5, chunkBB);
         this.placeBlock(level, LAMP_BLOCK, 2, 2, 10, chunkBB);
         this.placeBlock(level, LAMP_BLOCK, 5, 2, 10, chunkBB);
         this.placeBlock(level, BASE_LIGHT, 2, 3, 5, chunkBB);
         this.placeBlock(level, BASE_LIGHT, 5, 3, 5, chunkBB);
         this.placeBlock(level, BASE_LIGHT, 2, 3, 10, chunkBB);
         this.placeBlock(level, BASE_LIGHT, 5, 3, 10, chunkBB);
         if (south.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 3, 1, 0, 4, 2, 0);
         }

         if (south.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 7, 1, 3, 7, 2, 4);
         }

         if (south.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 0, 1, 3, 0, 2, 4);
         }

         if (north.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 3, 1, 15, 4, 2, 15);
         }

         if (north.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 0, 1, 11, 0, 2, 12);
         }

         if (north.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 7, 1, 11, 7, 2, 12);
         }
      }
   }

   public static class OceanMonumentEntryRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentEntryRoom(final Direction orientation, final OceanMonumentPieces.RoomDefinition definition) {
         super(StructurePieceType.OCEAN_MONUMENT_ENTRY_ROOM, 1, orientation, definition, 1, 1, 1);
      }

      public OceanMonumentEntryRoom(final CompoundTag tag) {
         super(StructurePieceType.OCEAN_MONUMENT_ENTRY_ROOM, tag);
      }

      @Override
      public void postProcess(
         final WorldGenLevel level,
         final StructureManager structureManager,
         final ChunkGenerator generator,
         final RandomSource random,
         final BoundingBox chunkBB,
         final ChunkPos chunkPos,
         final BlockPos referencePos
      ) {
         this.generateBox(level, chunkBB, 0, 3, 0, 2, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 5, 3, 0, 7, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 0, 2, 0, 1, 2, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 6, 2, 0, 7, 2, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 0, 1, 0, 0, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 7, 1, 0, 7, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 0, 1, 7, 7, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 1, 1, 0, 2, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 5, 1, 0, 6, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
         if (this.roomDefinition.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 3, 1, 7, 4, 2, 7);
         }

         if (this.roomDefinition.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 0, 1, 3, 1, 2, 4);
         }

         if (this.roomDefinition.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 6, 1, 3, 7, 2, 4);
         }
      }
   }

   public static class OceanMonumentPenthouse extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentPenthouse(final Direction orientation, final BoundingBox boundingBox) {
         super(StructurePieceType.OCEAN_MONUMENT_PENTHOUSE, orientation, 1, boundingBox);
      }

      public OceanMonumentPenthouse(final CompoundTag tag) {
         super(StructurePieceType.OCEAN_MONUMENT_PENTHOUSE, tag);
      }

      @Override
      public void postProcess(
         final WorldGenLevel level,
         final StructureManager structureManager,
         final ChunkGenerator generator,
         final RandomSource random,
         final BoundingBox chunkBB,
         final ChunkPos chunkPos,
         final BlockPos referencePos
      ) {
         this.generateBox(level, chunkBB, 2, -1, 2, 11, -1, 11, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 0, -1, 0, 1, -1, 11, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(level, chunkBB, 12, -1, 0, 13, -1, 11, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(level, chunkBB, 2, -1, 0, 11, -1, 1, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(level, chunkBB, 2, -1, 12, 11, -1, 13, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(level, chunkBB, 0, 0, 0, 0, 0, 13, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 13, 0, 0, 13, 0, 13, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 1, 0, 0, 12, 0, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 1, 0, 13, 12, 0, 13, BASE_LIGHT, BASE_LIGHT, false);

         for (int i = 2; i <= 11; i += 3) {
            this.placeBlock(level, LAMP_BLOCK, 0, 0, i, chunkBB);
            this.placeBlock(level, LAMP_BLOCK, 13, 0, i, chunkBB);
            this.placeBlock(level, LAMP_BLOCK, i, 0, 0, chunkBB);
         }

         this.generateBox(level, chunkBB, 2, 0, 3, 4, 0, 9, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 9, 0, 3, 11, 0, 9, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 4, 0, 9, 9, 0, 11, BASE_LIGHT, BASE_LIGHT, false);
         this.placeBlock(level, BASE_LIGHT, 5, 0, 8, chunkBB);
         this.placeBlock(level, BASE_LIGHT, 8, 0, 8, chunkBB);
         this.placeBlock(level, BASE_LIGHT, 10, 0, 10, chunkBB);
         this.placeBlock(level, BASE_LIGHT, 3, 0, 10, chunkBB);
         this.generateBox(level, chunkBB, 3, 0, 3, 3, 0, 7, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(level, chunkBB, 10, 0, 3, 10, 0, 7, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(level, chunkBB, 6, 0, 10, 7, 0, 10, BASE_BLACK, BASE_BLACK, false);
         int x = 3;

         for (int i = 0; i < 2; i++) {
            for (int z = 2; z <= 8; z += 3) {
               this.generateBox(level, chunkBB, x, 0, z, x, 2, z, BASE_LIGHT, BASE_LIGHT, false);
            }

            x = 10;
         }

         this.generateBox(level, chunkBB, 5, 0, 10, 5, 2, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 8, 0, 10, 8, 2, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 6, -1, 7, 7, -1, 8, BASE_BLACK, BASE_BLACK, false);
         this.generateWaterBox(level, chunkBB, 6, -1, 3, 7, -1, 4);
         this.spawnElder(level, chunkBB, 6, 1, 6);
      }
   }

   protected abstract static class OceanMonumentPiece extends StructurePiece {
      protected static final BlockState BASE_GRAY = Blocks.PRISMARINE.defaultBlockState();
      protected static final BlockState BASE_LIGHT = Blocks.PRISMARINE_BRICKS.defaultBlockState();
      protected static final BlockState BASE_BLACK = Blocks.DARK_PRISMARINE.defaultBlockState();
      protected static final BlockState DOT_DECO_DATA = BASE_LIGHT;
      protected static final BlockState LAMP_BLOCK = Blocks.SEA_LANTERN.defaultBlockState();
      protected static final boolean DO_FILL = true;
      protected static final BlockState FILL_BLOCK = Blocks.WATER.defaultBlockState();
      protected static final Set<Block> FILL_KEEP = ImmutableSet.builder()
         .add(Blocks.ICE)
         .add(Blocks.PACKED_ICE)
         .add(Blocks.BLUE_ICE)
         .add(FILL_BLOCK.getBlock())
         .build();
      protected static final int GRIDROOM_WIDTH = 8;
      protected static final int GRIDROOM_DEPTH = 8;
      protected static final int GRIDROOM_HEIGHT = 4;
      protected static final int GRID_WIDTH = 5;
      protected static final int GRID_DEPTH = 5;
      protected static final int GRID_HEIGHT = 3;
      protected static final int GRID_FLOOR_COUNT = 25;
      protected static final int GRID_SIZE = 75;
      protected static final int GRIDROOM_SOURCE_INDEX = getRoomIndex(2, 0, 0);
      protected static final int GRIDROOM_TOP_CONNECT_INDEX = getRoomIndex(2, 2, 0);
      protected static final int GRIDROOM_LEFTWING_CONNECT_INDEX = getRoomIndex(0, 1, 0);
      protected static final int GRIDROOM_RIGHTWING_CONNECT_INDEX = getRoomIndex(4, 1, 0);
      protected static final int LEFTWING_INDEX = 1001;
      protected static final int RIGHTWING_INDEX = 1002;
      protected static final int PENTHOUSE_INDEX = 1003;
      protected OceanMonumentPieces.RoomDefinition roomDefinition;

      protected static int getRoomIndex(final int roomX, final int roomY, final int roomZ) {
         return roomY * 25 + roomZ * 5 + roomX;
      }

      public OceanMonumentPiece(final StructurePieceType type, final Direction orientation, final int genDepth, final BoundingBox boundingBox) {
         super(type, genDepth, boundingBox);
         this.setOrientation(orientation);
      }

      protected OceanMonumentPiece(
         final StructurePieceType type,
         final int genDepth,
         final Direction orientation,
         final OceanMonumentPieces.RoomDefinition roomDefinition,
         final int roomWidth,
         final int roomHeight,
         final int roomDepth
      ) {
         super(type, genDepth, makeBoundingBox(orientation, roomDefinition, roomWidth, roomHeight, roomDepth));
         this.setOrientation(orientation);
         this.roomDefinition = roomDefinition;
      }

      private static BoundingBox makeBoundingBox(
         final Direction orientation, final OceanMonumentPieces.RoomDefinition roomDefinition, final int roomWidth, final int roomHeight, final int roomDepth
      ) {
         int roomIndex = roomDefinition.index;
         int roomX = roomIndex % 5;
         int roomZ = roomIndex / 5 % 5;
         int roomY = roomIndex / 25;
         BoundingBox boundingBox = makeBoundingBox(0, 0, 0, orientation, roomWidth * 8, roomHeight * 4, roomDepth * 8);
         switch (orientation) {
            case NORTH:
               boundingBox.move(roomX * 8, roomY * 4, -(roomZ + roomDepth) * 8 + 1);
               break;
            case SOUTH:
               boundingBox.move(roomX * 8, roomY * 4, roomZ * 8);
               break;
            case WEST:
               boundingBox.move(-(roomZ + roomDepth) * 8 + 1, roomY * 4, roomX * 8);
               break;
            case EAST:
            default:
               boundingBox.move(roomZ * 8, roomY * 4, roomX * 8);
         }

         return boundingBox;
      }

      public OceanMonumentPiece(final StructurePieceType type, final CompoundTag tag) {
         super(type, tag);
      }

      @Override
      protected void addAdditionalSaveData(final StructurePieceSerializationContext context, final CompoundTag tag) {
      }

      protected void generateWaterBox(
         final WorldGenLevel level, final BoundingBox chunkBB, final int x0, final int y0, final int z0, final int x1, final int y1, final int z1
      ) {
         for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
               for (int z = z0; z <= z1; z++) {
                  BlockState block = this.getBlock(level, x, y, z, chunkBB);
                  if (!FILL_KEEP.contains(block.getBlock())) {
                     if (this.getWorldY(y) >= level.getSeaLevel() && block != FILL_BLOCK) {
                        this.placeBlock(level, Blocks.AIR.defaultBlockState(), x, y, z, chunkBB);
                     } else {
                        this.placeBlock(level, FILL_BLOCK, x, y, z, chunkBB);
                     }
                  }
               }
            }
         }
      }

      protected void generateDefaultFloor(final WorldGenLevel level, final BoundingBox chunkBB, final int xOff, final int zOff, final boolean downOpening) {
         if (downOpening) {
            this.generateBox(level, chunkBB, xOff + 0, 0, zOff + 0, xOff + 2, 0, zOff + 8 - 1, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, xOff + 5, 0, zOff + 0, xOff + 8 - 1, 0, zOff + 8 - 1, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, xOff + 3, 0, zOff + 0, xOff + 4, 0, zOff + 2, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, xOff + 3, 0, zOff + 5, xOff + 4, 0, zOff + 8 - 1, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, xOff + 3, 0, zOff + 2, xOff + 4, 0, zOff + 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, xOff + 3, 0, zOff + 5, xOff + 4, 0, zOff + 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, xOff + 2, 0, zOff + 3, xOff + 2, 0, zOff + 4, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, xOff + 5, 0, zOff + 3, xOff + 5, 0, zOff + 4, BASE_LIGHT, BASE_LIGHT, false);
         } else {
            this.generateBox(level, chunkBB, xOff + 0, 0, zOff + 0, xOff + 8 - 1, 0, zOff + 8 - 1, BASE_GRAY, BASE_GRAY, false);
         }
      }

      protected void generateBoxOnFillOnly(
         final WorldGenLevel level,
         final BoundingBox chunkBB,
         final int x0,
         final int y0,
         final int z0,
         final int x1,
         final int y1,
         final int z1,
         final BlockState targetBlock
      ) {
         for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
               for (int z = z0; z <= z1; z++) {
                  if (this.getBlock(level, x, y, z, chunkBB) == FILL_BLOCK) {
                     this.placeBlock(level, targetBlock, x, y, z, chunkBB);
                  }
               }
            }
         }
      }

      protected boolean chunkIntersects(final BoundingBox chunkBB, final int x0, final int z0, final int x1, final int z1) {
         int wx0 = this.getWorldX(x0, z0);
         int wz0 = this.getWorldZ(x0, z0);
         int wx1 = this.getWorldX(x1, z1);
         int wz1 = this.getWorldZ(x1, z1);
         return chunkBB.intersects(Math.min(wx0, wx1), Math.min(wz0, wz1), Math.max(wx0, wx1), Math.max(wz0, wz1));
      }

      protected void spawnElder(final WorldGenLevel level, final BoundingBox chunkBB, final int x, final int y, final int z) {
         BlockPos pos = this.getWorldPos(x, y, z);
         if (chunkBB.isInside(pos)) {
            ElderGuardian elder = EntityTypes.ELDER_GUARDIAN.create(level.getLevel(), EntitySpawnReason.STRUCTURE);
            if (elder != null) {
               elder.heal(elder.getMaxHealth());
               elder.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, 0.0F, 0.0F);
               elder.finalizeSpawn(level, level.getCurrentDifficultyAt(elder.blockPosition()), EntitySpawnReason.STRUCTURE, null);
               level.addFreshEntityWithPassengers(elder);
            }
         }
      }
   }

   public static class OceanMonumentSimpleRoom extends OceanMonumentPieces.OceanMonumentPiece {
      private int mainDesign;

      public OceanMonumentSimpleRoom(final Direction orientation, final OceanMonumentPieces.RoomDefinition definition, final RandomSource random) {
         super(StructurePieceType.OCEAN_MONUMENT_SIMPLE_ROOM, 1, orientation, definition, 1, 1, 1);
         this.mainDesign = random.nextInt(3);
      }

      public OceanMonumentSimpleRoom(final CompoundTag tag) {
         super(StructurePieceType.OCEAN_MONUMENT_SIMPLE_ROOM, tag);
      }

      @Override
      public void postProcess(
         final WorldGenLevel level,
         final StructureManager structureManager,
         final ChunkGenerator generator,
         final RandomSource random,
         final BoundingBox chunkBB,
         final ChunkPos chunkPos,
         final BlockPos referencePos
      ) {
         if (this.roomDefinition.index / 25 > 0) {
            this.generateDefaultFloor(level, chunkBB, 0, 0, this.roomDefinition.hasOpening[Direction.DOWN.get3DDataValue()]);
         }

         if (this.roomDefinition.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(level, chunkBB, 1, 4, 1, 6, 4, 6, BASE_GRAY);
         }

         boolean centerPillar = this.mainDesign != 0
            && random.nextBoolean()
            && !this.roomDefinition.hasOpening[Direction.DOWN.get3DDataValue()]
            && !this.roomDefinition.hasOpening[Direction.UP.get3DDataValue()]
            && this.roomDefinition.countOpenings() > 1;
         if (this.mainDesign == 0) {
            this.generateBox(level, chunkBB, 0, 1, 0, 2, 1, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 0, 3, 0, 2, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 0, 2, 0, 0, 2, 2, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, 1, 2, 0, 2, 2, 0, BASE_GRAY, BASE_GRAY, false);
            this.placeBlock(level, LAMP_BLOCK, 1, 2, 1, chunkBB);
            this.generateBox(level, chunkBB, 5, 1, 0, 7, 1, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 5, 3, 0, 7, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 7, 2, 0, 7, 2, 2, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, 5, 2, 0, 6, 2, 0, BASE_GRAY, BASE_GRAY, false);
            this.placeBlock(level, LAMP_BLOCK, 6, 2, 1, chunkBB);
            this.generateBox(level, chunkBB, 0, 1, 5, 2, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 0, 3, 5, 2, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 0, 2, 5, 0, 2, 7, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, 1, 2, 7, 2, 2, 7, BASE_GRAY, BASE_GRAY, false);
            this.placeBlock(level, LAMP_BLOCK, 1, 2, 6, chunkBB);
            this.generateBox(level, chunkBB, 5, 1, 5, 7, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 5, 3, 5, 7, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 7, 2, 5, 7, 2, 7, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, 5, 2, 7, 6, 2, 7, BASE_GRAY, BASE_GRAY, false);
            this.placeBlock(level, LAMP_BLOCK, 6, 2, 6, chunkBB);
            if (this.roomDefinition.hasOpening[Direction.SOUTH.get3DDataValue()]) {
               this.generateBox(level, chunkBB, 3, 3, 0, 4, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(level, chunkBB, 3, 3, 0, 4, 3, 1, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, 3, 2, 0, 4, 2, 0, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(level, chunkBB, 3, 1, 0, 4, 1, 1, BASE_LIGHT, BASE_LIGHT, false);
            }

            if (this.roomDefinition.hasOpening[Direction.NORTH.get3DDataValue()]) {
               this.generateBox(level, chunkBB, 3, 3, 7, 4, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(level, chunkBB, 3, 3, 6, 4, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, 3, 2, 7, 4, 2, 7, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(level, chunkBB, 3, 1, 6, 4, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            }

            if (this.roomDefinition.hasOpening[Direction.WEST.get3DDataValue()]) {
               this.generateBox(level, chunkBB, 0, 3, 3, 0, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(level, chunkBB, 0, 3, 3, 1, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, 0, 2, 3, 0, 2, 4, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(level, chunkBB, 0, 1, 3, 1, 1, 4, BASE_LIGHT, BASE_LIGHT, false);
            }

            if (this.roomDefinition.hasOpening[Direction.EAST.get3DDataValue()]) {
               this.generateBox(level, chunkBB, 7, 3, 3, 7, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(level, chunkBB, 6, 3, 3, 7, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, 7, 2, 3, 7, 2, 4, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(level, chunkBB, 6, 1, 3, 7, 1, 4, BASE_LIGHT, BASE_LIGHT, false);
            }
         } else if (this.mainDesign == 1) {
            this.generateBox(level, chunkBB, 2, 1, 2, 2, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 2, 1, 5, 2, 3, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 5, 1, 5, 5, 3, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 5, 1, 2, 5, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.placeBlock(level, LAMP_BLOCK, 2, 2, 2, chunkBB);
            this.placeBlock(level, LAMP_BLOCK, 2, 2, 5, chunkBB);
            this.placeBlock(level, LAMP_BLOCK, 5, 2, 5, chunkBB);
            this.placeBlock(level, LAMP_BLOCK, 5, 2, 2, chunkBB);
            this.generateBox(level, chunkBB, 0, 1, 0, 1, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 0, 1, 1, 0, 3, 1, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 0, 1, 7, 1, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 0, 1, 6, 0, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 6, 1, 7, 7, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 7, 1, 6, 7, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 6, 1, 0, 7, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 7, 1, 1, 7, 3, 1, BASE_LIGHT, BASE_LIGHT, false);
            this.placeBlock(level, BASE_GRAY, 1, 2, 0, chunkBB);
            this.placeBlock(level, BASE_GRAY, 0, 2, 1, chunkBB);
            this.placeBlock(level, BASE_GRAY, 1, 2, 7, chunkBB);
            this.placeBlock(level, BASE_GRAY, 0, 2, 6, chunkBB);
            this.placeBlock(level, BASE_GRAY, 6, 2, 7, chunkBB);
            this.placeBlock(level, BASE_GRAY, 7, 2, 6, chunkBB);
            this.placeBlock(level, BASE_GRAY, 6, 2, 0, chunkBB);
            this.placeBlock(level, BASE_GRAY, 7, 2, 1, chunkBB);
            if (!this.roomDefinition.hasOpening[Direction.SOUTH.get3DDataValue()]) {
               this.generateBox(level, chunkBB, 1, 3, 0, 6, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, 1, 2, 0, 6, 2, 0, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(level, chunkBB, 1, 1, 0, 6, 1, 0, BASE_LIGHT, BASE_LIGHT, false);
            }

            if (!this.roomDefinition.hasOpening[Direction.NORTH.get3DDataValue()]) {
               this.generateBox(level, chunkBB, 1, 3, 7, 6, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, 1, 2, 7, 6, 2, 7, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(level, chunkBB, 1, 1, 7, 6, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            }

            if (!this.roomDefinition.hasOpening[Direction.WEST.get3DDataValue()]) {
               this.generateBox(level, chunkBB, 0, 3, 1, 0, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, 0, 2, 1, 0, 2, 6, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(level, chunkBB, 0, 1, 1, 0, 1, 6, BASE_LIGHT, BASE_LIGHT, false);
            }

            if (!this.roomDefinition.hasOpening[Direction.EAST.get3DDataValue()]) {
               this.generateBox(level, chunkBB, 7, 3, 1, 7, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, 7, 2, 1, 7, 2, 6, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(level, chunkBB, 7, 1, 1, 7, 1, 6, BASE_LIGHT, BASE_LIGHT, false);
            }
         } else if (this.mainDesign == 2) {
            this.generateBox(level, chunkBB, 0, 1, 0, 0, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 7, 1, 0, 7, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 1, 1, 0, 6, 1, 0, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 1, 1, 7, 6, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 0, 2, 0, 0, 2, 7, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(level, chunkBB, 7, 2, 0, 7, 2, 7, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(level, chunkBB, 1, 2, 0, 6, 2, 0, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(level, chunkBB, 1, 2, 7, 6, 2, 7, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(level, chunkBB, 0, 3, 0, 0, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 7, 3, 0, 7, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 1, 3, 0, 6, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 1, 3, 7, 6, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 0, 1, 3, 0, 2, 4, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(level, chunkBB, 7, 1, 3, 7, 2, 4, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(level, chunkBB, 3, 1, 0, 4, 2, 0, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(level, chunkBB, 3, 1, 7, 4, 2, 7, BASE_BLACK, BASE_BLACK, false);
            if (this.roomDefinition.hasOpening[Direction.SOUTH.get3DDataValue()]) {
               this.generateWaterBox(level, chunkBB, 3, 1, 0, 4, 2, 0);
            }

            if (this.roomDefinition.hasOpening[Direction.NORTH.get3DDataValue()]) {
               this.generateWaterBox(level, chunkBB, 3, 1, 7, 4, 2, 7);
            }

            if (this.roomDefinition.hasOpening[Direction.WEST.get3DDataValue()]) {
               this.generateWaterBox(level, chunkBB, 0, 1, 3, 0, 2, 4);
            }

            if (this.roomDefinition.hasOpening[Direction.EAST.get3DDataValue()]) {
               this.generateWaterBox(level, chunkBB, 7, 1, 3, 7, 2, 4);
            }
         }

         if (centerPillar) {
            this.generateBox(level, chunkBB, 3, 1, 3, 4, 1, 4, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 3, 2, 3, 4, 2, 4, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(level, chunkBB, 3, 3, 3, 4, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
         }
      }
   }

   public static class OceanMonumentSimpleTopRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentSimpleTopRoom(final Direction orientation, final OceanMonumentPieces.RoomDefinition definition) {
         super(StructurePieceType.OCEAN_MONUMENT_SIMPLE_TOP_ROOM, 1, orientation, definition, 1, 1, 1);
      }

      public OceanMonumentSimpleTopRoom(final CompoundTag tag) {
         super(StructurePieceType.OCEAN_MONUMENT_SIMPLE_TOP_ROOM, tag);
      }

      @Override
      public void postProcess(
         final WorldGenLevel level,
         final StructureManager structureManager,
         final ChunkGenerator generator,
         final RandomSource random,
         final BoundingBox chunkBB,
         final ChunkPos chunkPos,
         final BlockPos referencePos
      ) {
         if (this.roomDefinition.index / 25 > 0) {
            this.generateDefaultFloor(level, chunkBB, 0, 0, this.roomDefinition.hasOpening[Direction.DOWN.get3DDataValue()]);
         }

         if (this.roomDefinition.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(level, chunkBB, 1, 4, 1, 6, 4, 6, BASE_GRAY);
         }

         for (int x = 1; x <= 6; x++) {
            for (int z = 1; z <= 6; z++) {
               if (random.nextInt(3) != 0) {
                  int y0 = 2 + (random.nextInt(4) == 0 ? 0 : 1);
                  BlockState wetSponge = Blocks.WET_SPONGE.defaultBlockState();
                  this.generateBox(level, chunkBB, x, y0, z, x, 3, z, wetSponge, wetSponge, false);
               }
            }
         }

         this.generateBox(level, chunkBB, 0, 1, 0, 0, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 7, 1, 0, 7, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 1, 1, 0, 6, 1, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 1, 1, 7, 6, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 0, 2, 0, 0, 2, 7, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(level, chunkBB, 7, 2, 0, 7, 2, 7, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(level, chunkBB, 1, 2, 0, 6, 2, 0, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(level, chunkBB, 1, 2, 7, 6, 2, 7, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(level, chunkBB, 0, 3, 0, 0, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 7, 3, 0, 7, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 1, 3, 0, 6, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 1, 3, 7, 6, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(level, chunkBB, 0, 1, 3, 0, 2, 4, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(level, chunkBB, 7, 1, 3, 7, 2, 4, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(level, chunkBB, 3, 1, 0, 4, 2, 0, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(level, chunkBB, 3, 1, 7, 4, 2, 7, BASE_BLACK, BASE_BLACK, false);
         if (this.roomDefinition.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(level, chunkBB, 3, 1, 0, 4, 2, 0);
         }
      }
   }

   public static class OceanMonumentWingRoom extends OceanMonumentPieces.OceanMonumentPiece {
      private int mainDesign;

      public OceanMonumentWingRoom(final Direction orientation, final BoundingBox boundingBox, final int randomValue) {
         super(StructurePieceType.OCEAN_MONUMENT_WING_ROOM, orientation, 1, boundingBox);
         this.mainDesign = randomValue & 1;
      }

      public OceanMonumentWingRoom(final CompoundTag tag) {
         super(StructurePieceType.OCEAN_MONUMENT_WING_ROOM, tag);
      }

      @Override
      public void postProcess(
         final WorldGenLevel level,
         final StructureManager structureManager,
         final ChunkGenerator generator,
         final RandomSource random,
         final BoundingBox chunkBB,
         final ChunkPos chunkPos,
         final BlockPos referencePos
      ) {
         if (this.mainDesign == 0) {
            for (int i = 0; i < 4; i++) {
               this.generateBox(level, chunkBB, 10 - i, 3 - i, 20 - i, 12 + i, 3 - i, 20, BASE_LIGHT, BASE_LIGHT, false);
            }

            this.generateBox(level, chunkBB, 7, 0, 6, 15, 0, 16, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 6, 0, 6, 6, 3, 20, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 16, 0, 6, 16, 3, 20, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 7, 1, 7, 7, 1, 20, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 15, 1, 7, 15, 1, 20, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 7, 1, 6, 9, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 13, 1, 6, 15, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 8, 1, 7, 9, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 13, 1, 7, 14, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 9, 0, 5, 13, 0, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 10, 0, 7, 12, 0, 7, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(level, chunkBB, 8, 0, 10, 8, 0, 12, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(level, chunkBB, 14, 0, 10, 14, 0, 12, BASE_BLACK, BASE_BLACK, false);

            for (int z = 18; z >= 7; z -= 3) {
               this.placeBlock(level, LAMP_BLOCK, 6, 3, z, chunkBB);
               this.placeBlock(level, LAMP_BLOCK, 16, 3, z, chunkBB);
            }

            this.placeBlock(level, LAMP_BLOCK, 10, 0, 10, chunkBB);
            this.placeBlock(level, LAMP_BLOCK, 12, 0, 10, chunkBB);
            this.placeBlock(level, LAMP_BLOCK, 10, 0, 12, chunkBB);
            this.placeBlock(level, LAMP_BLOCK, 12, 0, 12, chunkBB);
            this.placeBlock(level, LAMP_BLOCK, 8, 3, 6, chunkBB);
            this.placeBlock(level, LAMP_BLOCK, 14, 3, 6, chunkBB);
            this.placeBlock(level, BASE_LIGHT, 4, 2, 4, chunkBB);
            this.placeBlock(level, LAMP_BLOCK, 4, 1, 4, chunkBB);
            this.placeBlock(level, BASE_LIGHT, 4, 0, 4, chunkBB);
            this.placeBlock(level, BASE_LIGHT, 18, 2, 4, chunkBB);
            this.placeBlock(level, LAMP_BLOCK, 18, 1, 4, chunkBB);
            this.placeBlock(level, BASE_LIGHT, 18, 0, 4, chunkBB);
            this.placeBlock(level, BASE_LIGHT, 4, 2, 18, chunkBB);
            this.placeBlock(level, LAMP_BLOCK, 4, 1, 18, chunkBB);
            this.placeBlock(level, BASE_LIGHT, 4, 0, 18, chunkBB);
            this.placeBlock(level, BASE_LIGHT, 18, 2, 18, chunkBB);
            this.placeBlock(level, LAMP_BLOCK, 18, 1, 18, chunkBB);
            this.placeBlock(level, BASE_LIGHT, 18, 0, 18, chunkBB);
            this.placeBlock(level, BASE_LIGHT, 9, 7, 20, chunkBB);
            this.placeBlock(level, BASE_LIGHT, 13, 7, 20, chunkBB);
            this.generateBox(level, chunkBB, 6, 0, 21, 7, 4, 21, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 15, 0, 21, 16, 4, 21, BASE_LIGHT, BASE_LIGHT, false);
            this.spawnElder(level, chunkBB, 11, 2, 16);
         } else if (this.mainDesign == 1) {
            this.generateBox(level, chunkBB, 9, 3, 18, 13, 3, 20, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 9, 0, 18, 9, 2, 18, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(level, chunkBB, 13, 0, 18, 13, 2, 18, BASE_LIGHT, BASE_LIGHT, false);
            int x = 9;
            int z = 20;
            int y = 5;

            for (int i = 0; i < 2; i++) {
               this.placeBlock(level, BASE_LIGHT, x, 6, 20, chunkBB);
               this.placeBlock(level, LAMP_BLOCK, x, 5, 20, chunkBB);
               this.placeBlock(level, BASE_LIGHT, x, 4, 20, chunkBB);
               x = 13;
            }

            this.generateBox(level, chunkBB, 7, 3, 7, 15, 3, 14, BASE_LIGHT, BASE_LIGHT, false);
            int var14 = 10;

            for (int i = 0; i < 2; i++) {
               this.generateBox(level, chunkBB, var14, 0, 10, var14, 6, 10, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, var14, 0, 12, var14, 6, 12, BASE_LIGHT, BASE_LIGHT, false);
               this.placeBlock(level, LAMP_BLOCK, var14, 0, 10, chunkBB);
               this.placeBlock(level, LAMP_BLOCK, var14, 0, 12, chunkBB);
               this.placeBlock(level, LAMP_BLOCK, var14, 4, 10, chunkBB);
               this.placeBlock(level, LAMP_BLOCK, var14, 4, 12, chunkBB);
               var14 = 12;
            }

            var14 = 8;

            for (int i = 0; i < 2; i++) {
               this.generateBox(level, chunkBB, var14, 0, 7, var14, 2, 7, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(level, chunkBB, var14, 0, 14, var14, 2, 14, BASE_LIGHT, BASE_LIGHT, false);
               var14 = 14;
            }

            this.generateBox(level, chunkBB, 8, 3, 8, 8, 3, 13, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(level, chunkBB, 14, 3, 8, 14, 3, 13, BASE_BLACK, BASE_BLACK, false);
            this.spawnElder(level, chunkBB, 11, 5, 13);
         }
      }
   }

   private static class RoomDefinition {
      private final int index;
      private final OceanMonumentPieces.RoomDefinition[] connections = new OceanMonumentPieces.RoomDefinition[6];
      private final boolean[] hasOpening = new boolean[6];
      private boolean claimed;
      private boolean isSource;
      private int scanIndex;

      public RoomDefinition(final int roomIndex) {
         this.index = roomIndex;
      }

      public void setConnection(final Direction direction, final OceanMonumentPieces.RoomDefinition definition) {
         this.connections[direction.get3DDataValue()] = definition;
         definition.connections[direction.getOpposite().get3DDataValue()] = this;
      }

      public void updateOpenings() {
         for (int i = 0; i < 6; i++) {
            this.hasOpening[i] = this.connections[i] != null;
         }
      }

      public boolean findSource(final int scanIndex) {
         if (this.isSource) {
            return true;
         }

         this.scanIndex = scanIndex;

         for (int i = 0; i < 6; i++) {
            if (this.connections[i] != null && this.hasOpening[i] && this.connections[i].scanIndex != scanIndex && this.connections[i].findSource(scanIndex)) {
               return true;
            }
         }

         return false;
      }

      public boolean isSpecial() {
         return this.index >= 75;
      }

      public int countOpenings() {
         int c = 0;

         for (int i = 0; i < 6; i++) {
            if (this.hasOpening[i]) {
               c++;
            }
         }

         return c;
      }
   }
}
