## Tweakfork
Tweakfork is a fork of Masa/Maruhon's mod [Tweakeroo](https://github.com/maruohon/tweakeroo). Its basically the same thing, except with some small changes to make life a little bit easier. 

1. Multi-layer block break restriction -> can prevent you from making potholes in the ground when speedmining
2. Block break whitelist/blacklist restriction -> can prevent you from accidentally mining the wrong block and flooding your redstone
3. Block based right click whitelist/blacklist restriction -> can prevent you from accidentally placing water outside of cauldrons and flooding your redstone
4. Toggleable flexible block placement -> may save you from arthritis
5. Stacking flexible block placement -> apply rotation to adjacent/offset placements
6. tweakHideScoreboard -> may save your sanity when servers turn on sidebar scoreboards during big digs
7. disableClientBlockEvents -> stops client side piston lag
8. disableStructureRendering -> disables structure block rendering. (bounding box, etc)
9. Fixed afterClicker with accurate placement protocol -> so placing repeaters isn't pain
10. tweakDayCycleOverride -> Overrides the time of day client-side
11. tweakNoSneakSlowdown -> Doesnt slow you down when you sneak
12. tweakScaffoldPlace -> Place blocks in a similar way to how scaffold placing works.
13. disableNametags -> Disables nametag rendering for unobstrusive replays
14. disableBossBar -> Disables the boss bar rendering
15. chestMirrorFix -> Fixes wonky chest mirroring behavior (works with litematica).
16. lavaDestroyFix -> Fixes [MC-246465](https://bugs.mojang.com/browse/MC-246465).
17. tweakAfkTimeout -> Automatically disconnects you (or does command/send chat) when afk.
18. tweakWeatherOverride -> Overrides the weather client side
19. tweakContainerScan -> Scans containers and tells you how many items/types are in it. Useful for debugging storage tech.
    * Easily see which containers have items or not
    * Compile a list of all the items it found
    * Search the list and click to easily find the location of the items.
    * To use, set the `openItemList` hotkey in Hotkeys.
20. Selectively Visible Blocks -> Allows you to make nice replays/vids with transparent parts (Works with optifucked and sodium+iris). Selectively renders blocks depending on position based white/blacklists.
    * Lists.selectiveBlocksWhitelist and Lists.selectiveBlocksBlacklist -> strings encoding the white/blacklists. Format: `x,y,z|x2,y2,z2|...`
    * tweakAreaSelector -> allows you to add/remove selection from list easily with areaSelectionAddToList and areaSelectionRemoveFromList hotkeys. works in replay editor. Use areaSelectionOffset hotkey to offset selection box.
    * tweakSelectiveBlocksRendering -> Turns on selective block rendering with the lists. Turn on before you render replay.
    * tweakSelectiveBlocksRenderOutline -> Renders an outline over the listed positions. Allows you to see whats in the white/blacklists.
    * Generic.selectiveBlocksTrackPistons -> When enabled, the mod will track piston movements and update the position lists accordingly.
    * Generic.areaSelectionUseAll -> When enabled, the area selector will include air blocks.
    * Generic.selectiveBlocksNoHit -> When enabled, disables trace-hitting hidden blocks. (hint: can also use to build stuff in hard to reach places in survival mode)
    * Generic.selectiveBlocksHideEntities -> When enabled, selective block rendering will also hide entities.
    * Generic.selectiveBlocksHideParticles -> When enabled, selective block rendering will also hide particles.
21. Noteblock edit -> tune noteblocks without getting carpal tunnel
    * While looking at noteblock, press number keys to add the amount of notes. eg, for high F note 23, press 1 two times then press 3 (1 = 10, 0 = reset)
    * Use plus key to add 1, minus key to subtract 1. 
    * Press letter keys A-G to set notes by letter name (only when Configs.Generic.noteEditLetters is set to true). Use Tab to toggle octave.
22. afkIgnoreInput -> if true, afk timeout ignores all player input and the timer never resets.
23. noteblockScroll -> use the scrollwheel to change noteblock pitch<
34. notePlayKey -> play the current noteblock pitch using the key specified under `Hotkeys -> notePlayKey`
35. disableOtherPlayerRendering -> Disables rendering of all players except yourself (only for multiplayer).
36. disableLivingEntityRendering -> Disables all living entity rendering (all entities with a health bar).
37. disableItemEntityRendering -> Disables item entity rendering.
38. disablePhantomSpawning -> Disables phantom spawning globally. Only works in singleplayer/server side.


*Using Selectively Visible Blocks for the [brewer](https://www.youtube.com/watch?v=1_jSkyq-WOs) video*
<img width="1280" alt="Screen Shot 2021-02-16 at 2 01 08 PM" src="https://user-images.githubusercontent.com/13282284/108109105-7742c280-705f-11eb-81cf-b5341ca740c3.png">

Tweakeroo
==============
Tweakeroo is a client-side-only Minecraft mod using LiteLoader.
It adds a selection of miscellaneous, configurable, client-side tweaks to the game.
Some examples of these are the "flexible block placement" tweak and the "fast block placement" tweak.
For more information and the downloads (compiled builds), see http://minecraft.curseforge.com/projects/tweakeroo

Compiling
=========
* Clone the repository
* Open a command prompt/terminal to the repository directory
* run 'gradlew build'
* The built jar file will be in build/libs/
