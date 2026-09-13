package net.minecraft.world.level.portal;

import java.util.Set;
import net.minecraft.core.BlockPos;
import net.minecraft.network.protocol.game.ClientboundLevelEventPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.Relative;
import net.minecraft.world.level.storage.LevelData;
import net.minecraft.world.phys.Vec3;

public record TeleportTransition(
   ServerLevel newLevel,
   Vec3 position,
   Vec3 deltaMovement,
   float yRot,
   float xRot,
   boolean missingRespawnBlock,
   boolean asPassenger,
   Set<Relative> relatives,
   TeleportTransition.PostTeleportTransition postTeleportTransition
) {
   public static final TeleportTransition.PostTeleportTransition DO_NOTHING = p_360923_ -> {};
   public static final TeleportTransition.PostTeleportTransition PLAY_PORTAL_SOUND = TeleportTransition::playPortalSound;
   public static final TeleportTransition.PostTeleportTransition PLACE_PORTAL_TICKET = TeleportTransition::placePortalTicket;

   public TeleportTransition(
      ServerLevel p_367673_, Vec3 p_361950_, Vec3 p_369034_, float p_365740_, float p_364147_, TeleportTransition.PostTeleportTransition p_368988_
   ) {
      this(p_367673_, p_361950_, p_369034_, p_365740_, p_364147_, Set.of(), p_368988_);
   }

   public TeleportTransition(
      ServerLevel p_366139_,
      Vec3 p_369335_,
      Vec3 p_364793_,
      float p_366788_,
      float p_367305_,
      Set<Relative> p_369752_,
      TeleportTransition.PostTeleportTransition p_360762_
   ) {
      this(p_366139_, p_369335_, p_364793_, p_366788_, p_367305_, false, false, p_369752_, p_360762_);
   }

   private static void playPortalSound(Entity p_361275_) {
      if (p_361275_ instanceof ServerPlayer serverplayer) {
         serverplayer.connection.send(new ClientboundLevelEventPacket(1032, BlockPos.ZERO, 0, false));
      }
   }

   private static void placePortalTicket(Entity p_369312_) {
      p_369312_.placePortalTicket(BlockPos.containing(p_369312_.position()));
   }

   public static TeleportTransition createDefault(ServerPlayer p_427614_, TeleportTransition.PostTeleportTransition p_423796_) {
      ServerLevel serverlevel = p_427614_.level().getServer().findRespawnDimension();
      LevelData.RespawnData leveldata$respawndata = serverlevel.getRespawnData();
      return new TeleportTransition(
         serverlevel,
         findAdjustedSharedSpawnPos(serverlevel, p_427614_),
         Vec3.ZERO,
         leveldata$respawndata.yaw(),
         leveldata$respawndata.pitch(),
         false,
         false,
         Set.of(),
         p_423796_
      );
   }

   public static TeleportTransition missingRespawnBlock(ServerPlayer p_425766_, TeleportTransition.PostTeleportTransition p_360765_) {
      ServerLevel serverlevel = p_425766_.level().getServer().findRespawnDimension();
      LevelData.RespawnData leveldata$respawndata = serverlevel.getRespawnData();
      return new TeleportTransition(
         serverlevel,
         findAdjustedSharedSpawnPos(serverlevel, p_425766_),
         Vec3.ZERO,
         leveldata$respawndata.yaw(),
         leveldata$respawndata.pitch(),
         true,
         false,
         Set.of(),
         p_360765_
      );
   }

   private static Vec3 findAdjustedSharedSpawnPos(ServerLevel p_369125_, Entity p_366828_) {
      return p_366828_.adjustSpawnLocation(p_369125_, p_369125_.getRespawnData().pos()).getBottomCenter();
   }

   public TeleportTransition withRotation(float p_365894_, float p_364460_) {
      return new TeleportTransition(
         this.newLevel(),
         this.position(),
         this.deltaMovement(),
         p_365894_,
         p_364460_,
         this.missingRespawnBlock(),
         this.asPassenger(),
         this.relatives(),
         this.postTeleportTransition()
      );
   }

   public TeleportTransition withPosition(Vec3 p_364591_) {
      return new TeleportTransition(
         this.newLevel(),
         p_364591_,
         this.deltaMovement(),
         this.yRot(),
         this.xRot(),
         this.missingRespawnBlock(),
         this.asPassenger(),
         this.relatives(),
         this.postTeleportTransition()
      );
   }

   public TeleportTransition transitionAsPassenger() {
      return new TeleportTransition(
         this.newLevel(),
         this.position(),
         this.deltaMovement(),
         this.yRot(),
         this.xRot(),
         this.missingRespawnBlock(),
         true,
         this.relatives(),
         this.postTeleportTransition()
      );
   }

   @FunctionalInterface
   public interface PostTeleportTransition {
      void onTransition(Entity var1);

      default TeleportTransition.PostTeleportTransition then(TeleportTransition.PostTeleportTransition p_368257_) {
         return p_362346_ -> {
            this.onTransition(p_362346_);
            p_368257_.onTransition(p_362346_);
         };
      }
   }
}
