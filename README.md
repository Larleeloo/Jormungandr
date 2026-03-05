Jörmungandr READ ME:

Jörmungandr will be a traditional (turn-based) roguelike mobile game that features the player adventuring through different themed rooms in cutaway view. Rooms will be filled with clickable icons and sometimes monsters to fight via text prompts. The game should be partially online via synchronization in my Google drive for various JSON based save files. 

Only 25 initial player access codes are available for testing purposes.

**Gameplay**

- Circular map subdivided into 8 regions, one central hub and 8 radial (concentric circle) sections off of said hub.  
- The vibes are medieval fantasy dungeon crawler with some roguelike elements.  
- Each region contains 10,000 rooms, one of which is a Waypoint Room.   
- Waypoint Rooms will allow players to bank excess items, retrieve banked items, trade, and teleport to other discovered Waypoint Rooms.  
- The central room should provide access to the 8 regions, the shop, a trading post between players, and a storage area for extra items.  
- Players inventories should vary, but start at 8 stacks of items and progress to 48 at max level and max points invested in strength.  
- The other 9,999 rooms in each region will be a combination of empty rooms (80%) that have randomly generated chests and containers (jars, barrels, etc) with items, hidden creatures, and various other secrets ranging from harmless to deadly, and Creature Den’s (20%) which will contain a regionally specific enemy that players must defeat to get past. These enemies will drop particularly valuable items, with the value of drops increasing as enemy difficulty increases.   
- When a player travels to a room, the game will first check if the room file exists (e.g. room 123 \-\> room 124\) and if it does not, the room file will be procedurally generated. If it does, the data for this new room will be pulled from the Drive JSON save file. The game will start with only the center room or “hub” data generated. More rooms will be generated as players progress.  
- There will always be a chance to summon a monster in a room, even if the room has already been discovered by another or the same player. This will be procedurally generated on the fly as the player progresses through rooms.  
- Each of the 8 regions will be a different biome. A red banner dungeon (1), an orange volcanic waste (2), a yellow meadow (3), a green forest (4), a blue ocean (5), a pink castle (6), an ice cave (7), and a purple and black void (8). The central hub will be a desert marketplace.  
- Different regions will drop items that can debuff creatures from higher numbered regions. For example, one might expect to find debuff items for regions 2-5 in region 1, and debuffs for 4-8 in region 3\. These will help in monster combat.  
- Players can cross over to other regions while within certain regions. Doors and textures will change indicating this cross over.  
- The 8 regions will be numbered 1 through 8 and that number will correspond to how many levels above the player's level that region's enemies will be.   
- There will be a robust leveling system with skills and powerups.  
- Skill investment for player level ups will function similarly to D\&D in that players will choose what skills to invest in (strength, constitution, intelligence, wisdom, charisma, dexterity, etc) and those investments will impact their options in combat.  
- Enemies (monsters) will scale to the players level, and each region will scale enemies roughly one level higher than the previous. Gameplay will always be balanced so that enemies in region 1 are at the player's levels, and enemies in region 8 are 8 levels above the player.  
- Difficulty should increase based on the number of visited rooms before returning to the center, as well as distance from the center (the concentric circles should be “zones” within biomes and should be indicated to players). This should feature by increasing the number of high level monsters, not increasing the level of low level monsters or their numbers.   
- Combat will feel like a pokemon battle screen, where the player selects from 8-48 actions based on what items they have equipped. Equipable slots will vary from 8-48 based on selected levels.  
- Items can stack up to 16 of the same type of item in a slot  
- Players can leave notes that will update to all other players maps in Waypoint Rooms and Creature Dens, as well as in the central hub. This will allow for independent gameplay with a multi-player communication element.  
- There will be a robust inventory system that allows players to transfer items from their inventory back and forth to any other entity (chest, monster, etc) similar to skyrim.  
- There will be an item rarity tier where items will have a pre-programmed glow based on their rarity (white \= common(50%), green \= uncommon(25%), blue \= rare(15%), purple \= epic(8%), orange \= legendary(1.5%), cyan \= mythic(0.5%))  
- Rooms will display from a “dollhouse” (or “cutaway”) perspective, as if the player is looking into the room from the 4th wall. This will be in vertical view.    
- Rooms will be initially generated procedurally into individual save files to be loaded from the cloud (Drive) save files. These files will be editable.  
- Every player will exist in the same cloud based world.  
- Monsters will have an increased percent chance of encounters as time increases.  
- The rooms will be a large mesh-like maze of “node” rooms, where the player can go into any open door (left, right, forward) or return to where they came from. Cardinal directions will help the player decide their intended direction.  
- There will be a map of the region based on where the player has traveled previously. Each room will display as an icon. The map will be accessible from any place in the game.  
- Each room will be a different save file that can be interacted with by the player.  
- Each room is a static screen with some PNG or GIF based objects that are interactable  
- The player must be on the current version of the game to play, which will be checked via the cloud (Drive) save sync system.   
- All assets will be provided later, and any that don’t exist yet will be replaced with a colored square based on what the item is.  
- All assets will be placed into pre-programmed folders labeled by category. (See “assets manifest”)  
- Sound effects and music will be handled internally via a sound manager class.  
- The individual rooms should be easily managed and editable via save file editing in the Drive save files

**A standard game will go as follows (players do not reset at the main hub unless they die or decide to travel there via a waypoint):**

- A player loads into their save (or spawns in the central hub)  
- They are presented with 1-3 doors and some various clickable icons/items in this room (or options to interact with the central hub)  
- They progress forward  
- They find a level 7 demon (the player is level 3 and in region 4 (forest)) with 50HP  
- The player uses their broadsword (select broadsword \-\> actions (swing/throw/drop) \-\> swing)  
- The demon loses 12HP (Broadsword damage \+ 2 points in strength perk)  
- The demon attacks for 8 damage.  
- The player blocks 6 damage and breaks their equipped shield and then takes two damage  
- The player drinks a potion of super strength for \+10 attack (unlimited buffs/potions/debuffs per turn)   
- The player attacks with broadsword again for \-22HP on the demon  
- The demon repeats its last move and attacks for 8 damage  
- The player takes the whole 8 damage  
- The player uses a potion of weakness on the demon and attacks, dealing 18 damage and killing the demon  
- The player can now search the room or progress forward, left or right through 3 open doors

Assets Manifest Example (Fill in based on “GAMEITEMS” file and create unique asset names and file locations. The game should have roughly 800 items (with textures), 100 backgrounds, 300 icons, and 100-200 unique creatures. This will not be available on the first pass, but rather as the game develops. Start with 10% of the total and focus on filling out region 1.):

| Type | Name | Identifier (.gif/.png) | Location (assets/) | Description |
| :---- | :---- | :---- | :---- | :---- |
| Item | Broadsword | broadsword | items/tools/weapons | \-10 Damage; common |
| Item | Health Potion | health\_potion | items/potions/health | \+5HP; common |
| Background | Red Dungeon Standard Background 1 | red\_dungeon\_bg1 | backgrounds/red\_dungeon | Background for one of the red dungeon rooms. scaled to full screen based on device |
| Icon | Menu Selection Screen | menu\_texture | icons/menu | The main background for full screen popup menus. scaled to full screen based on device |
| Icon | Red Door 1 (LEFT/RIGHT) | red\_door\_1\_lr | icons/rooms | Icon to placed for doors in the red dungeon. Can be clicked to travel to a room on your left or right depending on mirroring |
| Icon | Red Door 1 (FORWARD) | red\_door\_1\_f | icons/rooms | Same as left and right door but centered in perspective and can travel forward in rooms. |
| Creature | Demon | demon | entities/hostile | A random enemy creature |

**Save Files:** 

- Save files will be in json format and stored at “[https://drive.google.com/drive/folders/1B59VgFb9TcZGkB6H8i9bAXnodImx-g3k?dmr=1\&ec=wgc-drive-hero-goto](https://drive.google.com/drive/folders/1B59VgFb9TcZGkB6H8i9bAXnodImx-g3k?dmr=1&ec=wgc-drive-hero-goto)” (please provide setup instructions and files for cloud sync. Google web app deployment will be necessary.)  
- Player saves will be stored at: “[https://drive.google.com/drive/folders/12QCd57ODE-IbImzPMSqvmKVvyYR5MQdv?dmr=1\&ec=wgc-drive-hero-goto](https://drive.google.com/drive/folders/12QCd57ODE-IbImzPMSqvmKVvyYR5MQdv?dmr=1&ec=wgc-drive-hero-goto)”   
- Player data will contain inventory, hp/mana, map, achievement, and perk selection data  
- Room data will be stored at “[https://drive.google.com/drive/folders/12QCd57ODE-IbImzPMSqvmKVvyYR5MQdv?dmr=1\&ec=wgc-drive-hero-goto](https://drive.google.com/drive/folders/12QCd57ODE-IbImzPMSqvmKVvyYR5MQdv?dmr=1&ec=wgc-drive-hero-goto)”  
- Room data will contain background texture, items/chest inventory, creature, trap, door link (to room mesh) data
