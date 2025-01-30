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
import org.friselis.cobblestars.Datas.SafariAuctionData;
import org.friselis.cobblestars.Utils;
import org.friselis.cobblestars.WebSocket;

public class SafariAuction {
    private ScheduledExecutorService scheduleAuction;
    private ScheduledFuture<?> scheduleFutureAuction;
    private ScheduledExecutorService scheduleAuctionEnd;
    private ScheduledFuture<?> scheduleFutureAuctionEnd;
    private final int initialBid = 2000;
    public int minimumBidIncrement = 100;
    public List<Species> speciesList = new ArrayList<>();
    public List<UUID> notificationEnabled = new ArrayList<>();
    public static SafariAuctionData safariAuctionData;
    public static SafariAuction instance;

    public SafariAuction() {
        instance = this;
        SafariAuctionData.Load();
        safariAuctionData = SafariAuctionData.data;
        this.scheduleAuctionEnd = Executors.newSingleThreadScheduledExecutor();
        this.scheduleAuction = Executors.newSingleThreadScheduledExecutor();

        this.speciesList.addAll(PokemonSpecies.INSTANCE.getSpecies());

        this.startNextAuctionTimer();
    }

    public static void Start() {
        new SafariAuction();
    }

    public static boolean isActive() {
        return safariAuctionData.currentPokemon != null;
    }

    public long getMinutesLeftToAuction() {
        return this.scheduleFutureAuctionEnd.getDelay(TimeUnit.MINUTES);
    }

    public ServerPlayerEntity getPlayer() {
        return safariAuctionData.playerUUID == null ? null : CobbleStars.instance.server.getPlayerManager().getPlayer(safariAuctionData.playerUUID);
    }

    public void addBid(ServerPlayerEntity player, int bid) {
        bid += initialBid;
        if (!isActive()) {
            SendNotification(player, "No auction currently");
        } else if (Objects.equals(safariAuctionData.playerUUID, player.getUuid())) {
            SendNotification(player, "Your already the highest bidder");
        } else if (bid < safariAuctionData.currentBid + this.minimumBidIncrement) {
            SendNotification(player, "You need to bid a minimum of " + Utils.convertValue(this.minimumBidIncrement) + " more than the last bid !");
        } else {
            if (ModComponents.CURRENCY.get(player).getValue() >= (long) bid && bid > safariAuctionData.currentBid) {
                if (safariAuctionData.playerUUID != null) {
                    ServerPlayerEntity oldPlayer = CobbleStars.instance.server.getPlayerManager().getPlayer(safariAuctionData.playerUUID);
                    if (oldPlayer != null) {
                        SendNotification(oldPlayer, player.getDisplayName().toString() + " took ur bid u got refund");
                        this.refund(oldPlayer);
                    }
                }

                SendNotification(player, "U made a bid ! -" + Utils.convertValue(bid));
                ModComponents.CURRENCY.get(player).modify((long) (-bid));
                safariAuctionData.currentBid = bid;
                safariAuctionData.playerUUID = player.getUuid();
                if (this.scheduleFutureAuctionEnd.getDelay(TimeUnit.MINUTES) <= 30L) {
                    SendNotification(player, "Ur bid made the timer go back to 30 minutes !");
                    this.scheduleFutureAuctionEnd.cancel(true);
                    this.scheduleFutureAuctionEnd = this.scheduleAuctionEnd.schedule(this::AuctionEnd, 30L, TimeUnit.MINUTES);
                    return;
                }

                if (this.scheduleFutureAuctionEnd.getDelay(TimeUnit.MINUTES) <= 15L) {
                    SendNotification(player, "Ur bid made the timer go back to 15 minutes !");
                    this.scheduleFutureAuctionEnd.cancel(true);
                    this.scheduleFutureAuctionEnd = this.scheduleAuctionEnd.schedule(this::AuctionEnd, 15L, TimeUnit.MINUTES);
                    return;
                }

                if (this.scheduleFutureAuctionEnd.getDelay(TimeUnit.MINUTES) <= 5L) {
                    SendNotification(player, "Ur bid made the timer go back to 5 minutes !");
                    this.scheduleFutureAuctionEnd.cancel(true);
                    this.scheduleFutureAuctionEnd = this.scheduleAuctionEnd.schedule(this::AuctionEnd, 5L, TimeUnit.MINUTES);
                }
            } else {
                SendNotification(player, "Not enough money!");
            }

        }
    }

    public void refund(ServerPlayerEntity player) {
        ModComponents.CURRENCY.get(player).modify(safariAuctionData.currentBid);
        SendNotification(player, "Someone bid higher than u\nU got back " + Utils.convertValue(safariAuctionData.currentBid));
    }

    public void AuctionStart() {
        if (this.scheduleFutureAuction.getDelay(TimeUnit.MINUTES) >= 15L) {
            this.scheduleFutureAuction.cancel(true);
        }

        safariAuctionData.currentPokemon = getRandomLegitPokemon();
        safariAuctionData.currentBid = Utils.getRandomInt100(minimumBidIncrement, initialBid);
        CobbleStars.LOGGER.info("New auction started!\n\n{}\n\nStart with a default bid of {}", safariAuctionData.currentPokemon.getName(), Utils.convertValue(safariAuctionData.currentBid));
        String pokemonName = safariAuctionData.currentPokemon.getName();
        CobbleStars.instance.sendGlobalMessage("New auction started!\n\n" + pokemonName + "\n\nStart with a default bid of " + Utils.convertValue(safariAuctionData.currentBid));
        WebSocket.sendNewAuctionMessage(safariAuctionData.currentPokemon.getName(), Utils.convertValue(safariAuctionData.currentBid));
        this.scheduleFutureAuctionEnd = this.scheduleAuctionEnd.schedule(this::AuctionEnd, 60L, TimeUnit.MINUTES);
    }

    public void AuctionEnd() {
        if (this.scheduleFutureAuctionEnd.getDelay(TimeUnit.MINUTES) >= 15L) {
            this.scheduleFutureAuctionEnd.cancel(true);
        }

        PlayerPartyStore party;
        try {
            party = Cobblemon.INSTANCE.getStorage().getParty(safariAuctionData.playerUUID);
        } catch (Exception var11) {
            safariAuctionData.currentPokemon = null;
            safariAuctionData.currentBid = 0;
            safariAuctionData.playerUUID = null;
            return;
        }

        int max = 1;
        int min = 20;
        int range = max - min + 1;
        int rand = (int) (Math.random() * (double) range) + min;
        Pokemon pokemon = safariAuctionData.currentPokemon.create(rand);
        int max2 = 1;
        int min2 = 20;
        int range2 = max2 - min2 + 1;
        int rand2 = (int) (Math.random() * (double) range2) + min2;
        pokemon.setShiny(rand2 == 1);
        party.add(pokemon);
        notificationEnabled = new ArrayList<>();
        safariAuctionData.currentPokemon = null;
        safariAuctionData.currentBid = 0;
        safariAuctionData.playerUUID = null;
        this.startNextAuctionTimer();
    }

    public static Species getRandomLegitPokemon() {
        int max = instance.speciesList.size();
        int rand = (int) (Math.random() * (double) max);
        return instance.speciesList.get(rand);
    }

    public void startNextAuctionTimer() {
        int max = 8;
        int min = 1;
        int range = max - min + 1;
        int rand = (int) (Math.random() * (double) range) + min;
        this.scheduleFutureAuction = this.scheduleAuction.schedule(this::AuctionStart, (long) rand, TimeUnit.HOURS);
    }

    public void toggleNotification(UUID uuid) {
        if (this.notificationEnabled.contains(uuid)) {
            this.notificationEnabled.remove(uuid);
        } else {
            this.notificationEnabled.add(uuid);
        }

    }

    public void SendNotification(ServerPlayerEntity player, String message) {
        player.sendMessage(Text.of(("[&6Auction] &f&l" + message).replaceAll("&", "§")));
    }

    public void SendNotification(String message) {
        for (UUID uuid : this.notificationEnabled) {
            var player = CobbleStars.instance.server.getPlayerManager().getPlayer(uuid);
            if (player != null) {
                SendNotification(player, message);
            }
        }
    }

    private long lastTimer = 0;

    public void tick() {
        if (getMinutesLeftToAuction() != lastTimer) {
            lastTimer = getMinutesLeftToAuction();
            if (lastTimer == 5) {
                SendNotification("&45&f minutes left");
            }
            if (lastTimer == 15) {
                SendNotification("&415&f minutes left");
            }
            if (lastTimer == 30) {
                SendNotification("&430&f minutes left");
            }
        }
    }
}
