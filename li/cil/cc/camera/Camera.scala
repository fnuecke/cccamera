package li.cil.cc.camera

import java.security.MessageDigest
import java.security.NoSuchAlgorithmException
import java.util.logging.Logger

import cpw.mods.fml.common.FMLLog
import cpw.mods.fml.common.Mod
import cpw.mods.fml.common.Mod.Init
import cpw.mods.fml.common.Mod.PostInit
import cpw.mods.fml.common.Mod.PreInit
import cpw.mods.fml.common.SidedProxy
import cpw.mods.fml.common.event.FMLInitializationEvent
import cpw.mods.fml.common.event.FMLPostInitializationEvent
import cpw.mods.fml.common.event.FMLPreInitializationEvent
import cpw.mods.fml.common.network.NetworkMod
import cpw.mods.fml.common.network.NetworkMod.SidedPacketHandler
import li.cil.cc.camera.common.CommonProxy
import li.cil.cc.camera.common.block.BlockCamera
import li.cil.cc.camera.network.PacketHandler
import net.minecraft.block.Block
import net.minecraftforge.common.Configuration

@Mod(modid = "CCCP",
  name = "ComputerCraft Camera Peripheral",
  version = "1.5.2.0",
  dependencies = "required-after:Forge@[7.8.1.737,);required-after:ComputerCraft@[1.53,);after:BuildCraft",
  modLanguage = "scala")
@NetworkMod(clientSideRequired = true, serverSideRequired = false,
  clientPacketHandlerSpec = new SidedPacketHandler(
    channels = Array("CCCPFlashPFX"), packetHandler = classOf[PacketHandler]))
object Camera {
  object Blocks {
    var camera: BlockCamera = null
  }

  object Config {
    var cameraBlockID = 3600
    var turtleUpgradeID = 253
    var hashSalt = "if you run a public server you may want to change this"
    var minLightLevel = 7.0
    var cooldown = 1.0
    var minNoise = 0.5
    var enableSound = true
    var enableParticles = true

    /** A list of blocks for which we will only return noise. */
    var blacklisted = Set(
      0, /* Air */
      Block.waterMoving.blockID,
      Block.waterStill.blockID,
      Block.lavaMoving.blockID,
      Block.lavaStill.blockID,
      Block.leaves.blockID,
      Block.glass.blockID,
      Block.fire.blockID,
      Block.ice.blockID,
      Block.portal.blockID,
      Block.thinGlass.blockID,
      Block.endPortal.blockID,
      Block.tripWire.blockID)

    /** Automatically filled in in postInit. */
    var cameraBlockRenderID = 0

    /** Automatically filled in once to avoid unnecessary exceptions when hashing. */
    var hasSHA512 = true

    /** The noise introduced from being on cooldown (linearly shrinks during cooldown period). */
    val noiseFromCooldown = 10
  }

  val logger = Logger.getLogger("CCCP")
  logger.setParent(FMLLog.getLogger)

  /** No distinction for now. May come later if I decide to play with advanced rendering. */
  @SidedProxy(
    clientSide = "li.cil.cc.camera.client.ClientProxy",
    serverSide = "li.cil.cc.camera.common.CommonProxy")
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
      "The ID for the turtle upgrade. Set this to -1 to disable the upgrade.").
      getInt(Config.turtleUpgradeID)

    Config.hashSalt = config.get("options", "hashSalt", Config.hashSalt, "" +
      "An additional salt to use for hashing block IDs.\n" +
      "This is applied in addition to the world seed and can be used on servers\n" +
      "to avoid clients generating hashes for block IDs by themselves, thereby\n" +
      "circumventing the need to figure out the signatures via calibration.").
      getString()

    Config.minLightLevel = config.get("options", "minLightLevel", Config.minLightLevel, "" +
      "If the light level is lower than this, the signature returned from a call\n" +
      "to trigger() will, even in best conditions, contain more noise than data.\n" +
      "Note that the 'power' of the true signature embedded in the returned value\n" +
      "will decline linearly with the light level (this cannot be disabled, hack\n" +
      "yourself an ID reading peripheral if you want that). That scaled data will\n" +
      "then be overlayed with random (standard normal distributed) noise, to make\n" +
      "figuring out the underlying signature and comparing single reads to that a\n" +
      "bit more challenging." +
      "Set this to zero to disable this constant base noise.").
      getDouble(Config.minLightLevel) max 0

    Config.cooldown = config.get("options", "cooldown", Config.cooldown, "" +
      "The cooldown of a camera, i.e. how many seconds have to pass before taking\n" +
      "another snapshot will not be penalized with additional noise. Used to make\n" +
      "it infeasible to just call trigger() a bunch of times in a loop to\n" +
      "determine the true signature of a block. The minimum value is 0.1 to at\n" +
      "least discourage spamming it in a single tick.").
      getDouble(Config.cooldown) max 0.1

    Config.minNoise = config.get("options", "minNoise", Config.minNoise,
      "The base noise level to apply to all measurements. The minimum value is 0.1.").
      getDouble(Config.minNoise) max 0.1

    Config.enableSound = config.get("options", "enableSound", Config.enableSound,
      "Whether to play a clicking sound when the camera is triggered.\n" +
        "This can be useful because the sound when the camera is still on cooldown\n" +
        "is different than the one from when it isn't.").
      getBoolean(Config.enableSound)

    Config.enableParticles = config.get("options", "enableParticles", Config.enableParticles,
      "Whether to show a particle effect when the camera's flash is used.").
      getBoolean(Config.enableParticles)

    Config.blacklisted = config.get("options", "blackisted", Config.blacklisted.toArray.sortWith(_ < _), "" +
      "A list of blacklisted block IDs. For these the camera will always return\n" +
      "pure noise. The default list is built on the principle that transparent\n" +
      "blocks (such as air and glass) cannot be properly captured, and that\n" +
      "fully animated blocks (flowing water, fire) cannot produce a robust\n" +
      "signal, since they always change.")
      .getIntList().toSet

    if (config.hasChanged)
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
