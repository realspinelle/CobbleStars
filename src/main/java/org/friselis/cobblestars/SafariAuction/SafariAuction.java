package org.friselis.cobblestars.SafariAuction;

import com.cobblemon.mod.common.Cobblemon;
import com.cobblemon.mod.common.api.pokemon.PokemonSpecies;
import com.cobblemon.mod.common.api.storage.party.PlayerPartyStore;
import com.cobblemon.mod.common.pokemon.Pokemon;
import com.cobblemon.mod.common.pokemon.Species;
import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import org.friselis.cobblestars.CobbleStars;
import org.friselis.cobblestars.Utils;
import org.friselis.cobblestars.WebSocket;

public class SafariAuction {
   private ScheduledExecutorService scheduleAuction;
   private ScheduledFuture<?> scheduleFutureAuction;
   private ScheduledExecutorService scheduleAuctionEnd;
   private ScheduledFuture<?> scheduleFutureAuctionEnd;
   public int currentBid;
   private final int initialBid = 2000;
   public int minimumBidIncrement = 100;
   public UUID playerUUID;
   public Species currentPokemon;
   public static SafariAuction instance;
   public List<Species> speciesList = new ArrayList();
   public List<UUID> notificationEnabled = new ArrayList();

   public SafariAuction() {
      instance = this;
      this.scheduleAuctionEnd = Executors.newSingleThreadScheduledExecutor();
      this.scheduleAuction = Executors.newSingleThreadScheduledExecutor();
      Iterator var1 = PokemonSpecies.INSTANCE.getSpecies().iterator();

      while(var1.hasNext()) {
         Species specie = (Species)var1.next();
         if (specie.getEvolutions().size() >= 0 && specie.getPreEvolution() == null) {
            Pokemon pkm = specie.create(1);
            if (!pkm.isLegendary() && !pkm.isMythical() && !pkm.isUltraBeast() && !specie.getName().startsWith("mega")) {
               this.speciesList.add(specie);
            }
         }
      }

      this.startNextAuction();
   }

   public static void Start() {
      new SafariAuction();
   }

   public static boolean isActive() {
      return instance.currentPokemon != null;
   }

   public String getMinutesLeftToAuction() {
      return String.valueOf(this.scheduleFutureAuctionEnd.getDelay(TimeUnit.MINUTES));
   }

   public ServerPlayerEntity getPlayer() {
      return this.playerUUID == null ? null : CobbleStars.instance.server.getPlayerManager().getPlayer(this.playerUUID);
   }

   public void addBid(ServerPlayerEntity player, int bid) {
      bid += 2000;
      if (!isActive()) {
         player.sendMessage(Text.of("No auction currently"));
      } else if (Objects.equals(this.playerUUID, player.getUuid())) {
         player.sendMessage(Text.of("Your already the highest bidder"));
      } else if (bid < this.currentBid + this.minimumBidIncrement) {
         player.sendMessage(Text.of("You need to bid a minimum of " + Utils.convertValue(this.minimumBidIncrement) + " more than the last bid !"));
      } else {
         if (((CurrencyComponent)ModComponents.CURRENCY.get(player)).getValue() >= (long)bid && bid > this.currentBid) {
            if (this.playerUUID != null) {
               ServerPlayerEntity oldPlayer = CobbleStars.instance.server.getPlayerManager().getPlayer(this.playerUUID);
               if (oldPlayer != null) {
                  oldPlayer.sendMessage(Text.of(player.getDisplayName().toString() + " took ur bid u got refund"));
                  this.refund(oldPlayer);
               }
            }

            player.sendMessage(Text.of("U made a bid ! -" + Utils.convertValue(bid)));
            ((CurrencyComponent)ModComponents.CURRENCY.get(player)).modify((long)(-bid));
            this.currentBid = bid;
            this.playerUUID = player.getUuid();
            if (this.scheduleFutureAuctionEnd.getDelay(TimeUnit.MINUTES) <= 30L) {
               player.sendMessage(Text.of("Ur bid made the timer go back to 30 minutes !"));
               this.scheduleFutureAuctionEnd.cancel(true);
               this.scheduleFutureAuctionEnd = this.scheduleAuctionEnd.schedule(this::AuctionEnd, 30L, TimeUnit.MINUTES);
               return;
            }

            if (this.scheduleFutureAuctionEnd.getDelay(TimeUnit.MINUTES) <= 15L) {
               player.sendMessage(Text.of("Ur bid made the timer go back to 15 minutes !"));
               this.scheduleFutureAuctionEnd.cancel(true);
               this.scheduleFutureAuctionEnd = this.scheduleAuctionEnd.schedule(this::AuctionEnd, 15L, TimeUnit.MINUTES);
               return;
            }

            if (this.scheduleFutureAuctionEnd.getDelay(TimeUnit.MINUTES) <= 5L) {
               player.sendMessage(Text.of("Ur bid made the timer go back to 5 minutes !"));
               this.scheduleFutureAuctionEnd.cancel(true);
               this.scheduleFutureAuctionEnd = this.scheduleAuctionEnd.schedule(this::AuctionEnd, 5L, TimeUnit.MINUTES);
            }
         } else {
            player.sendMessage(Text.of("Not enough money!"));
         }

      }
   }

   public void refund(ServerPlayerEntity player) {
      ((CurrencyComponent)ModComponents.CURRENCY.get(player)).modify((long)this.currentBid);
      player.sendMessage(Text.of("Someone bid higher than u\nU got back " + Utils.convertValue(this.currentBid)));
   }

   public void AuctionStart() {
      if (this.scheduleFutureAuction.getDelay(TimeUnit.MINUTES) >= 15L) {
         this.scheduleFutureAuction.cancel(true);
      }

      this.currentPokemon = getRandomLegitPokemon();
      this.currentBid = Utils.getRandomInt100(100, 2000);
      CobbleStars.LOGGER.info((String)"New auction started!\n\n{}\n\nStart with a default bid of {}", (Object)this.currentPokemon.getName(), (Object)Utils.convertValue(this.currentBid));
      CobbleStars var10000 = CobbleStars.instance;
      String var10001 = this.currentPokemon.getName();
      var10000.sendGlobalMessage("New auction started!\n\n" + var10001 + "\n\nStart with a default bid of " + Utils.convertValue(this.currentBid));
      WebSocket.sendNewAuctionMessage(this.currentPokemon.getName(), Utils.convertValue(this.currentBid));
      this.scheduleFutureAuctionEnd = this.scheduleAuctionEnd.schedule(this::AuctionEnd, 60L, TimeUnit.MINUTES);
   }

   public void AuctionEnd() {
      if (this.scheduleFutureAuctionEnd.getDelay(TimeUnit.MINUTES) >= 15L) {
         this.scheduleFutureAuctionEnd.cancel(true);
      }

      PlayerPartyStore party;
      try {
         party = Cobblemon.INSTANCE.getStorage().getParty(this.playerUUID);
      } catch (Exception var11) {
         this.currentPokemon = null;
         this.currentBid = 0;
         this.playerUUID = null;
         return;
      }

      int max = 1;
      int min = 20;
      int range = max - min + 1;
      int rand = (int)(Math.random() * (double)range) + min;
      Pokemon pokemon = this.currentPokemon.create(rand);
      int max2 = 1;
      int min2 = 20;
      int range2 = max2 - min2 + 1;
      int rand2 = (int)(Math.random() * (double)range2) + min2;
      pokemon.setShiny(rand2 == 1);
      party.add(pokemon);
      this.currentPokemon = null;
      this.currentBid = 0;
      this.playerUUID = null;
      this.startNextAuction();
   }

   public static Species getRandomLegitPokemon() {
      int max = instance.speciesList.size();
      int rand = (int)(Math.random() * (double)max);
      return (Species)instance.speciesList.get(rand);
   }

   public void startNextAuction() {
      int max = 8;
      int min = 1;
      int range = max - min + 1;
      int rand = (int)(Math.random() * (double)range) + min;
      this.scheduleFutureAuction = this.scheduleAuction.schedule(this::AuctionStart, (long)rand, TimeUnit.HOURS);
   }

   public void toggleNotification(UUID uuid) {
      if (this.notificationEnabled.contains(uuid)) {
         this.notificationEnabled.remove(uuid);
      } else {
         this.notificationEnabled.add(uuid);
      }

   }

   public static void tick() {
   }
}
