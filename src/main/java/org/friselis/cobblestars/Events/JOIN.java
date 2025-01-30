package org.friselis.cobblestars.Events;

import java.util.Objects;
import net.fabricmc.fabric.api.networking.v1.PacketSender;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.network.ServerPlayNetworkHandler;
import org.friselis.cobblestars.Datas.PlayerData;
import org.friselis.cobblestars.WebSocket;

public class JOIN {
   public static void RUN(ServerPlayNetworkHandler packet, PacketSender packetSender, MinecraftServer server) {
      PlayerData data = PlayerData.get(packet.player);

      if (!Objects.equals(data.username, packet.player.getDisplayName().getString())) {
         WebSocket.sendNameChange(data.username, packet.player.getDisplayName().getString());
         data.username = packet.player.getDisplayName().getString();
         PlayerData.Save();
      }

      WebSocket.sendJoiningMessage(packet.player.getDisplayName().getString());
   }
}
