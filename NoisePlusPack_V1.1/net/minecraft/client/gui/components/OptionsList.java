package net.minecraft.client.gui.components;

import com.google.common.collect.Lists;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.client.Options;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.events.GuiEventListener;
import net.minecraft.client.gui.narration.NarratableEntry;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.options.OptionsSubScreen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;
import org.jspecify.annotations.Nullable;

@OnlyIn(Dist.CLIENT)
public class OptionsList extends ContainerObjectSelectionList<OptionsList.AbstractEntry> {
   private static final int BIG_BUTTON_WIDTH = 310;
   private static final int DEFAULT_ITEM_HEIGHT = 25;
   private final OptionsSubScreen screen;

   public OptionsList(Minecraft p_94465_, int p_94466_, OptionsSubScreen p_342734_) {
      super(p_94465_, p_94466_, p_342734_.layout.getContentHeight(), p_342734_.layout.getHeaderHeight(), 25);
      this.centerListVertically = false;
      this.screen = p_342734_;
   }

   public void addBig(OptionInstance<?> p_232529_) {
      this.addEntry(OptionsList.Entry.big(this.minecraft.options, p_232529_, this.screen));
   }

   public void addSmall(OptionInstance<?>... p_232534_) {
      for (int i = 0; i < p_232534_.length; i += 2) {
         OptionInstance<?> optioninstance = i < p_232534_.length - 1 ? p_232534_[i + 1] : null;
         this.addEntry(OptionsList.Entry.small(this.minecraft.options, p_232534_[i], optioninstance, this.screen));
      }
   }

   public void addSmall(List<AbstractWidget> p_334237_) {
      for (int i = 0; i < p_334237_.size(); i += 2) {
         this.addSmall(p_334237_.get(i), i < p_334237_.size() - 1 ? p_334237_.get(i + 1) : null);
      }
   }

   public void addSmall(AbstractWidget p_330860_, @Nullable AbstractWidget p_333864_) {
      this.addEntry(OptionsList.Entry.small(p_330860_, p_333864_, this.screen));
   }

   public void addSmall(AbstractWidget p_460380_, OptionInstance<?> p_453945_, @Nullable AbstractWidget p_458893_) {
      this.addEntry(OptionsList.Entry.small(p_460380_, p_453945_, p_458893_, this.screen));
   }

   public void addHeader(Component p_453184_) {
      int i = 9;
      int j = this.children().isEmpty() ? 0 : i * 2;
      this.addEntry(new OptionsList.HeaderEntry(this.screen, p_453184_, j), j + i + 4);
   }

   @Override
   public int getRowWidth() {
      return 310;
   }

   public @Nullable AbstractWidget findOption(OptionInstance<?> p_232536_) {
      for (OptionsList.AbstractEntry optionslist$abstractentry : this.children()) {
         if (optionslist$abstractentry instanceof OptionsList.Entry optionslist$entry) {
            AbstractWidget abstractwidget = optionslist$entry.findOption(p_232536_);
            if (abstractwidget != null) {
               return abstractwidget;
            }
         }
      }

      return null;
   }

   public void applyUnsavedChanges() {
      for (OptionsList.AbstractEntry optionslist$abstractentry : this.children()) {
         if (optionslist$abstractentry instanceof OptionsList.Entry optionslist$entry) {
            for (OptionsList.OptionInstanceWidget optionslist$optioninstancewidget : optionslist$entry.children) {
               if (optionslist$optioninstancewidget.optionInstance() != null
                  && optionslist$optioninstancewidget.widget() instanceof OptionInstance.OptionInstanceSliderButton<?> optioninstancesliderbutton) {
                  optioninstancesliderbutton.applyUnsavedValue();
               }
            }
         }
      }
   }

   public void resetOption(OptionInstance<?> p_461019_) {
      for (OptionsList.AbstractEntry optionslist$abstractentry : this.children()) {
         if (optionslist$abstractentry instanceof OptionsList.Entry optionslist$entry) {
            for (OptionsList.OptionInstanceWidget optionslist$optioninstancewidget : optionslist$entry.children) {
               if (optionslist$optioninstancewidget.optionInstance() == p_461019_
                  && optionslist$optioninstancewidget.widget() instanceof ResettableOptionWidget resettableoptionwidget) {
                  resettableoptionwidget.resetValue();
                  return;
               }
            }
         }
      }
   }

   @OnlyIn(Dist.CLIENT)
   protected abstract static class AbstractEntry extends ContainerObjectSelectionList.Entry<OptionsList.AbstractEntry> {
   }

   @OnlyIn(Dist.CLIENT)
   protected static class Entry extends OptionsList.AbstractEntry {
      final List<OptionsList.OptionInstanceWidget> children;
      private final Screen screen;
      private static final int X_OFFSET = 160;

      private Entry(List<OptionsList.OptionInstanceWidget> p_328739_, Screen p_332963_) {
         this.children = p_328739_;
         this.screen = p_332963_;
      }

      public static OptionsList.Entry big(Options p_455319_, OptionInstance<?> p_453811_, Screen p_332678_) {
         return new OptionsList.Entry(List.of(new OptionsList.OptionInstanceWidget(p_453811_.createButton(p_455319_, 0, 0, 310), p_453811_)), p_332678_);
      }

      public static OptionsList.Entry small(AbstractWidget p_332778_, @Nullable AbstractWidget p_330638_, Screen p_328012_) {
         return p_330638_ == null
            ? new OptionsList.Entry(List.of(new OptionsList.OptionInstanceWidget(p_332778_)), p_328012_)
            : new OptionsList.Entry(List.of(new OptionsList.OptionInstanceWidget(p_332778_), new OptionsList.OptionInstanceWidget(p_330638_)), p_328012_);
      }

      public static OptionsList.Entry small(AbstractWidget p_450250_, OptionInstance<?> p_450223_, @Nullable AbstractWidget p_450285_, Screen p_451142_) {
         return p_450285_ == null
            ? new OptionsList.Entry(List.of(new OptionsList.OptionInstanceWidget(p_450250_, p_450223_)), p_451142_)
            : new OptionsList.Entry(
               List.of(new OptionsList.OptionInstanceWidget(p_450250_, p_450223_), new OptionsList.OptionInstanceWidget(p_450285_)), p_451142_
            );
      }

      public static OptionsList.Entry small(Options p_450641_, OptionInstance<?> p_456116_, @Nullable OptionInstance<?> p_453534_, OptionsSubScreen p_452963_) {
         AbstractWidget abstractwidget = p_456116_.createButton(p_450641_);
         return p_453534_ == null
            ? new OptionsList.Entry(List.of(new OptionsList.OptionInstanceWidget(abstractwidget, p_456116_)), p_452963_)
            : new OptionsList.Entry(
               List.of(
                  new OptionsList.OptionInstanceWidget(abstractwidget, p_456116_),
                  new OptionsList.OptionInstanceWidget(p_453534_.createButton(p_450641_), p_453534_)
               ),
               p_452963_
            );
      }

      @Override
      public void renderContent(GuiGraphics p_281311_, int p_94497_, int p_94498_, boolean p_94504_, float p_94505_) {
         int i = 0;
         int j = this.screen.width / 2 - 155;

         for (OptionsList.OptionInstanceWidget optionslist$optioninstancewidget : this.children) {
            optionslist$optioninstancewidget.widget().setPosition(j + i, this.getContentY());
            optionslist$optioninstancewidget.widget().render(p_281311_, p_94497_, p_94498_, p_94505_);
            i += 160;
         }
      }

      @Override
      public List<? extends GuiEventListener> children() {
         return Lists.transform(this.children, OptionsList.OptionInstanceWidget::widget);
      }

      @Override
      public List<? extends NarratableEntry> narratables() {
         return Lists.transform(this.children, OptionsList.OptionInstanceWidget::widget);
      }

      public @Nullable AbstractWidget findOption(OptionInstance<?> p_458955_) {
         for (OptionsList.OptionInstanceWidget optionslist$optioninstancewidget : this.children) {
            if (optionslist$optioninstancewidget.optionInstance == p_458955_) {
               return optionslist$optioninstancewidget.widget();
            }
         }

         return null;
      }
   }

   @OnlyIn(Dist.CLIENT)
   protected static class HeaderEntry extends OptionsList.AbstractEntry {
      private final Screen screen;
      private final int paddingTop;
      private final StringWidget widget;

      protected HeaderEntry(Screen p_455678_, Component p_459088_, int p_457334_) {
         this.screen = p_455678_;
         this.paddingTop = p_457334_;
         this.widget = new StringWidget(p_459088_, p_455678_.getFont());
      }

      @Override
      public List<? extends NarratableEntry> narratables() {
         return List.of(this.widget);
      }

      @Override
      public void renderContent(GuiGraphics p_453739_, int p_452309_, int p_450743_, boolean p_450258_, float p_460858_) {
         this.widget.setPosition(this.screen.width / 2 - 155, this.getContentY() + this.paddingTop);
         this.widget.render(p_453739_, p_452309_, p_450743_, p_460858_);
      }

      @Override
      public List<? extends GuiEventListener> children() {
         return List.of(this.widget);
      }
   }

   @OnlyIn(Dist.CLIENT)
   public record OptionInstanceWidget(AbstractWidget widget, @Nullable OptionInstance<?> optionInstance) {
      public OptionInstanceWidget(AbstractWidget p_459261_) {
         this(p_459261_, null);
      }
   }
}
