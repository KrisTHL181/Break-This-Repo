package net.minecraft.advancements.predicates.entity;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.raid.Raider;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public record RaiderPredicate(boolean hasRaid, boolean isCaptain) implements EntitySubPredicate {
   public static final Codec<RaiderPredicate> CODEC = RecordCodecBuilder.create(
      i -> i.group(
            Codec.BOOL.optionalFieldOf("has_raid", false).forGetter(RaiderPredicate::hasRaid),
            Codec.BOOL.optionalFieldOf("is_captain", false).forGetter(RaiderPredicate::isCaptain)
         )
         .apply(i, RaiderPredicate::new)
   );
   public static final RaiderPredicate CAPTAIN_WITHOUT_RAID = new RaiderPredicate(false, true);

   @Override
   public boolean matches(final Entity entity, final ServerLevel level, final @Nullable Vec3 position) {
      return !(entity instanceof Raider raider) ? false : raider.hasRaid() == this.hasRaid && raider.isCaptain() == this.isCaptain;
   }
}
