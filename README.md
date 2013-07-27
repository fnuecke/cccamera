ComputerCraft Camera Peripheral
===============================

This is a Minecraft mod that adds a [ComputerCraft][] [peripheral][] which can be used to "see" blocks.

You'll need Minecraft 1.5.2 (because I use a lot of mods that haven't updated yet) and the latest forge for 1.5.2, as well as ComputerCraft, of course.

It uses one block ID (3600 by default) and one turtle upgrade ID (253 by default).

Disclaimer
----------

Now let me say this first: this will *not* let you take pwetty pictures and look at them on monitors. If that's what you were hoping, sorry, but this is not the peripheral you are looking for.

You're still here? Great! Now that that's out of the way...

What is this trickery?
======================

So you can't take pictures with it, how is it a camera then? Well, let's call it role playing. It's a device that lets computers and turtles "see". (For all the disillusioning implementation details see explanations below. But what's way more interesting is what it *pretends* to do, no?)

Let's start out with the recipe:

![Camera Recipe](http://i.imgur.com/Reigp2T.png)

That's right, put a spider eye in a box, wire it up with some redstone and you have yourself a camera!

"But that's the eye of a dead spider," you may say, "how well could that possibly work?"
Not well at all, I can proudly assure you! Send the eye a little impulse and it will transmit some data that actually represents - in some inconceivable fashion - what it sees! Experiments have shown that all spiders seem to be notoriously short sighted, though. Anything not right in front of the camera does not seem to have any impact. Also, a spider's eye apparently can't really see very well in the dark. Maybe I should have used a different organ... because yet another downside of this organic component is that the output is not very stable. Even in the best conditions it is rare to get the a clear signature, but at least the fluctuation in the data is at an acceptable level.

As for the turtles... after some turtles getting terribly lost underground, it was decided that cameras attached to turtles should also have a flash. Please keep in mind that triggering it does consume some of the turtle's fuel reserves. A clever turtle would be better served placing torches now and then.

Plaintext
=========

You can use the camera peripheral by calling the `trigger` function it exposes, which takes an optional boolean parameter if it's attached to a turtle, that being whether to use the flash or not. If the flash is used and one fuel can be consumed, maximum light level is guaranteed. The function returns 64 numbers, which together are a signature representing the type of block in front of the camera.

```lua
local data = {peripheral.call("left", "trigger", useFlash)}
```

The returned numbers will generally be different each time you call the function, but for the same block they will be similar. So to calibrate you can repeatedly take
a snapshot of the same block, over and over, and slowly get a stable average, which will be close to the true signature for that block. You can then later on take a single snapshot and see to which known signature it is the most similar. You may wish to employ some kind of statistical test for best results.

The returned numbers are influenced by some random noise and the light level at the camera position. While the noise is of constant strength (think of it as film grain), the light level determines the strength of the underlying true signature.

Considerations
==============

The main concerns I had when designing this were the thing not getting too overpowered, and it feeling totally out of place. Originally I just wanted something that could identify blocks, because carrying around five blocks when digging just to avoid the uninteresting stuff really really s... ssseems not so great. But simply getting the ID of a block was obviously out of the question, since that would be totally lame and, as often mentioned on the ComputerCraft forums, breaks the 4th wall pretty damn hard (then again, http API anyone?)

So I set out to make up some limitations that felt natural and intuitive in the context of the world. Here's what I came up with:

1. **Limited field of view**  
   The peripheral can only see the block directly in front of it. So if it's attached to a turtle, there's *no* `triggerUp` and `triggerDown`. Also note that for turtles, the camera looks to the side on which it is attached, which seems to generally be the right side. So if you call `trigger` you'll get the values for the block on the turtle's right.
2. **A fuzzy "ID"**  
   What the `trigger` function returns is
   - a hash that incorporates the block's ID and metadata, the world seed and a configurable salt,
   - dampened in dark places, i.e. scaled using `lightLevel / 15`,
   - and roughened up with some random noise.

   I feel the explanation that it's just some data based on impulses the spider eye sends back is pretty in character. Having to figure out how best to work with those values also suits the DIY philosophy of ComputerCraft, IMHO.

   *Regarding the salt:* the configurable salt is only used on the server, so it can be secret for public servers, to avoid people generating the hashes for each ID and thus circumventing the need to figure out a signature for each block manually.
3. **A "soft" cooldown**  
   Taking a snapshot has a cooldown period. It can be configured but has a minimum cooldown of 0.1 to discourage it being triggered more than once per tick. This is not a "hard" cooldown in the sense that the camera won't accept the `trigger` call, it'll just return a value with so much noise in it that it won't be worth it.
4. **Flash costs fuel**  
   To avoid having the turtle having to place torches all over the place (although that might be cheaper resource-wise) you can tell the peripheral to use a flash when taking a snapshot. This will use one fuel and ensure maximum light level (no visible effect, at least not yet). The block peripheral doesn't have a flash.

I tried breaking this down to some relatively intuitive settings. Have a look at the config file, it'll contain comments with explanations.

Plans
=====

All of these may or may not happen.

- Make the camera block wrenchable.
- Add some particle / lighting effect when the flash is used.
- Create a nicer block model.
- Inspect items in a turtle's inventory if and only if it is standing directly in front of a camera block (or next to another turtle with the camera upgrade).

Acknowledgements
================
I'm really grateful for [OpenCCSensors][] being open source. Since this is my first proper mod for Minecraft that was a *huge* help in figuring out how all the different components work together.

License
=======

This mod is licensed under the [MIT License][license], which basically means you can do with it as you please.

[computercraft]: http://www.computercraft.info/
[license]: http://opensource.org/licenses/mit-license.php
[OpenCCSensors]: https://github.com/Cloudhunter/OpenCCSensors
[peripheral]: http://computercraft.info/wiki/Category:User_Created_Peripherals