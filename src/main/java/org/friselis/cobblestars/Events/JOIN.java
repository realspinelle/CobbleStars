package org.friselis.cobblestars.Events;

import java.util.Objects;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.friselis.cobblestars.CobbleStars;
import org.friselis.cobblestars.PlayerData;
import org.friselis.cobblestars.WebSocket;

public class JOIN {
   public static void RUN(ServerPlayNetworkHandler packet, PacketSender packetSender, MinecraftServer server) {
      PlayerData data = (PlayerData)CobbleStars.instance.playersData.get(packet.player.getUuid());
      if (data == null) {
         data = new PlayerData();
         data.username = packet.player.getDisplayName().getString();
         CobbleStars.instance.playersData.put(packet.player.getUuid(), data);
      }

      if (!Objects.equals(data.username, packet.player.getDisplayName().getString())) {
         WebSocket.sendNameChange(data.username, packet.player.getDisplayName().getString());
         data.username = packet.player.getDisplayName().getString();
         CobbleStars.instance.SavePlayerData();
      }

      WebSocket.sendJoiningMessage(packet.player.getDisplayName().getString());
   }
}
