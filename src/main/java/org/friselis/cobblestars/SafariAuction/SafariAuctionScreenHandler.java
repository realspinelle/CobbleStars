package org.friselis.cobblestars.SafariAuction;

import com.cobblemon.mod.common.item.PokemonItem;
import com.glisco.numismaticoverhaul.ModComponents;
import com.glisco.numismaticoverhaul.currency.CurrencyComponent;

import java.util.*;

import me.lucko.fabric.api.permissions.v0.Permissions;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.screen.ScreenHandler;
import net.minecraft.screen.ScreenHandlerType;
import net.minecraft.screen.SimpleNamedScreenHandlerFactory;
import net.minecraft.screen.slot.SlotActionType;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.collection.DefaultedList;
import org.friselis.cobblestars.CobbleStars;
import org.friselis.cobblestars.Utils;

public class SafariAuctionScreenHandler extends ScreenHandler {
    public static List<UUID> opennedInventories = new ArrayList<>();

    public SafariAuctionScreenHandler(int syncId, PlayerInventory playerInventory) {
        super(ScreenHandlerType.GENERIC_9X3, syncId);
        this.setupInventory(playerInventory);
    }

    @Override
    public void onClosed(PlayerEntity player) {
        super.onClosed(player);
        opennedInventories.remove(player.getUuid());
    }

    @Override
    public ItemStack quickMove(PlayerEntity player, int slot) {
        return null;
    }

    @Override
    public boolean canUse(PlayerEntity player) {
        return true;
    }

    @Override
    public void onSlotClick(int slotIndex, int button, SlotActionType actionType, PlayerEntity player) {
        SafariAuction auction = SafariAuction.instance;
        boolean needReload = false;
        switch (slotIndex) {
            case 8:
                if (SafariAuction.isActive()) {
                    auction.AuctionEnd();
                } else {
                    auction.AuctionStart();
                }

                needReload = true;
                break;
            case 20:
                needReload = true;
                auction.addBid(CobbleStars.instance.server.getPlayerManager().getPlayer(player.getUuid()), 100);
                break;
            case 21:
                needReload = true;
                auction.addBid(CobbleStars.instance.server.getPlayerManager().getPlayer(player.getUuid()), 1000);
                break;
            case 23:
                needReload = true;
                auction.addBid(CobbleStars.instance.server.getPlayerManager().getPlayer(player.getUuid()), 10000);
                break;
            case 24:
                needReload = true;
                auction.addBid(CobbleStars.instance.server.getPlayerManager().getPlayer(player.getUuid()), 100000);
                break;
            case 26:
                auction.toggleNotification(player.getUuid());
                break;
        }

        if (needReload) {
            updateInventories();
        }

    }

    @Override
    public boolean onButtonClick(PlayerEntity player, int id) {
        return super.onButtonClick(player, id);
    }

    public void setupInventory(PlayerInventory playerInventory) {
        DefaultedList<ItemStack> inventory = DefaultedList.ofSize(27, new ItemStack(Items.AIR));
        inventory.replaceAll((ignored) -> {
            ItemStack default_item = new ItemStack(Items.BLACK_STAINED_GLASS_PANE, 1);
            default_item.setCustomName(Text.of(""));
            return default_item;
        });
        SafariAuction auction = SafariAuction.instance;
        if (SafariAuction.isActive()) {
            var pokemonItem = PokemonItem.from(SafariAuction.safariAuctionData.currentPokemon.create(1), 1);
            pokemonItem.setCustomName(pokemonItem.getName().copy().styled((style) -> style.withItalic(false).withColor(Formatting.BLUE)));
            inventory.set(13, pokemonItem);
            int value = SafariAuction.safariAuctionData.currentBid;
            int gold = value / 10000;
            int silver = value % 10000 / 100;
            ItemStack auctionInfo = Utils.getMoneyBagStack(0, silver, gold);
            auctionInfo.setCustomName(Text.of("Auction info").copy().styled((style) -> style.withItalic(false).withColor(Formatting.RED)));
            ServerPlayerEntity currentBidder = auction.getPlayer();
            String[] var10003 = new String[]{"&r&6Time left : " + auction.getMinutesLeftToAuction() + "min", null};
            Object var10006 = currentBidder == null ? "None" : currentBidder.getDisplayName();
            var10003[1] = "&rCurrent bidder : &c&l" + var10006;
            inventory.set(4, Utils.addLore(auctionInfo, var10003));
            int purseValue = (int) ModComponents.CURRENCY.get(playerInventory.player).getValue();
            int purseGold = purseValue / 10000;
            int purseSilver = purseValue % 10000 / 100;
            int purseBronze = purseValue % 100;
            ItemStack moneyInfo = Utils.getMoneyBagStack(purseBronze, purseSilver, purseGold);
            moneyInfo.setCustomName(Text.of("Your purse").copy().styled((style) -> style.withItalic(false).withColor(Formatting.RED)));
            inventory.set(18, moneyInfo);
            ItemStack add1Silver = Utils.getSilverItemStack(1);
            inventory.set(20, add1Silver);
            ItemStack add10Silver = Utils.getSilverItemStack(10);
            inventory.set(21, add10Silver);
            ItemStack add1Gold = Utils.getGoldItemStack(1);
            inventory.set(23, add1Gold);
            ItemStack add10Gold = Utils.getGoldItemStack(10);
            inventory.set(24, add10Gold);
            boolean hasNotificationEnabled = auction.notificationEnabled.contains(playerInventory.player.getUuid());
            ItemStack toggleUpdateNotification = new ItemStack(hasNotificationEnabled ? Items.GREEN_STAINED_GLASS_PANE : Items.RED_STAINED_GLASS_PANE, 1);
            toggleUpdateNotification.setCustomName(Text.of(hasNotificationEnabled ? "Notifications ON" : "Notifications OFF"));
            inventory.set(26, Utils.addLore(toggleUpdateNotification, new String[]{hasNotificationEnabled ? "&r&2Click to turn off" : "&r&2Click to turn on"}));
        } else {
            var InfoItem = new ItemStack(Items.RED_STAINED_GLASS_PANE, 1);
            InfoItem.setCustomName(Text.of("No auction currently").copy().styled((style) -> style.withItalic(false).withColor(Formatting.RED)));
            inventory.set(13, InfoItem);
        }

        boolean hasStartPermission = Permissions.check(playerInventory.player, "discord.bridge.auction.force", 4);
        if (hasStartPermission) {
            ItemStack toggleUpdateNotification = new ItemStack(SafariAuction.isActive() ? Items.GREEN_STAINED_GLASS_PANE : Items.RED_STAINED_GLASS_PANE, 1);
            toggleUpdateNotification.setCustomName(Text.of(SafariAuction.isActive() ? "Stop" : "Start").copy().styled((style) -> style.withItalic(false).withColor(Formatting.RED)));
            inventory.set(8, Utils.addLore(toggleUpdateNotification, new String[]{SafariAuction.isActive() ? "&r&2Click to stop" : "&r&2Click to start"}));
        }
        if (!opennedInventories.contains(playerInventory.player.getUuid())) {
            opennedInventories.add(playerInventory.player.getUuid());
        }
    }

    public static void updateInventories() {
        for (UUID uuid : opennedInventories) {
            var player = CobbleStars.instance.server.getPlayerManager().getPlayer(uuid);
            if (player != null) {
                open(player);
            }
        }
    }

    public static void open(PlayerEntity player) {
        player.openHandledScreen(new SimpleNamedScreenHandlerFactory((syncId, playerInventory, playerEntity) -> new SafariAuctionScreenHandler(syncId, playerInventory), Text.literal("§5Auction")));
    }
}
