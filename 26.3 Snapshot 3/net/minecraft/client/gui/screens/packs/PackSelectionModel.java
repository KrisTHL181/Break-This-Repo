package net.minecraft.client.gui.screens.packs;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import java.util.Collections;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Stream;
import net.minecraft.client.Minecraft;
import net.minecraft.client.OptionInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.repository.Pack;
import net.minecraft.server.packs.repository.PackCompatibility;
import net.minecraft.server.packs.repository.PackRepository;
import net.minecraft.server.packs.repository.PackSource;

public class PackSelectionModel {
   private final PackRepository repository;
   private final List<Pack> selected;
   private final List<Pack> unselected;
   private final Function<Pack, Identifier> iconGetter;
   private final Consumer<PackSelectionModel.EntryBase> onListChanged;
   private final Consumer<PackRepository> output;

   public PackSelectionModel(
      final Consumer<PackSelectionModel.EntryBase> onListChanged,
      final Function<Pack, Identifier> iconGetter,
      final PackRepository repository,
      final Consumer<PackRepository> output
   ) {
      this.onListChanged = onListChanged;
      this.iconGetter = iconGetter;
      this.repository = repository;
      this.selected = Lists.newArrayList(repository.getSelectedPacks());
      Collections.reverse(this.selected);
      this.unselected = Lists.newArrayList(repository.getAvailablePacks());
      this.unselected.removeAll(this.selected);
      this.output = output;
   }

   public Stream<PackSelectionModel.Entry> getUnselected() {
      return this.unselected.stream().map(x$0 -> new PackSelectionModel.UnselectedPackEntry(x$0));
   }

   public Stream<PackSelectionModel.Entry> getSelected() {
      return this.selected.stream().map(x$0 -> new PackSelectionModel.SelectedPackEntry(x$0));
   }

   private void updateRepoSelectedList() {
      this.repository.setSelected(Lists.reverse(this.selected).stream().map(Pack::getId).collect(ImmutableList.toImmutableList()));
   }

   public void commit() {
      this.updateRepoSelectedList();
      this.output.accept(this.repository);
   }

   public void findNewPacks() {
      this.repository.reload();
      this.selected.retainAll(this.repository.getAvailablePacks());
      this.unselected.clear();
      this.unselected.addAll(this.repository.getAvailablePacks());
      this.unselected.removeAll(this.selected);
   }

   public interface Entry {
      Identifier getIconTexture();

      PackCompatibility getCompatibility();

      String getId();

      Component getTitle();

      Component getDescription();

      PackSource getPackSource();

      default Component getExtendedDescription() {
         return this.getPackSource().decorate(this.getDescription());
      }

      boolean isFixedPosition();

      boolean isRequired();

      void select();

      void unselect();

      void moveUp();

      void moveDown();

      boolean isSelected();

      default boolean canSelect() {
         return !this.isSelected();
      }

      default boolean canUnselect() {
         return this.isSelected() && !this.isRequired();
      }

      boolean canMoveUp();

      boolean canMoveDown();
   }

   public abstract class EntryBase implements PackSelectionModel.Entry {
      private final Pack pack;

      public EntryBase(final Pack pack) {
         this.pack = pack;
      }

      protected abstract List<Pack> getSelfList();

      protected abstract List<Pack> getOtherList();

      @Override
      public Identifier getIconTexture() {
         return PackSelectionModel.this.iconGetter.apply(this.pack);
      }

      @Override
      public PackCompatibility getCompatibility() {
         return this.pack.getCompatibility();
      }

      @Override
      public String getId() {
         return this.pack.getId();
      }

      @Override
      public Component getTitle() {
         return this.pack.getTitle();
      }

      @Override
      public Component getDescription() {
         return this.pack.getDescription();
      }

      @Override
      public PackSource getPackSource() {
         return this.pack.getPackSource();
      }

      @Override
      public boolean isFixedPosition() {
         return this.pack.isFixedPosition();
      }

      @Override
      public boolean isRequired() {
         return this.pack.isRequired();
      }

      protected void toggleSelection() {
         this.getSelfList().remove(this.pack);
         this.pack.getDefaultPosition().insert(this.getOtherList(), this.pack, Pack::selectionConfig, true);
         PackSelectionModel.this.onListChanged.accept(this);
         PackSelectionModel.this.updateRepoSelectedList();
         this.updateHighContrastOptionInstance();
      }

      private void updateHighContrastOptionInstance() {
         if (this.pack.getId().equals("high_contrast")) {
            OptionInstance<Boolean> highContrastMode = Minecraft.getInstance().options.highContrast();
            highContrastMode.set(!highContrastMode.get());
         }
      }

      protected void move(final int direction) {
         List<Pack> list = this.getSelfList();
         int currentPos = list.indexOf(this.pack);
         list.remove(currentPos);
         list.add(currentPos + direction, this.pack);
         PackSelectionModel.this.onListChanged.accept(this);
      }

      @Override
      public boolean canMoveUp() {
         List<Pack> list = this.getSelfList();
         int index = list.indexOf(this.pack);
         return index > 0 && !list.get(index - 1).isFixedPosition();
      }

      @Override
      public void moveUp() {
         this.move(-1);
      }

      @Override
      public boolean canMoveDown() {
         List<Pack> list = this.getSelfList();
         int index = list.indexOf(this.pack);
         return index >= 0 && index < list.size() - 1 && !list.get(index + 1).isFixedPosition();
      }

      @Override
      public void moveDown() {
         this.move(1);
      }
   }

   private class SelectedPackEntry extends PackSelectionModel.EntryBase {
      public SelectedPackEntry(final Pack pack) {
         super(pack);
      }

      @Override
      protected List<Pack> getSelfList() {
         return PackSelectionModel.this.selected;
      }

      @Override
      protected List<Pack> getOtherList() {
         return PackSelectionModel.this.unselected;
      }

      @Override
      public boolean isSelected() {
         return true;
      }

      @Override
      public void select() {
      }

      @Override
      public void unselect() {
         this.toggleSelection();
      }
   }

   private class UnselectedPackEntry extends PackSelectionModel.EntryBase {
      public UnselectedPackEntry(final Pack pack) {
         super(pack);
      }

      @Override
      protected List<Pack> getSelfList() {
         return PackSelectionModel.this.unselected;
      }

      @Override
      protected List<Pack> getOtherList() {
         return PackSelectionModel.this.selected;
      }

      @Override
      public boolean isSelected() {
         return false;
      }

      @Override
      public void select() {
         this.toggleSelection();
      }

      @Override
      public void unselect() {
      }
   }
}
