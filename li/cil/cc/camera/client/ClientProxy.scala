package li.cil.cc.camera.client

import cpw.mods.fml.client.registry.RenderingRegistry
import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.network.NetworkMod
import li.cil.cc.camera.Camera
import li.cil.cc.camera.client.registry.CameraBlockRenderingHandler
import li.cil.cc.camera.common.CommonProxy

class ClientProxy extends CommonProxy {
  override def init() = {
    super.init()
    Camera.Config.cameraBlockRenderID = RenderingRegistry.getNextAvailableRenderId()
    RenderingRegistry.registerBlockHandler(new CameraBlockRenderingHandler())
  }
}