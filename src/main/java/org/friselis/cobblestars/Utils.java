package org.friselis.cobblestars;

import java.util.Random;
import java.util.concurrent.TimeUnit;
import net.minecraft.item.ItemConvertible;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtList;
import net.minecraft.nbt.NbtString;
import net.minecraft.registry.Registries;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.Vec3d;

public class Utils {
   public static String convertValue(int value) {
      int gold = value / 10000;
      int silver = value % 10000 / 100;
      int bronze = value % 100;
      StringBuilder result = new StringBuilder();
      if (gold > 0) {
         result.append(gold).append(" gold");
         if (silver > 0 || bronze > 0) {
            result.append(" and ");
         }
      }

      if (silver > 0) {
         result.append(silver).append(" silver");
         if (bronze > 0) {
            result.append(" and ");
         }
      }

      if (bronze > 0) {
         result.append(bronze).append(" bronze");
      }

      return result.toString();
   }

   public static ItemStack getBronzeItemStack(int number) {
      return new ItemStack((ItemConvertible)Registries.ITEM.get(new Identifier("numismatic-overhaul:bronze_coin")), number);
   }

   public static ItemStack getSilverItemStack(int number) {
      return new ItemStack((ItemConvertible)Registries.ITEM.get(new Identifier("numismatic-overhaul:silver_coin")), number);
   }

   public static ItemStack getGoldItemStack(int number) {
      return new ItemStack((ItemConvertible)Registries.ITEM.get(new Identifier("numismatic-overhaul:gold_coin")), number);
   }

   public static ItemStack addLore(ItemStack stack, String[] lore) {
      NbtCompound nbt = stack.getOrCreateNbt();
      NbtCompound displayNbt = nbt.getCompound("display");
      if (!displayNbt.contains("Lore")) {
         displayNbt.put("Lore", new NbtList());
      }

      NbtList loreList = displayNbt.getList("Lore", 8);
      String[] var5 = lore;
      int var6 = lore.length;

      for(int var7 = 0; var7 < var6; ++var7) {
         String line = var5[var7];
         line = line.replaceAll("&", "§");
         loreList.add(NbtString.of("\"" + line + "\""));
      }

      nbt.put("display", displayNbt);
      stack.setNbt(nbt);
      return stack;
   }

   public static ItemStack getMoneyBagStack(int bronze, int silver, int gold) {
      ItemStack itemstack = new ItemStack((ItemConvertible)Registries.ITEM.get(new Identifier("numismatic-overhaul:money_bag")), 1);
      NbtCompound nbt = itemstack.getOrCreateNbt();
      nbt.putLongArray("Values", new long[]{(long)bronze, (long)silver, (long)gold});
      nbt.putBoolean("Combined", true);
      itemstack.setNbt(nbt);
      return itemstack;
   }

   public static int getRandomInt100(int min, int max) {
      max = Math.max(max, 1000);
      min = (int)Math.ceil((double)min / 100.0D) * 100;
      max = (int)Math.floor((double)max / 100.0D) * 100;
      Random random = new Random();
      int range = (max - min) / 100 + 1;
      return min + random.nextInt(range) * 100;
   }

   public static String formatTimeRemaining(long millis) {
      if (millis < 0L) {
         return "Time must be non-negative.";
      } else {
         long days = TimeUnit.MILLISECONDS.toDays(millis);
         millis -= TimeUnit.DAYS.toMillis(days);
         long hours = TimeUnit.MILLISECONDS.toHours(millis);
         millis -= TimeUnit.HOURS.toMillis(hours);
         long minutes = TimeUnit.MILLISECONDS.toMinutes(millis);
         millis -= TimeUnit.MINUTES.toMillis(minutes);
         long seconds = TimeUnit.MILLISECONDS.toSeconds(millis);
         StringBuilder result = new StringBuilder();
         if (days > 0L) {
            result.append(days).append("d ");
         }

         if (hours > 0L || !result.isEmpty()) {
            result.append(hours).append("h ");
         }

         if (minutes > 0L || !result.isEmpty()) {
            result.append(minutes).append("m ");
         }

         result.append(seconds).append("s");
         return result.toString().trim();
      }
   }

   public static boolean isPlayerBetween(Vec3d playerPos, Vec3d pos1, Vec3d pos2) {
      double minX = Math.min(pos1.x, pos2.x);
      double maxX = Math.max(pos1.x, pos2.x);
      double minZ = Math.min(pos1.z, pos2.z);
      double maxZ = Math.max(pos1.z, pos2.z);
      return playerPos.x >= minX && playerPos.x <= maxX && playerPos.z >= minZ && playerPos.z <= maxZ;
   }
}
