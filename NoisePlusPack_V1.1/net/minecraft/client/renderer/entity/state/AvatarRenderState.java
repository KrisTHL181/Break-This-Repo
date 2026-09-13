package net.minecraft.client.renderer.entity.state;

import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.animal.parrot.Parrot;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class AvatarRenderState extends HumanoidRenderState {
   public PlayerSkin skin = DefaultPlayerSkin.getDefaultSkin();
   public float capeFlap;
   public float capeLean;
   public float capeLean2;
   public int arrowCount;
   public int stingerCount;
   public boolean isSpectator;
   public boolean showHat = true;
   public boolean showJacket = true;
   public boolean showLeftPants = true;
   public boolean showRightPants = true;
   public boolean showLeftSleeve = true;
   public boolean showRightSleeve = true;
   public boolean showCape = true;
   public float fallFlyingTimeInTicks;
   public boolean shouldApplyFlyingYRot;
   public float flyingYRot;
   public @Nullable Component scoreText;
   public Parrot.@Nullable Variant parrotOnLeftShoulder;
   public Parrot.@Nullable Variant parrotOnRightShoulder;
   public int id;
   public boolean showExtraEars = false;
   public final ItemStackRenderState heldOnHead = new ItemStackRenderState();

   public float fallFlyingScale() {
      return Mth.clamp(this.fallFlyingTimeInTicks * this.fallFlyingTimeInTicks / 100.0F, 0.0F, 1.0F);
   }
}
