package net.minecraft.client.gui.spectator.categories;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.gui.spectator.SpectatorMenu;
import net.minecraft.client.gui.spectator.SpectatorMenuCategory;
import net.minecraft.client.gui.spectator.SpectatorMenuItem;
import net.minecraft.client.multiplayer.PlayerInfo;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.RandomSource;
import net.minecraft.world.entity.player.PlayerSkin;
import net.minecraft.world.level.GameType;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.Scoreboard;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class TeleportToTeamMenuCategory implements SpectatorMenuCategory, SpectatorMenuItem {
   private static final Identifier TELEPORT_TO_TEAM_SPRITE = Identifier.withDefaultNamespace("spectator/teleport_to_team");
   private static final Component TELEPORT_TEXT = Component.translatable("spectatorMenu.team_teleport");
   private static final Component TELEPORT_PROMPT = Component.translatable("spectatorMenu.team_teleport.prompt");
   private final List<SpectatorMenuItem> items;

   public TeleportToTeamMenuCategory() {
      Minecraft minecraft = Minecraft.getInstance();
      this.items = createTeamEntries(minecraft, minecraft.level.getScoreboard());
   }

   private static List<SpectatorMenuItem> createTeamEntries(Minecraft p_260258_, Scoreboard p_259249_) {
      return p_259249_.getPlayerTeams()
         .stream()
         .flatMap(p_260025_ -> TeleportToTeamMenuCategory.TeamSelectionItem.create(p_260258_, p_260025_).stream())
         .toList();
   }

   @Override
   public List<SpectatorMenuItem> getItems() {
      return this.items;
   }

   @Override
   public Component getPrompt() {
      return TELEPORT_PROMPT;
   }

   @Override
   public void selectItem(SpectatorMenu p_101886_) {
      p_101886_.selectCategory(this);
   }

   @Override
   public Component getName() {
      return TELEPORT_TEXT;
   }

   @Override
   public void renderIcon(GuiGraphics p_282933_, float p_283568_, float p_367476_) {
      p_282933_.blitSprite(RenderPipelines.GUI_TEXTURED, TELEPORT_TO_TEAM_SPRITE, 0, 0, 16, 16, ARGB.colorFromFloat(p_367476_, p_283568_, p_283568_, p_283568_));
   }

   @Override
   public boolean isEnabled() {
      return !this.items.isEmpty();
   }

   @OnlyIn(Dist.CLIENT)
   static class TeamSelectionItem implements SpectatorMenuItem {
      private final PlayerTeam team;
      private final Supplier<PlayerSkin> iconSkin;
      private final List<PlayerInfo> players;

      private TeamSelectionItem(PlayerTeam p_259176_, List<PlayerInfo> p_259231_, Supplier<PlayerSkin> p_300864_) {
         this.team = p_259176_;
         this.players = p_259231_;
         this.iconSkin = p_300864_;
      }

      public static Optional<SpectatorMenuItem> create(Minecraft p_260048_, PlayerTeam p_259058_) {
         List<PlayerInfo> list = new ArrayList<>();

         for (String s : p_259058_.getPlayers()) {
            PlayerInfo playerinfo = p_260048_.getConnection().getPlayerInfo(s);
            if (playerinfo != null && playerinfo.getGameMode() != GameType.SPECTATOR) {
               list.add(playerinfo);
            }
         }

         if (list.isEmpty()) {
            return Optional.empty();
         }

         PlayerInfo playerinfo1 = list.get(RandomSource.create().nextInt(list.size()));
         return Optional.of(new TeleportToTeamMenuCategory.TeamSelectionItem(p_259058_, list, playerinfo1::getSkin));
      }

      @Override
      public void selectItem(SpectatorMenu p_101902_) {
         p_101902_.selectCategory(new TeleportToPlayerMenuCategory(this.players));
      }

      @Override
      public Component getName() {
         return this.team.getDisplayName();
      }

      @Override
      public void renderIcon(GuiGraphics p_283215_, float p_282946_, float p_364297_) {
         Integer integer = this.team.getColor().getColor();
         if (integer != null) {
            float f = (integer >> 16 & 0xFF) / 255.0F;
            float f1 = (integer >> 8 & 0xFF) / 255.0F;
            float f2 = (integer & 0xFF) / 255.0F;
            p_283215_.fill(1, 1, 15, 15, ARGB.colorFromFloat(p_364297_, f * p_282946_, f1 * p_282946_, f2 * p_282946_));
         }

         PlayerFaceRenderer.draw(p_283215_, this.iconSkin.get(), 2, 2, 12, ARGB.colorFromFloat(p_364297_, p_282946_, p_282946_, p_282946_));
      }

      @Override
      public boolean isEnabled() {
         return true;
      }
   }
}
