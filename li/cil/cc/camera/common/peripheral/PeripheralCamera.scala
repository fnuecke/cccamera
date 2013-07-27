package li.cil.cc.camera.common.peripheral

import java.nio.ByteBuffer
import java.security.MessageDigest

import scala.Array.canBuildFrom
import scala.util.Random

import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.network.NetworkMod
import dan200.computer.api.IComputerAccess
import dan200.computer.api.IHostedPeripheral
import li.cil.cc.camera.Camera
import net.minecraft.nbt.NBTTagCompound
import net.minecraft.world.World
import net.minecraftforge.common.ForgeDirection

/**
 * Interface used to provide contextual information to the peripheral.
 *
 * This is used so that we can use the actual peripheral class for both the
 * peripheral block as well as the turtle upgrade (as seen in OpenCCSensors).
 */
trait IPeripheralContext {
  /** The world we're working with. */
  def world: World

  /** The current location of the camera. */
  def x: Int
  def y: Int
  def z: Int

  /** The direction the camera is looking. */
  def facing: ForgeDirection

  /** Called when turtles use the flash. */
  def consumeFuel(): Unit
}

class PeripheralCamera(val context: IPeripheralContext, val isTurtle: Boolean) extends IHostedPeripheral {
  // ----------------------------------------------------------------------- //
  // General
  // ----------------------------------------------------------------------- //

  /** The last time a snapshot was taken. Used for cooldown. */
  private var lastTrigger: Long = 0

  // ----------------------------------------------------------------------- //
  // IHostedPeripheral
  // ----------------------------------------------------------------------- //

  def update() = {}

  def readFromNBT(nbt: NBTTagCompound) = {
    lastTrigger = nbt.getLong("lastTrigger")
  }

  def writeToNBT(nbt: NBTTagCompound) = {
    nbt.setLong("lastTrigger", lastTrigger)
  }

  // ----------------------------------------------------------------------- //
  // IPeripheral
  // ----------------------------------------------------------------------- //

  def getType = "camera"

  def getMethodNames = Array("trigger")

  def callMethod(computer: IComputerAccess, method: Int, arguments: Array[Object]) = method match {
    case 0 => {
      if (arguments == null || arguments.length < 1)
        trigger(computer, false)
      else
        trigger(computer, arguments(0).asInstanceOf[Boolean])
    }
  }

  /** Allow all sides in our unbound form. */
  def canAttachToSide(side: Int) = true

  def attach(computer: IComputerAccess) = {}

  def detach(computer: IComputerAccess) = {}

  // ----------------------------------------------------------------------- //
  // Logic
  // ----------------------------------------------------------------------- //

  private def trigger(computer: IComputerAccess, flash: Boolean): Array[Object] = {
    // Return nothing (nil) when we're on the client side. Which should never happen.
    if (context.world.isRemote) return Array[Object]()

    // Get the block ID and metadata of the block we're looking at.
    val dir = context.facing
    val (x, y, z) = (context.x + dir.offsetX, context.y + dir.offsetY, context.z + dir.offsetZ)
    val id = context.world.getBlockId(x, y, z)
    val meta = context.world.getBlockMetadata(x, y, z)

    // Hash the block ID and metadata it into a byte array (length of 64) using
    // the world seed and configurable hash as salt.
    val hash = (Camera.Config.hasSHA512 match {
      case true => {
        // Try to use SHA512, using the world seed and configurable value as salt.
        val digest = MessageDigest.getInstance("SHA-512")
        digest.reset()
        digest.update(ByteBuffer.allocate(8).putLong(context.world.getSeed).array)
        digest.update(Camera.Config.hashSalt.getBytes("UTF-8"))
        digest.update(ByteBuffer.allocate(4).putInt(meta).array)
        digest.digest(ByteBuffer.allocate(4).putInt(id).array)
      }
      case false => {
        // We don't have SHA-512 available, fall back to random based weak version.
        // Use the configurable salt and hash it up with the world seed and block ID.
        def hash(seed: Long, value: Int) = seed * 31 + value
        val worldSalt = hash(hash(context.world.getSeed, id), meta)
        val userSalt =
          if (Camera.Config.hashSalt != null && Camera.Config.hashSalt.length > 0)
            Camera.Config.hashSalt
          else
            "this is what happens when you don't provide some salt"
        val seed = (worldSalt /: userSalt)((seed, char) => hash(seed, char))
        // Then based on that (weak!) hash generate the 64 chars via a seeded RNG.
        var result = Array.ofDim[Byte](64)
        new Random(seed).nextBytes(result)
        result
      }
    })

    // Get the absolute light level at the camera's position. Apply flash if
    // specified. Let's hope this really ever only returns values in [0, 15].
    val maxLightLevel = 15
    val lightLevel =
      if (isTurtle && flash) {
        context.consumeFuel()
        maxLightLevel
      } else context.world.getBlockLightValue(context.x, context.y, context.z)

    // Adjust the light level to a range of [0, 1], to apply it to the original data.
    val lightScale = lightLevel / 15.0

    // Map the hash to the interval [-1,1] and scale it based on the light level.
    val signature = hash map {
      case x if x < 0 => x / 128.0
      case x => x / 127.0
    } map (_ * lightScale)

    // Compute noise introduced from our minimum light level. Note that this value
    // does in fact not change; we adjust the "power" of the true signal instead.
    // I think it's more realistic that way (noise being constant, real image data
    // goodness depending on light power that is).
    val generalNoise = Camera.Config.minLightLevel / 15.0

    // Remaining relative cooldown, i.e. how far we're away from getting rid of
    // noise introduced from overuse, in percent. This is used to add some
    // additional noise to avoid overuse.
    val cooldownInTicks = 20.0 * Camera.Config.cooldown
    val timePassed = context.world.getTotalWorldTime() - lastTrigger
    val relativeCooldownRemaining = Math.max(1 - timePassed / cooldownInTicks, 0)
    val noiseFromCooldown = 5.0 // Fixed, large value.
    val cooldownInducedNoise = relativeCooldownRemaining * noiseFromCooldown

    // Set our last trigger time to enforce cooldown based noise for future calls.
    lastTrigger = context.world.getTotalWorldTime()

    // Compute the complete noise scaling to be used on the random numbers. We
    // enforce some minimum noise to avoid making things too easy ;)
    val minimalNoise = 0.1
    val noise = Math.max(generalNoise + cooldownInducedNoise, minimalNoise)

    // Box the values to get an object array and return it. Also apply noise if any.
    signature map (_ + context.world.rand.nextGaussian() * noise) map double2Double toArray
  }
}