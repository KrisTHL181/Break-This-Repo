package net.minecraft.client.gui.layouts;

import com.mojang.math.Divisor;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.util.Mth;
import net.minecraft.util.Util;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GridLayout extends AbstractLayout {
   private final List<LayoutElement> children = new ArrayList<>();
   private final List<GridLayout.CellInhabitant> cellInhabitants = new ArrayList<>();
   private final LayoutSettings defaultCellSettings = LayoutSettings.defaults();
   private int rowSpacing = 0;
   private int columnSpacing = 0;

   public GridLayout() {
      this(0, 0);
   }

   public GridLayout(int p_265045_, int p_265035_) {
      super(p_265045_, p_265035_, 0, 0);
   }

   @Override
   public void arrangeElements() {
      super.arrangeElements();
      int i = 0;
      int j = 0;

      for (GridLayout.CellInhabitant gridlayout$cellinhabitant : this.cellInhabitants) {
         i = Math.max(gridlayout$cellinhabitant.getLastOccupiedRow(), i);
         j = Math.max(gridlayout$cellinhabitant.getLastOccupiedColumn(), j);
      }

      int[] aint = new int[j + 1];
      int[] aint1 = new int[i + 1];

      for (GridLayout.CellInhabitant gridlayout$cellinhabitant1 : this.cellInhabitants) {
         int k = gridlayout$cellinhabitant1.getHeight() - (gridlayout$cellinhabitant1.occupiedRows - 1) * this.rowSpacing;
         Divisor divisor = new Divisor(k, gridlayout$cellinhabitant1.occupiedRows);

         for (int l = gridlayout$cellinhabitant1.row; l <= gridlayout$cellinhabitant1.getLastOccupiedRow(); l++) {
            aint1[l] = Math.max(aint1[l], divisor.nextInt());
         }

         int l1 = gridlayout$cellinhabitant1.getWidth() - (gridlayout$cellinhabitant1.occupiedColumns - 1) * this.columnSpacing;
         Divisor divisor1 = new Divisor(l1, gridlayout$cellinhabitant1.occupiedColumns);

         for (int i1 = gridlayout$cellinhabitant1.column; i1 <= gridlayout$cellinhabitant1.getLastOccupiedColumn(); i1++) {
            aint[i1] = Math.max(aint[i1], divisor1.nextInt());
         }
      }

      int[] aint2 = new int[j + 1];
      int[] aint3 = new int[i + 1];
      aint2[0] = 0;

      for (int j1 = 1; j1 <= j; j1++) {
         aint2[j1] = aint2[j1 - 1] + aint[j1 - 1] + this.columnSpacing;
      }

      aint3[0] = 0;

      for (int k1 = 1; k1 <= i; k1++) {
         aint3[k1] = aint3[k1 - 1] + aint1[k1 - 1] + this.rowSpacing;
      }

      for (GridLayout.CellInhabitant gridlayout$cellinhabitant2 : this.cellInhabitants) {
         int i2 = 0;

         for (int j2 = gridlayout$cellinhabitant2.column; j2 <= gridlayout$cellinhabitant2.getLastOccupiedColumn(); j2++) {
            i2 += aint[j2];
         }

         i2 += this.columnSpacing * (gridlayout$cellinhabitant2.occupiedColumns - 1);
         gridlayout$cellinhabitant2.setX(this.getX() + aint2[gridlayout$cellinhabitant2.column], i2);
         int k2 = 0;

         for (int l2 = gridlayout$cellinhabitant2.row; l2 <= gridlayout$cellinhabitant2.getLastOccupiedRow(); l2++) {
            k2 += aint1[l2];
         }

         k2 += this.rowSpacing * (gridlayout$cellinhabitant2.occupiedRows - 1);
         gridlayout$cellinhabitant2.setY(this.getY() + aint3[gridlayout$cellinhabitant2.row], k2);
      }

      this.width = aint2[j] + aint[j];
      this.height = aint3[i] + aint1[i];
   }

   public <T extends LayoutElement> T addChild(T p_265485_, int p_265720_, int p_265679_) {
      return this.addChild(p_265485_, p_265720_, p_265679_, this.newCellSettings());
   }

   public <T extends LayoutElement> T addChild(T p_265061_, int p_265080_, int p_265105_, LayoutSettings p_265057_) {
      return this.addChild(p_265061_, p_265080_, p_265105_, 1, 1, p_265057_);
   }

   public <T extends LayoutElement> T addChild(T p_297804_, int p_298357_, int p_300223_, Consumer<LayoutSettings> p_299636_) {
      return this.addChild(p_297804_, p_298357_, p_300223_, 1, 1, Util.make(this.newCellSettings(), p_299636_));
   }

   public <T extends LayoutElement> T addChild(T p_265590_, int p_265556_, int p_265323_, int p_265531_, int p_265352_) {
      return this.addChild(p_265590_, p_265556_, p_265323_, p_265531_, p_265352_, this.newCellSettings());
   }

   public <T extends LayoutElement> T addChild(T p_265031_, int p_265582_, int p_265782_, int p_265612_, int p_265448_, LayoutSettings p_265579_) {
      if (p_265612_ < 1) {
         throw new IllegalArgumentException("Occupied rows must be at least 1");
      }

      if (p_265448_ < 1) {
         throw new IllegalArgumentException("Occupied columns must be at least 1");
      }

      this.cellInhabitants.add(new GridLayout.CellInhabitant(p_265031_, p_265582_, p_265782_, p_265612_, p_265448_, p_265579_));
      this.children.add(p_265031_);
      return p_265031_;
   }

   public <T extends LayoutElement> T addChild(T p_299753_, int p_298560_, int p_297873_, int p_297702_, int p_298330_, Consumer<LayoutSettings> p_301225_) {
      return this.addChild(p_299753_, p_298560_, p_297873_, p_297702_, p_298330_, Util.make(this.newCellSettings(), p_301225_));
   }

   public GridLayout columnSpacing(int p_268135_) {
      this.columnSpacing = p_268135_;
      return this;
   }

   public GridLayout rowSpacing(int p_268237_) {
      this.rowSpacing = p_268237_;
      return this;
   }

   public GridLayout spacing(int p_268351_) {
      return this.columnSpacing(p_268351_).rowSpacing(p_268351_);
   }

   @Override
   public void visitChildren(Consumer<LayoutElement> p_265389_) {
      this.children.forEach(p_265389_);
   }

   public LayoutSettings newCellSettings() {
      return this.defaultCellSettings.copy();
   }

   public LayoutSettings defaultCellSetting() {
      return this.defaultCellSettings;
   }

   public GridLayout.RowHelper createRowHelper(int p_265327_) {
      return new GridLayout.RowHelper(p_265327_);
   }

   @OnlyIn(Dist.CLIENT)
   static class CellInhabitant extends AbstractLayout.AbstractChildWrapper {
      final int row;
      final int column;
      final int occupiedRows;
      final int occupiedColumns;

      CellInhabitant(LayoutElement p_265063_, int p_265675_, int p_265198_, int p_265625_, int p_265517_, LayoutSettings p_265036_) {
         super(p_265063_, p_265036_.getExposed());
         this.row = p_265675_;
         this.column = p_265198_;
         this.occupiedRows = p_265625_;
         this.occupiedColumns = p_265517_;
      }

      public int getLastOccupiedRow() {
         return this.row + this.occupiedRows - 1;
      }

      public int getLastOccupiedColumn() {
         return this.column + this.occupiedColumns - 1;
      }
   }

   @OnlyIn(Dist.CLIENT)
   public final class RowHelper {
      private final int columns;
      private int index;

      RowHelper(final int p_265633_) {
         this.columns = p_265633_;
      }

      public <T extends LayoutElement> T addChild(T p_265455_) {
         return this.addChild(p_265455_, 1);
      }

      public <T extends LayoutElement> T addChild(T p_265413_, int p_265491_) {
         return this.addChild(p_265413_, p_265491_, this.defaultCellSetting());
      }

      public <T extends LayoutElement> T addChild(T p_265411_, LayoutSettings p_265755_) {
         return this.addChild(p_265411_, 1, p_265755_);
      }

      public <T extends LayoutElement> T addChild(T p_265200_, int p_265044_, LayoutSettings p_265797_) {
         int i = this.index / this.columns;
         int j = this.index % this.columns;
         if (j + p_265044_ > this.columns) {
            i++;
            j = 0;
            this.index = Mth.roundToward(this.index, this.columns);
         }

         this.index += p_265044_;
         return GridLayout.this.addChild(p_265200_, i, j, 1, p_265044_, p_265797_);
      }

      public GridLayout getGrid() {
         return GridLayout.this;
      }

      public LayoutSettings newCellSettings() {
         return GridLayout.this.newCellSettings();
      }

      public LayoutSettings defaultCellSetting() {
         return GridLayout.this.defaultCellSetting();
      }
   }
}
