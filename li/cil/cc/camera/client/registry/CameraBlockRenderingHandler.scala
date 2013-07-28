package li.cil.cc.camera.client.registry

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.network.NetworkMod
import li.cil.cc.camera.Camera
import li.cil.cc.camera.common.block.BlockCamera
import net.minecraft.block.Block
import net.minecraft.client.renderer.RenderBlocks
import net.minecraft.world.IBlockAccess

/** Custom renderer so we can rotate the top texture. */
class CameraBlockRenderingHandler extends DefaultInventoryBlockRenderingHandler {
  override def renderInventoryBlock(block: Block, metadata: Int, modelID: Int, renderer: RenderBlocks) {
    val uvRotateTop = renderer.uvRotateTop
    renderer.uvRotateTop = 2
    val result = super.renderInventoryBlock(block, metadata, modelID, renderer)
    renderer.uvRotateTop = uvRotateTop
    result
  }
  def renderWorldBlock(world: IBlockAccess, x: Int, y: Int, z: Int, block: Block, modelId: Int, renderer: RenderBlocks) = {
    val (uvRotateTop, uvRotateBottom) = (renderer.uvRotateTop, renderer.uvRotateBottom)
    renderer.uvRotateTop = Array(0, 1, 3, 2)(block.asInstanceOf[BlockCamera].rotation(world, x, y, z))
    renderer.uvRotateBottom = Array(0, 2, 1, 3)(renderer.uvRotateTop)
    val result = renderer.renderStandardBlock(block, x, y, z)
    renderer.uvRotateTop = uvRotateTop
    renderer.uvRotateBottom = uvRotateBottom
    result
  }
  def getRenderId = Camera.Config.cameraBlockRenderID
}