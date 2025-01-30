package org.friselis.cobblestars;

import abeshutt.staracademy.init.ModConfigs;
import com.cobblemon.mod.common.api.Priority;
import com.cobblemon.mod.common.api.battles.model.actor.ActorType;
import com.cobblemon.mod.common.api.battles.model.actor.BattleActor;
import com.cobblemon.mod.common.api.events.CobblemonEvents;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.UUID;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerLifecycleEvents;
import net.fabricmc.fabric.api.event.lifecycle.v1.ServerTickEvents;
import net.fabricmc.fabric.api.message.v1.ServerMessageEvents;
import net.fabricmc.fabric.api.networking.v1.ServerPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.luckperms.api.LuckPerms;
import net.luckperms.api.LuckPermsProvider;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.HoverEvent;
import net.minecraft.text.Style;
import net.minecraft.text.HoverEvent.Action;
import net.minecraft.text.Text;
import org.friselis.cobblestars.Commands.Auction;
import org.friselis.cobblestars.Commands.Glove;
import org.friselis.cobblestars.Commands.Stars;
import org.friselis.cobblestars.Commands.Translate;
import org.friselis.cobblestars.Events.ALLOW_CHAT_MESSAGE;
import org.friselis.cobblestars.Events.DISCONNECT;
import org.friselis.cobblestars.Events.JOIN;
import org.friselis.cobblestars.SafariAuction.SafariAuction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class CobbleStars implements ModInitializer {
   public static final String MOD_ID = "discordbridge";
   public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);
   public static CobbleStars instance;
   public MinecraftServer server;
   public HashMap<UUID, PlayerData> playersData = new HashMap();
   public Config config = new Config();
   public final ObjectMapper mapper = new ObjectMapper();
   private final File saveFile = FabricLoader.getInstance().getConfigDir().resolve("CobbleStarsData.json").toFile();
   private final File configFile = FabricLoader.getInstance().getConfigDir().resolve("CobbleStars.json").toFile();
   public LuckPerms luckPerms;
   public List<UUID> duelLists = new ArrayList();

   public void onInitialize() {
      instance = this;
      this.LoadConfig();
      this.LoadPlayerData();
      ServerMessageEvents.ALLOW_CHAT_MESSAGE.register(ALLOW_CHAT_MESSAGE::RUN);
      ServerPlayConnectionEvents.JOIN.register(JOIN::RUN);
      ServerPlayConnectionEvents.DISCONNECT.register(DISCONNECT::RUN);
      CommandRegistrationCallback.EVENT.register((dispatcher, registryAccess, environment) -> {
         Translate.register(dispatcher);
         Stars.register(dispatcher);
         Glove.register(dispatcher);
         Auction.register(dispatcher);
      });
      ServerLifecycleEvents.SERVER_STARTED.register((s) -> {
         this.server = s;
         this.luckPerms = LuckPermsProvider.get();
         WebSocket.Start();
         SafariAuction.Start();
      });
      ServerLifecycleEvents.SERVER_STOPPING.register((s) -> {
         WebSocket.instance.stopping = true;
         WebSocket.instance.close();
      });
      CobblemonEvents.BATTLE_VICTORY.subscribe(Priority.LOWEST, (b) -> {
         List<BattleActor> winners = b.getWinners();
         List<BattleActor> losers = b.getLosers();
         if (winners.size() != 1) {
            return null;
         } else if (losers.size() != 1) {
            return null;
         } else {
            BattleActor winnerActor = (BattleActor)winners.get(0);
            BattleActor loserActor = (BattleActor)losers.get(0);
            if (winnerActor != null && winnerActor.getType() != ActorType.PLAYER) {
               if (loserActor != null && loserActor.getType() != ActorType.PLAYER) {
                  ServerPlayerEntity winner = this.server.getPlayerManager().getPlayer(UUID.fromString(winnerActor.getPlayerUUIDs().toString().substring(1, winnerActor.getPlayerUUIDs().toString().length() - 1)));
                  ServerPlayerEntity loser = this.server.getPlayerManager().getPlayer(UUID.fromString(loserActor.getPlayerUUIDs().toString().substring(1, loserActor.getPlayerUUIDs().toString().length() - 1)));
                  if (winner == null) {
                     return null;
                  } else if (loser == null) {
                     return null;
                  } else if (!this.duelLists.contains(winner.getUuid()) && !this.duelLists.contains(loser.getUuid())) {
                     return null;
                  } else {
                     this.duelLists.remove(winner.getUuid());
                     this.duelLists.remove(loser.getUuid());
                     StarInventory.transferStar(loser, winner);
                     PlayerData winnerData = (PlayerData)instance.playersData.get(winner.getUuid());
                     PlayerData loserData = (PlayerData)instance.playersData.get(loser.getUuid());
                     winnerData.duelCooldown = System.currentTimeMillis() + (long)ModConfigs.DUELING.getCooldownTicks() * 10L;
                     loserData.duelCooldown = System.currentTimeMillis() + (long)ModConfigs.DUELING.getCooldownTicks() * 15L;
                     instance.SavePlayerData();
                     return null;
                  }
               } else {
                  return null;
               }
            } else {
               return null;
            }
         }
      });
      ServerTickEvents.START_SERVER_TICK.register(this::onServerTick);

      try {
         Translator.init();
      } catch (Exception var2) {
      }

   }

   private void onServerTick(MinecraftServer server) {
      SafariAuction.tick();
   }

   private void LoadConfig() {
      try {
         if (this.configFile.createNewFile()) {
            LOGGER.info((String)"File created: {}", (Object)this.configFile.getName());

            try {
               FileWriter myWriter = new FileWriter(this.configFile);
               String json = this.mapper.writeValueAsString(this.config);
               myWriter.write(json);
               myWriter.close();
            } catch (IOException var3) {
               LOGGER.error(var3.getMessage());
            }
         }

         try {
            Scanner myReader = new Scanner(this.configFile);
            StringBuilder data = new StringBuilder();

            while(myReader.hasNextLine()) {
               data.append(myReader.nextLine());
            }

            myReader.close();
            this.config = (Config)this.mapper.readValue(data.toString(), Config.class);
         } catch (FileNotFoundException var4) {
            LOGGER.error((String)"An error occurred.", (Throwable)var4);
         }
      } catch (IOException var5) {
         LOGGER.error((String)"An error occurred.", (Throwable)var5);
      }

   }

   private void LoadPlayerData() {
      try {
         if (this.saveFile.createNewFile()) {
            LOGGER.info((String)"File created: {}", (Object)this.saveFile.getName());

            try {
               FileWriter myWriter = new FileWriter(this.saveFile);
               String json = this.mapper.writeValueAsString(this.playersData);
               myWriter.write(json);
               myWriter.close();
            } catch (IOException var4) {
               LOGGER.error(var4.getMessage());
            }
         }

         try {
            Scanner myReader = new Scanner(this.saveFile);
            StringBuilder data = new StringBuilder();

            while(myReader.hasNextLine()) {
               data.append(myReader.nextLine());
            }

            myReader.close();
            this.playersData = this.mapper.readValue(data.toString(), new TypeReference<HashMap<UUID, PlayerData>>(){});
         } catch (FileNotFoundException var5) {
            LOGGER.error((String)"An error occurred.", (Throwable)var5);
         }
      } catch (IOException var6) {
         LOGGER.error((String)"An error occurred.", (Throwable)var6);
      }

   }

   public void SavePlayerData() {
      try {
         String json = this.mapper.writeValueAsString(this.playersData);

         try {
            FileWriter myWriter = new FileWriter(this.saveFile);
            myWriter.write(json);
            myWriter.close();
         } catch (IOException var3) {
            LOGGER.error(var3.getMessage());
         }
      } catch (JsonProcessingException var4) {
         LOGGER.error(var4.getMessage());
      }

   }

   public void SaveConfig() {
      try {
         String json = this.mapper.writeValueAsString(this.config);

         try {
            FileWriter myWriter = new FileWriter(this.configFile);
            myWriter.write(json);
            myWriter.close();
         } catch (IOException var3) {
            LOGGER.error(var3.getMessage());
         }
      } catch (JsonProcessingException var4) {
         LOGGER.error(var4.getMessage());
      }

   }

   public void sendGlobalMessage(String message) {
      this.server.getPlayerManager().getPlayerList().forEach((player) -> {
         player.sendMessage(Text.of(message));
      });
   }

   public void sendGlobalTranslatedMessage(String name, String message) {
      Map<String, String> translatedLangs = new HashMap();
      Iterator var4 = this.server.getPlayerManager().getPlayerList().iterator();

      ServerPlayerEntity player;
      PlayerData pdata;
      String plang;
      while(var4.hasNext()) {
         player = (ServerPlayerEntity)var4.next();
         pdata = (PlayerData)this.playersData.computeIfAbsent(player.getUuid(), (k) -> {
            return new PlayerData();
         });
         plang = pdata.lang;
         if (!translatedLangs.containsKey(plang)) {
            try {
               translatedLangs.put(plang, Translator.performTranslate(message, GoogleTranslateLanguage.AUTO_DETECT, (GoogleTranslateLanguage)Translator.knownLanguages.get(plang)));
            } catch (Exception var9) {
            }
         }
      }

      var4 = this.server.getPlayerManager().getPlayerList().iterator();

      while(var4.hasNext()) {
         player = (ServerPlayerEntity)var4.next();
         pdata = (PlayerData)instance.playersData.computeIfAbsent(player.getUuid(), (k) -> {
            return new PlayerData();
         });
         plang = pdata.lang;
         String formattedMessage = "[D]" + name + ": " + (String)translatedLangs.get(plang);
         player.sendMessage(Text.literal(formattedMessage).setStyle(Style.EMPTY.withHoverEvent(new HoverEvent(Action.SHOW_TEXT, Text.literal("From Discord\nOriginal: " + message)))));
      }

   }
}
