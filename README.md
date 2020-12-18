# Patchwork: Patcher

Patchwork Patcher is a tool that lets you load Forge mods using Fabric.

## How to use

Patchwork as a project isn't really meant for regular use yet, it's still alpha/beta quality software. If you're adventurous you can compile the Patcher code from source and run it using Gradle. Once you've got it running it should be pretty simple to use. Put mods in the input folder, hit Patch, wait a bit, and then you can use the mod files in the output folder in your Fabric installation.

Note that you need to have [Patchwork API](https://github.com/PatchworkMC/patchwork-api) in your `mods` folder as well.


## Technical details

Patchwork Patcher is a set of tools for transforming and patching Forge mod jars into jars that are directly loadable by Fabric Loader. It applies a bunch of transformations to Forge mods to make it easier to load them with Fabric, including but not limited to:

* Rewriting the mod metadata so that Fabric can properly discover and load the Forge mods
* Remapping from Forge's runtime mappings (srg) to Fabric's runtime mappings (intermediary)

The resulting mod files require a Fabric mod acting as a compatibility layer to run. [Patchwork API](https://github.com/PatchworkMC/patchwork-api) currently fulfills this role, and most development will happen there. There is also work on a project currently called [Crabwork](https://github.com/PatchworkMC/crabwork) which hopes to fulfill a similar purpose without requiring nearly as much manual work.
