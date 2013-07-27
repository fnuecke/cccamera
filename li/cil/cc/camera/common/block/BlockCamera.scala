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
  // TODO add to ComputerCraft tab?
  setCreativeTab(CreativeTabs.tabRedstone)

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

  override def isOpaqueCube = false

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

  override def onBlockPlacedBy(world: World, x: Int, y: Int, z: Int, entity: EntityLiving, itemStack: ItemStack) = {
    if (!world.isRemote) {
      val dir = MathHelper.floor_double(entity.rotationYaw * 4 / 360 + 0.5) & 3
      world.setBlockMetadataWithNotify(x, y, z, Array(2, 5, 3, 4)(dir), 3)
    }
  }

  override def getValidRotations(world: World, x: Int, y: Int, z: Int) =
    Array(ForgeDirection.SOUTH, ForgeDirection.WEST, ForgeDirection.NORTH, ForgeDirection.EAST)

  // ----------------------------------------------------------------------- //
  // Debugging
  // ----------------------------------------------------------------------- //

  override def onBlockActivated(world: World, x: Int, y: Int, z: Int,
    player: net.minecraft.entity.player.EntityPlayer, side: Int,
    hitX: Float, hitY: Float, hitZ: Float) = {
    if (player.isSneaking()) {
      false
    } else {
      if (!world.isRemote) {
        val reps = 10
        val tileEntity = world.getBlockTileEntity(x, y, z).asInstanceOf[TileEntityCamera]
        val signatures = 1 to reps map (_ => tileEntity.callMethod(null, 0, null)(5).asInstanceOf[Double])
        
        val avg = (0.0 /: signatures)(_ + _) / reps
        println("avg: ", avg)
        
        val delta = signatures map (_ - avg)
        val absdelta = delta map Math.abs
        val avgdelta = (0.0 /: absdelta)(_ + _) / reps
        
        println("min/max/avg delta: ", absdelta min, absdelta max, avgdelta)
        

        //val avg = signatures.map(_.getBytes("UTF-8")).foldLeft(Array.fill(64)(0))((a, b) => a zip b map (x => x._1 + x._2)) map (_ / 100) map (_.toChar)
//        val avg = (Seq.fill(64)(0.0) /: signatures)((a, b) => a zip b map (x => x._1 + x._2)) map (_ / reps)
//
//        val deltamaxs = (Seq.fill(64)(0.0) /: signatures)((a, b) => avg zip b map (x => Math.abs(x._1 - x._2)) zip a map (x => Math.max(x._1, x._2)))
//        val deltaavgs = (Seq.fill(64)(0.0) /: signatures)((a, b) => avg zip b map (x => Math.abs(x._1 - x._2)) zip a map (x => x._1 + x._2)) map (_ / reps)
//        println(deltaavgs.length)
//        val deltaavg = (0.0 /: deltaavgs)(_ + _) / 64
//        println("avg/min/max delta: ", deltaavg, deltamaxs min, deltamaxs max)
      }
      true
    }
  }
}