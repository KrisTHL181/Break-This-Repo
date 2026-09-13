package net.minecraft.client.gui.screens.worldselection;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.google.common.collect.Sets;
import com.google.common.collect.ImmutableList.Builder;
import com.mojang.serialization.DataResult;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractWidget;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.ContainerObjectSelectionList;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.layouts.HeaderAndFooterLayout;
import net.minecraft.client.gui.layouts.LinearLayout;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.narration.NarratedElementType;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.resources.language.I18n;
import net.minecraft.network.chat.CommonComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.util.FormattedCharSequence;
import net.minecraft.world.level.gamerules.GameRule;
import net.minecraft.world.level.gamerules.GameRuleCategory;
import net.minecraft.world.level.gamerules.GameRuleTypeVisitor;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class EditGameRulesScreen extends Screen {
   private static final Component TITLE = Component.translatable("editGamerule.title");
   private static final int SPACING = 8;
   final HeaderAndFooterLayout layout = new HeaderAndFooterLayout(this);
   private final Consumer<Optional<GameRules>> exitCallback;
   private final Set<EditGameRulesScreen.RuleEntry> invalidEntries = Sets.newHashSet();
   final GameRules gameRules;
   private EditGameRulesScreen.@Nullable RuleList ruleList;
   private @Nullable Button doneButton;

   public EditGameRulesScreen(GameRules p_458520_, Consumer<Optional<GameRules>> p_101052_) {
      super(TITLE);
      this.gameRules = p_458520_;
      this.exitCallback = p_101052_;
   }

   @Override
   protected void init() {
      this.layout.addTitleHeader(TITLE, this.font);
      this.ruleList = this.layout.addToContents(new EditGameRulesScreen.RuleList(this.gameRules));
      LinearLayout linearlayout = this.layout.addToFooter(LinearLayout.horizontal().spacing(8));
      this.doneButton = linearlayout.addChild(
         Button.builder(CommonComponents.GUI_DONE, p_448083_ -> this.exitCallback.accept(Optional.of(this.gameRules))).build()
      );
      linearlayout.addChild(Button.builder(CommonComponents.GUI_CANCEL, p_325430_ -> this.onClose()).build());
      this.layout.visitWidgets(p_325432_ -> {
         AbstractWidget abstractwidget = this.addRenderableWidget(p_325432_);
      });
      this.repositionElements();
   }

   @Override
   protected void repositionElements() {
      this.layout.arrangeElements();
      if (this.ruleList != null) {
         this.ruleList.updateSize(this.width, this.layout);
      }
   }

   @Override
   public void onClose() {
      this.exitCallback.accept(Optional.empty());
   }

   private void updateDoneButton() {
      if (this.doneButton != null) {
         this.doneButton.active = this.invalidEntries.isEmpty();
      }
   }

   void markInvalid(EditGameRulesScreen.RuleEntry p_101061_) {
      this.invalidEntries.add(p_101061_);
      this.updateDoneButton();
   }

   void clearInvalid(EditGameRulesScreen.RuleEntry p_101075_) {
      this.invalidEntries.remove(p_101075_);
      this.updateDoneButton();
   }

   @OnlyIn(Dist.CLIENT)
   public class BooleanRuleEntry extends EditGameRulesScreen.GameRuleEntry {
      private final CycleButton<Boolean> checkbox;

      public BooleanRuleEntry(final Component p_101101_, final List<FormattedCharSequence> p_101102_, final String p_101103_, final GameRule<Boolean> p_454199_) {
         super(p_101102_, p_101101_);
         this.checkbox = CycleButton.onOffBuilder(EditGameRulesScreen.this.gameRules.get(p_454199_))
            .displayOnlyValue()
            .withCustomNarration(p_170219_ -> p_170219_.createDefaultNarrationMessage().append("\n").append(p_101103_))
            .create(10, 5, 44, 20, p_101101_, (p_448085_, p_448086_) -> EditGameRulesScreen.this.gameRules.set(p_454199_, p_448086_, null));
         this.children.add(this.checkbox);
      }

      @Override
      public void renderContent(GuiGraphics p_422381_, int p_427447_, int p_428291_, boolean p_428977_, float p_424947_) {
         this.renderLabel(p_422381_, this.getContentY(), this.getContentX());
         this.checkbox.setX(this.getContentRight() - 45);
         this.checkbox.setY(this.getContentY());
         this.checkbox.render(p_422381_, p_427447_, p_428291_, p_424947_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public class CategoryRuleEntry extends EditGameRulesScreen.RuleEntry {
      final Component label;

      public CategoryRuleEntry(final Component p_101141_) {
         super(null);
         this.label = p_101141_;
      }

      @Override
      public void renderContent(GuiGraphics p_430420_, int p_423429_, int p_430337_, boolean p_426019_, float p_423977_) {
         p_430420_.drawCenteredString(EditGameRulesScreen.this.minecraft.font, this.label, this.getContentXMiddle(), this.getContentY() + 5, -1);
      }

      @Override
      public List<? extends GuiEventListener> children() {
         return ImmutableList.of();
      }

      @Override
      public List<? extends NarratableEntry> narratables() {
         return ImmutableList.of(new NarratableEntry() {
            @Override
            public NarratableEntry.NarrationPriority narrationPriority() {
               return NarratableEntry.NarrationPriority.HOVERED;
            }

            @Override
            public void updateNarration(NarrationElementOutput p_170225_) {
               p_170225_.add(NarratedElementType.TITLE, CategoryRuleEntry.this.label);
            }
         });
      }
   }

   @FunctionalInterface
   @OnlyIn(Dist.CLIENT)
   interface EntryFactory<T> {
      EditGameRulesScreen.RuleEntry create(Component var1, List<FormattedCharSequence> var2, String var3, GameRule<T> var4);
   }

   @OnlyIn(Dist.CLIENT)
   public abstract class GameRuleEntry extends EditGameRulesScreen.RuleEntry {
      private final List<FormattedCharSequence> label;
      protected final List<AbstractWidget> children = Lists.newArrayList();

      public GameRuleEntry(final List<FormattedCharSequence> p_101164_, final Component p_101165_) {
         super(p_101164_);
         this.label = EditGameRulesScreen.this.minecraft.font.split(p_101165_, 175);
      }

      @Override
      public List<? extends GuiEventListener> children() {
         return this.children;
      }

      @Override
      public List<? extends NarratableEntry> narratables() {
         return this.children;
      }

      protected void renderLabel(GuiGraphics p_282711_, int p_281539_, int p_281414_) {
         if (this.label.size() == 1) {
            p_282711_.drawString(EditGameRulesScreen.this.minecraft.font, this.label.get(0), p_281414_, p_281539_ + 5, -1);
         } else if (this.label.size() >= 2) {
            p_282711_.drawString(EditGameRulesScreen.this.minecraft.font, this.label.get(0), p_281414_, p_281539_, -1);
            p_282711_.drawString(EditGameRulesScreen.this.minecraft.font, this.label.get(1), p_281414_, p_281539_ + 10, -1);
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   public class IntegerRuleEntry extends EditGameRulesScreen.GameRuleEntry {
      private final EditBox input;

      public IntegerRuleEntry(final Component p_101175_, final List<FormattedCharSequence> p_101176_, final String p_101177_, final GameRule<Integer> p_455419_) {
         super(p_101176_, p_101175_);
         this.input = new EditBox(EditGameRulesScreen.this.minecraft.font, 10, 5, 44, 20, p_101175_.copy().append("\n").append(p_101177_).append("\n"));
         this.input.setValue(EditGameRulesScreen.this.gameRules.getAsString(p_455419_));
         this.input.setResponder(p_448088_ -> {
            DataResult<Integer> dataresult = p_455419_.deserialize(p_448088_);
            if (dataresult.isSuccess()) {
               this.input.setTextColor(-2039584);
               EditGameRulesScreen.this.clearInvalid(this);
               EditGameRulesScreen.this.gameRules.set(p_455419_, (Integer)dataresult.getOrThrow(), null);
            } else {
               this.input.setTextColor(-65536);
               EditGameRulesScreen.this.markInvalid(this);
            }
         });
         this.children.add(this.input);
      }

      @Override
      public void renderContent(GuiGraphics p_426516_, int p_429872_, int p_425608_, boolean p_425733_, float p_425726_) {
         this.renderLabel(p_426516_, this.getContentY(), this.getContentX());
         this.input.setX(this.getContentRight() - 45);
         this.input.setY(this.getContentY());
         this.input.render(p_426516_, p_429872_, p_425608_, p_425726_);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public abstract static class RuleEntry extends ContainerObjectSelectionList.Entry<EditGameRulesScreen.RuleEntry> {
      final @Nullable List<FormattedCharSequence> tooltip;

      public RuleEntry(@Nullable List<FormattedCharSequence> p_194062_) {
         this.tooltip = p_194062_;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public class RuleList extends ContainerObjectSelectionList<EditGameRulesScreen.RuleEntry> {
      private static final int ITEM_HEIGHT = 24;

      public RuleList(final GameRules p_450762_) {
         super(
            Minecraft.getInstance(),
            EditGameRulesScreen.this.width,
            EditGameRulesScreen.this.layout.getContentHeight(),
            EditGameRulesScreen.this.layout.getHeaderHeight(),
            24
         );
         final Map<GameRuleCategory, Map<GameRule<?>, EditGameRulesScreen.RuleEntry>> map = Maps.newHashMap();
         p_450762_.visitGameRuleTypes(
            new GameRuleTypeVisitor() {
               @Override
               public void visitBoolean(GameRule<Boolean> p_459040_) {
                  this.addEntry(
                     p_459040_,
                     (p_448090_, p_448091_, p_448092_, p_448093_) -> EditGameRulesScreen.this.new BooleanRuleEntry(p_448090_, p_448091_, p_448092_, p_448093_)
                  );
               }

               @Override
               public void visitInteger(GameRule<Integer> p_452384_) {
                  this.addEntry(
                     p_452384_,
                     (p_448094_, p_448095_, p_448096_, p_448097_) -> EditGameRulesScreen.this.new IntegerRuleEntry(p_448094_, p_448095_, p_448096_, p_448097_)
                  );
               }

               private <T> void addEntry(GameRule<T> p_453871_, EditGameRulesScreen.EntryFactory<T> p_101226_) {
                  Component component = Component.translatable(p_453871_.getDescriptionId());
                  Component component1 = Component.literal(p_453871_.id()).withStyle(ChatFormatting.YELLOW);
                  Component component2 = Component.translatable("editGamerule.default", Component.literal(p_453871_.serialize(p_453871_.defaultValue())))
                     .withStyle(ChatFormatting.GRAY);
                  String s = p_453871_.getDescriptionId() + ".description";
                  List<FormattedCharSequence> list;
                  String s1;
                  if (I18n.exists(s)) {
                     Builder<FormattedCharSequence> builder = ImmutableList.builder().add(component1.getVisualOrderText());
                     Component component3 = Component.translatable(s);
                     EditGameRulesScreen.this.font.split(component3, 150).forEach(builder::add);
                     list = builder.add(component2.getVisualOrderText()).build();
                     s1 = component3.getString() + "\n" + component2.getString();
                  } else {
                     list = ImmutableList.of(component1.getVisualOrderText(), component2.getVisualOrderText());
                     s1 = component2.getString();
                  }

                  map.computeIfAbsent(p_453871_.category(), p_450934_ -> Maps.newHashMap()).put(p_453871_, p_101226_.create(component, list, s1, p_453871_));
               }
            }
         );
         map.entrySet()
            .stream()
            .sorted(Map.Entry.comparingByKey(Comparator.comparing(GameRuleCategory::getDescriptionId)))
            .forEach(
               p_448089_ -> {
                  this.addEntry(
                     EditGameRulesScreen.this.new CategoryRuleEntry(p_448089_.getKey().label().withStyle(ChatFormatting.BOLD, ChatFormatting.YELLOW))
                  );
                  p_448089_.getValue()
                     .entrySet()
                     .stream()
                     .sorted(Map.Entry.comparingByKey(Comparator.comparing(GameRule::getDescriptionId)))
                     .forEach(p_420785_ -> this.addEntry(p_420785_.getValue()));
               }
            );
      }

      @Override
      public void renderWidget(GuiGraphics p_309387_, int p_311816_, int p_311348_, float p_311962_) {
         super.renderWidget(p_309387_, p_311816_, p_311348_, p_311962_);
         EditGameRulesScreen.RuleEntry editgamerulesscreen$ruleentry = this.getHovered();
         if (editgamerulesscreen$ruleentry != null && editgamerulesscreen$ruleentry.tooltip != null) {
            p_309387_.setTooltipForNextFrame(editgamerulesscreen$ruleentry.tooltip, p_311816_, p_311348_);
         }
      }
   }
}
