package li.cil.cc.camera.common.block

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.network.NetworkMod
import cpw.mods.fml.common.registry.GameRegistry
import li.cil.cc.camera.Camera
import li.cil.cc.camera.common.tileentity.TileEntityCamera
import net.minecraft.block.Block
import net.minecraft.block.material.Material
import net.minecraft.client.renderer.texture.IconRegister
import net.minecraft.creativetab.CreativeTabs
import net.minecraft.entity.EntityLiving
import net.minecraft.entity.player.EntityPlayer
import net.minecraft.item.ItemStack
import net.minecraft.util.Icon
import net.minecraft.util.MathHelper
import net.minecraft.world.IBlockAccess
import net.minecraft.world.World
import net.minecraftforge.common.ForgeDirection

/** Static list of icons, so that we can access it from the turtle upgrade. */
object Icons {
  var front: Icon = null
  var side: Icon = null
  var top: Icon = null
  var turtle: Icon = null
}

/**
 * This is the block representation of the camera peripheral.
 *
 * It can be placed into the world and computers or turtles can connect to it
 * to scan whatever's in front of the camera. They cannot connect from the
 * front, however, because that's where the camera is looking!
 */
class BlockCamera extends Block(Camera.Config.cameraBlockID, Material.rock) {
  // ----------------------------------------------------------------------- //
  // Construction
  // ----------------------------------------------------------------------- //

  setHardness(2f)
  GameRegistry.registerBlock(this, "camera")
  GameRegistry.registerTileEntity(classOf[TileEntityCamera], "camera")
  setUnlocalizedName("cccp.camera")
  setCreativeTab(creativeTab)
  setLightOpacity(3)

  /*
   * Assuming we use 16x16 textures, shrink the box down by two pixels in
   * each direction, just to make it stick out a bit, and make it appear
   * that there's some room between the camera and the block it is scanning.
   */
  private val shrinkage = 1 / 16f
  setBlockBounds(shrinkage, 0f, shrinkage,
    1 - shrinkage, 1 - 2 * shrinkage, 1 - shrinkage)

  // ----------------------------------------------------------------------- //
  // Rendering stuff
  // ----------------------------------------------------------------------- //

  override def getRenderType = Camera.Config.cameraBlockRenderID

  override def isOpaqueCube = false

  override def renderAsNormalBlock = false

  override def getBlockTexture(block: IBlockAccess, x: Int, y: Int, z: Int, side: Int) = side match {
    case 0 | 1 => Icons.top
    case side if (side == block.getBlockMetadata(x, y, z)) => Icons.front
    case _ => Icons.side
  }

  override def getIcon(side: Int, metadata: Int) = side match {
    case 0 | 1 => Icons.top
    case 4 => Icons.front
    case _ => Icons.side
  }

  override def registerIcons(register: IconRegister) = {
    Icons.front = register.registerIcon("CCCP:camera")
    Icons.side = register.registerIcon("CCCP:cameraSide")
    Icons.top = register.registerIcon("CCCP:cameraTop")
    Icons.turtle = register.registerIcon("CCCP:cameraTurtle")
  }

  // ----------------------------------------------------------------------- //
  // Tile entity
  // ----------------------------------------------------------------------- //

  override def hasTileEntity(metadata: Int) = true

  override def createTileEntity(world: World, metadata: Int) = new TileEntityCamera

  // ----------------------------------------------------------------------- //
  // Block rotation
  // ----------------------------------------------------------------------- //

  override def onBlockPlacedBy(world: World, x: Int, y: Int, z: Int, entity: EntityLiving, itemStack: ItemStack) {
    if (!world.isRemote) {
      val facing = MathHelper.floor_double(entity.rotationYaw * 4 / 360 + 0.5) & 3
      setRotation(world, x, y, z, facing)
    }
  }

  override def getValidRotations(world: World, x: Int, y: Int, z: Int) =
    Array(ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.NORTH, ForgeDirection.EAST)

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int, player: EntityPlayer,
    side: Int, hitX: Float, hitY: Float, hitZ: Float) = {
    if (canWrench(player, x, y, z)) {
      val delta = if (player.isSneaking()) 1 else -1
      setRotation(world, x, y, z, rotation(world, x, y, z) + delta)
    } else
      false
  }

  def rotation(world: IBlockAccess, x: Int, y: Int, z: Int) =
    // Renderer(down, up, north, south, west, east) -> Facing(south, west, north, east) inverted.
    Array(0, 0, 0, 2, 3, 1)(world.getBlockMetadata(x, y, z))

  private def setRotation(world: World, x: Int, y: Int, z: Int, value: Int) =
    // Facing(south, west, north, east) -> Renderer(down, up, north, south, west, east) inverted.
    world.setBlockMetadataWithNotify(x, y, z, Array(2, 5, 3, 4)((value + 4) % 4), 3)

  private def canWrench(player: EntityPlayer, x: Int, y: Int, z: Int) = {
    if (player.getCurrentEquippedItem() != null)
      try {
        player.getCurrentEquippedItem().getItem().asInstanceOf[{
          def canWrench(player: EntityPlayer, x: Int, y: Int, z: Int): Boolean
        }].canWrench(player, x, y, z)
      } catch {
        case e: Throwable => false
      }
    else
      false
  }

  private def creativeTab = try {
    val cc = Class.forName("dan200.ComputerCraft")
    cc.getDeclaredField("creativeTab").get(cc).asInstanceOf[CreativeTabs]
  } catch {
    case _: Throwable => CreativeTabs.tabRedstone
  }
}