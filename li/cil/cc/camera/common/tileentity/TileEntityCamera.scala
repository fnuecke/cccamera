package li.cil.cc.camera.common.tileentity

import dan200.computer.api.IComputerAccess
import dan200.computer.api.IPeripheral
import li.cil.cc.camera.common.peripheral.IPeripheralContext
import li.cil.cc.camera.common.peripheral.PeripheralCamera
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.tileentity.TileEntity
import net.minecraft.world.World
import net.minecraftforge.common.ForgeDirection

/**
 * This is the tile entity that can be attached to our peripheral blocks.
 *
 * We use this to keep track of the actual peripheral instance. The actual
 * peripheral logic is kept in an extra class that we can use for both blocks
 * and turtle upgrades (as seen in OpenCCSensors).
 */
class TileEntityCamera extends TileEntity with IPeripheral with IPeripheralContext {
  // ----------------------------------------------------------------------- //
  // General
  // ----------------------------------------------------------------------- //

  private val peripheral = new PeripheralCamera(this)

  override def readFromNBT(nbt: NBTTagCompound) = {
    super.readFromNBT(nbt)
    peripheral.readFromNBT(nbt)
  }

  override def writeToNBT(nbt: NBTTagCompound) = {
    super.writeToNBT(nbt)
    peripheral.writeToNBT(nbt)
  }

  // ----------------------------------------------------------------------- //
  // IPeripheral
  // ----------------------------------------------------------------------- //

  /** Allow all sides except the front (that's where we're looking after all). */
  def canAttachToSide(side: Int) = side != getBlockMetadata

  def getType = peripheral.getType

  def getMethodNames = peripheral.getMethodNames

  def callMethod(computer: IComputerAccess, method: Int, arguments: Array[Object]) =
    peripheral.callMethod(computer, method, arguments)

  def attach(computer: IComputerAccess) = peripheral.attach(computer)

  def detach(computer: IComputerAccess) = peripheral.detach(computer)

  // ----------------------------------------------------------------------- //
  // IPeripheralContext
  // ----------------------------------------------------------------------- //

  def world: World = worldObj

  def x: Int = xCoord
  def y: Int = yCoord
  def z: Int = zCoord

  def facing: ForgeDirection = ForgeDirection.getOrientation(getBlockMetadata)

  /** No can do! We're a block, so we don't have fuel. Used to suppress flash for blocks. */
  def consumeFuel() = false
}