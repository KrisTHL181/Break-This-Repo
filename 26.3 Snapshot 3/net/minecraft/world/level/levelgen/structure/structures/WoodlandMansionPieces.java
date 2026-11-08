package net.minecraft.world.level.levelgen.structure.structures;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.util.Util;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.levelgen.structure.BoundingBox;
import net.minecraft.world.level.levelgen.structure.TemplateStructurePiece;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceSerializationContext;
import net.minecraft.world.level.levelgen.structure.pieces.StructurePieceType;
import net.minecraft.world.level.levelgen.structure.templatesystem.BlockIgnoreProcessor;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructurePlaceSettings;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplate;
import net.minecraft.world.level.levelgen.structure.templatesystem.StructureTemplateManager;
import net.minecraft.world.level.storage.loot.BuiltInLootTables;
import org.jspecify.annotations.Nullable;

public class WoodlandMansionPieces {
   public static void generateMansion(
      final StructureTemplateManager structureTemplateManager,
      final BlockPos origin,
      final Rotation rotation,
      final List<WoodlandMansionPieces.WoodlandMansionPiece> pieces,
      final RandomSource random
   ) {
      WoodlandMansionPieces.MansionGrid grid = new WoodlandMansionPieces.MansionGrid(random);
      WoodlandMansionPieces.MansionPiecePlacer placer = new WoodlandMansionPieces.MansionPiecePlacer(structureTemplateManager, random);
      placer.createMansion(origin, rotation, pieces, grid);
   }

   private static class FirstFloorRoomCollection extends WoodlandMansionPieces.FloorRoomCollection {
      @Override
      public String get1x1(final RandomSource random) {
         return "1x1_a" + (random.nextInt(5) + 1);
      }

      @Override
      public String get1x1Secret(final RandomSource random) {
         return "1x1_as" + (random.nextInt(4) + 1);
      }

      @Override
      public String get1x2SideEntrance(final RandomSource random, final boolean isStairsRoom) {
         return "1x2_a" + (random.nextInt(9) + 1);
      }

      @Override
      public String get1x2FrontEntrance(final RandomSource random, final boolean isStairsRoom) {
         return "1x2_b" + (random.nextInt(5) + 1);
      }

      @Override
      public String get1x2Secret(final RandomSource random) {
         return "1x2_s" + (random.nextInt(2) + 1);
      }

      @Override
      public String get2x2(final RandomSource random) {
         return "2x2_a" + (random.nextInt(4) + 1);
      }

      @Override
      public String get2x2Secret(final RandomSource random) {
         return "2x2_s1";
      }
   }

   private abstract static class FloorRoomCollection {
      public abstract String get1x1(RandomSource random);

      public abstract String get1x1Secret(RandomSource random);

      public abstract String get1x2SideEntrance(RandomSource random, boolean isStairsRoom);

      public abstract String get1x2FrontEntrance(RandomSource random, boolean isStairsRoom);

      public abstract String get1x2Secret(RandomSource random);

      public abstract String get2x2(RandomSource random);

      public abstract String get2x2Secret(RandomSource random);
   }

   private static class MansionGrid {
      private static final int DEFAULT_SIZE = 11;
      private static final int CLEAR = 0;
      private static final int CORRIDOR = 1;
      private static final int ROOM = 2;
      private static final int START_ROOM = 3;
      private static final int TEST_ROOM = 4;
      private static final int BLOCKED = 5;
      private static final int ROOM_1x1 = 65536;
      private static final int ROOM_1x2 = 131072;
      private static final int ROOM_2x2 = 262144;
      private static final int ROOM_ORIGIN_FLAG = 1048576;
      private static final int ROOM_DOOR_FLAG = 2097152;
      private static final int ROOM_STAIRS_FLAG = 4194304;
      private static final int ROOM_CORRIDOR_FLAG = 8388608;
      private static final int ROOM_TYPE_MASK = 983040;
      private static final int ROOM_ID_MASK = 65535;
      private final RandomSource random;
      private final WoodlandMansionPieces.SimpleGrid baseGrid;
      private final WoodlandMansionPieces.SimpleGrid thirdFloorGrid;
      private final WoodlandMansionPieces.SimpleGrid[] floorRooms;
      private final int entranceX;
      private final int entranceY;

      public MansionGrid(final RandomSource random) {
         this.random = random;
         int houseSize = 11;
         this.entranceX = 7;
         this.entranceY = 4;
         this.baseGrid = new WoodlandMansionPieces.SimpleGrid(11, 11, 5);
         this.baseGrid.set(this.entranceX, this.entranceY, this.entranceX + 1, this.entranceY + 1, 3);
         this.baseGrid.set(this.entranceX - 1, this.entranceY, this.entranceX - 1, this.entranceY + 1, 2);
         this.baseGrid.set(this.entranceX + 2, this.entranceY - 2, this.entranceX + 3, this.entranceY + 3, 5);
         this.baseGrid.set(this.entranceX + 1, this.entranceY - 2, this.entranceX + 1, this.entranceY - 1, 1);
         this.baseGrid.set(this.entranceX + 1, this.entranceY + 2, this.entranceX + 1, this.entranceY + 3, 1);
         this.baseGrid.set(this.entranceX - 1, this.entranceY - 1, 1);
         this.baseGrid.set(this.entranceX - 1, this.entranceY + 2, 1);
         this.baseGrid.set(0, 0, 11, 1, 5);
         this.baseGrid.set(0, 9, 11, 11, 5);
         this.recursiveCorridor(this.baseGrid, this.entranceX, this.entranceY - 2, Direction.WEST, 6);
         this.recursiveCorridor(this.baseGrid, this.entranceX, this.entranceY + 3, Direction.WEST, 6);
         this.recursiveCorridor(this.baseGrid, this.entranceX - 2, this.entranceY - 1, Direction.WEST, 3);
         this.recursiveCorridor(this.baseGrid, this.entranceX - 2, this.entranceY + 2, Direction.WEST, 3);

         while (this.cleanEdges(this.baseGrid)) {
         }

         this.floorRooms = new WoodlandMansionPieces.SimpleGrid[3];
         this.floorRooms[0] = new WoodlandMansionPieces.SimpleGrid(11, 11, 5);
         this.floorRooms[1] = new WoodlandMansionPieces.SimpleGrid(11, 11, 5);
         this.floorRooms[2] = new WoodlandMansionPieces.SimpleGrid(11, 11, 5);
         this.identifyRooms(this.baseGrid, this.floorRooms[0]);
         this.identifyRooms(this.baseGrid, this.floorRooms[1]);
         this.floorRooms[0].set(this.entranceX + 1, this.entranceY, this.entranceX + 1, this.entranceY + 1, 8388608);
         this.floorRooms[1].set(this.entranceX + 1, this.entranceY, this.entranceX + 1, this.entranceY + 1, 8388608);
         this.thirdFloorGrid = new WoodlandMansionPieces.SimpleGrid(this.baseGrid.width, this.baseGrid.height, 5);
         this.setupThirdFloor();
         this.identifyRooms(this.thirdFloorGrid, this.floorRooms[2]);
      }

      public static boolean isHouse(final WoodlandMansionPieces.SimpleGrid grid, final int x, final int y) {
         int value = grid.get(x, y);
         return value == 1 || value == 2 || value == 3 || value == 4;
      }

      public boolean isRoomId(final WoodlandMansionPieces.SimpleGrid grid, final int x, final int y, final int floor, final int roomId) {
         return (this.floorRooms[floor].get(x, y) & 65535) == roomId;
      }

      public @Nullable Direction get1x2RoomDirection(
         final WoodlandMansionPieces.SimpleGrid grid, final int x, final int y, final int floorNum, final int roomId
      ) {
         for (Direction direction : Direction.Plane.HORIZONTAL) {
            if (this.isRoomId(grid, x + direction.getStepX(), y + direction.getStepZ(), floorNum, roomId)) {
               return direction;
            }
         }

         return null;
      }

      private void recursiveCorridor(final WoodlandMansionPieces.SimpleGrid grid, final int x, final int y, final Direction heading, final int depth) {
         if (depth > 0) {
            grid.set(x, y, 1);
            grid.setif(x + heading.getStepX(), y + heading.getStepZ(), 0, 1);

            for (int attempts = 0; attempts < 8; attempts++) {
               Direction nextDir = Direction.from2DDataValue(this.random.nextInt(4));
               if (nextDir != heading.getOpposite() && (nextDir != Direction.EAST || !this.random.nextBoolean())) {
                  int nx = x + heading.getStepX();
                  int ny = y + heading.getStepZ();
                  if (grid.get(nx + nextDir.getStepX(), ny + nextDir.getStepZ()) == 0
                     && grid.get(nx + nextDir.getStepX() * 2, ny + nextDir.getStepZ() * 2) == 0) {
                     this.recursiveCorridor(grid, x + heading.getStepX() + nextDir.getStepX(), y + heading.getStepZ() + nextDir.getStepZ(), nextDir, depth - 1);
                     break;
                  }
               }
            }

            Direction cw = heading.getClockWise();
            Direction ccw = heading.getCounterClockWise();
            grid.setif(x + cw.getStepX(), y + cw.getStepZ(), 0, 2);
            grid.setif(x + ccw.getStepX(), y + ccw.getStepZ(), 0, 2);
            grid.setif(x + heading.getStepX() + cw.getStepX(), y + heading.getStepZ() + cw.getStepZ(), 0, 2);
            grid.setif(x + heading.getStepX() + ccw.getStepX(), y + heading.getStepZ() + ccw.getStepZ(), 0, 2);
            grid.setif(x + heading.getStepX() * 2, y + heading.getStepZ() * 2, 0, 2);
            grid.setif(x + cw.getStepX() * 2, y + cw.getStepZ() * 2, 0, 2);
            grid.setif(x + ccw.getStepX() * 2, y + ccw.getStepZ() * 2, 0, 2);
         }
      }

      private boolean cleanEdges(final WoodlandMansionPieces.SimpleGrid grid) {
         boolean touched = false;

         for (int y = 0; y < grid.height; y++) {
            for (int x = 0; x < grid.width; x++) {
               if (grid.get(x, y) == 0) {
                  int directNeighbors = 0;
                  directNeighbors += isHouse(grid, x + 1, y) ? 1 : 0;
                  directNeighbors += isHouse(grid, x - 1, y) ? 1 : 0;
                  directNeighbors += isHouse(grid, x, y + 1) ? 1 : 0;
                  directNeighbors += isHouse(grid, x, y - 1) ? 1 : 0;
                  if (directNeighbors >= 3) {
                     grid.set(x, y, 2);
                     touched = true;
                  } else if (directNeighbors == 2) {
                     int diagonalNeighbors = 0;
                     diagonalNeighbors += isHouse(grid, x + 1, y + 1) ? 1 : 0;
                     diagonalNeighbors += isHouse(grid, x - 1, y + 1) ? 1 : 0;
                     diagonalNeighbors += isHouse(grid, x + 1, y - 1) ? 1 : 0;
                     diagonalNeighbors += isHouse(grid, x - 1, y - 1) ? 1 : 0;
                     if (diagonalNeighbors <= 1) {
                        grid.set(x, y, 2);
                        touched = true;
                     }
                  }
               }
            }
         }

         return touched;
      }

      private void setupThirdFloor() {
         List<WoodlandMansionPieces.MansionGrid.GridPos> potentialRooms = new ArrayList<>();
         WoodlandMansionPieces.SimpleGrid floor = this.floorRooms[1];

         for (int y = 0; y < this.thirdFloorGrid.height; y++) {
            for (int x = 0; x < this.thirdFloorGrid.width; x++) {
               int roomData = floor.get(x, y);
               int roomType = roomData & 983040;
               if (roomType == 131072 && (roomData & 2097152) == 2097152) {
                  potentialRooms.add(new WoodlandMansionPieces.MansionGrid.GridPos(x, y));
               }
            }
         }

         if (potentialRooms.isEmpty()) {
            this.thirdFloorGrid.set(0, 0, this.thirdFloorGrid.width, this.thirdFloorGrid.height, 5);
         } else {
            WoodlandMansionPieces.MansionGrid.GridPos roomPos = potentialRooms.get(this.random.nextInt(potentialRooms.size()));
            int roomData = floor.get(roomPos.x(), roomPos.y());
            floor.set(roomPos.x(), roomPos.y(), roomData | 4194304);
            Direction roomDir = this.get1x2RoomDirection(this.baseGrid, roomPos.x(), roomPos.y(), 1, roomData & 65535);
            int roomEndX = roomPos.x() + roomDir.getStepX();
            int roomEndY = roomPos.y() + roomDir.getStepZ();

            for (int y = 0; y < this.thirdFloorGrid.height; y++) {
               for (int x = 0; x < this.thirdFloorGrid.width; x++) {
                  if (!isHouse(this.baseGrid, x, y)) {
                     this.thirdFloorGrid.set(x, y, 5);
                  } else if (x == roomPos.x() && y == roomPos.y()) {
                     this.thirdFloorGrid.set(x, y, 3);
                  } else if (x == roomEndX && y == roomEndY) {
                     this.thirdFloorGrid.set(x, y, 3);
                     this.floorRooms[2].set(x, y, 8388608);
                  }
               }
            }

            List<Direction> potentialCorridors = new ArrayList<>();

            for (Direction direction : Direction.Plane.HORIZONTAL) {
               if (this.thirdFloorGrid.get(roomEndX + direction.getStepX(), roomEndY + direction.getStepZ()) == 0) {
                  potentialCorridors.add(direction);
               }
            }

            if (potentialCorridors.isEmpty()) {
               this.thirdFloorGrid.set(0, 0, this.thirdFloorGrid.width, this.thirdFloorGrid.height, 5);
               floor.set(roomPos.x(), roomPos.y(), roomData);
            } else {
               Direction corridorDir = potentialCorridors.get(this.random.nextInt(potentialCorridors.size()));
               this.recursiveCorridor(this.thirdFloorGrid, roomEndX + corridorDir.getStepX(), roomEndY + corridorDir.getStepZ(), corridorDir, 4);

               while (this.cleanEdges(this.thirdFloorGrid)) {
               }
            }
         }
      }

      private void identifyRooms(final WoodlandMansionPieces.SimpleGrid fromGrid, final WoodlandMansionPieces.SimpleGrid roomGrid) {
         List<WoodlandMansionPieces.MansionGrid.GridPos> roomPos = new ArrayList<>();

         for (int y = 0; y < fromGrid.height; y++) {
            for (int x = 0; x < fromGrid.width; x++) {
               if (fromGrid.get(x, y) == 2) {
                  roomPos.add(new WoodlandMansionPieces.MansionGrid.GridPos(x, y));
               }
            }
         }

         Util.shuffle(roomPos, this.random);
         int roomId = 10;

         for (WoodlandMansionPieces.MansionGrid.GridPos pos : roomPos) {
            int x = pos.x();
            int y = pos.y();
            if (roomGrid.get(x, y) == 0) {
               int x0 = x;
               int x1 = x;
               int y0 = y;
               int y1 = y;
               int type = 65536;
               if (roomGrid.get(x + 1, y) == 0
                  && roomGrid.get(x, y + 1) == 0
                  && roomGrid.get(x + 1, y + 1) == 0
                  && fromGrid.get(x + 1, y) == 2
                  && fromGrid.get(x, y + 1) == 2
                  && fromGrid.get(x + 1, y + 1) == 2) {
                  x1++;
                  y1++;
                  type = 262144;
               } else if (roomGrid.get(x - 1, y) == 0
                  && roomGrid.get(x, y + 1) == 0
                  && roomGrid.get(x - 1, y + 1) == 0
                  && fromGrid.get(x - 1, y) == 2
                  && fromGrid.get(x, y + 1) == 2
                  && fromGrid.get(x - 1, y + 1) == 2) {
                  x0--;
                  y1++;
                  type = 262144;
               } else if (roomGrid.get(x - 1, y) == 0
                  && roomGrid.get(x, y - 1) == 0
                  && roomGrid.get(x - 1, y - 1) == 0
                  && fromGrid.get(x - 1, y) == 2
                  && fromGrid.get(x, y - 1) == 2
                  && fromGrid.get(x - 1, y - 1) == 2) {
                  x0--;
                  y0--;
                  type = 262144;
               } else if (roomGrid.get(x + 1, y) == 0 && fromGrid.get(x + 1, y) == 2) {
                  x1++;
                  type = 131072;
               } else if (roomGrid.get(x, y + 1) == 0 && fromGrid.get(x, y + 1) == 2) {
                  y1++;
                  type = 131072;
               } else if (roomGrid.get(x - 1, y) == 0 && fromGrid.get(x - 1, y) == 2) {
                  x0--;
                  type = 131072;
               } else if (roomGrid.get(x, y - 1) == 0 && fromGrid.get(x, y - 1) == 2) {
                  y0--;
                  type = 131072;
               }

               int doorX = this.random.nextBoolean() ? x0 : x1;
               int doorY = this.random.nextBoolean() ? y0 : y1;
               int doorFlag = 2097152;
               if (!fromGrid.edgesTo(doorX, doorY, 1)) {
                  doorX = doorX == x0 ? x1 : x0;
                  doorY = doorY == y0 ? y1 : y0;
                  if (!fromGrid.edgesTo(doorX, doorY, 1)) {
                     doorY = doorY == y0 ? y1 : y0;
                     if (!fromGrid.edgesTo(doorX, doorY, 1)) {
                        doorX = doorX == x0 ? x1 : x0;
                        doorY = doorY == y0 ? y1 : y0;
                        if (!fromGrid.edgesTo(doorX, doorY, 1)) {
                           doorFlag = 0;
                           doorX = x0;
                           doorY = y0;
                        }
                     }
                  }
               }

               for (int ry = y0; ry <= y1; ry++) {
                  for (int rx = x0; rx <= x1; rx++) {
                     if (rx == doorX && ry == doorY) {
                        roomGrid.set(rx, ry, 1048576 | doorFlag | type | roomId);
                     } else {
                        roomGrid.set(rx, ry, type | roomId);
                     }
                  }
               }

               roomId++;
            }
         }
      }

      private record GridPos(int x, int y) {
      }
   }

   private static class MansionPiecePlacer {
      private final StructureTemplateManager structureTemplateManager;
      private final RandomSource random;
      private int startX;
      private int startY;

      public MansionPiecePlacer(final StructureTemplateManager structureTemplateManager, final RandomSource random) {
         this.structureTemplateManager = structureTemplateManager;
         this.random = random;
      }

      public void createMansion(
         final BlockPos origin,
         final Rotation rotation,
         final List<WoodlandMansionPieces.WoodlandMansionPiece> pieces,
         final WoodlandMansionPieces.MansionGrid mansion
      ) {
         WoodlandMansionPieces.PlacementData data = new WoodlandMansionPieces.PlacementData();
         data.position = origin;
         data.rotation = rotation;
         data.wallType = "wall_flat";
         WoodlandMansionPieces.PlacementData secondData = new WoodlandMansionPieces.PlacementData();
         this.entrance(pieces, data);
         secondData.position = data.position.above(8);
         secondData.rotation = data.rotation;
         secondData.wallType = "wall_window";
         if (!pieces.isEmpty()) {
         }

         WoodlandMansionPieces.SimpleGrid baseGrid = mansion.baseGrid;
         WoodlandMansionPieces.SimpleGrid thirdGrid = mansion.thirdFloorGrid;
         this.startX = mansion.entranceX + 1;
         this.startY = mansion.entranceY + 1;
         int endX = mansion.entranceX + 1;
         int endY = mansion.entranceY;
         this.traverseOuterWalls(pieces, data, baseGrid, Direction.SOUTH, this.startX, this.startY, endX, endY);
         this.traverseOuterWalls(pieces, secondData, baseGrid, Direction.SOUTH, this.startX, this.startY, endX, endY);
         WoodlandMansionPieces.PlacementData thirdData = new WoodlandMansionPieces.PlacementData();
         thirdData.position = data.position.above(19);
         thirdData.rotation = data.rotation;
         thirdData.wallType = "wall_window";
         boolean done = false;

         for (int y = 0; y < thirdGrid.height && !done; y++) {
            for (int x = thirdGrid.width - 1; x >= 0 && !done; x--) {
               if (WoodlandMansionPieces.MansionGrid.isHouse(thirdGrid, x, y)) {
                  thirdData.position = thirdData.position.relative(rotation.rotate(Direction.SOUTH), 8 + (y - this.startY) * 8);
                  thirdData.position = thirdData.position.relative(rotation.rotate(Direction.EAST), (x - this.startX) * 8);
                  this.traverseWallPiece(pieces, thirdData);
                  this.traverseOuterWalls(pieces, thirdData, thirdGrid, Direction.SOUTH, x, y, x, y);
                  done = true;
               }
            }
         }

         this.createRoof(pieces, origin.above(16), rotation, baseGrid, thirdGrid);
         this.createRoof(pieces, origin.above(27), rotation, thirdGrid, null);
         if (!pieces.isEmpty()) {
         }

         WoodlandMansionPieces.FloorRoomCollection[] roomCollections = new WoodlandMansionPieces.FloorRoomCollection[]{
            new WoodlandMansionPieces.FirstFloorRoomCollection(),
            new WoodlandMansionPieces.SecondFloorRoomCollection(),
            new WoodlandMansionPieces.ThirdFloorRoomCollection()
         };

         for (int floorNum = 0; floorNum < 3; floorNum++) {
            BlockPos floorOrigin = origin.above(8 * floorNum + (floorNum == 2 ? 3 : 0));
            WoodlandMansionPieces.SimpleGrid rooms = mansion.floorRooms[floorNum];
            WoodlandMansionPieces.SimpleGrid grid = floorNum == 2 ? thirdGrid : baseGrid;
            String southPiece = floorNum == 0 ? "carpet_south_1" : "carpet_south_2";
            String westPiece = floorNum == 0 ? "carpet_west_1" : "carpet_west_2";

            for (int y = 0; y < grid.height; y++) {
               for (int x = 0; x < grid.width; x++) {
                  if (grid.get(x, y) == 1) {
                     BlockPos pos = floorOrigin.relative(rotation.rotate(Direction.SOUTH), 8 + (y - this.startY) * 8);
                     pos = pos.relative(rotation.rotate(Direction.EAST), (x - this.startX) * 8);
                     pieces.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "corridor_floor", pos, rotation));
                     if (grid.get(x, y - 1) == 1 || (rooms.get(x, y - 1) & 8388608) == 8388608) {
                        pieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "carpet_north", pos.relative(rotation.rotate(Direction.EAST), 1).above(), rotation
                           )
                        );
                     }

                     if (grid.get(x + 1, y) == 1 || (rooms.get(x + 1, y) & 8388608) == 8388608) {
                        pieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager,
                              "carpet_east",
                              pos.relative(rotation.rotate(Direction.SOUTH), 1).relative(rotation.rotate(Direction.EAST), 5).above(),
                              rotation
                           )
                        );
                     }

                     if (grid.get(x, y + 1) == 1 || (rooms.get(x, y + 1) & 8388608) == 8388608) {
                        pieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager,
                              southPiece,
                              pos.relative(rotation.rotate(Direction.SOUTH), 5).relative(rotation.rotate(Direction.WEST), 1),
                              rotation
                           )
                        );
                     }

                     if (grid.get(x - 1, y) == 1 || (rooms.get(x - 1, y) & 8388608) == 8388608) {
                        pieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager,
                              westPiece,
                              pos.relative(rotation.rotate(Direction.WEST), 1).relative(rotation.rotate(Direction.NORTH), 1),
                              rotation
                           )
                        );
                     }
                  }
               }
            }

            String wallPiece = floorNum == 0 ? "indoors_wall_1" : "indoors_wall_2";
            String doorPiece = floorNum == 0 ? "indoors_door_1" : "indoors_door_2";
            List<Direction> doorDirs = new ArrayList<>();

            for (int y = 0; y < grid.height; y++) {
               for (int x = 0; x < grid.width; x++) {
                  boolean thirdFloorStartRoom = floorNum == 2 && grid.get(x, y) == 3;
                  if (grid.get(x, y) == 2 || thirdFloorStartRoom) {
                     int roomData = rooms.get(x, y);
                     int roomType = roomData & 983040;
                     int roomId = roomData & 65535;
                     thirdFloorStartRoom = thirdFloorStartRoom && (roomData & 8388608) == 8388608;
                     doorDirs.clear();
                     if ((roomData & 2097152) == 2097152) {
                        for (Direction direction : Direction.Plane.HORIZONTAL) {
                           if (grid.get(x + direction.getStepX(), y + direction.getStepZ()) == 1) {
                              doorDirs.add(direction);
                           }
                        }
                     }

                     Direction doorDir = null;
                     if (!doorDirs.isEmpty()) {
                        doorDir = doorDirs.get(this.random.nextInt(doorDirs.size()));
                     } else if ((roomData & 1048576) == 1048576) {
                        doorDir = Direction.UP;
                     }

                     BlockPos roomPos = floorOrigin.relative(rotation.rotate(Direction.SOUTH), 8 + (y - this.startY) * 8);
                     roomPos = roomPos.relative(rotation.rotate(Direction.EAST), -1 + (x - this.startX) * 8);
                     if (WoodlandMansionPieces.MansionGrid.isHouse(grid, x - 1, y) && !mansion.isRoomId(grid, x - 1, y, floorNum, roomId)) {
                        pieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, doorDir == Direction.WEST ? doorPiece : wallPiece, roomPos, rotation
                           )
                        );
                     }

                     if (grid.get(x + 1, y) == 1 && !thirdFloorStartRoom) {
                        BlockPos pos = roomPos.relative(rotation.rotate(Direction.EAST), 8);
                        pieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, doorDir == Direction.EAST ? doorPiece : wallPiece, pos, rotation
                           )
                        );
                     }

                     if (WoodlandMansionPieces.MansionGrid.isHouse(grid, x, y + 1) && !mansion.isRoomId(grid, x, y + 1, floorNum, roomId)) {
                        BlockPos pos = roomPos.relative(rotation.rotate(Direction.SOUTH), 7);
                        pos = pos.relative(rotation.rotate(Direction.EAST), 7);
                        pieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager,
                              doorDir == Direction.SOUTH ? doorPiece : wallPiece,
                              pos,
                              rotation.getRotated(Rotation.CLOCKWISE_90)
                           )
                        );
                     }

                     if (grid.get(x, y - 1) == 1 && !thirdFloorStartRoom) {
                        BlockPos pos = roomPos.relative(rotation.rotate(Direction.NORTH), 1);
                        pos = pos.relative(rotation.rotate(Direction.EAST), 7);
                        pieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager,
                              doorDir == Direction.NORTH ? doorPiece : wallPiece,
                              pos,
                              rotation.getRotated(Rotation.CLOCKWISE_90)
                           )
                        );
                     }

                     if (roomType == 65536) {
                        this.addRoom1x1(pieces, roomPos, rotation, doorDir, roomCollections[floorNum]);
                     } else if (roomType == 131072 && doorDir != null) {
                        Direction roomDir = mansion.get1x2RoomDirection(grid, x, y, floorNum, roomId);
                        boolean isStairsRoom = (roomData & 4194304) == 4194304;
                        this.addRoom1x2(pieces, roomPos, rotation, roomDir, doorDir, roomCollections[floorNum], isStairsRoom);
                     } else if (roomType == 262144 && doorDir != null && doorDir != Direction.UP) {
                        Direction roomDir = doorDir.getClockWise();
                        if (!mansion.isRoomId(grid, x + roomDir.getStepX(), y + roomDir.getStepZ(), floorNum, roomId)) {
                           roomDir = roomDir.getOpposite();
                        }

                        this.addRoom2x2(pieces, roomPos, rotation, roomDir, doorDir, roomCollections[floorNum]);
                     } else if (roomType == 262144 && doorDir == Direction.UP) {
                        this.addRoom2x2Secret(pieces, roomPos, rotation, roomCollections[floorNum]);
                     }
                  }
               }
            }
         }
      }

      private void traverseOuterWalls(
         final List<WoodlandMansionPieces.WoodlandMansionPiece> pieces,
         final WoodlandMansionPieces.PlacementData data,
         final WoodlandMansionPieces.SimpleGrid grid,
         Direction gridDirection,
         final int startX,
         final int startY,
         final int endX,
         final int endY
      ) {
         int gridX = startX;
         int gridY = startY;
         Direction startDirection = gridDirection;

         do {
            if (!WoodlandMansionPieces.MansionGrid.isHouse(grid, gridX + gridDirection.getStepX(), gridY + gridDirection.getStepZ())) {
               this.traverseTurn(pieces, data);
               gridDirection = gridDirection.getClockWise();
               if (gridX != endX || gridY != endY || startDirection != gridDirection) {
                  this.traverseWallPiece(pieces, data);
               }
            } else if (WoodlandMansionPieces.MansionGrid.isHouse(grid, gridX + gridDirection.getStepX(), gridY + gridDirection.getStepZ())
               && WoodlandMansionPieces.MansionGrid.isHouse(
                  grid,
                  gridX + gridDirection.getStepX() + gridDirection.getCounterClockWise().getStepX(),
                  gridY + gridDirection.getStepZ() + gridDirection.getCounterClockWise().getStepZ()
               )) {
               this.traverseInnerTurn(pieces, data);
               gridX += gridDirection.getStepX();
               gridY += gridDirection.getStepZ();
               gridDirection = gridDirection.getCounterClockWise();
            } else {
               gridX += gridDirection.getStepX();
               gridY += gridDirection.getStepZ();
               if (gridX != endX || gridY != endY || startDirection != gridDirection) {
                  this.traverseWallPiece(pieces, data);
               }
            }
         } while (gridX != endX || gridY != endY || startDirection != gridDirection);
      }

      private void createRoof(
         final List<WoodlandMansionPieces.WoodlandMansionPiece> pieces,
         final BlockPos roofOrigin,
         final Rotation rotation,
         final WoodlandMansionPieces.SimpleGrid grid,
         final WoodlandMansionPieces.@Nullable SimpleGrid aboveGrid
      ) {
         for (int y = 0; y < grid.height; y++) {
            for (int x = 0; x < grid.width; x++) {
               BlockPos position = roofOrigin;
               position = position.relative(rotation.rotate(Direction.SOUTH), 8 + (y - this.startY) * 8);
               position = position.relative(rotation.rotate(Direction.EAST), (x - this.startX) * 8);
               boolean isAbove = aboveGrid != null && WoodlandMansionPieces.MansionGrid.isHouse(aboveGrid, x, y);
               if (WoodlandMansionPieces.MansionGrid.isHouse(grid, x, y) && !isAbove) {
                  pieces.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "roof", position.above(3), rotation));
                  if (!WoodlandMansionPieces.MansionGrid.isHouse(grid, x + 1, y)) {
                     BlockPos p2 = position.relative(rotation.rotate(Direction.EAST), 6);
                     pieces.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "roof_front", p2, rotation));
                  }

                  if (!WoodlandMansionPieces.MansionGrid.isHouse(grid, x - 1, y)) {
                     BlockPos p2 = position.relative(rotation.rotate(Direction.EAST), 0);
                     p2 = p2.relative(rotation.rotate(Direction.SOUTH), 7);
                     pieces.add(
                        new WoodlandMansionPieces.WoodlandMansionPiece(
                           this.structureTemplateManager, "roof_front", p2, rotation.getRotated(Rotation.CLOCKWISE_180)
                        )
                     );
                  }

                  if (!WoodlandMansionPieces.MansionGrid.isHouse(grid, x, y - 1)) {
                     BlockPos p2 = position.relative(rotation.rotate(Direction.WEST), 1);
                     pieces.add(
                        new WoodlandMansionPieces.WoodlandMansionPiece(
                           this.structureTemplateManager, "roof_front", p2, rotation.getRotated(Rotation.COUNTERCLOCKWISE_90)
                        )
                     );
                  }

                  if (!WoodlandMansionPieces.MansionGrid.isHouse(grid, x, y + 1)) {
                     BlockPos p2 = position.relative(rotation.rotate(Direction.EAST), 6);
                     p2 = p2.relative(rotation.rotate(Direction.SOUTH), 6);
                     pieces.add(
                        new WoodlandMansionPieces.WoodlandMansionPiece(
                           this.structureTemplateManager, "roof_front", p2, rotation.getRotated(Rotation.CLOCKWISE_90)
                        )
                     );
                  }
               }
            }
         }

         if (aboveGrid != null) {
            for (int y = 0; y < grid.height; y++) {
               for (int x = 0; x < grid.width; x++) {
                  BlockPos position = roofOrigin;
                  position = position.relative(rotation.rotate(Direction.SOUTH), 8 + (y - this.startY) * 8);
                  position = position.relative(rotation.rotate(Direction.EAST), (x - this.startX) * 8);
                  boolean isAbove = WoodlandMansionPieces.MansionGrid.isHouse(aboveGrid, x, y);
                  if (WoodlandMansionPieces.MansionGrid.isHouse(grid, x, y) && isAbove) {
                     if (!WoodlandMansionPieces.MansionGrid.isHouse(grid, x + 1, y)) {
                        BlockPos p2 = position.relative(rotation.rotate(Direction.EAST), 7);
                        pieces.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "small_wall", p2, rotation));
                     }

                     if (!WoodlandMansionPieces.MansionGrid.isHouse(grid, x - 1, y)) {
                        BlockPos p2 = position.relative(rotation.rotate(Direction.WEST), 1);
                        p2 = p2.relative(rotation.rotate(Direction.SOUTH), 6);
                        pieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "small_wall", p2, rotation.getRotated(Rotation.CLOCKWISE_180)
                           )
                        );
                     }

                     if (!WoodlandMansionPieces.MansionGrid.isHouse(grid, x, y - 1)) {
                        BlockPos p2 = position.relative(rotation.rotate(Direction.WEST), 0);
                        p2 = p2.relative(rotation.rotate(Direction.NORTH), 1);
                        pieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "small_wall", p2, rotation.getRotated(Rotation.COUNTERCLOCKWISE_90)
                           )
                        );
                     }

                     if (!WoodlandMansionPieces.MansionGrid.isHouse(grid, x, y + 1)) {
                        BlockPos p2 = position.relative(rotation.rotate(Direction.EAST), 6);
                        p2 = p2.relative(rotation.rotate(Direction.SOUTH), 7);
                        pieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "small_wall", p2, rotation.getRotated(Rotation.CLOCKWISE_90)
                           )
                        );
                     }

                     if (!WoodlandMansionPieces.MansionGrid.isHouse(grid, x + 1, y)) {
                        if (!WoodlandMansionPieces.MansionGrid.isHouse(grid, x, y - 1)) {
                           BlockPos p2 = position.relative(rotation.rotate(Direction.EAST), 7);
                           p2 = p2.relative(rotation.rotate(Direction.NORTH), 2);
                           pieces.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "small_wall_corner", p2, rotation));
                        }

                        if (!WoodlandMansionPieces.MansionGrid.isHouse(grid, x, y + 1)) {
                           BlockPos p2 = position.relative(rotation.rotate(Direction.EAST), 8);
                           p2 = p2.relative(rotation.rotate(Direction.SOUTH), 7);
                           pieces.add(
                              new WoodlandMansionPieces.WoodlandMansionPiece(
                                 this.structureTemplateManager, "small_wall_corner", p2, rotation.getRotated(Rotation.CLOCKWISE_90)
                              )
                           );
                        }
                     }

                     if (!WoodlandMansionPieces.MansionGrid.isHouse(grid, x - 1, y)) {
                        if (!WoodlandMansionPieces.MansionGrid.isHouse(grid, x, y - 1)) {
                           BlockPos p2 = position.relative(rotation.rotate(Direction.WEST), 2);
                           p2 = p2.relative(rotation.rotate(Direction.NORTH), 1);
                           pieces.add(
                              new WoodlandMansionPieces.WoodlandMansionPiece(
                                 this.structureTemplateManager, "small_wall_corner", p2, rotation.getRotated(Rotation.COUNTERCLOCKWISE_90)
                              )
                           );
                        }

                        if (!WoodlandMansionPieces.MansionGrid.isHouse(grid, x, y + 1)) {
                           BlockPos p2 = position.relative(rotation.rotate(Direction.WEST), 1);
                           p2 = p2.relative(rotation.rotate(Direction.SOUTH), 8);
                           pieces.add(
                              new WoodlandMansionPieces.WoodlandMansionPiece(
                                 this.structureTemplateManager, "small_wall_corner", p2, rotation.getRotated(Rotation.CLOCKWISE_180)
                              )
                           );
                        }
                     }
                  }
               }
            }
         }

         for (int y = 0; y < grid.height; y++) {
            for (int x = 0; x < grid.width; x++) {
               BlockPos position = roofOrigin;
               position = position.relative(rotation.rotate(Direction.SOUTH), 8 + (y - this.startY) * 8);
               position = position.relative(rotation.rotate(Direction.EAST), (x - this.startX) * 8);
               boolean isAbove = aboveGrid != null && WoodlandMansionPieces.MansionGrid.isHouse(aboveGrid, x, y);
               if (WoodlandMansionPieces.MansionGrid.isHouse(grid, x, y) && !isAbove) {
                  if (!WoodlandMansionPieces.MansionGrid.isHouse(grid, x + 1, y)) {
                     BlockPos p2 = position.relative(rotation.rotate(Direction.EAST), 6);
                     if (!WoodlandMansionPieces.MansionGrid.isHouse(grid, x, y + 1)) {
                        BlockPos p3 = p2.relative(rotation.rotate(Direction.SOUTH), 6);
                        pieces.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "roof_corner", p3, rotation));
                     } else if (WoodlandMansionPieces.MansionGrid.isHouse(grid, x + 1, y + 1)) {
                        BlockPos p3 = p2.relative(rotation.rotate(Direction.SOUTH), 5);
                        pieces.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "roof_inner_corner", p3, rotation));
                     }

                     if (!WoodlandMansionPieces.MansionGrid.isHouse(grid, x, y - 1)) {
                        pieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "roof_corner", p2, rotation.getRotated(Rotation.COUNTERCLOCKWISE_90)
                           )
                        );
                     } else if (WoodlandMansionPieces.MansionGrid.isHouse(grid, x + 1, y - 1)) {
                        BlockPos p3 = position.relative(rotation.rotate(Direction.EAST), 9);
                        p3 = p3.relative(rotation.rotate(Direction.NORTH), 2);
                        pieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "roof_inner_corner", p3, rotation.getRotated(Rotation.CLOCKWISE_90)
                           )
                        );
                     }
                  }

                  if (!WoodlandMansionPieces.MansionGrid.isHouse(grid, x - 1, y)) {
                     BlockPos p2 = position.relative(rotation.rotate(Direction.EAST), 0);
                     p2 = p2.relative(rotation.rotate(Direction.SOUTH), 0);
                     if (!WoodlandMansionPieces.MansionGrid.isHouse(grid, x, y + 1)) {
                        BlockPos p3 = p2.relative(rotation.rotate(Direction.SOUTH), 6);
                        pieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "roof_corner", p3, rotation.getRotated(Rotation.CLOCKWISE_90)
                           )
                        );
                     } else if (WoodlandMansionPieces.MansionGrid.isHouse(grid, x - 1, y + 1)) {
                        BlockPos p3 = p2.relative(rotation.rotate(Direction.SOUTH), 8);
                        p3 = p3.relative(rotation.rotate(Direction.WEST), 3);
                        pieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "roof_inner_corner", p3, rotation.getRotated(Rotation.COUNTERCLOCKWISE_90)
                           )
                        );
                     }

                     if (!WoodlandMansionPieces.MansionGrid.isHouse(grid, x, y - 1)) {
                        pieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "roof_corner", p2, rotation.getRotated(Rotation.CLOCKWISE_180)
                           )
                        );
                     } else if (WoodlandMansionPieces.MansionGrid.isHouse(grid, x - 1, y - 1)) {
                        BlockPos p3 = p2.relative(rotation.rotate(Direction.SOUTH), 1);
                        pieces.add(
                           new WoodlandMansionPieces.WoodlandMansionPiece(
                              this.structureTemplateManager, "roof_inner_corner", p3, rotation.getRotated(Rotation.CLOCKWISE_180)
                           )
                        );
                     }
                  }
               }
            }
         }
      }

      private void entrance(final List<WoodlandMansionPieces.WoodlandMansionPiece> pieces, final WoodlandMansionPieces.PlacementData data) {
         Direction west = data.rotation.rotate(Direction.WEST);
         pieces.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "entrance", data.position.relative(west, 9), data.rotation));
         data.position = data.position.relative(data.rotation.rotate(Direction.SOUTH), 16);
      }

      private void traverseWallPiece(final List<WoodlandMansionPieces.WoodlandMansionPiece> pieces, final WoodlandMansionPieces.PlacementData data) {
         pieces.add(
            new WoodlandMansionPieces.WoodlandMansionPiece(
               this.structureTemplateManager, data.wallType, data.position.relative(data.rotation.rotate(Direction.EAST), 7), data.rotation
            )
         );
         data.position = data.position.relative(data.rotation.rotate(Direction.SOUTH), 8);
      }

      private void traverseTurn(final List<WoodlandMansionPieces.WoodlandMansionPiece> pieces, final WoodlandMansionPieces.PlacementData data) {
         data.position = data.position.relative(data.rotation.rotate(Direction.SOUTH), -1);
         pieces.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, "wall_corner", data.position, data.rotation));
         data.position = data.position.relative(data.rotation.rotate(Direction.SOUTH), -7);
         data.position = data.position.relative(data.rotation.rotate(Direction.WEST), -6);
         data.rotation = data.rotation.getRotated(Rotation.CLOCKWISE_90);
      }

      private void traverseInnerTurn(final List<WoodlandMansionPieces.WoodlandMansionPiece> pieces, final WoodlandMansionPieces.PlacementData data) {
         data.position = data.position.relative(data.rotation.rotate(Direction.SOUTH), 6);
         data.position = data.position.relative(data.rotation.rotate(Direction.EAST), 8);
         data.rotation = data.rotation.getRotated(Rotation.COUNTERCLOCKWISE_90);
      }

      private void addRoom1x1(
         final List<WoodlandMansionPieces.WoodlandMansionPiece> pieces,
         final BlockPos roomPos,
         final Rotation rotation,
         final Direction doorDir,
         final WoodlandMansionPieces.FloorRoomCollection rooms
      ) {
         Rotation pieceRot = Rotation.NONE;
         String roomType = rooms.get1x1(this.random);
         if (doorDir != Direction.EAST) {
            if (doorDir == Direction.NORTH) {
               pieceRot = pieceRot.getRotated(Rotation.COUNTERCLOCKWISE_90);
            } else if (doorDir == Direction.WEST) {
               pieceRot = pieceRot.getRotated(Rotation.CLOCKWISE_180);
            } else if (doorDir == Direction.SOUTH) {
               pieceRot = pieceRot.getRotated(Rotation.CLOCKWISE_90);
            } else {
               roomType = rooms.get1x1Secret(this.random);
            }
         }

         BlockPos orientation = StructureTemplate.getZeroPositionWithTransform(new BlockPos(1, 0, 0), Mirror.NONE, pieceRot, 7, 7);
         pieceRot = pieceRot.getRotated(rotation);
         orientation = orientation.rotate(rotation);
         BlockPos pos = roomPos.offset(orientation.getX(), 0, orientation.getZ());
         pieces.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, roomType, pos, pieceRot));
      }

      private void addRoom1x2(
         final List<WoodlandMansionPieces.WoodlandMansionPiece> pieces,
         final BlockPos roomPos,
         final Rotation rotation,
         final Direction roomDir,
         final Direction doorDir,
         final WoodlandMansionPieces.FloorRoomCollection rooms,
         final boolean isStairsRoom
      ) {
         if (doorDir == Direction.EAST && roomDir == Direction.SOUTH) {
            BlockPos pos = roomPos.relative(rotation.rotate(Direction.EAST), 1);
            pieces.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, rooms.get1x2SideEntrance(this.random, isStairsRoom), pos, rotation)
            );
         } else if (doorDir == Direction.EAST && roomDir == Direction.NORTH) {
            BlockPos pos = roomPos.relative(rotation.rotate(Direction.EAST), 1);
            pos = pos.relative(rotation.rotate(Direction.SOUTH), 6);
            pieces.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, rooms.get1x2SideEntrance(this.random, isStairsRoom), pos, rotation, Mirror.LEFT_RIGHT
               )
            );
         } else if (doorDir == Direction.WEST && roomDir == Direction.NORTH) {
            BlockPos pos = roomPos.relative(rotation.rotate(Direction.EAST), 7);
            pos = pos.relative(rotation.rotate(Direction.SOUTH), 6);
            pieces.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, rooms.get1x2SideEntrance(this.random, isStairsRoom), pos, rotation.getRotated(Rotation.CLOCKWISE_180)
               )
            );
         } else if (doorDir == Direction.WEST && roomDir == Direction.SOUTH) {
            BlockPos pos = roomPos.relative(rotation.rotate(Direction.EAST), 7);
            pieces.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, rooms.get1x2SideEntrance(this.random, isStairsRoom), pos, rotation, Mirror.FRONT_BACK
               )
            );
         } else if (doorDir == Direction.SOUTH && roomDir == Direction.EAST) {
            BlockPos pos = roomPos.relative(rotation.rotate(Direction.EAST), 1);
            pieces.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager,
                  rooms.get1x2SideEntrance(this.random, isStairsRoom),
                  pos,
                  rotation.getRotated(Rotation.CLOCKWISE_90),
                  Mirror.LEFT_RIGHT
               )
            );
         } else if (doorDir == Direction.SOUTH && roomDir == Direction.WEST) {
            BlockPos pos = roomPos.relative(rotation.rotate(Direction.EAST), 7);
            pieces.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, rooms.get1x2SideEntrance(this.random, isStairsRoom), pos, rotation.getRotated(Rotation.CLOCKWISE_90)
               )
            );
         } else if (doorDir == Direction.NORTH && roomDir == Direction.WEST) {
            BlockPos pos = roomPos.relative(rotation.rotate(Direction.EAST), 7);
            pos = pos.relative(rotation.rotate(Direction.SOUTH), 6);
            pieces.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager,
                  rooms.get1x2SideEntrance(this.random, isStairsRoom),
                  pos,
                  rotation.getRotated(Rotation.CLOCKWISE_90),
                  Mirror.FRONT_BACK
               )
            );
         } else if (doorDir == Direction.NORTH && roomDir == Direction.EAST) {
            BlockPos pos = roomPos.relative(rotation.rotate(Direction.EAST), 1);
            pos = pos.relative(rotation.rotate(Direction.SOUTH), 6);
            pieces.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, rooms.get1x2SideEntrance(this.random, isStairsRoom), pos, rotation.getRotated(Rotation.COUNTERCLOCKWISE_90)
               )
            );
         } else if (doorDir == Direction.SOUTH && roomDir == Direction.NORTH) {
            BlockPos pos = roomPos.relative(rotation.rotate(Direction.EAST), 1);
            pos = pos.relative(rotation.rotate(Direction.NORTH), 8);
            pieces.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, rooms.get1x2FrontEntrance(this.random, isStairsRoom), pos, rotation
               )
            );
         } else if (doorDir == Direction.NORTH && roomDir == Direction.SOUTH) {
            BlockPos pos = roomPos.relative(rotation.rotate(Direction.EAST), 7);
            pos = pos.relative(rotation.rotate(Direction.SOUTH), 14);
            pieces.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, rooms.get1x2FrontEntrance(this.random, isStairsRoom), pos, rotation.getRotated(Rotation.CLOCKWISE_180)
               )
            );
         } else if (doorDir == Direction.WEST && roomDir == Direction.EAST) {
            BlockPos pos = roomPos.relative(rotation.rotate(Direction.EAST), 15);
            pieces.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, rooms.get1x2FrontEntrance(this.random, isStairsRoom), pos, rotation.getRotated(Rotation.CLOCKWISE_90)
               )
            );
         } else if (doorDir == Direction.EAST && roomDir == Direction.WEST) {
            BlockPos pos = roomPos.relative(rotation.rotate(Direction.WEST), 7);
            pos = pos.relative(rotation.rotate(Direction.SOUTH), 6);
            pieces.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, rooms.get1x2FrontEntrance(this.random, isStairsRoom), pos, rotation.getRotated(Rotation.COUNTERCLOCKWISE_90)
               )
            );
         } else if (doorDir == Direction.UP && roomDir == Direction.EAST) {
            BlockPos pos = roomPos.relative(rotation.rotate(Direction.EAST), 15);
            pieces.add(
               new WoodlandMansionPieces.WoodlandMansionPiece(
                  this.structureTemplateManager, rooms.get1x2Secret(this.random), pos, rotation.getRotated(Rotation.CLOCKWISE_90)
               )
            );
         } else if (doorDir == Direction.UP && roomDir == Direction.SOUTH) {
            BlockPos pos = roomPos.relative(rotation.rotate(Direction.EAST), 1);
            pos = pos.relative(rotation.rotate(Direction.NORTH), 0);
            pieces.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, rooms.get1x2Secret(this.random), pos, rotation));
         }
      }

      private void addRoom2x2(
         final List<WoodlandMansionPieces.WoodlandMansionPiece> pieces,
         final BlockPos roomPos,
         final Rotation rotation,
         final Direction roomDir,
         final Direction doorDir,
         final WoodlandMansionPieces.FloorRoomCollection rooms
      ) {
         int east = 0;
         int south = 0;
         Rotation rot = rotation;
         Mirror mirror = Mirror.NONE;
         if (doorDir == Direction.EAST && roomDir == Direction.SOUTH) {
            east = -7;
         } else if (doorDir == Direction.EAST && roomDir == Direction.NORTH) {
            east = -7;
            south = 6;
            mirror = Mirror.LEFT_RIGHT;
         } else if (doorDir == Direction.NORTH && roomDir == Direction.EAST) {
            east = 1;
            south = 14;
            rot = rotation.getRotated(Rotation.COUNTERCLOCKWISE_90);
         } else if (doorDir == Direction.NORTH && roomDir == Direction.WEST) {
            east = 7;
            south = 14;
            rot = rotation.getRotated(Rotation.COUNTERCLOCKWISE_90);
            mirror = Mirror.LEFT_RIGHT;
         } else if (doorDir == Direction.SOUTH && roomDir == Direction.WEST) {
            east = 7;
            south = -8;
            rot = rotation.getRotated(Rotation.CLOCKWISE_90);
         } else if (doorDir == Direction.SOUTH && roomDir == Direction.EAST) {
            east = 1;
            south = -8;
            rot = rotation.getRotated(Rotation.CLOCKWISE_90);
            mirror = Mirror.LEFT_RIGHT;
         } else if (doorDir == Direction.WEST && roomDir == Direction.NORTH) {
            east = 15;
            south = 6;
            rot = rotation.getRotated(Rotation.CLOCKWISE_180);
         } else if (doorDir == Direction.WEST && roomDir == Direction.SOUTH) {
            east = 15;
            mirror = Mirror.FRONT_BACK;
         }

         BlockPos pos = roomPos.relative(rotation.rotate(Direction.EAST), east);
         pos = pos.relative(rotation.rotate(Direction.SOUTH), south);
         pieces.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, rooms.get2x2(this.random), pos, rot, mirror));
      }

      private void addRoom2x2Secret(
         final List<WoodlandMansionPieces.WoodlandMansionPiece> pieces,
         final BlockPos roomPos,
         final Rotation rotation,
         final WoodlandMansionPieces.FloorRoomCollection rooms
      ) {
         BlockPos pos = roomPos.relative(rotation.rotate(Direction.EAST), 1);
         pieces.add(new WoodlandMansionPieces.WoodlandMansionPiece(this.structureTemplateManager, rooms.get2x2Secret(this.random), pos, rotation, Mirror.NONE));
      }
   }

   private static class PlacementData {
      public Rotation rotation;
      public BlockPos position;
      public String wallType;
   }

   private static class SecondFloorRoomCollection extends WoodlandMansionPieces.FloorRoomCollection {
      @Override
      public String get1x1(final RandomSource random) {
         return "1x1_b" + (random.nextInt(5) + 1);
      }

      @Override
      public String get1x1Secret(final RandomSource random) {
         return "1x1_as" + (random.nextInt(4) + 1);
      }

      @Override
      public String get1x2SideEntrance(final RandomSource random, final boolean isStairsRoom) {
         return isStairsRoom ? "1x2_c_stairs" : "1x2_c" + (random.nextInt(4) + 1);
      }

      @Override
      public String get1x2FrontEntrance(final RandomSource random, final boolean isStairsRoom) {
         return isStairsRoom ? "1x2_d_stairs" : "1x2_d" + (random.nextInt(5) + 1);
      }

      @Override
      public String get1x2Secret(final RandomSource random) {
         return "1x2_se" + (random.nextInt(1) + 1);
      }

      @Override
      public String get2x2(final RandomSource random) {
         return "2x2_b" + (random.nextInt(5) + 1);
      }

      @Override
      public String get2x2Secret(final RandomSource random) {
         return "2x2_s1";
      }
   }

   private static class SimpleGrid {
      private final int[][] grid;
      private final int width;
      private final int height;
      private final int valueIfOutside;

      public SimpleGrid(final int width, final int height, final int valueIfOutside) {
         this.width = width;
         this.height = height;
         this.valueIfOutside = valueIfOutside;
         this.grid = new int[width][height];
      }

      public void set(final int x, final int y, final int value) {
         if (x >= 0 && x < this.width && y >= 0 && y < this.height) {
            this.grid[x][y] = value;
         }
      }

      public void set(final int x0, final int y0, final int x1, final int y1, final int value) {
         for (int y = y0; y <= y1; y++) {
            for (int x = x0; x <= x1; x++) {
               this.set(x, y, value);
            }
         }
      }

      public int get(final int x, final int y) {
         return x >= 0 && x < this.width && y >= 0 && y < this.height ? this.grid[x][y] : this.valueIfOutside;
      }

      public void setif(final int x, final int y, final int ifValue, final int value) {
         if (this.get(x, y) == ifValue) {
            this.set(x, y, value);
         }
      }

      public boolean edgesTo(final int x, final int y, final int ifValue) {
         return this.get(x - 1, y) == ifValue || this.get(x + 1, y) == ifValue || this.get(x, y + 1) == ifValue || this.get(x, y - 1) == ifValue;
      }
   }

   private static class ThirdFloorRoomCollection extends WoodlandMansionPieces.SecondFloorRoomCollection {
   }

   public static class WoodlandMansionPiece extends TemplateStructurePiece {
      public WoodlandMansionPiece(
         final StructureTemplateManager structureTemplateManager, final String templateName, final BlockPos position, final Rotation rotation
      ) {
         this(structureTemplateManager, templateName, position, rotation, Mirror.NONE);
      }

      public WoodlandMansionPiece(
         final StructureTemplateManager structureTemplateManager,
         final String templateName,
         final BlockPos position,
         final Rotation rotation,
         final Mirror mirror
      ) {
         super(
            StructurePieceType.WOODLAND_MANSION_PIECE,
            0,
            structureTemplateManager,
            makeLocation(templateName),
            templateName,
            makeSettings(mirror, rotation),
            position
         );
      }

      public WoodlandMansionPiece(final StructureTemplateManager structureTemplateManager, final CompoundTag tag) {
         super(
            StructurePieceType.WOODLAND_MANSION_PIECE,
            tag,
            structureTemplateManager,
            location -> makeSettings(tag.<Mirror>read("Mi", Mirror.LEGACY_CODEC).orElseThrow(), tag.<Rotation>read("Rot", Rotation.LEGACY_CODEC).orElseThrow())
         );
      }

      @Override
      protected Identifier makeTemplateLocation() {
         return makeLocation(this.templateName);
      }

      private static Identifier makeLocation(final String templateName) {
         return Identifier.withDefaultNamespace("woodland_mansion/" + templateName);
      }

      private static StructurePlaceSettings makeSettings(final Mirror mirror, final Rotation rotation) {
         return new StructurePlaceSettings().setIgnoreEntities(true).setRotation(rotation).setMirror(mirror).addProcessor(BlockIgnoreProcessor.STRUCTURE_BLOCK);
      }

      @Override
      protected void addAdditionalSaveData(final StructurePieceSerializationContext context, final CompoundTag tag) {
         super.addAdditionalSaveData(context, tag);
         tag.store("Rot", Rotation.LEGACY_CODEC, this.placeSettings.getRotation());
         tag.store("Mi", Mirror.LEGACY_CODEC, this.placeSettings.getMirror());
      }

      @Override
      protected void handleDataMarker(
         final String markerId, final BlockPos position, final ServerLevelAccessor level, final RandomSource random, final BoundingBox chunkBB
      ) {
         if (markerId.startsWith("Chest")) {
            Rotation rot = this.placeSettings.getRotation();
            BlockState chestState = Blocks.CHEST.defaultBlockState();
            if ("ChestWest".equals(markerId)) {
               chestState = chestState.setValue(ChestBlock.FACING, rot.rotate(Direction.WEST));
            } else if ("ChestEast".equals(markerId)) {
               chestState = chestState.setValue(ChestBlock.FACING, rot.rotate(Direction.EAST));
            } else if ("ChestSouth".equals(markerId)) {
               chestState = chestState.setValue(ChestBlock.FACING, rot.rotate(Direction.SOUTH));
            } else if ("ChestNorth".equals(markerId)) {
               chestState = chestState.setValue(ChestBlock.FACING, rot.rotate(Direction.NORTH));
            }

            this.createChest(level, chunkBB, random, position, BuiltInLootTables.WOODLAND_MANSION, chestState);
         } else {
            List<Mob> mobs = new ArrayList<>();
            switch (markerId) {
               case "Mage":
                  mobs.add(EntityTypes.EVOKER.create(level.getLevel(), EntitySpawnReason.STRUCTURE));
                  break;
               case "Warrior":
                  mobs.add(EntityTypes.VINDICATOR.create(level.getLevel(), EntitySpawnReason.STRUCTURE));
                  break;
               case "Group of Allays":
                  int numberOfAllays = level.getRandom().nextInt(3) + 1;

                  for (int i = 0; i < numberOfAllays; i++) {
                     mobs.add(EntityTypes.ALLAY.create(level.getLevel(), EntitySpawnReason.STRUCTURE));
                  }
                  break;
               default:
                  return;
            }

            for (Mob mob : mobs) {
               if (mob != null) {
                  mob.setPersistenceRequired();
                  mob.snapTo(position, 0.0F, 0.0F);
                  mob.finalizeSpawn(level, level.getCurrentDifficultyAt(mob.blockPosition()), EntitySpawnReason.STRUCTURE, null);
                  level.addFreshEntityWithPassengers(mob);
                  level.setBlock(position, Blocks.AIR.defaultBlockState(), 2);
               }
            }
         }
      }
   }
}
