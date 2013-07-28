package li.cil.cc.camera.network

import java.io.ByteArrayInputStream
import java.io.DataInputStream

import cpw.mods.fml.common.network.IPacketHandler
import cpw.mods.fml.common.network.Player
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.network.INetworkManager
import net.minecraft.network.packet.Packet250CustomPayload

class PacketHandler extends IPacketHandler {
  def onPacketData(manager: INetworkManager, packet: Packet250CustomPayload, player: Player) {
    try {
      val dis = new DataInputStream(new ByteArrayInputStream(packet.data))
      val x = dis.readDouble
      val y = dis.readDouble
      val z = dis.readDouble

      player.asInstanceOf[EntityPlayer].worldObj.spawnParticle("flame", x, y, z, 0, 0, 0)
      player.asInstanceOf[EntityPlayer].worldObj.spawnParticle("smoke", x, y, z, 0, 0, 0)
    } catch {
      // Make sure we don't break the game because of bad packets.
      case _: Throwable => {}
    }
  }
}