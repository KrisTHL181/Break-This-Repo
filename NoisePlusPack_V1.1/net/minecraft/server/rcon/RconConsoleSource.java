package net.minecraft.server.rcon;

import net.minecraft.commands.CommandSource;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.network.chat.Component;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.permissions.LevelBasedPermissionSet;
import net.minecraft.world.phys.Vec2;
import net.minecraft.world.phys.Vec3;

public class RconConsoleSource implements CommandSource {
   private static final String RCON = "Rcon";
   private static final Component RCON_COMPONENT = Component.literal("Rcon");
   private final StringBuffer buffer = new StringBuffer();
   private final MinecraftServer server;

   public RconConsoleSource(MinecraftServer p_11505_) {
      this.server = p_11505_;
   }

   public void prepareForCommand() {
      this.buffer.setLength(0);
   }

   public String getCommandResponse() {
      return this.buffer.toString();
   }

   public CommandSourceStack createCommandSourceStack() {
      ServerLevel serverlevel = this.server.overworld();
      return new CommandSourceStack(
         this,
         Vec3.atLowerCornerOf(serverlevel.getRespawnData().pos()),
         Vec2.ZERO,
         serverlevel,
         LevelBasedPermissionSet.OWNER,
         "Rcon",
         RCON_COMPONENT,
         this.server,
         null
      );
   }

   @Override
   public void sendSystemMessage(Component p_215653_) {
      this.buffer.append(p_215653_.getString());
   }

   @Override
   public boolean acceptsSuccess() {
      return true;
   }

   @Override
   public boolean acceptsFailure() {
      return true;
   }

   @Override
   public boolean shouldInformAdmins() {
      return this.server.shouldRconBroadcast();
   }
}
