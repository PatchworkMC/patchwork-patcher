# Patchwork: Patcher

Patchwork Patcher is a set of tools for transforming and patching Forge mod jars into jars that are directly loadable by Fabric Loader. It does the following things currently:

* Remaps Minecraft from official (proguard's obfuscated names) to srg (MCP's runtime mappings)

* Remaps the mod jar from srg to official using Tiny Remapper with the srg minecraft jar on the classpath

* Remaps the mod jar from official to intermediary using Tiny Remapper with the official minecraft jar on the classpath

* Converts the Forge mods.toml manifest to a fabric.mod.json file

* Converts @OnlyIn annotations to @Environment for Fabric

* Strips @ObjectHolder annotations, removes the field's final modifiers, and creates Consumers that set the fields

* Strips @Mod.EventBusSubscriber and @SubscribeEvent annotations and generates event handlers (that handle events) and event registrars (that register event handlers on behalf of a class)

	* Non-static event handlers are not yet supported

* Generates a class implementing ForgeInitializer that registers all the object holders and event registrars
* 1.14.4 only. 1.15 and 1.16 planned

	
## Note on Patchwork API

Patchwork Patcher generates jars that require a Fabric mod acting as a compatibility layer to run. [Patchwork API](https://github.com/PatchworkMC/patchwork-api) fulfills this role, most development will happen there.
