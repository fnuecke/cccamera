package li.cil.cc.camera.common.turtle

import dan200.turtle.api.ITurtleAccess
import dan200.turtle.api.ITurtleUpgrade
import dan200.turtle.api.TurtleSide
import dan200.turtle.api.TurtleUpgradeType
import dan200.turtle.api.TurtleVerb
import li.cil.cc.camera.Camera
import li.cil.cc.camera.common.block.Icons
import li.cil.cc.camera.common.peripheral.IPeripheralContext
import li.cil.cc.camera.common.peripheral.PeripheralCamera
import net.minecraft.item.ItemStack
import net.minecraft.world.World
import net.minecraftforge.common.ForgeDirection

object TurtleCamera extends ITurtleUpgrade {
  // ----------------------------------------------------------------------- //
  // ITurtleUpgrade
  // ----------------------------------------------------------------------- //

  def getUpgradeID = Camera.Config.turtleUpgradeID

  def getAdjective = "Camera"

  def getType = TurtleUpgradeType.Peripheral

  def getCraftingItem = new ItemStack(Camera.Blocks.camera, 1)

  def isSecret = false

  def createPeripheral(turtle: ITurtleAccess, side: TurtleSide) =
    new PeripheralCamera(new TurtleContext(turtle, side), true)

  def useTool(turtle: ITurtleAccess, side: TurtleSide, verb: TurtleVerb, direction: Int) = false

  def getIcon(turtle: ITurtleAccess, side: TurtleSide) = Icons.turtle

  // ----------------------------------------------------------------------- //
  // IPeripheralContext
  // ----------------------------------------------------------------------- //

  private class TurtleContext(val turtle: ITurtleAccess, val side: TurtleSide) extends IPeripheralContext {
    def world: World = turtle.getWorld

    def x: Int = turtle.getPosition.xCoord.toInt
    def y: Int = turtle.getPosition.yCoord.toInt
    def z: Int = turtle.getPosition.zCoord.toInt

    /** Make it so the camera looks to the side it's attached to. */
    def facing: ForgeDirection = side match {
      case TurtleSide.Left => ForgeDirection.getOrientation(turtle.getFacingDir).
        getRotation(ForgeDirection.DOWN)
      case TurtleSide.Right => ForgeDirection.getOrientation(turtle.getFacingDir).
        getRotation(ForgeDirection.UP)
    }

    def consumeFuel() = turtle.consumeFuel(1)
  }
}