package net.minecraft.client.gui.components;

import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.mojang.blaze3d.platform.cursor.CursorTypes;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.Message;
import com.mojang.brigadier.ParseResults;
import com.mojang.brigadier.StringReader;
import com.mojang.brigadier.context.CommandContextBuilder;
import com.mojang.brigadier.context.ParsedArgument;
import com.mojang.brigadier.context.SuggestionContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.Suggestion;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import com.mojang.brigadier.tree.CommandNode;
import com.mojang.brigadier.tree.LiteralCommandNode;
import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CompletableFuture;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.KeyEvent;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.multiplayer.ClientSuggestionProvider;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.commands.Commands;
import net.minecraft.commands.SharedSuggestionProvider;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.ComponentUtils;
import net.minecraft.network.chat.Style;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec2;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class CommandSuggestions {
   private static final Pattern WHITESPACE_PATTERN = Pattern.compile("(\\s+)");
   private static final Style UNPARSED_STYLE = Style.EMPTY.withColor(ChatFormatting.RED);
   private static final Style LITERAL_STYLE = Style.EMPTY.withColor(ChatFormatting.GRAY);
   private static final List<Style> ARGUMENT_STYLES = Stream.of(
         ChatFormatting.AQUA, ChatFormatting.YELLOW, ChatFormatting.GREEN, ChatFormatting.LIGHT_PURPLE, ChatFormatting.GOLD
      )
      .map(Style.EMPTY::withColor)
      .collect(ImmutableList.toImmutableList());
   final Minecraft minecraft;
   private final Screen screen;
   final EditBox input;
   final Font font;
   private final boolean commandsOnly;
   private final boolean onlyShowIfCursorPastError;
   final int lineStartOffset;
   final int suggestionLineLimit;
   final boolean anchorToBottom;
   final int fillColor;
   private final List<FormattedCharSequence> commandUsage = Lists.newArrayList();
   private int commandUsagePosition;
   private int commandUsageWidth;
   private @Nullable ParseResults<ClientSuggestionProvider> currentParse;
   private @Nullable CompletableFuture<Suggestions> pendingSuggestions;
   private CommandSuggestions.@Nullable SuggestionsList suggestions;
   private boolean allowSuggestions;
   boolean keepSuggestions;
   private boolean allowHiding = true;

   public CommandSuggestions(
      Minecraft p_93871_,
      Screen p_93872_,
      EditBox p_93873_,
      Font p_93874_,
      boolean p_93875_,
      boolean p_93876_,
      int p_93877_,
      int p_93878_,
      boolean p_93879_,
      int p_93880_
   ) {
      this.minecraft = p_93871_;
      this.screen = p_93872_;
      this.input = p_93873_;
      this.font = p_93874_;
      this.commandsOnly = p_93875_;
      this.onlyShowIfCursorPastError = p_93876_;
      this.lineStartOffset = p_93877_;
      this.suggestionLineLimit = p_93878_;
      this.anchorToBottom = p_93879_;
      this.fillColor = p_93880_;
      p_93873_.addFormatter(this::formatChat);
   }

   public void setAllowSuggestions(boolean p_93923_) {
      this.allowSuggestions = p_93923_;
      if (!p_93923_) {
         this.suggestions = null;
      }
   }

   public void setAllowHiding(boolean p_301612_) {
      this.allowHiding = p_301612_;
   }

   public boolean keyPressed(KeyEvent p_424182_) {
      boolean flag = this.suggestions != null;
      if (flag && this.suggestions.keyPressed(p_424182_)) {
         return true;
      } else if (this.screen.getFocused() == this.input && p_424182_.isCycleFocus() && (!this.allowHiding || flag)) {
         this.showSuggestions(true);
         return true;
      } else {
         return false;
      }
   }

   public boolean mouseScrolled(double p_93883_) {
      return this.suggestions != null && this.suggestions.mouseScrolled(Mth.clamp(p_93883_, -1.0, 1.0));
   }

   public boolean mouseClicked(MouseButtonEvent p_427887_) {
      return this.suggestions != null && this.suggestions.mouseClicked((int)p_427887_.x(), (int)p_427887_.y());
   }

   public void showSuggestions(boolean p_93931_) {
      if (this.pendingSuggestions != null && this.pendingSuggestions.isDone()) {
         Suggestions suggestions = this.pendingSuggestions.join();
         if (!suggestions.isEmpty()) {
            int i = 0;

            for (Suggestion suggestion : suggestions.getList()) {
               i = Math.max(i, this.font.width(suggestion.getText()));
            }

            int j = Mth.clamp(this.input.getScreenX(suggestions.getRange().getStart()), 0, this.input.getScreenX(0) + this.input.getInnerWidth() - i);
            int k = this.anchorToBottom ? this.screen.height - 12 : 72;
            this.suggestions = new CommandSuggestions.SuggestionsList(j, k, i, this.sortSuggestions(suggestions), p_93931_);
         }
      }
   }

   public boolean isVisible() {
      return this.suggestions != null;
   }

   public Component getUsageNarration() {
      if (this.suggestions != null && this.suggestions.tabCycles) {
         return this.allowHiding
            ? Component.translatable("narration.suggestion.usage.cycle.hidable")
            : Component.translatable("narration.suggestion.usage.cycle.fixed");
      } else {
         return this.allowHiding
            ? Component.translatable("narration.suggestion.usage.fill.hidable")
            : Component.translatable("narration.suggestion.usage.fill.fixed");
      }
   }

   public void hide() {
      this.suggestions = null;
   }

   private List<Suggestion> sortSuggestions(Suggestions p_93899_) {
      String s = this.input.getValue().substring(0, this.input.getCursorPosition());
      int i = getLastWordIndex(s);
      String s1 = s.substring(i).toLowerCase(Locale.ROOT);
      List<Suggestion> list = Lists.newArrayList();
      List<Suggestion> list1 = Lists.newArrayList();

      for (Suggestion suggestion : p_93899_.getList()) {
         if (!suggestion.getText().startsWith(s1) && !suggestion.getText().startsWith("minecraft:" + s1)) {
            list1.add(suggestion);
         } else {
            list.add(suggestion);
         }
      }

      list.addAll(list1);
      return list;
   }

   public void updateCommandInfo() {
      String s = this.input.getValue();
      if (this.currentParse != null && !this.currentParse.getReader().getString().equals(s)) {
         this.currentParse = null;
      }

      if (!this.keepSuggestions) {
         this.input.setSuggestion(null);
         this.suggestions = null;
      }

      this.commandUsage.clear();
      StringReader stringreader = new StringReader(s);
      boolean flag = stringreader.canRead() && stringreader.peek() == '/';
      if (flag) {
         stringreader.skip();
      }

      boolean flag1 = this.commandsOnly || flag;
      int i = this.input.getCursorPosition();
      if (flag1) {
         CommandDispatcher<ClientSuggestionProvider> commanddispatcher = this.minecraft.player.connection.getCommands();
         if (this.currentParse == null) {
            this.currentParse = commanddispatcher.parse(stringreader, this.minecraft.player.connection.getSuggestionsProvider());
         }

         int j = this.onlyShowIfCursorPastError ? stringreader.getCursor() : 1;
         if (i >= j && (this.suggestions == null || !this.keepSuggestions)) {
            this.pendingSuggestions = commanddispatcher.getCompletionSuggestions(this.currentParse, i);
            this.pendingSuggestions.thenRun(() -> {
               if (this.pendingSuggestions.isDone()) {
                  this.updateUsageInfo();
               }
            });
         }
      } else {
         String s1 = s.substring(0, i);
         int k = getLastWordIndex(s1);
         Collection<String> collection = this.minecraft.player.connection.getSuggestionsProvider().getCustomTabSugggestions();
         this.pendingSuggestions = SharedSuggestionProvider.suggest(collection, new SuggestionsBuilder(s1, k));
      }
   }

   private static int getLastWordIndex(String p_93913_) {
      if (Strings.isNullOrEmpty(p_93913_)) {
         return 0;
      }

      int i = 0;
      Matcher matcher = WHITESPACE_PATTERN.matcher(p_93913_);

      while (matcher.find()) {
         i = matcher.end();
      }

      return i;
   }

   private static FormattedCharSequence getExceptionMessage(CommandSyntaxException p_93897_) {
      Component component = ComponentUtils.fromMessage(p_93897_.getRawMessage());
      String s = p_93897_.getContext();
      return s == null
         ? component.getVisualOrderText()
         : Component.translatable("command.context.parse_error", component, p_93897_.getCursor(), s).getVisualOrderText();
   }

   private void updateUsageInfo() {
      boolean flag = false;
      if (this.input.getCursorPosition() == this.input.getValue().length()) {
         if (this.pendingSuggestions.join().isEmpty() && !this.currentParse.getExceptions().isEmpty()) {
            int i = 0;

            for (Entry<CommandNode<ClientSuggestionProvider>, CommandSyntaxException> entry : this.currentParse.getExceptions().entrySet()) {
               CommandSyntaxException commandsyntaxexception = entry.getValue();
               if (commandsyntaxexception.getType() == CommandSyntaxException.BUILT_IN_EXCEPTIONS.literalIncorrect()) {
                  i++;
               } else {
                  this.commandUsage.add(getExceptionMessage(commandsyntaxexception));
               }
            }

            if (i > 0) {
               this.commandUsage
                  .add(
                     getExceptionMessage(
                        CommandSyntaxException.BUILT_IN_EXCEPTIONS.dispatcherUnknownArgument().createWithContext(this.currentParse.getReader())
                     )
                  );
            }
         } else if (this.currentParse.getReader().canRead()) {
            flag = true;
         }
      }

      this.commandUsagePosition = 0;
      this.commandUsageWidth = this.screen.width;
      if (this.commandUsage.isEmpty() && !this.fillNodeUsage(ChatFormatting.GRAY) && flag) {
         this.commandUsage.add(getExceptionMessage(Commands.getParseException(this.currentParse)));
      }

      this.suggestions = null;
      if (this.allowSuggestions && this.minecraft.options.autoSuggestions().get()) {
         this.showSuggestions(false);
      }
   }

   private boolean fillNodeUsage(ChatFormatting p_289002_) {
      CommandContextBuilder<ClientSuggestionProvider> commandcontextbuilder = this.currentParse.getContext();
      SuggestionContext<ClientSuggestionProvider> suggestioncontext = commandcontextbuilder.findSuggestionContext(this.input.getCursorPosition());
      Map<CommandNode<ClientSuggestionProvider>, String> map = this.minecraft
         .player
         .connection
         .getCommands()
         .getSmartUsage(suggestioncontext.parent, this.minecraft.player.connection.getSuggestionsProvider());
      List<FormattedCharSequence> list = Lists.newArrayList();
      int i = 0;
      Style style = Style.EMPTY.withColor(p_289002_);

      for (Entry<CommandNode<ClientSuggestionProvider>, String> entry : map.entrySet()) {
         if (!(entry.getKey() instanceof LiteralCommandNode)) {
            list.add(FormattedCharSequence.forward(entry.getValue(), style));
            i = Math.max(i, this.font.width(entry.getValue()));
         }
      }

      if (!list.isEmpty()) {
         this.commandUsage.addAll(list);
         this.commandUsagePosition = Mth.clamp(this.input.getScreenX(suggestioncontext.startPos), 0, this.input.getScreenX(0) + this.input.getInnerWidth() - i);
         this.commandUsageWidth = i;
         return true;
      } else {
         return false;
      }
   }

   private @Nullable FormattedCharSequence formatChat(String p_93915_, int p_93916_) {
      return this.currentParse != null ? formatText(this.currentParse, p_93915_, p_93916_) : null;
   }

   static @Nullable String calculateSuggestionSuffix(String p_93928_, String p_93929_) {
      return p_93929_.startsWith(p_93928_) ? p_93929_.substring(p_93928_.length()) : null;
   }

   private static FormattedCharSequence formatText(ParseResults<ClientSuggestionProvider> p_93893_, String p_93894_, int p_93895_) {
      List<FormattedCharSequence> list = Lists.newArrayList();
      int i = 0;
      int j = -1;
      CommandContextBuilder<ClientSuggestionProvider> commandcontextbuilder = p_93893_.getContext().getLastChild();

      for (ParsedArgument<ClientSuggestionProvider, ?> parsedargument : commandcontextbuilder.getArguments().values()) {
         if (++j >= ARGUMENT_STYLES.size()) {
            j = 0;
         }

         int k = Math.max(parsedargument.getRange().getStart() - p_93895_, 0);
         if (k >= p_93894_.length()) {
            break;
         }

         int l = Math.min(parsedargument.getRange().getEnd() - p_93895_, p_93894_.length());
         if (l > 0) {
            list.add(FormattedCharSequence.forward(p_93894_.substring(i, k), LITERAL_STYLE));
            list.add(FormattedCharSequence.forward(p_93894_.substring(k, l), ARGUMENT_STYLES.get(j)));
            i = l;
         }
      }

      if (p_93893_.getReader().canRead()) {
         int i1 = Math.max(p_93893_.getReader().getCursor() - p_93895_, 0);
         if (i1 < p_93894_.length()) {
            int j1 = Math.min(i1 + p_93893_.getReader().getRemainingLength(), p_93894_.length());
            list.add(FormattedCharSequence.forward(p_93894_.substring(i, i1), LITERAL_STYLE));
            list.add(FormattedCharSequence.forward(p_93894_.substring(i1, j1), UNPARSED_STYLE));
            i = j1;
         }
      }

      list.add(FormattedCharSequence.forward(p_93894_.substring(i), LITERAL_STYLE));
      return FormattedCharSequence.composite(list);
   }

   public void render(GuiGraphics p_282650_, int p_282266_, int p_281963_) {
      if (!this.renderSuggestions(p_282650_, p_282266_, p_281963_)) {
         this.renderUsage(p_282650_);
      }
   }

   public boolean renderSuggestions(GuiGraphics p_283503_, int p_281628_, int p_282260_) {
      if (this.suggestions != null) {
         this.suggestions.render(p_283503_, p_281628_, p_282260_);
         return true;
      } else {
         return false;
      }
   }

   public void renderUsage(GuiGraphics p_282763_) {
      int i = 0;

      for (FormattedCharSequence formattedcharsequence : this.commandUsage) {
         int j = this.anchorToBottom ? this.screen.height - 14 - 13 - 12 * i : 72 + 12 * i;
         p_282763_.fill(this.commandUsagePosition - 1, j, this.commandUsagePosition + this.commandUsageWidth + 1, j + 12, this.fillColor);
         p_282763_.drawString(this.font, formattedcharsequence, this.commandUsagePosition, j + 2, -1);
         i++;
      }
   }

   public Component getNarrationMessage() {
      return this.suggestions != null ? CommonComponents.NEW_LINE.copy().append(this.suggestions.getNarrationMessage()) : CommonComponents.EMPTY;
   }

   @OnlyIn(Dist.CLIENT)
   public class SuggestionsList {
      private final Rect2i rect;
      private final String originalContents;
      private final List<Suggestion> suggestionList;
      private int offset;
      private int current;
      private Vec2 lastMouse = Vec2.ZERO;
      boolean tabCycles;
      private int lastNarratedEntry;

      SuggestionsList(final int p_93957_, final int p_93958_, final int p_93959_, final List<Suggestion> p_93960_, final boolean p_93961_) {
         int i = p_93957_ - (CommandSuggestions.this.input.isBordered() ? 0 : 1);
         int j = CommandSuggestions.this.anchorToBottom
            ? p_93958_ - 3 - Math.min(p_93960_.size(), CommandSuggestions.this.suggestionLineLimit) * 12
            : p_93958_ - (CommandSuggestions.this.input.isBordered() ? 1 : 0);
         this.rect = new Rect2i(i, j, p_93959_ + 1, Math.min(p_93960_.size(), CommandSuggestions.this.suggestionLineLimit) * 12);
         this.originalContents = CommandSuggestions.this.input.getValue();
         this.lastNarratedEntry = p_93961_ ? -1 : 0;
         this.suggestionList = p_93960_;
         this.select(0);
      }

      public void render(GuiGraphics p_282264_, int p_283591_, int p_283236_) {
         int i = Math.min(this.suggestionList.size(), CommandSuggestions.this.suggestionLineLimit);
         int j = -5592406;
         boolean flag = this.offset > 0;
         boolean flag1 = this.suggestionList.size() > this.offset + i;
         boolean flag2 = flag || flag1;
         boolean flag3 = this.lastMouse.x != p_283591_ || this.lastMouse.y != p_283236_;
         if (flag3) {
            this.lastMouse = new Vec2(p_283591_, p_283236_);
         }

         if (flag2) {
            p_282264_.fill(this.rect.getX(), this.rect.getY() - 1, this.rect.getX() + this.rect.getWidth(), this.rect.getY(), CommandSuggestions.this.fillColor);
            p_282264_.fill(
               this.rect.getX(),
               this.rect.getY() + this.rect.getHeight(),
               this.rect.getX() + this.rect.getWidth(),
               this.rect.getY() + this.rect.getHeight() + 1,
               CommandSuggestions.this.fillColor
            );
            if (flag) {
               for (int k = 0; k < this.rect.getWidth(); k++) {
                  if (k % 2 == 0) {
                     p_282264_.fill(this.rect.getX() + k, this.rect.getY() - 1, this.rect.getX() + k + 1, this.rect.getY(), -1);
                  }
               }
            }

            if (flag1) {
               for (int i1 = 0; i1 < this.rect.getWidth(); i1++) {
                  if (i1 % 2 == 0) {
                     p_282264_.fill(
                        this.rect.getX() + i1,
                        this.rect.getY() + this.rect.getHeight(),
                        this.rect.getX() + i1 + 1,
                        this.rect.getY() + this.rect.getHeight() + 1,
                        -1
                     );
                  }
               }
            }
         }

         boolean flag4 = false;

         for (int l = 0; l < i; l++) {
            Suggestion suggestion = this.suggestionList.get(l + this.offset);
            p_282264_.fill(
               this.rect.getX(),
               this.rect.getY() + 12 * l,
               this.rect.getX() + this.rect.getWidth(),
               this.rect.getY() + 12 * l + 12,
               CommandSuggestions.this.fillColor
            );
            if (p_283591_ > this.rect.getX()
               && p_283591_ < this.rect.getX() + this.rect.getWidth()
               && p_283236_ > this.rect.getY() + 12 * l
               && p_283236_ < this.rect.getY() + 12 * l + 12) {
               if (flag3) {
                  this.select(l + this.offset);
               }

               flag4 = true;
            }

            p_282264_.drawString(
               CommandSuggestions.this.font,
               suggestion.getText(),
               this.rect.getX() + 1,
               this.rect.getY() + 2 + 12 * l,
               l + this.offset == this.current ? -256 : -5592406
            );
         }

         if (flag4) {
            Message message = this.suggestionList.get(this.current).getTooltip();
            if (message != null) {
               p_282264_.setTooltipForNextFrame(CommandSuggestions.this.font, ComponentUtils.fromMessage(message), p_283591_, p_283236_);
            }
         }

         if (this.rect.contains(p_283591_, p_283236_)) {
            p_282264_.requestCursor(CursorTypes.POINTING_HAND);
         }
      }

      public boolean mouseClicked(int p_93976_, int p_93977_) {
         if (!this.rect.contains(p_93976_, p_93977_)) {
            return false;
         }

         int i = (p_93977_ - this.rect.getY()) / 12 + this.offset;
         if (i >= 0 && i < this.suggestionList.size()) {
            this.select(i);
            this.useSuggestion();
         }

         return true;
      }

      public boolean mouseScrolled(double p_93972_) {
         int i = (int)CommandSuggestions.this.minecraft.mouseHandler.getScaledXPos(CommandSuggestions.this.minecraft.getWindow());
         int j = (int)CommandSuggestions.this.minecraft.mouseHandler.getScaledYPos(CommandSuggestions.this.minecraft.getWindow());
         if (this.rect.contains(i, j)) {
            this.offset = Mth.clamp((int)(this.offset - p_93972_), 0, Math.max(this.suggestionList.size() - CommandSuggestions.this.suggestionLineLimit, 0));
            return true;
         } else {
            return false;
         }
      }

      public boolean keyPressed(KeyEvent p_422430_) {
         if (p_422430_.isUp()) {
            this.cycle(-1);
            this.tabCycles = false;
            return true;
         }

         if (p_422430_.isDown()) {
            this.cycle(1);
            this.tabCycles = false;
            return true;
         }

         if (p_422430_.isCycleFocus()) {
            if (this.tabCycles) {
               this.cycle(p_422430_.hasShiftDown() ? -1 : 1);
            }

            this.useSuggestion();
            return true;
         } else if (p_422430_.isEscape()) {
            CommandSuggestions.this.hide();
            CommandSuggestions.this.input.setSuggestion(null);
            return true;
         } else {
            return false;
         }
      }

      public void cycle(int p_93974_) {
         this.select(this.current + p_93974_);
         int i = this.offset;
         int j = this.offset + CommandSuggestions.this.suggestionLineLimit - 1;
         if (this.current < i) {
            this.offset = Mth.clamp(this.current, 0, Math.max(this.suggestionList.size() - CommandSuggestions.this.suggestionLineLimit, 0));
         } else if (this.current > j) {
            this.offset = Mth.clamp(
               this.current + CommandSuggestions.this.lineStartOffset - CommandSuggestions.this.suggestionLineLimit,
               0,
               Math.max(this.suggestionList.size() - CommandSuggestions.this.suggestionLineLimit, 0)
            );
         }
      }

      public void select(int p_93987_) {
         this.current = p_93987_;
         if (this.current < 0) {
            this.current = this.current + this.suggestionList.size();
         }

         if (this.current >= this.suggestionList.size()) {
            this.current = this.current - this.suggestionList.size();
         }

         Suggestion suggestion = this.suggestionList.get(this.current);
         CommandSuggestions.this.input
            .setSuggestion(CommandSuggestions.calculateSuggestionSuffix(CommandSuggestions.this.input.getValue(), suggestion.apply(this.originalContents)));
         if (this.lastNarratedEntry != this.current) {
            CommandSuggestions.this.minecraft.getNarrator().saySystemNow(this.getNarrationMessage());
         }
      }

      public void useSuggestion() {
         Suggestion suggestion = this.suggestionList.get(this.current);
         CommandSuggestions.this.keepSuggestions = true;
         CommandSuggestions.this.input.setValue(suggestion.apply(this.originalContents));
         int i = suggestion.getRange().getStart() + suggestion.getText().length();
         CommandSuggestions.this.input.setCursorPosition(i);
         CommandSuggestions.this.input.setHighlightPos(i);
         this.select(this.current);
         CommandSuggestions.this.keepSuggestions = false;
         this.tabCycles = true;
      }

      Component getNarrationMessage() {
         this.lastNarratedEntry = this.current;
         Suggestion suggestion = this.suggestionList.get(this.current);
         Message message = suggestion.getTooltip();
         return message != null
            ? Component.translatable(
               "narration.suggestion.tooltip", this.current + 1, this.suggestionList.size(), suggestion.getText(), Component.translationArg(message)
            )
            : Component.translatable("narration.suggestion", this.current + 1, this.suggestionList.size(), suggestion.getText());
      }
   }
}
