package net.minecraft.client.gui.components;

public interface TabOrderedElement {
   default int getTabOrderGroup() {
      return 0;
   }
}
