package org.friselis.cobblestars;

public class PlayerData {
   public String lang = "en";
   public String username = "";
   public long duelCooldown = 0L;

   public boolean onCooldown() {
      return this.duelCooldown - System.currentTimeMillis() > 0L;
   }

   public String cooldownRemaining() {
      return Utils.formatTimeRemaining(this.duelCooldown - System.currentTimeMillis());
   }
}
