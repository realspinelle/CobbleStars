package org.friselis.cobblestars.SafariAuction;

import net.minecraft.inventory.SimpleInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.util.collection.DefaultedList;

class FakeInventory extends SimpleInventory {
   public FakeInventory(DefaultedList<ItemStack> items) {
      super(items.size());

      for(int i = 0; i < items.size(); ++i) {
         this.setStack(i, items.get(i));
      }

   }
}
