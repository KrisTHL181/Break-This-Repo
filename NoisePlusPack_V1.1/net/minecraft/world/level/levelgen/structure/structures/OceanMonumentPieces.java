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
import net.minecraft.world.entity.EntityType;
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

   static class FitDoubleXRoom implements OceanMonumentPieces.MonumentRoomFitter {
      @Override
      public boolean fits(OceanMonumentPieces.RoomDefinition p_228592_) {
         return p_228592_.hasOpening[Direction.EAST.get3DDataValue()] && !p_228592_.connections[Direction.EAST.get3DDataValue()].claimed;
      }

      @Override
      public OceanMonumentPieces.OceanMonumentPiece create(Direction p_228594_, OceanMonumentPieces.RoomDefinition p_228595_, RandomSource p_228596_) {
         p_228595_.claimed = true;
         p_228595_.connections[Direction.EAST.get3DDataValue()].claimed = true;
         return new OceanMonumentPieces.OceanMonumentDoubleXRoom(p_228594_, p_228595_);
      }
   }

   static class FitDoubleXYRoom implements OceanMonumentPieces.MonumentRoomFitter {
      @Override
      public boolean fits(OceanMonumentPieces.RoomDefinition p_228599_) {
         if (p_228599_.hasOpening[Direction.EAST.get3DDataValue()]
            && !p_228599_.connections[Direction.EAST.get3DDataValue()].claimed
            && p_228599_.hasOpening[Direction.UP.get3DDataValue()]
            && !p_228599_.connections[Direction.UP.get3DDataValue()].claimed) {
            OceanMonumentPieces.RoomDefinition oceanmonumentpieces$roomdefinition = p_228599_.connections[Direction.EAST.get3DDataValue()];
            return oceanmonumentpieces$roomdefinition.hasOpening[Direction.UP.get3DDataValue()]
               && !oceanmonumentpieces$roomdefinition.connections[Direction.UP.get3DDataValue()].claimed;
         } else {
            return false;
         }
      }

      @Override
      public OceanMonumentPieces.OceanMonumentPiece create(Direction p_228601_, OceanMonumentPieces.RoomDefinition p_228602_, RandomSource p_228603_) {
         p_228602_.claimed = true;
         p_228602_.connections[Direction.EAST.get3DDataValue()].claimed = true;
         p_228602_.connections[Direction.UP.get3DDataValue()].claimed = true;
         p_228602_.connections[Direction.EAST.get3DDataValue()].connections[Direction.UP.get3DDataValue()].claimed = true;
         return new OceanMonumentPieces.OceanMonumentDoubleXYRoom(p_228601_, p_228602_);
      }
   }

   static class FitDoubleYRoom implements OceanMonumentPieces.MonumentRoomFitter {
      @Override
      public boolean fits(OceanMonumentPieces.RoomDefinition p_228606_) {
         return p_228606_.hasOpening[Direction.UP.get3DDataValue()] && !p_228606_.connections[Direction.UP.get3DDataValue()].claimed;
      }

      @Override
      public OceanMonumentPieces.OceanMonumentPiece create(Direction p_228608_, OceanMonumentPieces.RoomDefinition p_228609_, RandomSource p_228610_) {
         p_228609_.claimed = true;
         p_228609_.connections[Direction.UP.get3DDataValue()].claimed = true;
         return new OceanMonumentPieces.OceanMonumentDoubleYRoom(p_228608_, p_228609_);
      }
   }

   static class FitDoubleYZRoom implements OceanMonumentPieces.MonumentRoomFitter {
      @Override
      public boolean fits(OceanMonumentPieces.RoomDefinition p_228613_) {
         if (p_228613_.hasOpening[Direction.NORTH.get3DDataValue()]
            && !p_228613_.connections[Direction.NORTH.get3DDataValue()].claimed
            && p_228613_.hasOpening[Direction.UP.get3DDataValue()]
            && !p_228613_.connections[Direction.UP.get3DDataValue()].claimed) {
            OceanMonumentPieces.RoomDefinition oceanmonumentpieces$roomdefinition = p_228613_.connections[Direction.NORTH.get3DDataValue()];
            return oceanmonumentpieces$roomdefinition.hasOpening[Direction.UP.get3DDataValue()]
               && !oceanmonumentpieces$roomdefinition.connections[Direction.UP.get3DDataValue()].claimed;
         } else {
            return false;
         }
      }

      @Override
      public OceanMonumentPieces.OceanMonumentPiece create(Direction p_228615_, OceanMonumentPieces.RoomDefinition p_228616_, RandomSource p_228617_) {
         p_228616_.claimed = true;
         p_228616_.connections[Direction.NORTH.get3DDataValue()].claimed = true;
         p_228616_.connections[Direction.UP.get3DDataValue()].claimed = true;
         p_228616_.connections[Direction.NORTH.get3DDataValue()].connections[Direction.UP.get3DDataValue()].claimed = true;
         return new OceanMonumentPieces.OceanMonumentDoubleYZRoom(p_228615_, p_228616_);
      }
   }

   static class FitDoubleZRoom implements OceanMonumentPieces.MonumentRoomFitter {
      @Override
      public boolean fits(OceanMonumentPieces.RoomDefinition p_228620_) {
         return p_228620_.hasOpening[Direction.NORTH.get3DDataValue()] && !p_228620_.connections[Direction.NORTH.get3DDataValue()].claimed;
      }

      @Override
      public OceanMonumentPieces.OceanMonumentPiece create(Direction p_228622_, OceanMonumentPieces.RoomDefinition p_228623_, RandomSource p_228624_) {
         OceanMonumentPieces.RoomDefinition oceanmonumentpieces$roomdefinition = p_228623_;
         if (!p_228623_.hasOpening[Direction.NORTH.get3DDataValue()] || p_228623_.connections[Direction.NORTH.get3DDataValue()].claimed) {
            oceanmonumentpieces$roomdefinition = p_228623_.connections[Direction.SOUTH.get3DDataValue()];
         }

         oceanmonumentpieces$roomdefinition.claimed = true;
         oceanmonumentpieces$roomdefinition.connections[Direction.NORTH.get3DDataValue()].claimed = true;
         return new OceanMonumentPieces.OceanMonumentDoubleZRoom(p_228622_, oceanmonumentpieces$roomdefinition);
      }
   }

   static class FitSimpleRoom implements OceanMonumentPieces.MonumentRoomFitter {
      @Override
      public boolean fits(OceanMonumentPieces.RoomDefinition p_228627_) {
         return true;
      }

      @Override
      public OceanMonumentPieces.OceanMonumentPiece create(Direction p_228629_, OceanMonumentPieces.RoomDefinition p_228630_, RandomSource p_228631_) {
         p_228630_.claimed = true;
         return new OceanMonumentPieces.OceanMonumentSimpleRoom(p_228629_, p_228630_, p_228631_);
      }
   }

   static class FitSimpleTopRoom implements OceanMonumentPieces.MonumentRoomFitter {
      @Override
      public boolean fits(OceanMonumentPieces.RoomDefinition p_228634_) {
         return !p_228634_.hasOpening[Direction.WEST.get3DDataValue()]
            && !p_228634_.hasOpening[Direction.EAST.get3DDataValue()]
            && !p_228634_.hasOpening[Direction.NORTH.get3DDataValue()]
            && !p_228634_.hasOpening[Direction.SOUTH.get3DDataValue()]
            && !p_228634_.hasOpening[Direction.UP.get3DDataValue()];
      }

      @Override
      public OceanMonumentPieces.OceanMonumentPiece create(Direction p_228636_, OceanMonumentPieces.RoomDefinition p_228637_, RandomSource p_228638_) {
         p_228637_.claimed = true;
         return new OceanMonumentPieces.OceanMonumentSimpleTopRoom(p_228636_, p_228637_);
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

      public MonumentBuilding(RandomSource p_228648_, int p_228649_, int p_228650_, Direction p_228651_) {
         super(StructurePieceType.OCEAN_MONUMENT_BUILDING, p_228651_, 0, makeBoundingBox(p_228649_, 39, p_228650_, p_228651_, 58, 23, 58));
         this.setOrientation(p_228651_);
         List<OceanMonumentPieces.RoomDefinition> list = this.generateRoomGraph(p_228648_);
         this.sourceRoom.claimed = true;
         this.childPieces.add(new OceanMonumentPieces.OceanMonumentEntryRoom(p_228651_, this.sourceRoom));
         this.childPieces.add(new OceanMonumentPieces.OceanMonumentCoreRoom(p_228651_, this.coreRoom));
         List<OceanMonumentPieces.MonumentRoomFitter> list1 = Lists.newArrayList();
         list1.add(new OceanMonumentPieces.FitDoubleXYRoom());
         list1.add(new OceanMonumentPieces.FitDoubleYZRoom());
         list1.add(new OceanMonumentPieces.FitDoubleZRoom());
         list1.add(new OceanMonumentPieces.FitDoubleXRoom());
         list1.add(new OceanMonumentPieces.FitDoubleYRoom());
         list1.add(new OceanMonumentPieces.FitSimpleTopRoom());
         list1.add(new OceanMonumentPieces.FitSimpleRoom());

         for (OceanMonumentPieces.RoomDefinition oceanmonumentpieces$roomdefinition : list) {
            if (!oceanmonumentpieces$roomdefinition.claimed && !oceanmonumentpieces$roomdefinition.isSpecial()) {
               for (OceanMonumentPieces.MonumentRoomFitter oceanmonumentpieces$monumentroomfitter : list1) {
                  if (oceanmonumentpieces$monumentroomfitter.fits(oceanmonumentpieces$roomdefinition)) {
                     this.childPieces.add(oceanmonumentpieces$monumentroomfitter.create(p_228651_, oceanmonumentpieces$roomdefinition, p_228648_));
                     break;
                  }
               }
            }
         }

         BlockPos blockpos = this.getWorldPos(9, 0, 22);

         for (OceanMonumentPieces.OceanMonumentPiece oceanmonumentpieces$oceanmonumentpiece : this.childPieces) {
            oceanmonumentpieces$oceanmonumentpiece.getBoundingBox().move(blockpos);
         }

         BoundingBox boundingbox = BoundingBox.fromCorners(this.getWorldPos(1, 1, 1), this.getWorldPos(23, 8, 21));
         BoundingBox boundingbox1 = BoundingBox.fromCorners(this.getWorldPos(34, 1, 1), this.getWorldPos(56, 8, 21));
         BoundingBox boundingbox2 = BoundingBox.fromCorners(this.getWorldPos(22, 13, 22), this.getWorldPos(35, 17, 35));
         int i = p_228648_.nextInt();
         this.childPieces.add(new OceanMonumentPieces.OceanMonumentWingRoom(p_228651_, boundingbox, i++));
         this.childPieces.add(new OceanMonumentPieces.OceanMonumentWingRoom(p_228651_, boundingbox1, i++));
         this.childPieces.add(new OceanMonumentPieces.OceanMonumentPenthouse(p_228651_, boundingbox2));
      }

      public MonumentBuilding(CompoundTag p_228653_) {
         super(StructurePieceType.OCEAN_MONUMENT_BUILDING, p_228653_);
      }

      private List<OceanMonumentPieces.RoomDefinition> generateRoomGraph(RandomSource p_228673_) {
         OceanMonumentPieces.RoomDefinition[] aoceanmonumentpieces$roomdefinition = new OceanMonumentPieces.RoomDefinition[75];

         for (int i = 0; i < 5; i++) {
            for (int j = 0; j < 4; j++) {
               int k = 0;
               int l = getRoomIndex(i, 0, j);
               aoceanmonumentpieces$roomdefinition[l] = new OceanMonumentPieces.RoomDefinition(l);
            }
         }

         for (int i2 = 0; i2 < 5; i2++) {
            for (int l2 = 0; l2 < 4; l2++) {
               int k3 = 1;
               int j4 = getRoomIndex(i2, 1, l2);
               aoceanmonumentpieces$roomdefinition[j4] = new OceanMonumentPieces.RoomDefinition(j4);
            }
         }

         for (int j2 = 1; j2 < 4; j2++) {
            for (int i3 = 0; i3 < 2; i3++) {
               int l3 = 2;
               int k4 = getRoomIndex(j2, 2, i3);
               aoceanmonumentpieces$roomdefinition[k4] = new OceanMonumentPieces.RoomDefinition(k4);
            }
         }

         this.sourceRoom = aoceanmonumentpieces$roomdefinition[GRIDROOM_SOURCE_INDEX];

         for (int k2 = 0; k2 < 5; k2++) {
            for (int j3 = 0; j3 < 5; j3++) {
               for (int i4 = 0; i4 < 3; i4++) {
                  int l4 = getRoomIndex(k2, i4, j3);
                  if (aoceanmonumentpieces$roomdefinition[l4] != null) {
                     for (Direction direction : Direction.values()) {
                        int i1 = k2 + direction.getStepX();
                        int j1 = i4 + direction.getStepY();
                        int k1 = j3 + direction.getStepZ();
                        if (i1 >= 0 && i1 < 5 && k1 >= 0 && k1 < 5 && j1 >= 0 && j1 < 3) {
                           int l1 = getRoomIndex(i1, j1, k1);
                           if (aoceanmonumentpieces$roomdefinition[l1] != null) {
                              if (k1 == j3) {
                                 aoceanmonumentpieces$roomdefinition[l4].setConnection(direction, aoceanmonumentpieces$roomdefinition[l1]);
                              } else {
                                 aoceanmonumentpieces$roomdefinition[l4].setConnection(direction.getOpposite(), aoceanmonumentpieces$roomdefinition[l1]);
                              }
                           }
                        }
                     }
                  }
               }
            }
         }

         OceanMonumentPieces.RoomDefinition oceanmonumentpieces$roomdefinition = new OceanMonumentPieces.RoomDefinition(1003);
         OceanMonumentPieces.RoomDefinition oceanmonumentpieces$roomdefinition1 = new OceanMonumentPieces.RoomDefinition(1001);
         OceanMonumentPieces.RoomDefinition oceanmonumentpieces$roomdefinition2 = new OceanMonumentPieces.RoomDefinition(1002);
         aoceanmonumentpieces$roomdefinition[GRIDROOM_TOP_CONNECT_INDEX].setConnection(Direction.UP, oceanmonumentpieces$roomdefinition);
         aoceanmonumentpieces$roomdefinition[GRIDROOM_LEFTWING_CONNECT_INDEX].setConnection(Direction.SOUTH, oceanmonumentpieces$roomdefinition1);
         aoceanmonumentpieces$roomdefinition[GRIDROOM_RIGHTWING_CONNECT_INDEX].setConnection(Direction.SOUTH, oceanmonumentpieces$roomdefinition2);
         oceanmonumentpieces$roomdefinition.claimed = true;
         oceanmonumentpieces$roomdefinition1.claimed = true;
         oceanmonumentpieces$roomdefinition2.claimed = true;
         this.sourceRoom.isSource = true;
         this.coreRoom = aoceanmonumentpieces$roomdefinition[getRoomIndex(p_228673_.nextInt(4), 0, 2)];
         this.coreRoom.claimed = true;
         this.coreRoom.connections[Direction.EAST.get3DDataValue()].claimed = true;
         this.coreRoom.connections[Direction.NORTH.get3DDataValue()].claimed = true;
         this.coreRoom.connections[Direction.EAST.get3DDataValue()].connections[Direction.NORTH.get3DDataValue()].claimed = true;
         this.coreRoom.connections[Direction.UP.get3DDataValue()].claimed = true;
         this.coreRoom.connections[Direction.EAST.get3DDataValue()].connections[Direction.UP.get3DDataValue()].claimed = true;
         this.coreRoom.connections[Direction.NORTH.get3DDataValue()].connections[Direction.UP.get3DDataValue()].claimed = true;
         this.coreRoom.connections[Direction.EAST.get3DDataValue()].connections[Direction.NORTH.get3DDataValue()].connections[Direction.UP.get3DDataValue()].claimed = true;
         ObjectArrayList<OceanMonumentPieces.RoomDefinition> objectarraylist = new ObjectArrayList();

         for (OceanMonumentPieces.RoomDefinition oceanmonumentpieces$roomdefinition4 : aoceanmonumentpieces$roomdefinition) {
            if (oceanmonumentpieces$roomdefinition4 != null) {
               oceanmonumentpieces$roomdefinition4.updateOpenings();
               objectarraylist.add(oceanmonumentpieces$roomdefinition4);
            }
         }

         oceanmonumentpieces$roomdefinition.updateOpenings();
         Util.shuffle(objectarraylist, p_228673_);
         int i5 = 1;
         ObjectListIterator var34 = objectarraylist.iterator();

         while (var34.hasNext()) {
            OceanMonumentPieces.RoomDefinition oceanmonumentpieces$roomdefinition3 = (OceanMonumentPieces.RoomDefinition)var34.next();
            int j5 = 0;
            int k5 = 0;

            while (j5 < 2 && k5 < 5) {
               k5++;
               int l5 = p_228673_.nextInt(6);
               if (oceanmonumentpieces$roomdefinition3.hasOpening[l5]) {
                  int i6 = Direction.from3DDataValue(l5).getOpposite().get3DDataValue();
                  oceanmonumentpieces$roomdefinition3.hasOpening[l5] = false;
                  oceanmonumentpieces$roomdefinition3.connections[l5].hasOpening[i6] = false;
                  if (oceanmonumentpieces$roomdefinition3.findSource(i5++) && oceanmonumentpieces$roomdefinition3.connections[l5].findSource(i5++)) {
                     j5++;
                  } else {
                     oceanmonumentpieces$roomdefinition3.hasOpening[l5] = true;
                     oceanmonumentpieces$roomdefinition3.connections[l5].hasOpening[i6] = true;
                  }
               }
            }
         }

         objectarraylist.add(oceanmonumentpieces$roomdefinition);
         objectarraylist.add(oceanmonumentpieces$roomdefinition1);
         objectarraylist.add(oceanmonumentpieces$roomdefinition2);
         return objectarraylist;
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228659_,
         StructureManager p_228660_,
         ChunkGenerator p_228661_,
         RandomSource p_228662_,
         BoundingBox p_228663_,
         ChunkPos p_228664_,
         BlockPos p_228665_
      ) {
         int i = Math.max(p_228659_.getSeaLevel(), 64) - this.boundingBox.minY();
         this.generateWaterBox(p_228659_, p_228663_, 0, 0, 0, 58, i, 58);
         this.generateWing(false, 0, p_228659_, p_228662_, p_228663_);
         this.generateWing(true, 33, p_228659_, p_228662_, p_228663_);
         this.generateEntranceArchs(p_228659_, p_228662_, p_228663_);
         this.generateEntranceWall(p_228659_, p_228662_, p_228663_);
         this.generateRoofPiece(p_228659_, p_228662_, p_228663_);
         this.generateLowerWall(p_228659_, p_228662_, p_228663_);
         this.generateMiddleWall(p_228659_, p_228662_, p_228663_);
         this.generateUpperWall(p_228659_, p_228662_, p_228663_);

         for (int j = 0; j < 7; j++) {
            int k = 0;

            while (k < 7) {
               if (k == 0 && j == 3) {
                  k = 6;
               }

               int l = j * 9;
               int i1 = k * 9;

               for (int j1 = 0; j1 < 4; j1++) {
                  for (int k1 = 0; k1 < 4; k1++) {
                     this.placeBlock(p_228659_, BASE_LIGHT, l + j1, 0, i1 + k1, p_228663_);
                     this.fillColumnDown(p_228659_, BASE_LIGHT, l + j1, -1, i1 + k1, p_228663_);
                  }
               }

               if (j != 0 && j != 6) {
                  k += 6;
               } else {
                  k++;
               }
            }
         }

         for (int l1 = 0; l1 < 5; l1++) {
            this.generateWaterBox(p_228659_, p_228663_, -1 - l1, 0 + l1 * 2, -1 - l1, -1 - l1, 23, 58 + l1);
            this.generateWaterBox(p_228659_, p_228663_, 58 + l1, 0 + l1 * 2, -1 - l1, 58 + l1, 23, 58 + l1);
            this.generateWaterBox(p_228659_, p_228663_, 0 - l1, 0 + l1 * 2, -1 - l1, 57 + l1, 23, -1 - l1);
            this.generateWaterBox(p_228659_, p_228663_, 0 - l1, 0 + l1 * 2, 58 + l1, 57 + l1, 23, 58 + l1);
         }

         for (OceanMonumentPieces.OceanMonumentPiece oceanmonumentpieces$oceanmonumentpiece : this.childPieces) {
            if (oceanmonumentpieces$oceanmonumentpiece.getBoundingBox().intersects(p_228663_)) {
               oceanmonumentpieces$oceanmonumentpiece.postProcess(p_228659_, p_228660_, p_228661_, p_228662_, p_228663_, p_228664_, p_228665_);
            }
         }
      }

      private void generateWing(boolean p_228667_, int p_228668_, WorldGenLevel p_228669_, RandomSource p_228670_, BoundingBox p_228671_) {
         int i = 24;
         if (this.chunkIntersects(p_228671_, p_228668_, 0, p_228668_ + 23, 20)) {
            this.generateBox(p_228669_, p_228671_, p_228668_ + 0, 0, 0, p_228668_ + 24, 0, 20, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(p_228669_, p_228671_, p_228668_ + 0, 1, 0, p_228668_ + 24, 10, 20);

            for (int j = 0; j < 4; j++) {
               this.generateBox(p_228669_, p_228671_, p_228668_ + j, j + 1, j, p_228668_ + j, j + 1, 20, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228669_, p_228671_, p_228668_ + j + 7, j + 5, j + 7, p_228668_ + j + 7, j + 5, 20, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228669_, p_228671_, p_228668_ + 17 - j, j + 5, j + 7, p_228668_ + 17 - j, j + 5, 20, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228669_, p_228671_, p_228668_ + 24 - j, j + 1, j, p_228668_ + 24 - j, j + 1, 20, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228669_, p_228671_, p_228668_ + j + 1, j + 1, j, p_228668_ + 23 - j, j + 1, j, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228669_, p_228671_, p_228668_ + j + 8, j + 5, j + 7, p_228668_ + 16 - j, j + 5, j + 7, BASE_LIGHT, BASE_LIGHT, false);
            }

            this.generateBox(p_228669_, p_228671_, p_228668_ + 4, 4, 4, p_228668_ + 6, 4, 20, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228669_, p_228671_, p_228668_ + 7, 4, 4, p_228668_ + 17, 4, 6, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228669_, p_228671_, p_228668_ + 18, 4, 4, p_228668_ + 20, 4, 20, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228669_, p_228671_, p_228668_ + 11, 8, 11, p_228668_ + 13, 8, 20, BASE_GRAY, BASE_GRAY, false);
            this.placeBlock(p_228669_, DOT_DECO_DATA, p_228668_ + 12, 9, 12, p_228671_);
            this.placeBlock(p_228669_, DOT_DECO_DATA, p_228668_ + 12, 9, 15, p_228671_);
            this.placeBlock(p_228669_, DOT_DECO_DATA, p_228668_ + 12, 9, 18, p_228671_);
            int j1 = p_228668_ + (p_228667_ ? 19 : 5);
            int k = p_228668_ + (p_228667_ ? 5 : 19);

            for (int l = 20; l >= 5; l -= 3) {
               this.placeBlock(p_228669_, DOT_DECO_DATA, j1, 5, l, p_228671_);
            }

            for (int k1 = 19; k1 >= 7; k1 -= 3) {
               this.placeBlock(p_228669_, DOT_DECO_DATA, k, 5, k1, p_228671_);
            }

            for (int l1 = 0; l1 < 4; l1++) {
               int i1 = p_228667_ ? p_228668_ + 24 - (17 - l1 * 3) : p_228668_ + 17 - l1 * 3;
               this.placeBlock(p_228669_, DOT_DECO_DATA, i1, 5, 5, p_228671_);
            }

            this.placeBlock(p_228669_, DOT_DECO_DATA, k, 5, 5, p_228671_);
            this.generateBox(p_228669_, p_228671_, p_228668_ + 11, 1, 12, p_228668_ + 13, 7, 12, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228669_, p_228671_, p_228668_ + 12, 1, 11, p_228668_ + 12, 7, 13, BASE_GRAY, BASE_GRAY, false);
         }
      }

      private void generateEntranceArchs(WorldGenLevel p_228655_, RandomSource p_228656_, BoundingBox p_228657_) {
         if (this.chunkIntersects(p_228657_, 22, 5, 35, 17)) {
            this.generateWaterBox(p_228655_, p_228657_, 25, 0, 0, 32, 8, 20);

            for (int i = 0; i < 4; i++) {
               this.generateBox(p_228655_, p_228657_, 24, 2, 5 + i * 4, 24, 4, 5 + i * 4, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228655_, p_228657_, 22, 4, 5 + i * 4, 23, 4, 5 + i * 4, BASE_LIGHT, BASE_LIGHT, false);
               this.placeBlock(p_228655_, BASE_LIGHT, 25, 5, 5 + i * 4, p_228657_);
               this.placeBlock(p_228655_, BASE_LIGHT, 26, 6, 5 + i * 4, p_228657_);
               this.placeBlock(p_228655_, LAMP_BLOCK, 26, 5, 5 + i * 4, p_228657_);
               this.generateBox(p_228655_, p_228657_, 33, 2, 5 + i * 4, 33, 4, 5 + i * 4, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228655_, p_228657_, 34, 4, 5 + i * 4, 35, 4, 5 + i * 4, BASE_LIGHT, BASE_LIGHT, false);
               this.placeBlock(p_228655_, BASE_LIGHT, 32, 5, 5 + i * 4, p_228657_);
               this.placeBlock(p_228655_, BASE_LIGHT, 31, 6, 5 + i * 4, p_228657_);
               this.placeBlock(p_228655_, LAMP_BLOCK, 31, 5, 5 + i * 4, p_228657_);
               this.generateBox(p_228655_, p_228657_, 27, 6, 5 + i * 4, 30, 6, 5 + i * 4, BASE_GRAY, BASE_GRAY, false);
            }
         }
      }

      private void generateEntranceWall(WorldGenLevel p_228675_, RandomSource p_228676_, BoundingBox p_228677_) {
         if (this.chunkIntersects(p_228677_, 15, 20, 42, 21)) {
            this.generateBox(p_228675_, p_228677_, 15, 0, 21, 42, 0, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(p_228675_, p_228677_, 26, 1, 21, 31, 3, 21);
            this.generateBox(p_228675_, p_228677_, 21, 12, 21, 36, 12, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228675_, p_228677_, 17, 11, 21, 40, 11, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228675_, p_228677_, 16, 10, 21, 41, 10, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228675_, p_228677_, 15, 7, 21, 42, 9, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228675_, p_228677_, 16, 6, 21, 41, 6, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228675_, p_228677_, 17, 5, 21, 40, 5, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228675_, p_228677_, 21, 4, 21, 36, 4, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228675_, p_228677_, 22, 3, 21, 26, 3, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228675_, p_228677_, 31, 3, 21, 35, 3, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228675_, p_228677_, 23, 2, 21, 25, 2, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228675_, p_228677_, 32, 2, 21, 34, 2, 21, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228675_, p_228677_, 28, 4, 20, 29, 4, 21, BASE_LIGHT, BASE_LIGHT, false);
            this.placeBlock(p_228675_, BASE_LIGHT, 27, 3, 21, p_228677_);
            this.placeBlock(p_228675_, BASE_LIGHT, 30, 3, 21, p_228677_);
            this.placeBlock(p_228675_, BASE_LIGHT, 26, 2, 21, p_228677_);
            this.placeBlock(p_228675_, BASE_LIGHT, 31, 2, 21, p_228677_);
            this.placeBlock(p_228675_, BASE_LIGHT, 25, 1, 21, p_228677_);
            this.placeBlock(p_228675_, BASE_LIGHT, 32, 1, 21, p_228677_);

            for (int i = 0; i < 7; i++) {
               this.placeBlock(p_228675_, BASE_BLACK, 28 - i, 6 + i, 21, p_228677_);
               this.placeBlock(p_228675_, BASE_BLACK, 29 + i, 6 + i, 21, p_228677_);
            }

            for (int j = 0; j < 4; j++) {
               this.placeBlock(p_228675_, BASE_BLACK, 28 - j, 9 + j, 21, p_228677_);
               this.placeBlock(p_228675_, BASE_BLACK, 29 + j, 9 + j, 21, p_228677_);
            }

            this.placeBlock(p_228675_, BASE_BLACK, 28, 12, 21, p_228677_);
            this.placeBlock(p_228675_, BASE_BLACK, 29, 12, 21, p_228677_);

            for (int k = 0; k < 3; k++) {
               this.placeBlock(p_228675_, BASE_BLACK, 22 - k * 2, 8, 21, p_228677_);
               this.placeBlock(p_228675_, BASE_BLACK, 22 - k * 2, 9, 21, p_228677_);
               this.placeBlock(p_228675_, BASE_BLACK, 35 + k * 2, 8, 21, p_228677_);
               this.placeBlock(p_228675_, BASE_BLACK, 35 + k * 2, 9, 21, p_228677_);
            }

            this.generateWaterBox(p_228675_, p_228677_, 15, 13, 21, 42, 15, 21);
            this.generateWaterBox(p_228675_, p_228677_, 15, 1, 21, 15, 6, 21);
            this.generateWaterBox(p_228675_, p_228677_, 16, 1, 21, 16, 5, 21);
            this.generateWaterBox(p_228675_, p_228677_, 17, 1, 21, 20, 4, 21);
            this.generateWaterBox(p_228675_, p_228677_, 21, 1, 21, 21, 3, 21);
            this.generateWaterBox(p_228675_, p_228677_, 22, 1, 21, 22, 2, 21);
            this.generateWaterBox(p_228675_, p_228677_, 23, 1, 21, 24, 1, 21);
            this.generateWaterBox(p_228675_, p_228677_, 42, 1, 21, 42, 6, 21);
            this.generateWaterBox(p_228675_, p_228677_, 41, 1, 21, 41, 5, 21);
            this.generateWaterBox(p_228675_, p_228677_, 37, 1, 21, 40, 4, 21);
            this.generateWaterBox(p_228675_, p_228677_, 36, 1, 21, 36, 3, 21);
            this.generateWaterBox(p_228675_, p_228677_, 33, 1, 21, 34, 1, 21);
            this.generateWaterBox(p_228675_, p_228677_, 35, 1, 21, 35, 2, 21);
         }
      }

      private void generateRoofPiece(WorldGenLevel p_228679_, RandomSource p_228680_, BoundingBox p_228681_) {
         if (this.chunkIntersects(p_228681_, 21, 21, 36, 36)) {
            this.generateBox(p_228679_, p_228681_, 21, 0, 22, 36, 0, 36, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(p_228679_, p_228681_, 21, 1, 22, 36, 23, 36);

            for (int i = 0; i < 4; i++) {
               this.generateBox(p_228679_, p_228681_, 21 + i, 13 + i, 21 + i, 36 - i, 13 + i, 21 + i, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228679_, p_228681_, 21 + i, 13 + i, 36 - i, 36 - i, 13 + i, 36 - i, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228679_, p_228681_, 21 + i, 13 + i, 22 + i, 21 + i, 13 + i, 35 - i, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228679_, p_228681_, 36 - i, 13 + i, 22 + i, 36 - i, 13 + i, 35 - i, BASE_LIGHT, BASE_LIGHT, false);
            }

            this.generateBox(p_228679_, p_228681_, 25, 16, 25, 32, 16, 32, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228679_, p_228681_, 25, 17, 25, 25, 19, 25, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228679_, p_228681_, 32, 17, 25, 32, 19, 25, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228679_, p_228681_, 25, 17, 32, 25, 19, 32, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228679_, p_228681_, 32, 17, 32, 32, 19, 32, BASE_LIGHT, BASE_LIGHT, false);
            this.placeBlock(p_228679_, BASE_LIGHT, 26, 20, 26, p_228681_);
            this.placeBlock(p_228679_, BASE_LIGHT, 27, 21, 27, p_228681_);
            this.placeBlock(p_228679_, LAMP_BLOCK, 27, 20, 27, p_228681_);
            this.placeBlock(p_228679_, BASE_LIGHT, 26, 20, 31, p_228681_);
            this.placeBlock(p_228679_, BASE_LIGHT, 27, 21, 30, p_228681_);
            this.placeBlock(p_228679_, LAMP_BLOCK, 27, 20, 30, p_228681_);
            this.placeBlock(p_228679_, BASE_LIGHT, 31, 20, 31, p_228681_);
            this.placeBlock(p_228679_, BASE_LIGHT, 30, 21, 30, p_228681_);
            this.placeBlock(p_228679_, LAMP_BLOCK, 30, 20, 30, p_228681_);
            this.placeBlock(p_228679_, BASE_LIGHT, 31, 20, 26, p_228681_);
            this.placeBlock(p_228679_, BASE_LIGHT, 30, 21, 27, p_228681_);
            this.placeBlock(p_228679_, LAMP_BLOCK, 30, 20, 27, p_228681_);
            this.generateBox(p_228679_, p_228681_, 28, 21, 27, 29, 21, 27, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228679_, p_228681_, 27, 21, 28, 27, 21, 29, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228679_, p_228681_, 28, 21, 30, 29, 21, 30, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228679_, p_228681_, 30, 21, 28, 30, 21, 29, BASE_GRAY, BASE_GRAY, false);
         }
      }

      private void generateLowerWall(WorldGenLevel p_228683_, RandomSource p_228684_, BoundingBox p_228685_) {
         if (this.chunkIntersects(p_228685_, 0, 21, 6, 58)) {
            this.generateBox(p_228683_, p_228685_, 0, 0, 21, 6, 0, 57, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(p_228683_, p_228685_, 0, 1, 21, 6, 7, 57);
            this.generateBox(p_228683_, p_228685_, 4, 4, 21, 6, 4, 53, BASE_GRAY, BASE_GRAY, false);

            for (int i = 0; i < 4; i++) {
               this.generateBox(p_228683_, p_228685_, i, i + 1, 21, i, i + 1, 57 - i, BASE_LIGHT, BASE_LIGHT, false);
            }

            for (int j = 23; j < 53; j += 3) {
               this.placeBlock(p_228683_, DOT_DECO_DATA, 5, 5, j, p_228685_);
            }

            this.placeBlock(p_228683_, DOT_DECO_DATA, 5, 5, 52, p_228685_);

            for (int k = 0; k < 4; k++) {
               this.generateBox(p_228683_, p_228685_, k, k + 1, 21, k, k + 1, 57 - k, BASE_LIGHT, BASE_LIGHT, false);
            }

            this.generateBox(p_228683_, p_228685_, 4, 1, 52, 6, 3, 52, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228683_, p_228685_, 5, 1, 51, 5, 3, 53, BASE_GRAY, BASE_GRAY, false);
         }

         if (this.chunkIntersects(p_228685_, 51, 21, 58, 58)) {
            this.generateBox(p_228683_, p_228685_, 51, 0, 21, 57, 0, 57, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(p_228683_, p_228685_, 51, 1, 21, 57, 7, 57);
            this.generateBox(p_228683_, p_228685_, 51, 4, 21, 53, 4, 53, BASE_GRAY, BASE_GRAY, false);

            for (int l = 0; l < 4; l++) {
               this.generateBox(p_228683_, p_228685_, 57 - l, l + 1, 21, 57 - l, l + 1, 57 - l, BASE_LIGHT, BASE_LIGHT, false);
            }

            for (int i1 = 23; i1 < 53; i1 += 3) {
               this.placeBlock(p_228683_, DOT_DECO_DATA, 52, 5, i1, p_228685_);
            }

            this.placeBlock(p_228683_, DOT_DECO_DATA, 52, 5, 52, p_228685_);
            this.generateBox(p_228683_, p_228685_, 51, 1, 52, 53, 3, 52, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228683_, p_228685_, 52, 1, 51, 52, 3, 53, BASE_GRAY, BASE_GRAY, false);
         }

         if (this.chunkIntersects(p_228685_, 0, 51, 57, 57)) {
            this.generateBox(p_228683_, p_228685_, 7, 0, 51, 50, 0, 57, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(p_228683_, p_228685_, 7, 1, 51, 50, 10, 57);

            for (int j1 = 0; j1 < 4; j1++) {
               this.generateBox(p_228683_, p_228685_, j1 + 1, j1 + 1, 57 - j1, 56 - j1, j1 + 1, 57 - j1, BASE_LIGHT, BASE_LIGHT, false);
            }
         }
      }

      private void generateMiddleWall(WorldGenLevel p_228687_, RandomSource p_228688_, BoundingBox p_228689_) {
         if (this.chunkIntersects(p_228689_, 7, 21, 13, 50)) {
            this.generateBox(p_228687_, p_228689_, 7, 0, 21, 13, 0, 50, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(p_228687_, p_228689_, 7, 1, 21, 13, 10, 50);
            this.generateBox(p_228687_, p_228689_, 11, 8, 21, 13, 8, 53, BASE_GRAY, BASE_GRAY, false);

            for (int i = 0; i < 4; i++) {
               this.generateBox(p_228687_, p_228689_, i + 7, i + 5, 21, i + 7, i + 5, 54, BASE_LIGHT, BASE_LIGHT, false);
            }

            for (int j = 21; j <= 45; j += 3) {
               this.placeBlock(p_228687_, DOT_DECO_DATA, 12, 9, j, p_228689_);
            }
         }

         if (this.chunkIntersects(p_228689_, 44, 21, 50, 54)) {
            this.generateBox(p_228687_, p_228689_, 44, 0, 21, 50, 0, 50, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(p_228687_, p_228689_, 44, 1, 21, 50, 10, 50);
            this.generateBox(p_228687_, p_228689_, 44, 8, 21, 46, 8, 53, BASE_GRAY, BASE_GRAY, false);

            for (int k = 0; k < 4; k++) {
               this.generateBox(p_228687_, p_228689_, 50 - k, k + 5, 21, 50 - k, k + 5, 54, BASE_LIGHT, BASE_LIGHT, false);
            }

            for (int l = 21; l <= 45; l += 3) {
               this.placeBlock(p_228687_, DOT_DECO_DATA, 45, 9, l, p_228689_);
            }
         }

         if (this.chunkIntersects(p_228689_, 8, 44, 49, 54)) {
            this.generateBox(p_228687_, p_228689_, 14, 0, 44, 43, 0, 50, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(p_228687_, p_228689_, 14, 1, 44, 43, 10, 50);

            for (int i1 = 12; i1 <= 45; i1 += 3) {
               this.placeBlock(p_228687_, DOT_DECO_DATA, i1, 9, 45, p_228689_);
               this.placeBlock(p_228687_, DOT_DECO_DATA, i1, 9, 52, p_228689_);
               if (i1 == 12 || i1 == 18 || i1 == 24 || i1 == 33 || i1 == 39 || i1 == 45) {
                  this.placeBlock(p_228687_, DOT_DECO_DATA, i1, 9, 47, p_228689_);
                  this.placeBlock(p_228687_, DOT_DECO_DATA, i1, 9, 50, p_228689_);
                  this.placeBlock(p_228687_, DOT_DECO_DATA, i1, 10, 45, p_228689_);
                  this.placeBlock(p_228687_, DOT_DECO_DATA, i1, 10, 46, p_228689_);
                  this.placeBlock(p_228687_, DOT_DECO_DATA, i1, 10, 51, p_228689_);
                  this.placeBlock(p_228687_, DOT_DECO_DATA, i1, 10, 52, p_228689_);
                  this.placeBlock(p_228687_, DOT_DECO_DATA, i1, 11, 47, p_228689_);
                  this.placeBlock(p_228687_, DOT_DECO_DATA, i1, 11, 50, p_228689_);
                  this.placeBlock(p_228687_, DOT_DECO_DATA, i1, 12, 48, p_228689_);
                  this.placeBlock(p_228687_, DOT_DECO_DATA, i1, 12, 49, p_228689_);
               }
            }

            for (int j1 = 0; j1 < 3; j1++) {
               this.generateBox(p_228687_, p_228689_, 8 + j1, 5 + j1, 54, 49 - j1, 5 + j1, 54, BASE_GRAY, BASE_GRAY, false);
            }

            this.generateBox(p_228687_, p_228689_, 11, 8, 54, 46, 8, 54, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228687_, p_228689_, 14, 8, 44, 43, 8, 53, BASE_GRAY, BASE_GRAY, false);
         }
      }

      private void generateUpperWall(WorldGenLevel p_228691_, RandomSource p_228692_, BoundingBox p_228693_) {
         if (this.chunkIntersects(p_228693_, 14, 21, 20, 43)) {
            this.generateBox(p_228691_, p_228693_, 14, 0, 21, 20, 0, 43, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(p_228691_, p_228693_, 14, 1, 22, 20, 14, 43);
            this.generateBox(p_228691_, p_228693_, 18, 12, 22, 20, 12, 39, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228691_, p_228693_, 18, 12, 21, 20, 12, 21, BASE_LIGHT, BASE_LIGHT, false);

            for (int i = 0; i < 4; i++) {
               this.generateBox(p_228691_, p_228693_, i + 14, i + 9, 21, i + 14, i + 9, 43 - i, BASE_LIGHT, BASE_LIGHT, false);
            }

            for (int j = 23; j <= 39; j += 3) {
               this.placeBlock(p_228691_, DOT_DECO_DATA, 19, 13, j, p_228693_);
            }
         }

         if (this.chunkIntersects(p_228693_, 37, 21, 43, 43)) {
            this.generateBox(p_228691_, p_228693_, 37, 0, 21, 43, 0, 43, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(p_228691_, p_228693_, 37, 1, 22, 43, 14, 43);
            this.generateBox(p_228691_, p_228693_, 37, 12, 22, 39, 12, 39, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228691_, p_228693_, 37, 12, 21, 39, 12, 21, BASE_LIGHT, BASE_LIGHT, false);

            for (int k = 0; k < 4; k++) {
               this.generateBox(p_228691_, p_228693_, 43 - k, k + 9, 21, 43 - k, k + 9, 43 - k, BASE_LIGHT, BASE_LIGHT, false);
            }

            for (int l = 23; l <= 39; l += 3) {
               this.placeBlock(p_228691_, DOT_DECO_DATA, 38, 13, l, p_228693_);
            }
         }

         if (this.chunkIntersects(p_228693_, 15, 37, 42, 43)) {
            this.generateBox(p_228691_, p_228693_, 21, 0, 37, 36, 0, 43, BASE_GRAY, BASE_GRAY, false);
            this.generateWaterBox(p_228691_, p_228693_, 21, 1, 37, 36, 14, 43);
            this.generateBox(p_228691_, p_228693_, 21, 12, 37, 36, 12, 39, BASE_GRAY, BASE_GRAY, false);

            for (int i1 = 0; i1 < 4; i1++) {
               this.generateBox(p_228691_, p_228693_, 15 + i1, i1 + 9, 43 - i1, 42 - i1, i1 + 9, 43 - i1, BASE_LIGHT, BASE_LIGHT, false);
            }

            for (int j1 = 21; j1 <= 36; j1 += 3) {
               this.placeBlock(p_228691_, DOT_DECO_DATA, j1, 13, 38, p_228693_);
            }
         }
      }
   }

   interface MonumentRoomFitter {
      boolean fits(OceanMonumentPieces.RoomDefinition var1);

      OceanMonumentPieces.OceanMonumentPiece create(Direction var1, OceanMonumentPieces.RoomDefinition var2, RandomSource var3);
   }

   public static class OceanMonumentCoreRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentCoreRoom(Direction p_228699_, OceanMonumentPieces.RoomDefinition p_228700_) {
         super(StructurePieceType.OCEAN_MONUMENT_CORE_ROOM, 1, p_228699_, p_228700_, 2, 2, 2);
      }

      public OceanMonumentCoreRoom(CompoundTag p_228702_) {
         super(StructurePieceType.OCEAN_MONUMENT_CORE_ROOM, p_228702_);
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228704_,
         StructureManager p_228705_,
         ChunkGenerator p_228706_,
         RandomSource p_228707_,
         BoundingBox p_228708_,
         ChunkPos p_228709_,
         BlockPos p_228710_
      ) {
         this.generateBoxOnFillOnly(p_228704_, p_228708_, 1, 8, 0, 14, 8, 14, BASE_GRAY);
         int i = 7;
         BlockState blockstate = BASE_LIGHT;
         this.generateBox(p_228704_, p_228708_, 0, 7, 0, 0, 7, 15, blockstate, blockstate, false);
         this.generateBox(p_228704_, p_228708_, 15, 7, 0, 15, 7, 15, blockstate, blockstate, false);
         this.generateBox(p_228704_, p_228708_, 1, 7, 0, 15, 7, 0, blockstate, blockstate, false);
         this.generateBox(p_228704_, p_228708_, 1, 7, 15, 14, 7, 15, blockstate, blockstate, false);

         for (int k = 1; k <= 6; k++) {
            blockstate = BASE_LIGHT;
            if (k == 2 || k == 6) {
               blockstate = BASE_GRAY;
            }

            for (int j = 0; j <= 15; j += 15) {
               this.generateBox(p_228704_, p_228708_, j, k, 0, j, k, 1, blockstate, blockstate, false);
               this.generateBox(p_228704_, p_228708_, j, k, 6, j, k, 9, blockstate, blockstate, false);
               this.generateBox(p_228704_, p_228708_, j, k, 14, j, k, 15, blockstate, blockstate, false);
            }

            this.generateBox(p_228704_, p_228708_, 1, k, 0, 1, k, 0, blockstate, blockstate, false);
            this.generateBox(p_228704_, p_228708_, 6, k, 0, 9, k, 0, blockstate, blockstate, false);
            this.generateBox(p_228704_, p_228708_, 14, k, 0, 14, k, 0, blockstate, blockstate, false);
            this.generateBox(p_228704_, p_228708_, 1, k, 15, 14, k, 15, blockstate, blockstate, false);
         }

         this.generateBox(p_228704_, p_228708_, 6, 3, 6, 9, 6, 9, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(p_228704_, p_228708_, 7, 4, 7, 8, 5, 8, Blocks.GOLD_BLOCK.defaultBlockState(), Blocks.GOLD_BLOCK.defaultBlockState(), false);

         for (int l = 3; l <= 6; l += 3) {
            for (int i1 = 6; i1 <= 9; i1 += 3) {
               this.placeBlock(p_228704_, LAMP_BLOCK, i1, l, 6, p_228708_);
               this.placeBlock(p_228704_, LAMP_BLOCK, i1, l, 9, p_228708_);
            }
         }

         this.generateBox(p_228704_, p_228708_, 5, 1, 6, 5, 2, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 5, 1, 9, 5, 2, 9, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 10, 1, 6, 10, 2, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 10, 1, 9, 10, 2, 9, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 6, 1, 5, 6, 2, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 9, 1, 5, 9, 2, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 6, 1, 10, 6, 2, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 9, 1, 10, 9, 2, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 5, 2, 5, 5, 6, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 5, 2, 10, 5, 6, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 10, 2, 5, 10, 6, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 10, 2, 10, 10, 6, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 5, 7, 1, 5, 7, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 10, 7, 1, 10, 7, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 5, 7, 9, 5, 7, 14, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 10, 7, 9, 10, 7, 14, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 1, 7, 5, 6, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 1, 7, 10, 6, 7, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 9, 7, 5, 14, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 9, 7, 10, 14, 7, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 2, 1, 2, 2, 1, 3, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 3, 1, 2, 3, 1, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 13, 1, 2, 13, 1, 3, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 12, 1, 2, 12, 1, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 2, 1, 12, 2, 1, 13, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 3, 1, 13, 3, 1, 13, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 13, 1, 12, 13, 1, 13, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228704_, p_228708_, 12, 1, 13, 12, 1, 13, BASE_LIGHT, BASE_LIGHT, false);
      }
   }

   public static class OceanMonumentDoubleXRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentDoubleXRoom(Direction p_228712_, OceanMonumentPieces.RoomDefinition p_228713_) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_X_ROOM, 1, p_228712_, p_228713_, 2, 1, 1);
      }

      public OceanMonumentDoubleXRoom(CompoundTag p_228715_) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_X_ROOM, p_228715_);
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228717_,
         StructureManager p_228718_,
         ChunkGenerator p_228719_,
         RandomSource p_228720_,
         BoundingBox p_228721_,
         ChunkPos p_228722_,
         BlockPos p_228723_
      ) {
         OceanMonumentPieces.RoomDefinition oceanmonumentpieces$roomdefinition = this.roomDefinition.connections[Direction.EAST.get3DDataValue()];
         OceanMonumentPieces.RoomDefinition oceanmonumentpieces$roomdefinition1 = this.roomDefinition;
         if (this.roomDefinition.index / 25 > 0) {
            this.generateDefaultFloor(p_228717_, p_228721_, 8, 0, oceanmonumentpieces$roomdefinition.hasOpening[Direction.DOWN.get3DDataValue()]);
            this.generateDefaultFloor(p_228717_, p_228721_, 0, 0, oceanmonumentpieces$roomdefinition1.hasOpening[Direction.DOWN.get3DDataValue()]);
         }

         if (oceanmonumentpieces$roomdefinition1.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(p_228717_, p_228721_, 1, 4, 1, 7, 4, 6, BASE_GRAY);
         }

         if (oceanmonumentpieces$roomdefinition.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(p_228717_, p_228721_, 8, 4, 1, 14, 4, 6, BASE_GRAY);
         }

         this.generateBox(p_228717_, p_228721_, 0, 3, 0, 0, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228717_, p_228721_, 15, 3, 0, 15, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228717_, p_228721_, 1, 3, 0, 15, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228717_, p_228721_, 1, 3, 7, 14, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228717_, p_228721_, 0, 2, 0, 0, 2, 7, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(p_228717_, p_228721_, 15, 2, 0, 15, 2, 7, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(p_228717_, p_228721_, 1, 2, 0, 15, 2, 0, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(p_228717_, p_228721_, 1, 2, 7, 14, 2, 7, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(p_228717_, p_228721_, 0, 1, 0, 0, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228717_, p_228721_, 15, 1, 0, 15, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228717_, p_228721_, 1, 1, 0, 15, 1, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228717_, p_228721_, 1, 1, 7, 14, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228717_, p_228721_, 5, 1, 0, 10, 1, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228717_, p_228721_, 6, 2, 0, 9, 2, 3, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(p_228717_, p_228721_, 5, 3, 0, 10, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.placeBlock(p_228717_, LAMP_BLOCK, 6, 2, 3, p_228721_);
         this.placeBlock(p_228717_, LAMP_BLOCK, 9, 2, 3, p_228721_);
         if (oceanmonumentpieces$roomdefinition1.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(p_228717_, p_228721_, 3, 1, 0, 4, 2, 0);
         }

         if (oceanmonumentpieces$roomdefinition1.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(p_228717_, p_228721_, 3, 1, 7, 4, 2, 7);
         }

         if (oceanmonumentpieces$roomdefinition1.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(p_228717_, p_228721_, 0, 1, 3, 0, 2, 4);
         }

         if (oceanmonumentpieces$roomdefinition.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(p_228717_, p_228721_, 11, 1, 0, 12, 2, 0);
         }

         if (oceanmonumentpieces$roomdefinition.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(p_228717_, p_228721_, 11, 1, 7, 12, 2, 7);
         }

         if (oceanmonumentpieces$roomdefinition.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(p_228717_, p_228721_, 15, 1, 3, 15, 2, 4);
         }
      }
   }

   public static class OceanMonumentDoubleXYRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentDoubleXYRoom(Direction p_228725_, OceanMonumentPieces.RoomDefinition p_228726_) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_XY_ROOM, 1, p_228725_, p_228726_, 2, 2, 1);
      }

      public OceanMonumentDoubleXYRoom(CompoundTag p_228728_) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_XY_ROOM, p_228728_);
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228730_,
         StructureManager p_228731_,
         ChunkGenerator p_228732_,
         RandomSource p_228733_,
         BoundingBox p_228734_,
         ChunkPos p_228735_,
         BlockPos p_228736_
      ) {
         OceanMonumentPieces.RoomDefinition oceanmonumentpieces$roomdefinition = this.roomDefinition.connections[Direction.EAST.get3DDataValue()];
         OceanMonumentPieces.RoomDefinition oceanmonumentpieces$roomdefinition1 = this.roomDefinition;
         OceanMonumentPieces.RoomDefinition oceanmonumentpieces$roomdefinition2 = oceanmonumentpieces$roomdefinition1.connections[Direction.UP.get3DDataValue()];
         OceanMonumentPieces.RoomDefinition oceanmonumentpieces$roomdefinition3 = oceanmonumentpieces$roomdefinition.connections[Direction.UP.get3DDataValue()];
         if (this.roomDefinition.index / 25 > 0) {
            this.generateDefaultFloor(p_228730_, p_228734_, 8, 0, oceanmonumentpieces$roomdefinition.hasOpening[Direction.DOWN.get3DDataValue()]);
            this.generateDefaultFloor(p_228730_, p_228734_, 0, 0, oceanmonumentpieces$roomdefinition1.hasOpening[Direction.DOWN.get3DDataValue()]);
         }

         if (oceanmonumentpieces$roomdefinition2.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(p_228730_, p_228734_, 1, 8, 1, 7, 8, 6, BASE_GRAY);
         }

         if (oceanmonumentpieces$roomdefinition3.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(p_228730_, p_228734_, 8, 8, 1, 14, 8, 6, BASE_GRAY);
         }

         for (int i = 1; i <= 7; i++) {
            BlockState blockstate = BASE_LIGHT;
            if (i == 2 || i == 6) {
               blockstate = BASE_GRAY;
            }

            this.generateBox(p_228730_, p_228734_, 0, i, 0, 0, i, 7, blockstate, blockstate, false);
            this.generateBox(p_228730_, p_228734_, 15, i, 0, 15, i, 7, blockstate, blockstate, false);
            this.generateBox(p_228730_, p_228734_, 1, i, 0, 15, i, 0, blockstate, blockstate, false);
            this.generateBox(p_228730_, p_228734_, 1, i, 7, 14, i, 7, blockstate, blockstate, false);
         }

         this.generateBox(p_228730_, p_228734_, 2, 1, 3, 2, 7, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228730_, p_228734_, 3, 1, 2, 4, 7, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228730_, p_228734_, 3, 1, 5, 4, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228730_, p_228734_, 13, 1, 3, 13, 7, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228730_, p_228734_, 11, 1, 2, 12, 7, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228730_, p_228734_, 11, 1, 5, 12, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228730_, p_228734_, 5, 1, 3, 5, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228730_, p_228734_, 10, 1, 3, 10, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228730_, p_228734_, 5, 7, 2, 10, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228730_, p_228734_, 5, 5, 2, 5, 7, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228730_, p_228734_, 10, 5, 2, 10, 7, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228730_, p_228734_, 5, 5, 5, 5, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228730_, p_228734_, 10, 5, 5, 10, 7, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.placeBlock(p_228730_, BASE_LIGHT, 6, 6, 2, p_228734_);
         this.placeBlock(p_228730_, BASE_LIGHT, 9, 6, 2, p_228734_);
         this.placeBlock(p_228730_, BASE_LIGHT, 6, 6, 5, p_228734_);
         this.placeBlock(p_228730_, BASE_LIGHT, 9, 6, 5, p_228734_);
         this.generateBox(p_228730_, p_228734_, 5, 4, 3, 6, 4, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228730_, p_228734_, 9, 4, 3, 10, 4, 4, BASE_LIGHT, BASE_LIGHT, false);
         this.placeBlock(p_228730_, LAMP_BLOCK, 5, 4, 2, p_228734_);
         this.placeBlock(p_228730_, LAMP_BLOCK, 5, 4, 5, p_228734_);
         this.placeBlock(p_228730_, LAMP_BLOCK, 10, 4, 2, p_228734_);
         this.placeBlock(p_228730_, LAMP_BLOCK, 10, 4, 5, p_228734_);
         if (oceanmonumentpieces$roomdefinition1.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(p_228730_, p_228734_, 3, 1, 0, 4, 2, 0);
         }

         if (oceanmonumentpieces$roomdefinition1.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(p_228730_, p_228734_, 3, 1, 7, 4, 2, 7);
         }

         if (oceanmonumentpieces$roomdefinition1.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(p_228730_, p_228734_, 0, 1, 3, 0, 2, 4);
         }

         if (oceanmonumentpieces$roomdefinition.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(p_228730_, p_228734_, 11, 1, 0, 12, 2, 0);
         }

         if (oceanmonumentpieces$roomdefinition.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(p_228730_, p_228734_, 11, 1, 7, 12, 2, 7);
         }

         if (oceanmonumentpieces$roomdefinition.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(p_228730_, p_228734_, 15, 1, 3, 15, 2, 4);
         }

         if (oceanmonumentpieces$roomdefinition2.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(p_228730_, p_228734_, 3, 5, 0, 4, 6, 0);
         }

         if (oceanmonumentpieces$roomdefinition2.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(p_228730_, p_228734_, 3, 5, 7, 4, 6, 7);
         }

         if (oceanmonumentpieces$roomdefinition2.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(p_228730_, p_228734_, 0, 5, 3, 0, 6, 4);
         }

         if (oceanmonumentpieces$roomdefinition3.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(p_228730_, p_228734_, 11, 5, 0, 12, 6, 0);
         }

         if (oceanmonumentpieces$roomdefinition3.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(p_228730_, p_228734_, 11, 5, 7, 12, 6, 7);
         }

         if (oceanmonumentpieces$roomdefinition3.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(p_228730_, p_228734_, 15, 5, 3, 15, 6, 4);
         }
      }
   }

   public static class OceanMonumentDoubleYRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentDoubleYRoom(Direction p_228738_, OceanMonumentPieces.RoomDefinition p_228739_) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_Y_ROOM, 1, p_228738_, p_228739_, 1, 2, 1);
      }

      public OceanMonumentDoubleYRoom(CompoundTag p_228741_) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_Y_ROOM, p_228741_);
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228743_,
         StructureManager p_228744_,
         ChunkGenerator p_228745_,
         RandomSource p_228746_,
         BoundingBox p_228747_,
         ChunkPos p_228748_,
         BlockPos p_228749_
      ) {
         if (this.roomDefinition.index / 25 > 0) {
            this.generateDefaultFloor(p_228743_, p_228747_, 0, 0, this.roomDefinition.hasOpening[Direction.DOWN.get3DDataValue()]);
         }

         OceanMonumentPieces.RoomDefinition oceanmonumentpieces$roomdefinition = this.roomDefinition.connections[Direction.UP.get3DDataValue()];
         if (oceanmonumentpieces$roomdefinition.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(p_228743_, p_228747_, 1, 8, 1, 6, 8, 6, BASE_GRAY);
         }

         this.generateBox(p_228743_, p_228747_, 0, 4, 0, 0, 4, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228743_, p_228747_, 7, 4, 0, 7, 4, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228743_, p_228747_, 1, 4, 0, 6, 4, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228743_, p_228747_, 1, 4, 7, 6, 4, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228743_, p_228747_, 2, 4, 1, 2, 4, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228743_, p_228747_, 1, 4, 2, 1, 4, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228743_, p_228747_, 5, 4, 1, 5, 4, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228743_, p_228747_, 6, 4, 2, 6, 4, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228743_, p_228747_, 2, 4, 5, 2, 4, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228743_, p_228747_, 1, 4, 5, 1, 4, 5, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228743_, p_228747_, 5, 4, 5, 5, 4, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228743_, p_228747_, 6, 4, 5, 6, 4, 5, BASE_LIGHT, BASE_LIGHT, false);
         OceanMonumentPieces.RoomDefinition oceanmonumentpieces$roomdefinition1 = this.roomDefinition;

         for (int i = 1; i <= 5; i += 4) {
            int j = 0;
            if (oceanmonumentpieces$roomdefinition1.hasOpening[Direction.SOUTH.get3DDataValue()]) {
               this.generateBox(p_228743_, p_228747_, 2, i, j, 2, i + 2, j, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228743_, p_228747_, 5, i, j, 5, i + 2, j, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228743_, p_228747_, 3, i + 2, j, 4, i + 2, j, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(p_228743_, p_228747_, 0, i, j, 7, i + 2, j, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228743_, p_228747_, 0, i + 1, j, 7, i + 1, j, BASE_GRAY, BASE_GRAY, false);
            }

            int b0 = 7;
            if (oceanmonumentpieces$roomdefinition1.hasOpening[Direction.NORTH.get3DDataValue()]) {
               this.generateBox(p_228743_, p_228747_, 2, i, b0, 2, i + 2, b0, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228743_, p_228747_, 5, i, b0, 5, i + 2, b0, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228743_, p_228747_, 3, i + 2, b0, 4, i + 2, b0, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(p_228743_, p_228747_, 0, i, b0, 7, i + 2, b0, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228743_, p_228747_, 0, i + 1, b0, 7, i + 1, b0, BASE_GRAY, BASE_GRAY, false);
            }

            int k = 0;
            if (oceanmonumentpieces$roomdefinition1.hasOpening[Direction.WEST.get3DDataValue()]) {
               this.generateBox(p_228743_, p_228747_, k, i, 2, k, i + 2, 2, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228743_, p_228747_, k, i, 5, k, i + 2, 5, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228743_, p_228747_, k, i + 2, 3, k, i + 2, 4, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(p_228743_, p_228747_, k, i, 0, k, i + 2, 7, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228743_, p_228747_, k, i + 1, 0, k, i + 1, 7, BASE_GRAY, BASE_GRAY, false);
            }

            int b1 = 7;
            if (oceanmonumentpieces$roomdefinition1.hasOpening[Direction.EAST.get3DDataValue()]) {
               this.generateBox(p_228743_, p_228747_, b1, i, 2, b1, i + 2, 2, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228743_, p_228747_, b1, i, 5, b1, i + 2, 5, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228743_, p_228747_, b1, i + 2, 3, b1, i + 2, 4, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(p_228743_, p_228747_, b1, i, 0, b1, i + 2, 7, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228743_, p_228747_, b1, i + 1, 0, b1, i + 1, 7, BASE_GRAY, BASE_GRAY, false);
            }

            oceanmonumentpieces$roomdefinition1 = oceanmonumentpieces$roomdefinition;
         }
      }
   }

   public static class OceanMonumentDoubleYZRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentDoubleYZRoom(Direction p_228751_, OceanMonumentPieces.RoomDefinition p_228752_) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_YZ_ROOM, 1, p_228751_, p_228752_, 1, 2, 2);
      }

      public OceanMonumentDoubleYZRoom(CompoundTag p_228754_) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_YZ_ROOM, p_228754_);
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228756_,
         StructureManager p_228757_,
         ChunkGenerator p_228758_,
         RandomSource p_228759_,
         BoundingBox p_228760_,
         ChunkPos p_228761_,
         BlockPos p_228762_
      ) {
         OceanMonumentPieces.RoomDefinition oceanmonumentpieces$roomdefinition = this.roomDefinition.connections[Direction.NORTH.get3DDataValue()];
         OceanMonumentPieces.RoomDefinition oceanmonumentpieces$roomdefinition1 = this.roomDefinition;
         OceanMonumentPieces.RoomDefinition oceanmonumentpieces$roomdefinition2 = oceanmonumentpieces$roomdefinition.connections[Direction.UP.get3DDataValue()];
         OceanMonumentPieces.RoomDefinition oceanmonumentpieces$roomdefinition3 = oceanmonumentpieces$roomdefinition1.connections[Direction.UP.get3DDataValue()];
         if (this.roomDefinition.index / 25 > 0) {
            this.generateDefaultFloor(p_228756_, p_228760_, 0, 8, oceanmonumentpieces$roomdefinition.hasOpening[Direction.DOWN.get3DDataValue()]);
            this.generateDefaultFloor(p_228756_, p_228760_, 0, 0, oceanmonumentpieces$roomdefinition1.hasOpening[Direction.DOWN.get3DDataValue()]);
         }

         if (oceanmonumentpieces$roomdefinition3.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(p_228756_, p_228760_, 1, 8, 1, 6, 8, 7, BASE_GRAY);
         }

         if (oceanmonumentpieces$roomdefinition2.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(p_228756_, p_228760_, 1, 8, 8, 6, 8, 14, BASE_GRAY);
         }

         for (int i = 1; i <= 7; i++) {
            BlockState blockstate = BASE_LIGHT;
            if (i == 2 || i == 6) {
               blockstate = BASE_GRAY;
            }

            this.generateBox(p_228756_, p_228760_, 0, i, 0, 0, i, 15, blockstate, blockstate, false);
            this.generateBox(p_228756_, p_228760_, 7, i, 0, 7, i, 15, blockstate, blockstate, false);
            this.generateBox(p_228756_, p_228760_, 1, i, 0, 6, i, 0, blockstate, blockstate, false);
            this.generateBox(p_228756_, p_228760_, 1, i, 15, 6, i, 15, blockstate, blockstate, false);
         }

         for (int j = 1; j <= 7; j++) {
            BlockState blockstate1 = BASE_BLACK;
            if (j == 2 || j == 6) {
               blockstate1 = LAMP_BLOCK;
            }

            this.generateBox(p_228756_, p_228760_, 3, j, 7, 4, j, 8, blockstate1, blockstate1, false);
         }

         if (oceanmonumentpieces$roomdefinition1.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(p_228756_, p_228760_, 3, 1, 0, 4, 2, 0);
         }

         if (oceanmonumentpieces$roomdefinition1.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(p_228756_, p_228760_, 7, 1, 3, 7, 2, 4);
         }

         if (oceanmonumentpieces$roomdefinition1.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(p_228756_, p_228760_, 0, 1, 3, 0, 2, 4);
         }

         if (oceanmonumentpieces$roomdefinition.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(p_228756_, p_228760_, 3, 1, 15, 4, 2, 15);
         }

         if (oceanmonumentpieces$roomdefinition.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(p_228756_, p_228760_, 0, 1, 11, 0, 2, 12);
         }

         if (oceanmonumentpieces$roomdefinition.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(p_228756_, p_228760_, 7, 1, 11, 7, 2, 12);
         }

         if (oceanmonumentpieces$roomdefinition3.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(p_228756_, p_228760_, 3, 5, 0, 4, 6, 0);
         }

         if (oceanmonumentpieces$roomdefinition3.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(p_228756_, p_228760_, 7, 5, 3, 7, 6, 4);
            this.generateBox(p_228756_, p_228760_, 5, 4, 2, 6, 4, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228756_, p_228760_, 6, 1, 2, 6, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228756_, p_228760_, 6, 1, 5, 6, 3, 5, BASE_LIGHT, BASE_LIGHT, false);
         }

         if (oceanmonumentpieces$roomdefinition3.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(p_228756_, p_228760_, 0, 5, 3, 0, 6, 4);
            this.generateBox(p_228756_, p_228760_, 1, 4, 2, 2, 4, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228756_, p_228760_, 1, 1, 2, 1, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228756_, p_228760_, 1, 1, 5, 1, 3, 5, BASE_LIGHT, BASE_LIGHT, false);
         }

         if (oceanmonumentpieces$roomdefinition2.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(p_228756_, p_228760_, 3, 5, 15, 4, 6, 15);
         }

         if (oceanmonumentpieces$roomdefinition2.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(p_228756_, p_228760_, 0, 5, 11, 0, 6, 12);
            this.generateBox(p_228756_, p_228760_, 1, 4, 10, 2, 4, 13, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228756_, p_228760_, 1, 1, 10, 1, 3, 10, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228756_, p_228760_, 1, 1, 13, 1, 3, 13, BASE_LIGHT, BASE_LIGHT, false);
         }

         if (oceanmonumentpieces$roomdefinition2.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(p_228756_, p_228760_, 7, 5, 11, 7, 6, 12);
            this.generateBox(p_228756_, p_228760_, 5, 4, 10, 6, 4, 13, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228756_, p_228760_, 6, 1, 10, 6, 3, 10, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228756_, p_228760_, 6, 1, 13, 6, 3, 13, BASE_LIGHT, BASE_LIGHT, false);
         }
      }
   }

   public static class OceanMonumentDoubleZRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentDoubleZRoom(Direction p_228764_, OceanMonumentPieces.RoomDefinition p_228765_) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_Z_ROOM, 1, p_228764_, p_228765_, 1, 1, 2);
      }

      public OceanMonumentDoubleZRoom(CompoundTag p_228767_) {
         super(StructurePieceType.OCEAN_MONUMENT_DOUBLE_Z_ROOM, p_228767_);
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228769_,
         StructureManager p_228770_,
         ChunkGenerator p_228771_,
         RandomSource p_228772_,
         BoundingBox p_228773_,
         ChunkPos p_228774_,
         BlockPos p_228775_
      ) {
         OceanMonumentPieces.RoomDefinition oceanmonumentpieces$roomdefinition = this.roomDefinition.connections[Direction.NORTH.get3DDataValue()];
         OceanMonumentPieces.RoomDefinition oceanmonumentpieces$roomdefinition1 = this.roomDefinition;
         if (this.roomDefinition.index / 25 > 0) {
            this.generateDefaultFloor(p_228769_, p_228773_, 0, 8, oceanmonumentpieces$roomdefinition.hasOpening[Direction.DOWN.get3DDataValue()]);
            this.generateDefaultFloor(p_228769_, p_228773_, 0, 0, oceanmonumentpieces$roomdefinition1.hasOpening[Direction.DOWN.get3DDataValue()]);
         }

         if (oceanmonumentpieces$roomdefinition1.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(p_228769_, p_228773_, 1, 4, 1, 6, 4, 7, BASE_GRAY);
         }

         if (oceanmonumentpieces$roomdefinition.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(p_228769_, p_228773_, 1, 4, 8, 6, 4, 14, BASE_GRAY);
         }

         this.generateBox(p_228769_, p_228773_, 0, 3, 0, 0, 3, 15, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 7, 3, 0, 7, 3, 15, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 1, 3, 0, 7, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 1, 3, 15, 6, 3, 15, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 0, 2, 0, 0, 2, 15, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(p_228769_, p_228773_, 7, 2, 0, 7, 2, 15, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(p_228769_, p_228773_, 1, 2, 0, 7, 2, 0, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(p_228769_, p_228773_, 1, 2, 15, 6, 2, 15, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(p_228769_, p_228773_, 0, 1, 0, 0, 1, 15, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 7, 1, 0, 7, 1, 15, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 1, 1, 0, 7, 1, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 1, 1, 15, 6, 1, 15, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 1, 1, 1, 1, 1, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 6, 1, 1, 6, 1, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 1, 3, 1, 1, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 6, 3, 1, 6, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 1, 1, 13, 1, 1, 14, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 6, 1, 13, 6, 1, 14, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 1, 3, 13, 1, 3, 14, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 6, 3, 13, 6, 3, 14, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 2, 1, 6, 2, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 5, 1, 6, 5, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 2, 1, 9, 2, 3, 9, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 5, 1, 9, 5, 3, 9, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 3, 2, 6, 4, 2, 6, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 3, 2, 9, 4, 2, 9, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 2, 2, 7, 2, 2, 8, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228769_, p_228773_, 5, 2, 7, 5, 2, 8, BASE_LIGHT, BASE_LIGHT, false);
         this.placeBlock(p_228769_, LAMP_BLOCK, 2, 2, 5, p_228773_);
         this.placeBlock(p_228769_, LAMP_BLOCK, 5, 2, 5, p_228773_);
         this.placeBlock(p_228769_, LAMP_BLOCK, 2, 2, 10, p_228773_);
         this.placeBlock(p_228769_, LAMP_BLOCK, 5, 2, 10, p_228773_);
         this.placeBlock(p_228769_, BASE_LIGHT, 2, 3, 5, p_228773_);
         this.placeBlock(p_228769_, BASE_LIGHT, 5, 3, 5, p_228773_);
         this.placeBlock(p_228769_, BASE_LIGHT, 2, 3, 10, p_228773_);
         this.placeBlock(p_228769_, BASE_LIGHT, 5, 3, 10, p_228773_);
         if (oceanmonumentpieces$roomdefinition1.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(p_228769_, p_228773_, 3, 1, 0, 4, 2, 0);
         }

         if (oceanmonumentpieces$roomdefinition1.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(p_228769_, p_228773_, 7, 1, 3, 7, 2, 4);
         }

         if (oceanmonumentpieces$roomdefinition1.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(p_228769_, p_228773_, 0, 1, 3, 0, 2, 4);
         }

         if (oceanmonumentpieces$roomdefinition.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(p_228769_, p_228773_, 3, 1, 15, 4, 2, 15);
         }

         if (oceanmonumentpieces$roomdefinition.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(p_228769_, p_228773_, 0, 1, 11, 0, 2, 12);
         }

         if (oceanmonumentpieces$roomdefinition.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(p_228769_, p_228773_, 7, 1, 11, 7, 2, 12);
         }
      }
   }

   public static class OceanMonumentEntryRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentEntryRoom(Direction p_228777_, OceanMonumentPieces.RoomDefinition p_228778_) {
         super(StructurePieceType.OCEAN_MONUMENT_ENTRY_ROOM, 1, p_228777_, p_228778_, 1, 1, 1);
      }

      public OceanMonumentEntryRoom(CompoundTag p_228780_) {
         super(StructurePieceType.OCEAN_MONUMENT_ENTRY_ROOM, p_228780_);
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228782_,
         StructureManager p_228783_,
         ChunkGenerator p_228784_,
         RandomSource p_228785_,
         BoundingBox p_228786_,
         ChunkPos p_228787_,
         BlockPos p_228788_
      ) {
         this.generateBox(p_228782_, p_228786_, 0, 3, 0, 2, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228782_, p_228786_, 5, 3, 0, 7, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228782_, p_228786_, 0, 2, 0, 1, 2, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228782_, p_228786_, 6, 2, 0, 7, 2, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228782_, p_228786_, 0, 1, 0, 0, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228782_, p_228786_, 7, 1, 0, 7, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228782_, p_228786_, 0, 1, 7, 7, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228782_, p_228786_, 1, 1, 0, 2, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228782_, p_228786_, 5, 1, 0, 6, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
         if (this.roomDefinition.hasOpening[Direction.NORTH.get3DDataValue()]) {
            this.generateWaterBox(p_228782_, p_228786_, 3, 1, 7, 4, 2, 7);
         }

         if (this.roomDefinition.hasOpening[Direction.WEST.get3DDataValue()]) {
            this.generateWaterBox(p_228782_, p_228786_, 0, 1, 3, 1, 2, 4);
         }

         if (this.roomDefinition.hasOpening[Direction.EAST.get3DDataValue()]) {
            this.generateWaterBox(p_228782_, p_228786_, 6, 1, 3, 7, 2, 4);
         }
      }
   }

   public static class OceanMonumentPenthouse extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentPenthouse(Direction p_228790_, BoundingBox p_228791_) {
         super(StructurePieceType.OCEAN_MONUMENT_PENTHOUSE, p_228790_, 1, p_228791_);
      }

      public OceanMonumentPenthouse(CompoundTag p_228793_) {
         super(StructurePieceType.OCEAN_MONUMENT_PENTHOUSE, p_228793_);
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228795_,
         StructureManager p_228796_,
         ChunkGenerator p_228797_,
         RandomSource p_228798_,
         BoundingBox p_228799_,
         ChunkPos p_228800_,
         BlockPos p_228801_
      ) {
         this.generateBox(p_228795_, p_228799_, 2, -1, 2, 11, -1, 11, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228795_, p_228799_, 0, -1, 0, 1, -1, 11, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(p_228795_, p_228799_, 12, -1, 0, 13, -1, 11, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(p_228795_, p_228799_, 2, -1, 0, 11, -1, 1, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(p_228795_, p_228799_, 2, -1, 12, 11, -1, 13, BASE_GRAY, BASE_GRAY, false);
         this.generateBox(p_228795_, p_228799_, 0, 0, 0, 0, 0, 13, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228795_, p_228799_, 13, 0, 0, 13, 0, 13, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228795_, p_228799_, 1, 0, 0, 12, 0, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228795_, p_228799_, 1, 0, 13, 12, 0, 13, BASE_LIGHT, BASE_LIGHT, false);

         for (int i = 2; i <= 11; i += 3) {
            this.placeBlock(p_228795_, LAMP_BLOCK, 0, 0, i, p_228799_);
            this.placeBlock(p_228795_, LAMP_BLOCK, 13, 0, i, p_228799_);
            this.placeBlock(p_228795_, LAMP_BLOCK, i, 0, 0, p_228799_);
         }

         this.generateBox(p_228795_, p_228799_, 2, 0, 3, 4, 0, 9, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228795_, p_228799_, 9, 0, 3, 11, 0, 9, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228795_, p_228799_, 4, 0, 9, 9, 0, 11, BASE_LIGHT, BASE_LIGHT, false);
         this.placeBlock(p_228795_, BASE_LIGHT, 5, 0, 8, p_228799_);
         this.placeBlock(p_228795_, BASE_LIGHT, 8, 0, 8, p_228799_);
         this.placeBlock(p_228795_, BASE_LIGHT, 10, 0, 10, p_228799_);
         this.placeBlock(p_228795_, BASE_LIGHT, 3, 0, 10, p_228799_);
         this.generateBox(p_228795_, p_228799_, 3, 0, 3, 3, 0, 7, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(p_228795_, p_228799_, 10, 0, 3, 10, 0, 7, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(p_228795_, p_228799_, 6, 0, 10, 7, 0, 10, BASE_BLACK, BASE_BLACK, false);
         int l = 3;

         for (int j = 0; j < 2; j++) {
            for (int k = 2; k <= 8; k += 3) {
               this.generateBox(p_228795_, p_228799_, l, 0, k, l, 2, k, BASE_LIGHT, BASE_LIGHT, false);
            }

            l = 10;
         }

         this.generateBox(p_228795_, p_228799_, 5, 0, 10, 5, 2, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228795_, p_228799_, 8, 0, 10, 8, 2, 10, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228795_, p_228799_, 6, -1, 7, 7, -1, 8, BASE_BLACK, BASE_BLACK, false);
         this.generateWaterBox(p_228795_, p_228799_, 6, -1, 3, 7, -1, 4);
         this.spawnElder(p_228795_, p_228799_, 6, 1, 6);
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

      protected static int getRoomIndex(int p_228890_, int p_228891_, int p_228892_) {
         return p_228891_ * 25 + p_228892_ * 5 + p_228890_;
      }

      public OceanMonumentPiece(StructurePieceType p_228836_, Direction p_228837_, int p_228838_, BoundingBox p_228839_) {
         super(p_228836_, p_228838_, p_228839_);
         this.setOrientation(p_228837_);
      }

      protected OceanMonumentPiece(
         StructurePieceType p_228828_,
         int p_228829_,
         Direction p_228830_,
         OceanMonumentPieces.RoomDefinition p_228831_,
         int p_228832_,
         int p_228833_,
         int p_228834_
      ) {
         super(p_228828_, p_228829_, makeBoundingBox(p_228830_, p_228831_, p_228832_, p_228833_, p_228834_));
         this.setOrientation(p_228830_);
         this.roomDefinition = p_228831_;
      }

      private static BoundingBox makeBoundingBox(Direction p_228875_, OceanMonumentPieces.RoomDefinition p_228876_, int p_228877_, int p_228878_, int p_228879_) {
         int i = p_228876_.index;
         int j = i % 5;
         int k = i / 5 % 5;
         int l = i / 25;
         BoundingBox boundingbox = makeBoundingBox(0, 0, 0, p_228875_, p_228877_ * 8, p_228878_ * 4, p_228879_ * 8);
         switch (p_228875_) {
            case NORTH:
               boundingbox.move(j * 8, l * 4, -(k + p_228879_) * 8 + 1);
               break;
            case SOUTH:
               boundingbox.move(j * 8, l * 4, k * 8);
               break;
            case WEST:
               boundingbox.move(-(k + p_228879_) * 8 + 1, l * 4, j * 8);
               break;
            case EAST:
            default:
               boundingbox.move(k * 8, l * 4, j * 8);
         }

         return boundingbox;
      }

      public OceanMonumentPiece(StructurePieceType p_228841_, CompoundTag p_228842_) {
         super(p_228841_, p_228842_);
      }

      @Override
      protected void addAdditionalSaveData(StructurePieceSerializationContext p_228872_, CompoundTag p_228873_) {
      }

      protected void generateWaterBox(
         WorldGenLevel p_228881_, BoundingBox p_228882_, int p_228883_, int p_228884_, int p_228885_, int p_228886_, int p_228887_, int p_228888_
      ) {
         for (int i = p_228884_; i <= p_228887_; i++) {
            for (int j = p_228883_; j <= p_228886_; j++) {
               for (int k = p_228885_; k <= p_228888_; k++) {
                  BlockState blockstate = this.getBlock(p_228881_, j, i, k, p_228882_);
                  if (!FILL_KEEP.contains(blockstate.getBlock())) {
                     if (this.getWorldY(i) >= p_228881_.getSeaLevel() && blockstate != FILL_BLOCK) {
                        this.placeBlock(p_228881_, Blocks.AIR.defaultBlockState(), j, i, k, p_228882_);
                     } else {
                        this.placeBlock(p_228881_, FILL_BLOCK, j, i, k, p_228882_);
                     }
                  }
               }
            }
         }
      }

      protected void generateDefaultFloor(WorldGenLevel p_228860_, BoundingBox p_228861_, int p_228862_, int p_228863_, boolean p_228864_) {
         if (p_228864_) {
            this.generateBox(p_228860_, p_228861_, p_228862_ + 0, 0, p_228863_ + 0, p_228862_ + 2, 0, p_228863_ + 8 - 1, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228860_, p_228861_, p_228862_ + 5, 0, p_228863_ + 0, p_228862_ + 8 - 1, 0, p_228863_ + 8 - 1, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228860_, p_228861_, p_228862_ + 3, 0, p_228863_ + 0, p_228862_ + 4, 0, p_228863_ + 2, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228860_, p_228861_, p_228862_ + 3, 0, p_228863_ + 5, p_228862_ + 4, 0, p_228863_ + 8 - 1, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228860_, p_228861_, p_228862_ + 3, 0, p_228863_ + 2, p_228862_ + 4, 0, p_228863_ + 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228860_, p_228861_, p_228862_ + 3, 0, p_228863_ + 5, p_228862_ + 4, 0, p_228863_ + 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228860_, p_228861_, p_228862_ + 2, 0, p_228863_ + 3, p_228862_ + 2, 0, p_228863_ + 4, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228860_, p_228861_, p_228862_ + 5, 0, p_228863_ + 3, p_228862_ + 5, 0, p_228863_ + 4, BASE_LIGHT, BASE_LIGHT, false);
         } else {
            this.generateBox(p_228860_, p_228861_, p_228862_ + 0, 0, p_228863_ + 0, p_228862_ + 8 - 1, 0, p_228863_ + 8 - 1, BASE_GRAY, BASE_GRAY, false);
         }
      }

      protected void generateBoxOnFillOnly(
         WorldGenLevel p_228850_,
         BoundingBox p_228851_,
         int p_228852_,
         int p_228853_,
         int p_228854_,
         int p_228855_,
         int p_228856_,
         int p_228857_,
         BlockState p_228858_
      ) {
         for (int i = p_228853_; i <= p_228856_; i++) {
            for (int j = p_228852_; j <= p_228855_; j++) {
               for (int k = p_228854_; k <= p_228857_; k++) {
                  if (this.getBlock(p_228850_, j, i, k, p_228851_) == FILL_BLOCK) {
                     this.placeBlock(p_228850_, p_228858_, j, i, k, p_228851_);
                  }
               }
            }
         }
      }

      protected boolean chunkIntersects(BoundingBox p_228866_, int p_228867_, int p_228868_, int p_228869_, int p_228870_) {
         int i = this.getWorldX(p_228867_, p_228868_);
         int j = this.getWorldZ(p_228867_, p_228868_);
         int k = this.getWorldX(p_228869_, p_228870_);
         int l = this.getWorldZ(p_228869_, p_228870_);
         return p_228866_.intersects(Math.min(i, k), Math.min(j, l), Math.max(i, k), Math.max(j, l));
      }

      protected void spawnElder(WorldGenLevel p_251919_, BoundingBox p_248944_, int p_251311_, int p_249326_, int p_252095_) {
         BlockPos blockpos = this.getWorldPos(p_251311_, p_249326_, p_252095_);
         if (p_248944_.isInside(blockpos)) {
            ElderGuardian elderguardian = EntityType.ELDER_GUARDIAN.create(p_251919_.getLevel(), EntitySpawnReason.STRUCTURE);
            if (elderguardian != null) {
               elderguardian.heal(elderguardian.getMaxHealth());
               elderguardian.snapTo(blockpos.getX() + 0.5, blockpos.getY(), blockpos.getZ() + 0.5, 0.0F, 0.0F);
               elderguardian.finalizeSpawn(p_251919_, p_251919_.getCurrentDifficultyAt(elderguardian.blockPosition()), EntitySpawnReason.STRUCTURE, null);
               p_251919_.addFreshEntityWithPassengers(elderguardian);
            }
         }
      }
   }

   public static class OceanMonumentSimpleRoom extends OceanMonumentPieces.OceanMonumentPiece {
      private int mainDesign;

      public OceanMonumentSimpleRoom(Direction p_228895_, OceanMonumentPieces.RoomDefinition p_228896_, RandomSource p_228897_) {
         super(StructurePieceType.OCEAN_MONUMENT_SIMPLE_ROOM, 1, p_228895_, p_228896_, 1, 1, 1);
         this.mainDesign = p_228897_.nextInt(3);
      }

      public OceanMonumentSimpleRoom(CompoundTag p_228899_) {
         super(StructurePieceType.OCEAN_MONUMENT_SIMPLE_ROOM, p_228899_);
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228901_,
         StructureManager p_228902_,
         ChunkGenerator p_228903_,
         RandomSource p_228904_,
         BoundingBox p_228905_,
         ChunkPos p_228906_,
         BlockPos p_228907_
      ) {
         if (this.roomDefinition.index / 25 > 0) {
            this.generateDefaultFloor(p_228901_, p_228905_, 0, 0, this.roomDefinition.hasOpening[Direction.DOWN.get3DDataValue()]);
         }

         if (this.roomDefinition.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(p_228901_, p_228905_, 1, 4, 1, 6, 4, 6, BASE_GRAY);
         }

         boolean flag = this.mainDesign != 0
            && p_228904_.nextBoolean()
            && !this.roomDefinition.hasOpening[Direction.DOWN.get3DDataValue()]
            && !this.roomDefinition.hasOpening[Direction.UP.get3DDataValue()]
            && this.roomDefinition.countOpenings() > 1;
         if (this.mainDesign == 0) {
            this.generateBox(p_228901_, p_228905_, 0, 1, 0, 2, 1, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 0, 3, 0, 2, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 0, 2, 0, 0, 2, 2, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228901_, p_228905_, 1, 2, 0, 2, 2, 0, BASE_GRAY, BASE_GRAY, false);
            this.placeBlock(p_228901_, LAMP_BLOCK, 1, 2, 1, p_228905_);
            this.generateBox(p_228901_, p_228905_, 5, 1, 0, 7, 1, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 5, 3, 0, 7, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 7, 2, 0, 7, 2, 2, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228901_, p_228905_, 5, 2, 0, 6, 2, 0, BASE_GRAY, BASE_GRAY, false);
            this.placeBlock(p_228901_, LAMP_BLOCK, 6, 2, 1, p_228905_);
            this.generateBox(p_228901_, p_228905_, 0, 1, 5, 2, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 0, 3, 5, 2, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 0, 2, 5, 0, 2, 7, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228901_, p_228905_, 1, 2, 7, 2, 2, 7, BASE_GRAY, BASE_GRAY, false);
            this.placeBlock(p_228901_, LAMP_BLOCK, 1, 2, 6, p_228905_);
            this.generateBox(p_228901_, p_228905_, 5, 1, 5, 7, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 5, 3, 5, 7, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 7, 2, 5, 7, 2, 7, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228901_, p_228905_, 5, 2, 7, 6, 2, 7, BASE_GRAY, BASE_GRAY, false);
            this.placeBlock(p_228901_, LAMP_BLOCK, 6, 2, 6, p_228905_);
            if (this.roomDefinition.hasOpening[Direction.SOUTH.get3DDataValue()]) {
               this.generateBox(p_228901_, p_228905_, 3, 3, 0, 4, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(p_228901_, p_228905_, 3, 3, 0, 4, 3, 1, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228901_, p_228905_, 3, 2, 0, 4, 2, 0, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(p_228901_, p_228905_, 3, 1, 0, 4, 1, 1, BASE_LIGHT, BASE_LIGHT, false);
            }

            if (this.roomDefinition.hasOpening[Direction.NORTH.get3DDataValue()]) {
               this.generateBox(p_228901_, p_228905_, 3, 3, 7, 4, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(p_228901_, p_228905_, 3, 3, 6, 4, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228901_, p_228905_, 3, 2, 7, 4, 2, 7, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(p_228901_, p_228905_, 3, 1, 6, 4, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            }

            if (this.roomDefinition.hasOpening[Direction.WEST.get3DDataValue()]) {
               this.generateBox(p_228901_, p_228905_, 0, 3, 3, 0, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(p_228901_, p_228905_, 0, 3, 3, 1, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228901_, p_228905_, 0, 2, 3, 0, 2, 4, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(p_228901_, p_228905_, 0, 1, 3, 1, 1, 4, BASE_LIGHT, BASE_LIGHT, false);
            }

            if (this.roomDefinition.hasOpening[Direction.EAST.get3DDataValue()]) {
               this.generateBox(p_228901_, p_228905_, 7, 3, 3, 7, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
            } else {
               this.generateBox(p_228901_, p_228905_, 6, 3, 3, 7, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228901_, p_228905_, 7, 2, 3, 7, 2, 4, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(p_228901_, p_228905_, 6, 1, 3, 7, 1, 4, BASE_LIGHT, BASE_LIGHT, false);
            }
         } else if (this.mainDesign == 1) {
            this.generateBox(p_228901_, p_228905_, 2, 1, 2, 2, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 2, 1, 5, 2, 3, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 5, 1, 5, 5, 3, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 5, 1, 2, 5, 3, 2, BASE_LIGHT, BASE_LIGHT, false);
            this.placeBlock(p_228901_, LAMP_BLOCK, 2, 2, 2, p_228905_);
            this.placeBlock(p_228901_, LAMP_BLOCK, 2, 2, 5, p_228905_);
            this.placeBlock(p_228901_, LAMP_BLOCK, 5, 2, 5, p_228905_);
            this.placeBlock(p_228901_, LAMP_BLOCK, 5, 2, 2, p_228905_);
            this.generateBox(p_228901_, p_228905_, 0, 1, 0, 1, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 0, 1, 1, 0, 3, 1, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 0, 1, 7, 1, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 0, 1, 6, 0, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 6, 1, 7, 7, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 7, 1, 6, 7, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 6, 1, 0, 7, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 7, 1, 1, 7, 3, 1, BASE_LIGHT, BASE_LIGHT, false);
            this.placeBlock(p_228901_, BASE_GRAY, 1, 2, 0, p_228905_);
            this.placeBlock(p_228901_, BASE_GRAY, 0, 2, 1, p_228905_);
            this.placeBlock(p_228901_, BASE_GRAY, 1, 2, 7, p_228905_);
            this.placeBlock(p_228901_, BASE_GRAY, 0, 2, 6, p_228905_);
            this.placeBlock(p_228901_, BASE_GRAY, 6, 2, 7, p_228905_);
            this.placeBlock(p_228901_, BASE_GRAY, 7, 2, 6, p_228905_);
            this.placeBlock(p_228901_, BASE_GRAY, 6, 2, 0, p_228905_);
            this.placeBlock(p_228901_, BASE_GRAY, 7, 2, 1, p_228905_);
            if (!this.roomDefinition.hasOpening[Direction.SOUTH.get3DDataValue()]) {
               this.generateBox(p_228901_, p_228905_, 1, 3, 0, 6, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228901_, p_228905_, 1, 2, 0, 6, 2, 0, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(p_228901_, p_228905_, 1, 1, 0, 6, 1, 0, BASE_LIGHT, BASE_LIGHT, false);
            }

            if (!this.roomDefinition.hasOpening[Direction.NORTH.get3DDataValue()]) {
               this.generateBox(p_228901_, p_228905_, 1, 3, 7, 6, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228901_, p_228905_, 1, 2, 7, 6, 2, 7, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(p_228901_, p_228905_, 1, 1, 7, 6, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            }

            if (!this.roomDefinition.hasOpening[Direction.WEST.get3DDataValue()]) {
               this.generateBox(p_228901_, p_228905_, 0, 3, 1, 0, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228901_, p_228905_, 0, 2, 1, 0, 2, 6, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(p_228901_, p_228905_, 0, 1, 1, 0, 1, 6, BASE_LIGHT, BASE_LIGHT, false);
            }

            if (!this.roomDefinition.hasOpening[Direction.EAST.get3DDataValue()]) {
               this.generateBox(p_228901_, p_228905_, 7, 3, 1, 7, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228901_, p_228905_, 7, 2, 1, 7, 2, 6, BASE_GRAY, BASE_GRAY, false);
               this.generateBox(p_228901_, p_228905_, 7, 1, 1, 7, 1, 6, BASE_LIGHT, BASE_LIGHT, false);
            }
         } else if (this.mainDesign == 2) {
            this.generateBox(p_228901_, p_228905_, 0, 1, 0, 0, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 7, 1, 0, 7, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 1, 1, 0, 6, 1, 0, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 1, 1, 7, 6, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 0, 2, 0, 0, 2, 7, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(p_228901_, p_228905_, 7, 2, 0, 7, 2, 7, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(p_228901_, p_228905_, 1, 2, 0, 6, 2, 0, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(p_228901_, p_228905_, 1, 2, 7, 6, 2, 7, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(p_228901_, p_228905_, 0, 3, 0, 0, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 7, 3, 0, 7, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 1, 3, 0, 6, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 1, 3, 7, 6, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 0, 1, 3, 0, 2, 4, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(p_228901_, p_228905_, 7, 1, 3, 7, 2, 4, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(p_228901_, p_228905_, 3, 1, 0, 4, 2, 0, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(p_228901_, p_228905_, 3, 1, 7, 4, 2, 7, BASE_BLACK, BASE_BLACK, false);
            if (this.roomDefinition.hasOpening[Direction.SOUTH.get3DDataValue()]) {
               this.generateWaterBox(p_228901_, p_228905_, 3, 1, 0, 4, 2, 0);
            }

            if (this.roomDefinition.hasOpening[Direction.NORTH.get3DDataValue()]) {
               this.generateWaterBox(p_228901_, p_228905_, 3, 1, 7, 4, 2, 7);
            }

            if (this.roomDefinition.hasOpening[Direction.WEST.get3DDataValue()]) {
               this.generateWaterBox(p_228901_, p_228905_, 0, 1, 3, 0, 2, 4);
            }

            if (this.roomDefinition.hasOpening[Direction.EAST.get3DDataValue()]) {
               this.generateWaterBox(p_228901_, p_228905_, 7, 1, 3, 7, 2, 4);
            }
         }

         if (flag) {
            this.generateBox(p_228901_, p_228905_, 3, 1, 3, 4, 1, 4, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228901_, p_228905_, 3, 2, 3, 4, 2, 4, BASE_GRAY, BASE_GRAY, false);
            this.generateBox(p_228901_, p_228905_, 3, 3, 3, 4, 3, 4, BASE_LIGHT, BASE_LIGHT, false);
         }
      }
   }

   public static class OceanMonumentSimpleTopRoom extends OceanMonumentPieces.OceanMonumentPiece {
      public OceanMonumentSimpleTopRoom(Direction p_228909_, OceanMonumentPieces.RoomDefinition p_228910_) {
         super(StructurePieceType.OCEAN_MONUMENT_SIMPLE_TOP_ROOM, 1, p_228909_, p_228910_, 1, 1, 1);
      }

      public OceanMonumentSimpleTopRoom(CompoundTag p_228912_) {
         super(StructurePieceType.OCEAN_MONUMENT_SIMPLE_TOP_ROOM, p_228912_);
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228914_,
         StructureManager p_228915_,
         ChunkGenerator p_228916_,
         RandomSource p_228917_,
         BoundingBox p_228918_,
         ChunkPos p_228919_,
         BlockPos p_228920_
      ) {
         if (this.roomDefinition.index / 25 > 0) {
            this.generateDefaultFloor(p_228914_, p_228918_, 0, 0, this.roomDefinition.hasOpening[Direction.DOWN.get3DDataValue()]);
         }

         if (this.roomDefinition.connections[Direction.UP.get3DDataValue()] == null) {
            this.generateBoxOnFillOnly(p_228914_, p_228918_, 1, 4, 1, 6, 4, 6, BASE_GRAY);
         }

         for (int i = 1; i <= 6; i++) {
            for (int j = 1; j <= 6; j++) {
               if (p_228917_.nextInt(3) != 0) {
                  int k = 2 + (p_228917_.nextInt(4) == 0 ? 0 : 1);
                  BlockState blockstate = Blocks.WET_SPONGE.defaultBlockState();
                  this.generateBox(p_228914_, p_228918_, i, k, j, i, 3, j, blockstate, blockstate, false);
               }
            }
         }

         this.generateBox(p_228914_, p_228918_, 0, 1, 0, 0, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228914_, p_228918_, 7, 1, 0, 7, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228914_, p_228918_, 1, 1, 0, 6, 1, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228914_, p_228918_, 1, 1, 7, 6, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228914_, p_228918_, 0, 2, 0, 0, 2, 7, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(p_228914_, p_228918_, 7, 2, 0, 7, 2, 7, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(p_228914_, p_228918_, 1, 2, 0, 6, 2, 0, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(p_228914_, p_228918_, 1, 2, 7, 6, 2, 7, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(p_228914_, p_228918_, 0, 3, 0, 0, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228914_, p_228918_, 7, 3, 0, 7, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228914_, p_228918_, 1, 3, 0, 6, 3, 0, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228914_, p_228918_, 1, 3, 7, 6, 3, 7, BASE_LIGHT, BASE_LIGHT, false);
         this.generateBox(p_228914_, p_228918_, 0, 1, 3, 0, 2, 4, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(p_228914_, p_228918_, 7, 1, 3, 7, 2, 4, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(p_228914_, p_228918_, 3, 1, 0, 4, 2, 0, BASE_BLACK, BASE_BLACK, false);
         this.generateBox(p_228914_, p_228918_, 3, 1, 7, 4, 2, 7, BASE_BLACK, BASE_BLACK, false);
         if (this.roomDefinition.hasOpening[Direction.SOUTH.get3DDataValue()]) {
            this.generateWaterBox(p_228914_, p_228918_, 3, 1, 0, 4, 2, 0);
         }
      }
   }

   public static class OceanMonumentWingRoom extends OceanMonumentPieces.OceanMonumentPiece {
      private int mainDesign;

      public OceanMonumentWingRoom(Direction p_228923_, BoundingBox p_228924_, int p_228925_) {
         super(StructurePieceType.OCEAN_MONUMENT_WING_ROOM, p_228923_, 1, p_228924_);
         this.mainDesign = p_228925_ & 1;
      }

      public OceanMonumentWingRoom(CompoundTag p_228927_) {
         super(StructurePieceType.OCEAN_MONUMENT_WING_ROOM, p_228927_);
      }

      @Override
      public void postProcess(
         WorldGenLevel p_228929_,
         StructureManager p_228930_,
         ChunkGenerator p_228931_,
         RandomSource p_228932_,
         BoundingBox p_228933_,
         ChunkPos p_228934_,
         BlockPos p_228935_
      ) {
         if (this.mainDesign == 0) {
            for (int i = 0; i < 4; i++) {
               this.generateBox(p_228929_, p_228933_, 10 - i, 3 - i, 20 - i, 12 + i, 3 - i, 20, BASE_LIGHT, BASE_LIGHT, false);
            }

            this.generateBox(p_228929_, p_228933_, 7, 0, 6, 15, 0, 16, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228929_, p_228933_, 6, 0, 6, 6, 3, 20, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228929_, p_228933_, 16, 0, 6, 16, 3, 20, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228929_, p_228933_, 7, 1, 7, 7, 1, 20, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228929_, p_228933_, 15, 1, 7, 15, 1, 20, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228929_, p_228933_, 7, 1, 6, 9, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228929_, p_228933_, 13, 1, 6, 15, 3, 6, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228929_, p_228933_, 8, 1, 7, 9, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228929_, p_228933_, 13, 1, 7, 14, 1, 7, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228929_, p_228933_, 9, 0, 5, 13, 0, 5, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228929_, p_228933_, 10, 0, 7, 12, 0, 7, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(p_228929_, p_228933_, 8, 0, 10, 8, 0, 12, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(p_228929_, p_228933_, 14, 0, 10, 14, 0, 12, BASE_BLACK, BASE_BLACK, false);

            for (int i1 = 18; i1 >= 7; i1 -= 3) {
               this.placeBlock(p_228929_, LAMP_BLOCK, 6, 3, i1, p_228933_);
               this.placeBlock(p_228929_, LAMP_BLOCK, 16, 3, i1, p_228933_);
            }

            this.placeBlock(p_228929_, LAMP_BLOCK, 10, 0, 10, p_228933_);
            this.placeBlock(p_228929_, LAMP_BLOCK, 12, 0, 10, p_228933_);
            this.placeBlock(p_228929_, LAMP_BLOCK, 10, 0, 12, p_228933_);
            this.placeBlock(p_228929_, LAMP_BLOCK, 12, 0, 12, p_228933_);
            this.placeBlock(p_228929_, LAMP_BLOCK, 8, 3, 6, p_228933_);
            this.placeBlock(p_228929_, LAMP_BLOCK, 14, 3, 6, p_228933_);
            this.placeBlock(p_228929_, BASE_LIGHT, 4, 2, 4, p_228933_);
            this.placeBlock(p_228929_, LAMP_BLOCK, 4, 1, 4, p_228933_);
            this.placeBlock(p_228929_, BASE_LIGHT, 4, 0, 4, p_228933_);
            this.placeBlock(p_228929_, BASE_LIGHT, 18, 2, 4, p_228933_);
            this.placeBlock(p_228929_, LAMP_BLOCK, 18, 1, 4, p_228933_);
            this.placeBlock(p_228929_, BASE_LIGHT, 18, 0, 4, p_228933_);
            this.placeBlock(p_228929_, BASE_LIGHT, 4, 2, 18, p_228933_);
            this.placeBlock(p_228929_, LAMP_BLOCK, 4, 1, 18, p_228933_);
            this.placeBlock(p_228929_, BASE_LIGHT, 4, 0, 18, p_228933_);
            this.placeBlock(p_228929_, BASE_LIGHT, 18, 2, 18, p_228933_);
            this.placeBlock(p_228929_, LAMP_BLOCK, 18, 1, 18, p_228933_);
            this.placeBlock(p_228929_, BASE_LIGHT, 18, 0, 18, p_228933_);
            this.placeBlock(p_228929_, BASE_LIGHT, 9, 7, 20, p_228933_);
            this.placeBlock(p_228929_, BASE_LIGHT, 13, 7, 20, p_228933_);
            this.generateBox(p_228929_, p_228933_, 6, 0, 21, 7, 4, 21, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228929_, p_228933_, 15, 0, 21, 16, 4, 21, BASE_LIGHT, BASE_LIGHT, false);
            this.spawnElder(p_228929_, p_228933_, 11, 2, 16);
         } else if (this.mainDesign == 1) {
            this.generateBox(p_228929_, p_228933_, 9, 3, 18, 13, 3, 20, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228929_, p_228933_, 9, 0, 18, 9, 2, 18, BASE_LIGHT, BASE_LIGHT, false);
            this.generateBox(p_228929_, p_228933_, 13, 0, 18, 13, 2, 18, BASE_LIGHT, BASE_LIGHT, false);
            int j1 = 9;
            int j = 20;
            int k = 5;

            for (int l = 0; l < 2; l++) {
               this.placeBlock(p_228929_, BASE_LIGHT, j1, 6, 20, p_228933_);
               this.placeBlock(p_228929_, LAMP_BLOCK, j1, 5, 20, p_228933_);
               this.placeBlock(p_228929_, BASE_LIGHT, j1, 4, 20, p_228933_);
               j1 = 13;
            }

            this.generateBox(p_228929_, p_228933_, 7, 3, 7, 15, 3, 14, BASE_LIGHT, BASE_LIGHT, false);
            int b0 = 10;

            for (int k1 = 0; k1 < 2; k1++) {
               this.generateBox(p_228929_, p_228933_, b0, 0, 10, b0, 6, 10, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228929_, p_228933_, b0, 0, 12, b0, 6, 12, BASE_LIGHT, BASE_LIGHT, false);
               this.placeBlock(p_228929_, LAMP_BLOCK, b0, 0, 10, p_228933_);
               this.placeBlock(p_228929_, LAMP_BLOCK, b0, 0, 12, p_228933_);
               this.placeBlock(p_228929_, LAMP_BLOCK, b0, 4, 10, p_228933_);
               this.placeBlock(p_228929_, LAMP_BLOCK, b0, 4, 12, p_228933_);
               b0 = 12;
            }

            int var16 = 8;

            for (int l1 = 0; l1 < 2; l1++) {
               this.generateBox(p_228929_, p_228933_, var16, 0, 7, var16, 2, 7, BASE_LIGHT, BASE_LIGHT, false);
               this.generateBox(p_228929_, p_228933_, var16, 0, 14, var16, 2, 14, BASE_LIGHT, BASE_LIGHT, false);
               var16 = 14;
            }

            this.generateBox(p_228929_, p_228933_, 8, 3, 8, 8, 3, 13, BASE_BLACK, BASE_BLACK, false);
            this.generateBox(p_228929_, p_228933_, 14, 3, 8, 14, 3, 13, BASE_BLACK, BASE_BLACK, false);
            this.spawnElder(p_228929_, p_228933_, 11, 5, 13);
         }
      }
   }

   static class RoomDefinition {
      final int index;
      final OceanMonumentPieces.RoomDefinition[] connections = new OceanMonumentPieces.RoomDefinition[6];
      final boolean[] hasOpening = new boolean[6];
      boolean claimed;
      boolean isSource;
      private int scanIndex;

      public RoomDefinition(int p_228943_) {
         this.index = p_228943_;
      }

      public void setConnection(Direction p_228948_, OceanMonumentPieces.RoomDefinition p_228949_) {
         this.connections[p_228948_.get3DDataValue()] = p_228949_;
         p_228949_.connections[p_228948_.getOpposite().get3DDataValue()] = this;
      }

      public void updateOpenings() {
         for (int i = 0; i < 6; i++) {
            this.hasOpening[i] = this.connections[i] != null;
         }
      }

      public boolean findSource(int p_228946_) {
         if (this.isSource) {
            return true;
         }

         this.scanIndex = p_228946_;

         for (int i = 0; i < 6; i++) {
            if (this.connections[i] != null && this.hasOpening[i] && this.connections[i].scanIndex != p_228946_ && this.connections[i].findSource(p_228946_)) {
               return true;
            }
         }

         return false;
      }

      public boolean isSpecial() {
         return this.index >= 75;
      }

      public int countOpenings() {
         int i = 0;

         for (int j = 0; j < 6; j++) {
            if (this.hasOpening[j]) {
               i++;
            }
         }

         return i;
      }
   }
}
