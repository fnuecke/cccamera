package li.cil.cc.camera.client.registry

import cpw.mods.fml.client.registry.ISimpleBlockRenderingHandler
import net.minecraft.client.renderer.RenderBlocks
import net.minecraft.client.renderer.Tessellator
import org.lwjgl.opengl.GL11
import net.minecraft.block.Block

abstract class DefaultInventoryBlockRenderingHandler extends ISimpleBlockRenderingHandler {
  def renderInventoryBlock(block: Block, metadata: Int, modelID: Int, renderer: RenderBlocks) = {
    val tessellator = Tessellator.instance;
    block.setBlockBoundsForItemRender()
    renderer.setRenderBoundsFromBlock(block)
    GL11.glRotatef(90.0F, 0.0F, 1.0F, 0.0F)
    GL11.glTranslatef(-0.5F, -0.5F, -0.5F)
    tessellator.startDrawingQuads()
    tessellator.setNormal(0.0F, -1.0F, 0.0F)
    renderer.renderFaceYNeg(block, 0.0D, 0.0D, 0.0D, renderer.getBlockIconFromSideAndMetadata(block, 0, metadata))
    tessellator.draw()
    tessellator.startDrawingQuads()
    tessellator.setNormal(0.0F, 1.0F, 0.0F)
    renderer.renderFaceYPos(block, 0.0D, 0.0D, 0.0D, renderer.getBlockIconFromSideAndMetadata(block, 1, metadata))
    tessellator.draw()
    tessellator.startDrawingQuads()
    tessellator.setNormal(0.0F, 0.0F, -1.0F)
    renderer.renderFaceZNeg(block, 0.0D, 0.0D, 0.0D, renderer.getBlockIconFromSideAndMetadata(block, 2, metadata))
    tessellator.draw()
    tessellator.startDrawingQuads()
    tessellator.setNormal(0.0F, 0.0F, 1.0F)
    renderer.renderFaceZPos(block, 0.0D, 0.0D, 0.0D, renderer.getBlockIconFromSideAndMetadata(block, 3, metadata))
    tessellator.draw()
    tessellator.startDrawingQuads()
    tessellator.setNormal(-1.0F, 0.0F, 0.0F)
    renderer.renderFaceXNeg(block, 0.0D, 0.0D, 0.0D, renderer.getBlockIconFromSideAndMetadata(block, 4, metadata))
    tessellator.draw()
    tessellator.startDrawingQuads()
    tessellator.setNormal(1.0F, 0.0F, 0.0F)
    renderer.renderFaceXPos(block, 0.0D, 0.0D, 0.0D, renderer.getBlockIconFromSideAndMetadata(block, 5, metadata))
    tessellator.draw()
    GL11.glTranslatef(0.5F, 0.5F, 0.5F)
  }
  def shouldRender3DInInventory = true
}