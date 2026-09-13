package net.minecraft.server.commands;

import com.google.common.collect.Lists;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.arguments.StringArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import com.mojang.brigadier.builder.RequiredArgumentBuilder;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.exceptions.SimpleCommandExceptionType;
import java.util.Collection;
import java.util.Collections;
import net.minecraft.ChatFormatting;
import net.minecraft.commands.CommandBuildContext;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;
import net.minecraft.commands.arguments.ColorArgument;
import net.minecraft.commands.arguments.ComponentArgument;
import net.minecraft.commands.arguments.ScoreHolderArgument;
import net.minecraft.commands.arguments.TeamArgument;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.world.scores.PlayerTeam;
import net.minecraft.world.scores.ScoreHolder;
import net.minecraft.world.scores.Scoreboard;
import net.minecraft.world.scores.Team;

public class TeamCommand {
   private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_EXISTS = new SimpleCommandExceptionType(
      Component.translatable("commands.team.add.duplicate")
   );
   private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_EMPTY = new SimpleCommandExceptionType(
      Component.translatable("commands.team.empty.unchanged")
   );
   private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_NAME = new SimpleCommandExceptionType(
      Component.translatable("commands.team.option.name.unchanged")
   );
   private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_COLOR = new SimpleCommandExceptionType(
      Component.translatable("commands.team.option.color.unchanged")
   );
   private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_FRIENDLYFIRE_ENABLED = new SimpleCommandExceptionType(
      Component.translatable("commands.team.option.friendlyfire.alreadyEnabled")
   );
   private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_FRIENDLYFIRE_DISABLED = new SimpleCommandExceptionType(
      Component.translatable("commands.team.option.friendlyfire.alreadyDisabled")
   );
   private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_FRIENDLYINVISIBLES_ENABLED = new SimpleCommandExceptionType(
      Component.translatable("commands.team.option.seeFriendlyInvisibles.alreadyEnabled")
   );
   private static final SimpleCommandExceptionType ERROR_TEAM_ALREADY_FRIENDLYINVISIBLES_DISABLED = new SimpleCommandExceptionType(
      Component.translatable("commands.team.option.seeFriendlyInvisibles.alreadyDisabled")
   );
   private static final SimpleCommandExceptionType ERROR_TEAM_NAMETAG_VISIBLITY_UNCHANGED = new SimpleCommandExceptionType(
      Component.translatable("commands.team.option.nametagVisibility.unchanged")
   );
   private static final SimpleCommandExceptionType ERROR_TEAM_DEATH_MESSAGE_VISIBLITY_UNCHANGED = new SimpleCommandExceptionType(
      Component.translatable("commands.team.option.deathMessageVisibility.unchanged")
   );
   private static final SimpleCommandExceptionType ERROR_TEAM_COLLISION_UNCHANGED = new SimpleCommandExceptionType(
      Component.translatable("commands.team.option.collisionRule.unchanged")
   );

   public static void register(CommandDispatcher<CommandSourceStack> p_138878_, CommandBuildContext p_335664_) {
      p_138878_.register(
         (LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal(
                                    "team"
                                 )
                                 .requires(Commands.hasPermission(Commands.LEVEL_GAMEMASTERS)))
                              .then(
                                 ((LiteralArgumentBuilder)Commands.literal("list").executes(p_183711_ -> listTeams((CommandSourceStack)p_183711_.getSource())))
                                    .then(
                                       Commands.argument("team", TeamArgument.team())
                                          .executes(
                                             p_138876_ -> listMembers((CommandSourceStack)p_138876_.getSource(), TeamArgument.getTeam(p_138876_, "team"))
                                          )
                                    )
                              ))
                           .then(
                              Commands.literal("add")
                                 .then(
                                    ((RequiredArgumentBuilder)Commands.argument("team", StringArgumentType.word())
                                          .executes(
                                             p_138995_ -> createTeam((CommandSourceStack)p_138995_.getSource(), StringArgumentType.getString(p_138995_, "team"))
                                          ))
                                       .then(
                                          Commands.argument("displayName", ComponentArgument.textComponent(p_335664_))
                                             .executes(
                                                p_390106_ -> createTeam(
                                                   (CommandSourceStack)p_390106_.getSource(),
                                                   StringArgumentType.getString(p_390106_, "team"),
                                                   ComponentArgument.getResolvedComponent(p_390106_, "displayName")
                                                )
                                             )
                                       )
                                 )
                           ))
                        .then(
                           Commands.literal("remove")
                              .then(
                                 Commands.argument("team", TeamArgument.team())
                                    .executes(p_138991_ -> deleteTeam((CommandSourceStack)p_138991_.getSource(), TeamArgument.getTeam(p_138991_, "team")))
                              )
                        ))
                     .then(
                        Commands.literal("empty")
                           .then(
                              Commands.argument("team", TeamArgument.team())
                                 .executes(p_138989_ -> emptyTeam((CommandSourceStack)p_138989_.getSource(), TeamArgument.getTeam(p_138989_, "team")))
                           )
                     ))
                  .then(
                     Commands.literal("join")
                        .then(
                           ((RequiredArgumentBuilder)Commands.argument("team", TeamArgument.team())
                                 .executes(
                                    p_308898_ -> joinTeam(
                                       (CommandSourceStack)p_308898_.getSource(),
                                       TeamArgument.getTeam(p_308898_, "team"),
                                       Collections.singleton(((CommandSourceStack)p_308898_.getSource()).getEntityOrException())
                                    )
                                 ))
                              .then(
                                 Commands.argument("members", ScoreHolderArgument.scoreHolders())
                                    .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                                    .executes(
                                       p_138985_ -> joinTeam(
                                          (CommandSourceStack)p_138985_.getSource(),
                                          TeamArgument.getTeam(p_138985_, "team"),
                                          ScoreHolderArgument.getNamesWithDefaultWildcard(p_138985_, "members")
                                       )
                                    )
                              )
                        )
                  ))
               .then(
                  Commands.literal("leave")
                     .then(
                        Commands.argument("members", ScoreHolderArgument.scoreHolders())
                           .suggests(ScoreHolderArgument.SUGGEST_SCORE_HOLDERS)
                           .executes(
                              p_138983_ -> leaveTeam(
                                 (CommandSourceStack)p_138983_.getSource(), ScoreHolderArgument.getNamesWithDefaultWildcard(p_138983_, "members")
                              )
                           )
                     )
               ))
            .then(
               Commands.literal("modify")
                  .then(
                     ((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)((RequiredArgumentBuilder)Commands.argument(
                                                   "team", TeamArgument.team()
                                                )
                                                .then(
                                                   Commands.literal("displayName")
                                                      .then(
                                                         Commands.argument("displayName", ComponentArgument.textComponent(p_335664_))
                                                            .executes(
                                                               p_390108_ -> setDisplayName(
                                                                  (CommandSourceStack)p_390108_.getSource(),
                                                                  TeamArgument.getTeam(p_390108_, "team"),
                                                                  ComponentArgument.getResolvedComponent(p_390108_, "displayName")
                                                               )
                                                            )
                                                      )
                                                ))
                                             .then(
                                                Commands.literal("color")
                                                   .then(
                                                      Commands.argument("value", ColorArgument.color())
                                                         .executes(
                                                            p_138979_ -> setColor(
                                                               (CommandSourceStack)p_138979_.getSource(),
                                                               TeamArgument.getTeam(p_138979_, "team"),
                                                               ColorArgument.getColor(p_138979_, "value")
                                                            )
                                                         )
                                                   )
                                             ))
                                          .then(
                                             Commands.literal("friendlyFire")
                                                .then(
                                                   Commands.argument("allowed", BoolArgumentType.bool())
                                                      .executes(
                                                         p_138977_ -> setFriendlyFire(
                                                            (CommandSourceStack)p_138977_.getSource(),
                                                            TeamArgument.getTeam(p_138977_, "team"),
                                                            BoolArgumentType.getBool(p_138977_, "allowed")
                                                         )
                                                      )
                                                )
                                          ))
                                       .then(
                                          Commands.literal("seeFriendlyInvisibles")
                                             .then(
                                                Commands.argument("allowed", BoolArgumentType.bool())
                                                   .executes(
                                                      p_138975_ -> setFriendlySight(
                                                         (CommandSourceStack)p_138975_.getSource(),
                                                         TeamArgument.getTeam(p_138975_, "team"),
                                                         BoolArgumentType.getBool(p_138975_, "allowed")
                                                      )
                                                   )
                                             )
                                       ))
                                    .then(
                                       ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("nametagVisibility")
                                                   .then(
                                                      Commands.literal("never")
                                                         .executes(
                                                            p_138973_ -> setNametagVisibility(
                                                               (CommandSourceStack)p_138973_.getSource(),
                                                               TeamArgument.getTeam(p_138973_, "team"),
                                                               Team.Visibility.NEVER
                                                            )
                                                         )
                                                   ))
                                                .then(
                                                   Commands.literal("hideForOtherTeams")
                                                      .executes(
                                                         p_138971_ -> setNametagVisibility(
                                                            (CommandSourceStack)p_138971_.getSource(),
                                                            TeamArgument.getTeam(p_138971_, "team"),
                                                            Team.Visibility.HIDE_FOR_OTHER_TEAMS
                                                         )
                                                      )
                                                ))
                                             .then(
                                                Commands.literal("hideForOwnTeam")
                                                   .executes(
                                                      p_138969_ -> setNametagVisibility(
                                                         (CommandSourceStack)p_138969_.getSource(),
                                                         TeamArgument.getTeam(p_138969_, "team"),
                                                         Team.Visibility.HIDE_FOR_OWN_TEAM
                                                      )
                                                   )
                                             ))
                                          .then(
                                             Commands.literal("always")
                                                .executes(
                                                   p_138967_ -> setNametagVisibility(
                                                      (CommandSourceStack)p_138967_.getSource(),
                                                      TeamArgument.getTeam(p_138967_, "team"),
                                                      Team.Visibility.ALWAYS
                                                   )
                                                )
                                          )
                                    ))
                                 .then(
                                    ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("deathMessageVisibility")
                                                .then(
                                                   Commands.literal("never")
                                                      .executes(
                                                         p_138965_ -> setDeathMessageVisibility(
                                                            (CommandSourceStack)p_138965_.getSource(),
                                                            TeamArgument.getTeam(p_138965_, "team"),
                                                            Team.Visibility.NEVER
                                                         )
                                                      )
                                                ))
                                             .then(
                                                Commands.literal("hideForOtherTeams")
                                                   .executes(
                                                      p_138963_ -> setDeathMessageVisibility(
                                                         (CommandSourceStack)p_138963_.getSource(),
                                                         TeamArgument.getTeam(p_138963_, "team"),
                                                         Team.Visibility.HIDE_FOR_OTHER_TEAMS
                                                      )
                                                   )
                                             ))
                                          .then(
                                             Commands.literal("hideForOwnTeam")
                                                .executes(
                                                   p_138961_ -> setDeathMessageVisibility(
                                                      (CommandSourceStack)p_138961_.getSource(),
                                                      TeamArgument.getTeam(p_138961_, "team"),
                                                      Team.Visibility.HIDE_FOR_OWN_TEAM
                                                   )
                                                )
                                          ))
                                       .then(
                                          Commands.literal("always")
                                             .executes(
                                                p_138959_ -> setDeathMessageVisibility(
                                                   (CommandSourceStack)p_138959_.getSource(), TeamArgument.getTeam(p_138959_, "team"), Team.Visibility.ALWAYS
                                                )
                                             )
                                       )
                                 ))
                              .then(
                                 ((LiteralArgumentBuilder)((LiteralArgumentBuilder)((LiteralArgumentBuilder)Commands.literal("collisionRule")
                                             .then(
                                                Commands.literal("never")
                                                   .executes(
                                                      p_138957_ -> setCollision(
                                                         (CommandSourceStack)p_138957_.getSource(),
                                                         TeamArgument.getTeam(p_138957_, "team"),
                                                         Team.CollisionRule.NEVER
                                                      )
                                                   )
                                             ))
                                          .then(
                                             Commands.literal("pushOwnTeam")
                                                .executes(
                                                   p_138955_ -> setCollision(
                                                      (CommandSourceStack)p_138955_.getSource(),
                                                      TeamArgument.getTeam(p_138955_, "team"),
                                                      Team.CollisionRule.PUSH_OWN_TEAM
                                                   )
                                                )
                                          ))
                                       .then(
                                          Commands.literal("pushOtherTeams")
                                             .executes(
                                                p_138953_ -> setCollision(
                                                   (CommandSourceStack)p_138953_.getSource(),
                                                   TeamArgument.getTeam(p_138953_, "team"),
                                                   Team.CollisionRule.PUSH_OTHER_TEAMS
                                                )
                                             )
                                       ))
                                    .then(
                                       Commands.literal("always")
                                          .executes(
                                             p_138951_ -> setCollision(
                                                (CommandSourceStack)p_138951_.getSource(), TeamArgument.getTeam(p_138951_, "team"), Team.CollisionRule.ALWAYS
                                             )
                                          )
                                    )
                              ))
                           .then(
                              Commands.literal("prefix")
                                 .then(
                                    Commands.argument("prefix", ComponentArgument.textComponent(p_335664_))
                                       .executes(
                                          p_390107_ -> setPrefix(
                                             (CommandSourceStack)p_390107_.getSource(),
                                             TeamArgument.getTeam(p_390107_, "team"),
                                             ComponentArgument.getResolvedComponent(p_390107_, "prefix")
                                          )
                                       )
                                 )
                           ))
                        .then(
                           Commands.literal("suffix")
                              .then(
                                 Commands.argument("suffix", ComponentArgument.textComponent(p_335664_))
                                    .executes(
                                       p_390109_ -> setSuffix(
                                          (CommandSourceStack)p_390109_.getSource(),
                                          TeamArgument.getTeam(p_390109_, "team"),
                                          ComponentArgument.getResolvedComponent(p_390109_, "suffix")
                                       )
                                    )
                              )
                        )
                  )
            )
      );
   }

   private static Component getFirstMemberName(Collection<ScoreHolder> p_311655_) {
      return p_311655_.iterator().next().getFeedbackDisplayName();
   }

   private static int leaveTeam(CommandSourceStack p_138918_, Collection<ScoreHolder> p_138919_) {
      Scoreboard scoreboard = p_138918_.getServer().getScoreboard();

      for (ScoreHolder scoreholder : p_138919_) {
         scoreboard.removePlayerFromTeam(scoreholder.getScoreboardName());
      }

      if (p_138919_.size() == 1) {
         p_138918_.sendSuccess(() -> Component.translatable("commands.team.leave.success.single", getFirstMemberName(p_138919_)), true);
      } else {
         p_138918_.sendSuccess(() -> Component.translatable("commands.team.leave.success.multiple", p_138919_.size()), true);
      }

      return p_138919_.size();
   }

   private static int joinTeam(CommandSourceStack p_138895_, PlayerTeam p_138896_, Collection<ScoreHolder> p_138897_) {
      Scoreboard scoreboard = p_138895_.getServer().getScoreboard();

      for (ScoreHolder scoreholder : p_138897_) {
         scoreboard.addPlayerToTeam(scoreholder.getScoreboardName(), p_138896_);
      }

      if (p_138897_.size() == 1) {
         p_138895_.sendSuccess(
            () -> Component.translatable("commands.team.join.success.single", getFirstMemberName(p_138897_), p_138896_.getFormattedDisplayName()), true
         );
      } else {
         p_138895_.sendSuccess(() -> Component.translatable("commands.team.join.success.multiple", p_138897_.size(), p_138896_.getFormattedDisplayName()), true);
      }

      return p_138897_.size();
   }

   private static int setNametagVisibility(CommandSourceStack p_138891_, PlayerTeam p_138892_, Team.Visibility p_138893_) throws CommandSyntaxException {
      if (p_138892_.getNameTagVisibility() == p_138893_) {
         throw ERROR_TEAM_NAMETAG_VISIBLITY_UNCHANGED.create();
      }

      p_138892_.setNameTagVisibility(p_138893_);
      p_138891_.sendSuccess(
         () -> Component.translatable("commands.team.option.nametagVisibility.success", p_138892_.getFormattedDisplayName(), p_138893_.getDisplayName()), true
      );
      return 0;
   }

   private static int setDeathMessageVisibility(CommandSourceStack p_138930_, PlayerTeam p_138931_, Team.Visibility p_138932_) throws CommandSyntaxException {
      if (p_138931_.getDeathMessageVisibility() == p_138932_) {
         throw ERROR_TEAM_DEATH_MESSAGE_VISIBLITY_UNCHANGED.create();
      }

      p_138931_.setDeathMessageVisibility(p_138932_);
      p_138930_.sendSuccess(
         () -> Component.translatable("commands.team.option.deathMessageVisibility.success", p_138931_.getFormattedDisplayName(), p_138932_.getDisplayName()),
         true
      );
      return 0;
   }

   private static int setCollision(CommandSourceStack p_138887_, PlayerTeam p_138888_, Team.CollisionRule p_138889_) throws CommandSyntaxException {
      if (p_138888_.getCollisionRule() == p_138889_) {
         throw ERROR_TEAM_COLLISION_UNCHANGED.create();
      }

      p_138888_.setCollisionRule(p_138889_);
      p_138887_.sendSuccess(
         () -> Component.translatable("commands.team.option.collisionRule.success", p_138888_.getFormattedDisplayName(), p_138889_.getDisplayName()), true
      );
      return 0;
   }

   private static int setFriendlySight(CommandSourceStack p_138907_, PlayerTeam p_138908_, boolean p_138909_) throws CommandSyntaxException {
      if (p_138908_.canSeeFriendlyInvisibles() == p_138909_) {
         if (p_138909_) {
            throw ERROR_TEAM_ALREADY_FRIENDLYINVISIBLES_ENABLED.create();
         } else {
            throw ERROR_TEAM_ALREADY_FRIENDLYINVISIBLES_DISABLED.create();
         }
      } else {
         p_138908_.setSeeFriendlyInvisibles(p_138909_);
         p_138907_.sendSuccess(
            () -> Component.translatable(
               "commands.team.option.seeFriendlyInvisibles." + (p_138909_ ? "enabled" : "disabled"), p_138908_.getFormattedDisplayName()
            ),
            true
         );
         return 0;
      }
   }

   private static int setFriendlyFire(CommandSourceStack p_138938_, PlayerTeam p_138939_, boolean p_138940_) throws CommandSyntaxException {
      if (p_138939_.isAllowFriendlyFire() == p_138940_) {
         if (p_138940_) {
            throw ERROR_TEAM_ALREADY_FRIENDLYFIRE_ENABLED.create();
         } else {
            throw ERROR_TEAM_ALREADY_FRIENDLYFIRE_DISABLED.create();
         }
      } else {
         p_138939_.setAllowFriendlyFire(p_138940_);
         p_138938_.sendSuccess(
            () -> Component.translatable("commands.team.option.friendlyfire." + (p_138940_ ? "enabled" : "disabled"), p_138939_.getFormattedDisplayName()),
            true
         );
         return 0;
      }
   }

   private static int setDisplayName(CommandSourceStack p_138903_, PlayerTeam p_138904_, Component p_138905_) throws CommandSyntaxException {
      if (p_138904_.getDisplayName().equals(p_138905_)) {
         throw ERROR_TEAM_ALREADY_NAME.create();
      }

      p_138904_.setDisplayName(p_138905_);
      p_138903_.sendSuccess(() -> Component.translatable("commands.team.option.name.success", p_138904_.getFormattedDisplayName()), true);
      return 0;
   }

   private static int setColor(CommandSourceStack p_138899_, PlayerTeam p_138900_, ChatFormatting p_138901_) throws CommandSyntaxException {
      if (p_138900_.getColor() == p_138901_) {
         throw ERROR_TEAM_ALREADY_COLOR.create();
      }

      p_138900_.setColor(p_138901_);
      p_138899_.sendSuccess(() -> Component.translatable("commands.team.option.color.success", p_138900_.getFormattedDisplayName(), p_138901_.getName()), true);
      return 0;
   }

   private static int emptyTeam(CommandSourceStack p_138884_, PlayerTeam p_138885_) throws CommandSyntaxException {
      Scoreboard scoreboard = p_138884_.getServer().getScoreboard();
      Collection<String> collection = Lists.newArrayList(p_138885_.getPlayers());
      if (collection.isEmpty()) {
         throw ERROR_TEAM_ALREADY_EMPTY.create();
      }

      for (String s : collection) {
         scoreboard.removePlayerFromTeam(s, p_138885_);
      }

      p_138884_.sendSuccess(() -> Component.translatable("commands.team.empty.success", collection.size(), p_138885_.getFormattedDisplayName()), true);
      return collection.size();
   }

   private static int deleteTeam(CommandSourceStack p_138927_, PlayerTeam p_138928_) {
      Scoreboard scoreboard = p_138927_.getServer().getScoreboard();
      scoreboard.removePlayerTeam(p_138928_);
      p_138927_.sendSuccess(() -> Component.translatable("commands.team.remove.success", p_138928_.getFormattedDisplayName()), true);
      return scoreboard.getPlayerTeams().size();
   }

   private static int createTeam(CommandSourceStack p_138911_, String p_138912_) throws CommandSyntaxException {
      return createTeam(p_138911_, p_138912_, Component.literal(p_138912_));
   }

   private static int createTeam(CommandSourceStack p_138914_, String p_138915_, Component p_138916_) throws CommandSyntaxException {
      Scoreboard scoreboard = p_138914_.getServer().getScoreboard();
      if (scoreboard.getPlayerTeam(p_138915_) != null) {
         throw ERROR_TEAM_ALREADY_EXISTS.create();
      }

      PlayerTeam playerteam = scoreboard.addPlayerTeam(p_138915_);
      playerteam.setDisplayName(p_138916_);
      p_138914_.sendSuccess(() -> Component.translatable("commands.team.add.success", playerteam.getFormattedDisplayName()), true);
      return scoreboard.getPlayerTeams().size();
   }

   private static int listMembers(CommandSourceStack p_138944_, PlayerTeam p_138945_) {
      Collection<String> collection = p_138945_.getPlayers();
      if (collection.isEmpty()) {
         p_138944_.sendSuccess(() -> Component.translatable("commands.team.list.members.empty", p_138945_.getFormattedDisplayName()), false);
      } else {
         p_138944_.sendSuccess(
            () -> Component.translatable(
               "commands.team.list.members.success", p_138945_.getFormattedDisplayName(), collection.size(), ComponentUtils.formatList(collection)
            ),
            false
         );
      }

      return collection.size();
   }

   private static int listTeams(CommandSourceStack p_138882_) {
      Collection<PlayerTeam> collection = p_138882_.getServer().getScoreboard().getPlayerTeams();
      if (collection.isEmpty()) {
         p_138882_.sendSuccess(() -> Component.translatable("commands.team.list.teams.empty"), false);
      } else {
         p_138882_.sendSuccess(
            () -> Component.translatable(
               "commands.team.list.teams.success", collection.size(), ComponentUtils.formatList(collection, PlayerTeam::getFormattedDisplayName)
            ),
            false
         );
      }

      return collection.size();
   }

   private static int setPrefix(CommandSourceStack p_138934_, PlayerTeam p_138935_, Component p_138936_) {
      p_138935_.setPlayerPrefix(p_138936_);
      p_138934_.sendSuccess(() -> Component.translatable("commands.team.option.prefix.success", p_138936_), false);
      return 1;
   }

   private static int setSuffix(CommandSourceStack p_138947_, PlayerTeam p_138948_, Component p_138949_) {
      p_138948_.setPlayerSuffix(p_138949_);
      p_138947_.sendSuccess(() -> Component.translatable("commands.team.option.suffix.success", p_138949_), false);
      return 1;
   }
}
