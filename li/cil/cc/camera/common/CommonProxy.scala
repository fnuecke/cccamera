package li.cil.cc.camera.common

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.network.NetworkMod
import cpw.mods.fml.common.registry.LanguageRegistry
import dan200.turtle.api.TurtleAPI
import li.cil.cc.camera.Camera
import li.cil.cc.camera.common.block.BlockCamera
import li.cil.cc.camera.common.turtle.TurtleCamera
import net.minecraft.block.Block
import net.minecraft.item.Item
import net.minecraft.item.ItemStack
import net.minecraft.item.crafting.CraftingManager

class CommonProxy {
  def init() = {
    Camera.Blocks.camera = new BlockCamera
    LanguageRegistry.addName(Camera.Blocks.camera, "Camera")
    if (Camera.Config.turtleUpgradeID > 0) {
      TurtleAPI.registerUpgrade(TurtleCamera)
    }
  }

  def postInit() = {
    CraftingManager.getInstance().addRecipe(
      new ItemStack(Camera.Blocks.camera, 1),
      "SSS", "REG", "SSS",
      'S': Character, Block.stone,
      'R': Character, Item.redstone,
      'E': Character, Item.spiderEye,
      'G': Character, Block.glass)
  }
}