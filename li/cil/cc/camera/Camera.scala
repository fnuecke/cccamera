package li.cil.cc.camera

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import cpw.mods.fml.common.FMLLog
import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.SidedProxy
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPostInitializationEvent
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import cpw.mods.fml.common.network.NetworkMod
import li.cil.cc.camera.common.CommonProxy
import li.cil.cc.camera.common.block.BlockCamera
import net.minecraftforge.common.Configuration
import cpw.mods.fml.common.Mod.PostInit
import cpw.mods.fml.common.Mod.PreInit
import cpw.mods.fml.common.Mod.Init
import java.util.logging.Logger

@Mod(modid = "CCCP",
  name = "ComputerCraft Camera Peripheral",
  version = "1.5.2.0",
  //dependencies = "required-after:ComputerCraft",
  modLanguage = "scala")
@NetworkMod(clientSideRequired = true, serverSideRequired = false)
object Camera {
  object Blocks {
    var camera: BlockCamera = null
  }

  object Config {
    var cameraBlockID = 3600
    var turtleUpgradeID = 253
    var hashSalt = "if you run a public server you may want to change this"
    var minLightLevel = 3
    var cooldown = 1.0
    var noiseFromCooldown = 2.0
    var noiseBaseStrength = 0.5
    var hasSHA512 = true
  }

  val logger = Logger.getLogger("CCCP")
  logger.setParent(FMLLog.getLogger)

  /** No distinction for now. May come later if I decide to play with advanced rendering. */
  @SidedProxy(clientSide = "li.cil.cc.camera.common.CommonProxy", serverSide = "li.cil.cc.camera.common.CommonProxy")
  var proxy: CommonProxy = null

  @PreInit
  def preInit(e: FMLPreInitializationEvent) {
    var config = new Configuration(e.getSuggestedConfigurationFile)

    Config.cameraBlockID = config.getBlock("cameraId", Config.cameraBlockID,
      "The block ID for the camera block.").
      getInt(Config.cameraBlockID)

    // Note: the pattern of having an empty string in the first line is to
    // make the auto formatter behave and evenly indent the following lines.
    Config.turtleUpgradeID = config.get("options", "turtleUpgradeID", Config.turtleUpgradeID,
      "The ID for the turtle upgrade. Last I checked 253 was still available.").
      getInt(Config.turtleUpgradeID)

    Config.hashSalt = config.get("options", "hashSalt", Config.hashSalt, "" +
      "An additional salt to use for hashing block IDs.\n" +
      "This is applied in addition to the world seed and can be used on servers\n" +
      "to avoid clients generating hashes for block IDs by themselves, thereby\n" +
      "circumventing the calibration of their computers.").
      getString()

    Config.minLightLevel = config.get("options", "minLightLevel", Config.minLightLevel, "" +
      "This is the minimal light level required for a signature returned from\n" +
      "a call to trigger() to actually carry any data. For light levels below\n" +
      "this value the returned value will only be noise. Set this to zero to\n" +
      "disable light levels influencing the quality of the returned signature.\n" +
      "Turtles using the flash will have no effect and will not cost any fuel\n" +
      "in that case.\n" +
      "For any light level above and including the minimum the actual\n" +
      "signature value will be dampened linearly the lower the light level,\n" +
      "starting with identity scale of one at the maximum level down to zero\n" +
      "at the light level one below the minimum light level.").
      getInt(Config.minLightLevel) max 0 min 15

    Config.cooldown = config.get("options", "cooldown", Config.cooldown, "" +
      "The cooldown of a camera, i.e. how many seconds have to pass before taking\n" +
      "a snapshot will not be penalized with additional noise.").
      getDouble(Config.cooldown) max 0

    Config.noiseFromCooldown = config.get("options", "noiseFromCooldown", Config.noiseFromCooldown, "" +
      "The amount of noise cooldown introduces, which is added directly to\n" +
      "the noise based on the light level. This will linearly decay over the\n" +
      "cooldown period.").
      getDouble(Config.noiseFromCooldown) max 0

    Config.noiseBaseStrength = config.get("options", "noiseBaseStrength", Config.noiseBaseStrength, "" +
      "Some base level of noise to add when taking snapshots, so that even in\n" +
      "sunlight we won't get the same result all the time.\n" +
      "Noise is applied by generating random numbers from a standard normal\n" +
      "distribution, scaling them with the noise value and then adding that\n" +
      "to the existing hash values, which are in the interval [-1,1].").
      getDouble(Config.noiseBaseStrength) max 0

    config.save()

    // See if SHA512 is available, which is what we preferably use to hash block IDs.
    try {
      MessageDigest.getInstance("SHA-512")
      logger.info("SHA-512 implementation is available and will be used.")
    } catch {
      case _: NoSuchAlgorithmException => {
        logger.warning("No SHA-512 implementation available. Falling back to weak pseudo hashing.")
        Config.hasSHA512 = false
      }
    }
  }

  @Init
  def init(e: FMLInitializationEvent) = proxy.init()

  @PostInit
  def postInit(e: FMLPostInitializationEvent) = proxy.postInit()
}