package net.minecraft.world.level.block;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.RailShape;
import org.jspecify.annotations.Nullable;

public class RailState {
   private final Level level;
   private final BlockPos pos;
   private final BaseRailBlock block;
   private BlockState state;
   private final boolean isStraight;
   private final List<BlockPos> connections = Lists.newArrayList();

   public RailState(final Level level, final BlockPos pos, final BlockState state) {
      this.level = level;
      this.pos = pos;
      this.state = state;
      this.block = (BaseRailBlock)state.getBlock();
      RailShape direction = state.getValue(this.block.getShapeProperty());
      this.isStraight = this.block.isStraight();
      this.updateConnections(direction);
   }

   public List<BlockPos> getConnections() {
      return this.connections;
   }

   private void updateConnections(final RailShape direction) {
      this.connections.clear();
      switch (direction) {
         case NORTH_SOUTH:
            this.connections.add(this.pos.north());
            this.connections.add(this.pos.south());
            break;
         case EAST_WEST:
            this.connections.add(this.pos.west());
            this.connections.add(this.pos.east());
            break;
         case ASCENDING_EAST:
            this.connections.add(this.pos.west());
            this.connections.add(this.pos.east().above());
            break;
         case ASCENDING_WEST:
            this.connections.add(this.pos.west().above());
            this.connections.add(this.pos.east());
            break;
         case ASCENDING_NORTH:
            this.connections.add(this.pos.north().above());
            this.connections.add(this.pos.south());
            break;
         case ASCENDING_SOUTH:
            this.connections.add(this.pos.north());
            this.connections.add(this.pos.south().above());
            break;
         case SOUTH_EAST:
            this.connections.add(this.pos.east());
            this.connections.add(this.pos.south());
            break;
         case SOUTH_WEST:
            this.connections.add(this.pos.west());
            this.connections.add(this.pos.south());
            break;
         case NORTH_WEST:
            this.connections.add(this.pos.west());
            this.connections.add(this.pos.north());
            break;
         case NORTH_EAST:
            this.connections.add(this.pos.east());
            this.connections.add(this.pos.north());
      }
   }

   private void removeSoftConnections() {
      for (int i = 0; i < this.connections.size(); i++) {
         RailState rail = this.getRail(this.connections.get(i));
         if (rail != null && rail.connectsTo(this)) {
            this.connections.set(i, rail.pos);
         } else {
            this.connections.remove(i--);
         }
      }
   }

   private boolean hasRail(final BlockPos pos) {
      return BaseRailBlock.isRail(this.level, pos) || BaseRailBlock.isRail(this.level, pos.above()) || BaseRailBlock.isRail(this.level, pos.below());
   }

   private @Nullable RailState getRail(final BlockPos pos) {
      BlockPos testPos = pos;
      BlockState testState = this.level.getBlockState(testPos);
      if (BaseRailBlock.isRail(testState)) {
         return new RailState(this.level, testPos, testState);
      }

      testPos = pos.above();
      testState = this.level.getBlockState(testPos);
      if (BaseRailBlock.isRail(testState)) {
         return new RailState(this.level, testPos, testState);
      }

      testPos = pos.below();
      testState = this.level.getBlockState(testPos);
      return BaseRailBlock.isRail(testState) ? new RailState(this.level, testPos, testState) : null;
   }

   private boolean connectsTo(final RailState rail) {
      return this.hasConnection(rail.pos);
   }

   private boolean hasConnection(final BlockPos railPos) {
      for (int i = 0; i < this.connections.size(); i++) {
         BlockPos pos = this.connections.get(i);
         if (pos.getX() == railPos.getX() && pos.getZ() == railPos.getZ()) {
            return true;
         }
      }

      return false;
   }

   protected int countPotentialConnections() {
      int count = 0;

      for (Direction direction : Direction.Plane.HORIZONTAL) {
         if (this.hasRail(this.pos.relative(direction))) {
            count++;
         }
      }

      return count;
   }

   private boolean canConnectTo(final RailState rail) {
      return this.connectsTo(rail) || this.connections.size() != 2;
   }

   private void connectTo(final RailState rail) {
      this.connections.add(rail.pos);
      BlockPos north = this.pos.north();
      BlockPos south = this.pos.south();
      BlockPos west = this.pos.west();
      BlockPos east = this.pos.east();
      boolean n = this.hasConnection(north);
      boolean s = this.hasConnection(south);
      boolean w = this.hasConnection(west);
      boolean e = this.hasConnection(east);
      RailShape shape = null;
      if (n || s) {
         shape = RailShape.NORTH_SOUTH;
      }

      if (w || e) {
         shape = RailShape.EAST_WEST;
      }

      if (!this.isStraight) {
         if (s && e && !n && !w) {
            shape = RailShape.SOUTH_EAST;
         }

         if (s && w && !n && !e) {
            shape = RailShape.SOUTH_WEST;
         }

         if (n && w && !s && !e) {
            shape = RailShape.NORTH_WEST;
         }

         if (n && e && !s && !w) {
            shape = RailShape.NORTH_EAST;
         }
      }

      if (shape == RailShape.NORTH_SOUTH) {
         if (BaseRailBlock.isRail(this.level, north.above())) {
            shape = RailShape.ASCENDING_NORTH;
         }

         if (BaseRailBlock.isRail(this.level, south.above())) {
            shape = RailShape.ASCENDING_SOUTH;
         }
      }

      if (shape == RailShape.EAST_WEST) {
         if (BaseRailBlock.isRail(this.level, east.above())) {
            shape = RailShape.ASCENDING_EAST;
         }

         if (BaseRailBlock.isRail(this.level, west.above())) {
            shape = RailShape.ASCENDING_WEST;
         }
      }

      if (shape == null) {
         shape = RailShape.NORTH_SOUTH;
      }

      this.state = this.state.setValue(this.block.getShapeProperty(), shape);
      this.level.setBlockAndUpdate(this.pos, this.state);
   }

   private boolean hasNeighborRail(final BlockPos railPos) {
      RailState neighbor = this.getRail(railPos);
      if (neighbor == null) {
         return false;
      }

      neighbor.removeSoftConnections();
      return neighbor.canConnectTo(this);
   }

   public RailState place(final boolean hasSignal, final boolean first, final RailShape defaultShape) {
      BlockPos north = this.pos.north();
      BlockPos south = this.pos.south();
      BlockPos west = this.pos.west();
      BlockPos east = this.pos.east();
      boolean n = this.hasNeighborRail(north);
      boolean s = this.hasNeighborRail(south);
      boolean w = this.hasNeighborRail(west);
      boolean e = this.hasNeighborRail(east);
      RailShape shape = null;
      boolean northOrSouth = n || s;
      boolean westOrEast = w || e;
      if (northOrSouth && !westOrEast) {
         shape = RailShape.NORTH_SOUTH;
      }

      if (westOrEast && !northOrSouth) {
         shape = RailShape.EAST_WEST;
      }

      boolean southAndEast = s && e;
      boolean southAndWest = s && w;
      boolean northAndEast = n && e;
      boolean northAndWest = n && w;
      if (!this.isStraight) {
         if (southAndEast && !n && !w) {
            shape = RailShape.SOUTH_EAST;
         }

         if (southAndWest && !n && !e) {
            shape = RailShape.SOUTH_WEST;
         }

         if (northAndWest && !s && !e) {
            shape = RailShape.NORTH_WEST;
         }

         if (northAndEast && !s && !w) {
            shape = RailShape.NORTH_EAST;
         }
      }

      if (shape == null) {
         if (northOrSouth && westOrEast) {
            shape = defaultShape;
         } else if (northOrSouth) {
            shape = RailShape.NORTH_SOUTH;
         } else if (westOrEast) {
            shape = RailShape.EAST_WEST;
         }

         if (!this.isStraight) {
            if (hasSignal) {
               if (southAndEast) {
                  shape = RailShape.SOUTH_EAST;
               }

               if (southAndWest) {
                  shape = RailShape.SOUTH_WEST;
               }

               if (northAndEast) {
                  shape = RailShape.NORTH_EAST;
               }

               if (northAndWest) {
                  shape = RailShape.NORTH_WEST;
               }
            } else {
               if (northAndWest) {
                  shape = RailShape.NORTH_WEST;
               }

               if (northAndEast) {
                  shape = RailShape.NORTH_EAST;
               }

               if (southAndWest) {
                  shape = RailShape.SOUTH_WEST;
               }

               if (southAndEast) {
                  shape = RailShape.SOUTH_EAST;
               }
            }
         }
      }

      if (shape == RailShape.NORTH_SOUTH) {
         if (BaseRailBlock.isRail(this.level, north.above())) {
            shape = RailShape.ASCENDING_NORTH;
         }

         if (BaseRailBlock.isRail(this.level, south.above())) {
            shape = RailShape.ASCENDING_SOUTH;
         }
      }

      if (shape == RailShape.EAST_WEST) {
         if (BaseRailBlock.isRail(this.level, east.above())) {
            shape = RailShape.ASCENDING_EAST;
         }

         if (BaseRailBlock.isRail(this.level, west.above())) {
            shape = RailShape.ASCENDING_WEST;
         }
      }

      if (shape == null) {
         shape = defaultShape;
      }

      this.updateConnections(shape);
      this.state = this.state.setValue(this.block.getShapeProperty(), shape);
      if (first || this.level.getBlockState(this.pos) != this.state) {
         this.level.setBlockAndUpdate(this.pos, this.state);

         for (int i = 0; i < this.connections.size(); i++) {
            RailState neighbor = this.getRail(this.connections.get(i));
            if (neighbor != null) {
               neighbor.removeSoftConnections();
               if (neighbor.canConnectTo(this)) {
                  neighbor.connectTo(this);
               }
            }
         }
      }

      return this;
   }

   public BlockState getState() {
      return this.state;
   }
}
