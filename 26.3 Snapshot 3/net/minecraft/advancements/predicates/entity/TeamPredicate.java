package net.minecraft.advancements.predicates.entity;

import com.mojang.serialization.Codec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.scores.Team;
import org.jspecify.annotations.Nullable;

public record TeamPredicate(String team) implements EntitySubPredicate {
   public static final Codec<TeamPredicate> CODEC = Codec.STRING.xmap(TeamPredicate::new, TeamPredicate::team);

   @Override
   public boolean matches(final Entity entity, final ServerLevel level, final @Nullable Vec3 position) {
      Team team = entity.getTeam();
      return team != null && this.team.equals(team.getName());
   }
}
