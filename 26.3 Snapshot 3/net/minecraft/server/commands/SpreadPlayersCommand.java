package net.minecraft.server.commands;

import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.FloatArgumentType;
import com.mojang.brigadier.arguments.IntegerArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.Dynamic2CommandExceptionType;
import com.mojang.brigadier.exceptions.Dynamic4CommandExceptionType;
import java.util.Collection;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.EntityArgument;
import net.minecraft.commands.arguments.coordinates.Vec2Argument;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.BlockTags;
import net.minecraft.util.Mth;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.scores.Team;

public class SpreadPlayersCommand {
   private static final int MAX_ITERATION_COUNT = 10000;
   private static final Dynamic4CommandExceptionType ERROR_FAILED_TO_SPREAD_TEAMS = new Dynamic4CommandExceptionType(
      (count, x, z, recommended) -> Component.translatableEscape("commands.spreadplayers.failed.teams", count, x, z, recommended)
   );
   private static final Dynamic4CommandExceptionType ERROR_FAILED_TO_SPREAD_ENTITIES = new Dynamic4CommandExceptionType(
      (count, x, z, recommended) -> Component.translatableEscape("commands.spreadplayers.failed.entities", count, x, z, recommended)
   );
   private static final Dynamic2CommandExceptionType ERROR_INVALID_MAX_HEIGHT = new Dynamic2CommandExceptionType(
      (suppliedMaxHeight, worldMinHeight) -> Component.translatableEscape("commands.spreadplayers.failed.invalid.height", suppliedMaxHeight, worldMinHeight)
   );

   public static void register(final CommandDispatcher<CommandSourceStack> dispatcher) {
      dispatcher.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("spreadplayers").requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
            .then(
               Commands.argument("center", Vec2Argument.vec2())
                  .then(
                     Commands.argument("spreadDistance", FloatArgumentType.floatArg(0.0F))
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("maxRange", FloatArgumentType.floatArg(1.0F))
                                 .then(
                                    Commands.argument("respectTeams", BoolArgumentType.bool())
                                       .then(
                                          Commands.argument("targets", EntityArgument.entities())
                                             .executes(
                                                c -> spreadPlayers(
                                                   (CommandSourceStack)c.getSource(),
                                                   Vec2Argument.getVec2(c, "center"),
                                                   FloatArgumentType.getFloat(c, "spreadDistance"),
                                                   FloatArgumentType.getFloat(c, "maxRange"),
                                                   ((CommandSourceStack)c.getSource()).getLevel().getMaxY() + 1,
                                                   BoolArgumentType.getBool(c, "respectTeams"),
                                                   EntityArgument.getEntities(c, "targets")
                                                )
                                             )
                                       )
                                 ))
                              .then(
                                 Commands.literal("under")
                                    .then(
                                       Commands.argument("maxHeight", IntegerArgumentType.integer())
                                          .then(
                                             Commands.argument("respectTeams", BoolArgumentType.bool())
                                                .then(
                                                   Commands.argument("targets", EntityArgument.entities())
                                                      .executes(
                                                         c -> spreadPlayers(
                                                            (CommandSourceStack)c.getSource(),
                                                            Vec2Argument.getVec2(c, "center"),
                                                            FloatArgumentType.getFloat(c, "spreadDistance"),
                                                            FloatArgumentType.getFloat(c, "maxRange"),
                                                            IntegerArgumentType.getInteger(c, "maxHeight"),
                                                            BoolArgumentType.getBool(c, "respectTeams"),
                                                            EntityArgument.getEntities(c, "targets")
                                                         )
                                                      )
                                                )
                                          )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static int spreadPlayers(
      final CommandSourceStack source,
      final Vec2 center,
      final float spreadDistance,
      final float maxRange,
      final int maxHeight,
      final boolean respectTeams,
      final Collection<? extends Entity> entities
   ) throws CommandSyntaxException {
      ServerLevel level = source.getLevel();
      int minY = level.getMinY();
      if (maxHeight < minY) {
         throw ERROR_INVALID_MAX_HEIGHT.create(maxHeight, minY);
      }

      RandomSource random = RandomSource.createThreadLocalInstance();
      double minX = center.x - maxRange;
      double minZ = center.y - maxRange;
      double maxX = center.x + maxRange;
      double maxZ = center.y + maxRange;
      SpreadPlayersCommand.Position[] positions = createInitialPositions(
         random, respectTeams ? getNumberOfTeams(entities) : entities.size(), minX, minZ, maxX, maxZ
      );
      spreadPositions(center, spreadDistance, level, random, minX, minZ, maxX, maxZ, maxHeight, positions, respectTeams);
      double distance = setPlayerPositions(entities, level, positions, maxHeight, respectTeams);
      source.sendSuccess(
         () -> Component.translatable(
            "commands.spreadplayers.success." + (respectTeams ? "teams" : "entities"),
            positions.length,
            center.x,
            center.y,
            String.format(Locale.ROOT, "%.2f", distance)
         ),
         true
      );
      return positions.length;
   }

   private static int getNumberOfTeams(final Collection<? extends Entity> players) {
      Set<Team> teams = Sets.newHashSet();

      for (Entity player : players) {
         if (player instanceof Player) {
            teams.add(player.getTeam());
         } else {
            teams.add(null);
         }
      }

      return teams.size();
   }

   private static void spreadPositions(
      final Vec2 center,
      final double spreadDist,
      final ServerLevel level,
      final RandomSource random,
      final double minX,
      final double minZ,
      final double maxX,
      final double maxZ,
      final int maxHeight,
      final SpreadPlayersCommand.Position[] positions,
      final boolean respectTeams
   ) throws CommandSyntaxException {
      boolean hasCollisions = true;
      double minDistance = Float.MAX_VALUE;

      int iteration;
      for (iteration = 0; iteration < 10000 && hasCollisions; iteration++) {
         hasCollisions = false;
         minDistance = Float.MAX_VALUE;

         for (int i = 0; i < positions.length; i++) {
            SpreadPlayersCommand.Position position = positions[i];
            int neighbourCount = 0;
            SpreadPlayersCommand.Position averageNeighbourPos = new SpreadPlayersCommand.Position();

            for (int j = 0; j < positions.length; j++) {
               if (i != j) {
                  SpreadPlayersCommand.Position neighbour = positions[j];
                  double dist = position.dist(neighbour);
                  minDistance = Math.min(dist, minDistance);
                  if (dist < spreadDist) {
                     neighbourCount++;
                     averageNeighbourPos.x = averageNeighbourPos.x + (neighbour.x - position.x);
                     averageNeighbourPos.z = averageNeighbourPos.z + (neighbour.z - position.z);
                  }
               }
            }

            if (neighbourCount > 0) {
               averageNeighbourPos.x /= neighbourCount;
               averageNeighbourPos.z /= neighbourCount;
               double length = averageNeighbourPos.getLength();
               if (length > 0.0) {
                  averageNeighbourPos.normalize();
                  position.moveAway(averageNeighbourPos);
               } else {
                  position.randomize(random, minX, minZ, maxX, maxZ);
               }

               hasCollisions = true;
            }

            if (position.clamp(minX, minZ, maxX, maxZ)) {
               hasCollisions = true;
            }
         }

         if (!hasCollisions) {
            for (SpreadPlayersCommand.Position position : positions) {
               if (!position.isSafe(level, maxHeight)) {
                  position.randomize(random, minX, minZ, maxX, maxZ);
                  hasCollisions = true;
               }
            }
         }
      }

      if (minDistance == Float.MAX_VALUE) {
         minDistance = 0.0;
      }

      if (iteration >= 10000) {
         if (respectTeams) {
            throw ERROR_FAILED_TO_SPREAD_TEAMS.create(positions.length, center.x, center.y, String.format(Locale.ROOT, "%.2f", minDistance));
         } else {
            throw ERROR_FAILED_TO_SPREAD_ENTITIES.create(positions.length, center.x, center.y, String.format(Locale.ROOT, "%.2f", minDistance));
         }
      }
   }

   private static double setPlayerPositions(
      final Collection<? extends Entity> entities,
      final ServerLevel level,
      final SpreadPlayersCommand.Position[] positions,
      final int maxHeight,
      final boolean respectTeams
   ) {
      double avgDistance = 0.0;
      int positionIndex = 0;
      Map<Team, SpreadPlayersCommand.Position> teamPositions = Maps.newHashMap();

      for (Entity entity : entities) {
         SpreadPlayersCommand.Position position;
         if (respectTeams) {
            Team team = entity instanceof Player ? entity.getTeam() : null;
            if (!teamPositions.containsKey(team)) {
               teamPositions.put(team, positions[positionIndex++]);
            }

            position = teamPositions.get(team);
         } else {
            position = positions[positionIndex++];
         }

         entity.teleportTo(
            level,
            Mth.floor(position.x) + 0.5,
            position.getSpawnY(level, maxHeight),
            Mth.floor(position.z) + 0.5,
            Set.of(),
            entity.getYRot(),
            entity.getXRot(),
            true
         );
         double closest = Double.MAX_VALUE;

         for (SpreadPlayersCommand.Position testPosition : positions) {
            if (position != testPosition) {
               double dist = position.dist(testPosition);
               closest = Math.min(dist, closest);
            }
         }

         avgDistance += closest;
      }

      return entities.size() < 2 ? 0.0 : avgDistance / entities.size();
   }

   private static SpreadPlayersCommand.Position[] createInitialPositions(
      final RandomSource random, final int count, final double minX, final double minZ, final double maxX, final double maxZ
   ) {
      SpreadPlayersCommand.Position[] result = new SpreadPlayersCommand.Position[count];

      for (int i = 0; i < result.length; i++) {
         SpreadPlayersCommand.Position position = new SpreadPlayersCommand.Position();
         position.randomize(random, minX, minZ, maxX, maxZ);
         result[i] = position;
      }

      return result;
   }

   private static class Position {
      private double x;
      private double z;

      public double dist(final SpreadPlayersCommand.Position target) {
         double dx = this.x - target.x;
         double dz = this.z - target.z;
         return Math.sqrt(dx * dx + dz * dz);
      }

      public void normalize() {
         double dist = this.getLength();
         this.x /= dist;
         this.z /= dist;
      }

      public double getLength() {
         return Math.sqrt(this.x * this.x + this.z * this.z);
      }

      public void moveAway(final SpreadPlayersCommand.Position pos) {
         this.x = this.x - pos.x;
         this.z = this.z - pos.z;
      }

      public boolean clamp(final double minX, final double minZ, final double maxX, final double maxZ) {
         boolean changed = false;
         if (this.x < minX) {
            this.x = minX;
            changed = true;
         } else if (this.x > maxX) {
            this.x = maxX;
            changed = true;
         }

         if (this.z < minZ) {
            this.z = minZ;
            changed = true;
         } else if (this.z > maxZ) {
            this.z = maxZ;
            changed = true;
         }

         return changed;
      }

      public int getSpawnY(final BlockGetter level, final int maxHeight) {
         BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos(this.x, maxHeight + 1, this.z);
         boolean air2Above = level.getBlockState(pos).isAir();
         pos.move(Direction.DOWN);
         boolean air1Above = level.getBlockState(pos).isAir();

         while (pos.getY() > level.getMinY()) {
            pos.move(Direction.DOWN);
            boolean currentIsAir = level.getBlockState(pos).isAir();
            if (!currentIsAir && air1Above && air2Above) {
               return pos.getY() + 1;
            }

            air2Above = air1Above;
            air1Above = currentIsAir;
         }

         return maxHeight + 1;
      }

      public boolean isSafe(final BlockGetter level, final int maxHeight) {
         BlockPos pos = BlockPos.containing(this.x, this.getSpawnY(level, maxHeight) - 1, this.z);
         BlockState state = level.getBlockState(pos);
         return pos.getY() < maxHeight && !state.liquid() && !state.is(BlockTags.FIRE);
      }

      public void randomize(final RandomSource random, final double minX, final double minZ, final double maxX, final double maxZ) {
         this.x = Mth.nextDouble(random, minX, maxX);
         this.z = Mth.nextDouble(random, minZ, maxZ);
      }
   }
}
