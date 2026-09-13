package net.minecraft.client.gui.screens.inventory;

import net.minecraft.network.protocol.game.ServerboundSetCommandMinecartPacket;
import net.minecraft.world.entity.vehicle.minecart.MinecartCommandBlock;
import net.minecraft.world.level.BaseCommandBlock;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class MinecartCommandBlockEditScreen extends AbstractCommandBlockEditScreen {
   private final MinecartCommandBlock minecart;

   public MinecartCommandBlockEditScreen(MinecartCommandBlock p_457161_) {
      this.minecart = p_457161_;
   }

   @Override
   public BaseCommandBlock getCommandBlock() {
      return this.minecart.getCommandBlock();
   }

   @Override
   int getPreviousY() {
      return 150;
   }

   @Override
   protected void init() {
      super.init();
      this.commandEdit.setValue(this.getCommandBlock().getCommand());
   }

   @Override
   protected void populateAndSendPacket() {
      this.minecraft
         .getConnection()
         .send(new ServerboundSetCommandMinecartPacket(this.minecart.getId(), this.commandEdit.getValue(), this.minecart.getCommandBlock().isTrackOutput()));
   }
}
