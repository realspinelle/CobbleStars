package org.friselis.cobblestars;

import abeshutt.staracademy.init.ModWorldData;
import abeshutt.staracademy.world.data.StarBadgeData;
import abeshutt.staracademy.world.inventory.BaseInventory;
import java.util.UUID;
import net.minecraft.item.ItemStack;
import net.minecraft.server.network.ServerPlayerEntity;

public class StarInventory {
   public static StarBadgeData starBadgeData;
   public UUID uuid;
   public BaseInventory inventory;

   public static StarInventory get(ServerPlayerEntity player) {
      starBadgeData = (StarBadgeData)ModWorldData.STAR_BADGE.getGlobal(CobbleStars.instance.server);
      return new StarInventory(player);
   }

   private StarInventory(ServerPlayerEntity player) {
      this.uuid = player.getUuid();
      this.inventory = starBadgeData.getOrCreate(player);
   }

   public int getStarSlot() {
      int response = -1;

      for(int i = this.inventory.size(); i >= 0; --i) {
         ItemStack slot = this.inventory.getStack(i);
         if (!slot.isEmpty()) {
            response = i;
            break;
         }
      }

      return response;
   }

   public int getCount() {
      int response = 0;

      for(int i = 0; i < this.inventory.size(); ++i) {
         ItemStack slot = this.inventory.getStack(i);
         if (slot.isEmpty()) {
            ++response;
         }
      }

      return this.inventory.size() - response;
   }

   public int getAvailableSlot() {
      int response = -1;

      for(int i = 0; i < this.inventory.size(); ++i) {
         ItemStack slot = this.inventory.getStack(i);
         if (slot.isEmpty()) {
            response = i;
            break;
         }
      }

      return response;
   }

   public int getSlotLeft() {
      int response = 0;

      for(int i = 0; i < this.inventory.size(); ++i) {
         ItemStack slot = this.inventory.getStack(i);
         if (slot.isEmpty()) {
            ++response;
         }
      }

      return response;
   }

   public boolean canTransfer() {
      return this.getAvailableSlot() != -1;
   }

   public static void transferStar(StarInventory inventory, StarInventory targetInventory) {
      int originalItemSlot = inventory.getStarSlot();
      ItemStack item = inventory.inventory.getStack(originalItemSlot);
      if (targetInventory.canTransfer()) {
         targetInventory.inventory.addStack(item.copy());
         inventory.inventory.removeStack(originalItemSlot);
      }
   }

   public static void transferStar(ServerPlayerEntity player, ServerPlayerEntity targetPlayer) {
      transferStar(get(player), get(targetPlayer));
   }
}
