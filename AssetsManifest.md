# Jormungandr Assets Manifest

> Auto-generated asset reference for the Jormungandr roguelike game.
> Sprite paths are relative to `app/src/main/assets/`.
> Android resources are in `app/src/main/res/`.

---

## Table of Contents

- [Data Files](#data-files)
- [Creatures (Hostile Entities)](#creatures-hostile-entities)
- [Items](#items)
  - [Weapons](#weapons)
  - [Armor](#armor)
  - [Shields](#shields)
  - [Clothing](#clothing)
  - [Accessories](#accessories)
  - [Potions](#potions)
  - [Scrolls](#scrolls)
  - [Food](#food)
  - [Materials](#materials)
  - [Tools](#tools)
  - [Keys](#keys)
  - [Ammo](#ammo)
  - [Dyes](#dyes)
  - [Misc](#misc)
- [Backgrounds](#backgrounds)
- [Sounds](#sounds)
- [Icons](#icons)
- [Fonts](#fonts)
- [Sprites (Additional)](#sprites-additional)
- [Android Resources (res/)](#android-resources)

---

## Data Files

| File | Folder | Description |
|------|--------|-------------|
| `creatures.json` | `data/` | All creature definitions (45 creatures) |
| `items.json` | `data/` | All item definitions (277 items) |

---

## Creatures (Hostile Entities)

All creature sprites live in `entities/hostile/`.

### Region 1 — Red Dungeon

| Tag | Display Name | Sprite Path | Placeholder Color |
|-----|-------------|-------------|-------------------|
| `dungeon_rat` | Dungeon Rat | `entities/hostile/dungeon_rat.png` | `#AA0000` |
| `skeleton` | Skeleton | `entities/hostile/skeleton.png` | `#CC0000` |
| `fire_imp` | Fire Imp | `entities/hostile/fire_imp.png` | `#FF4444` |
| `dark_spider` | Dark Spider | `entities/hostile/dark_spider.png` | `#880000` |
| `zombie` | Zombie | `entities/hostile/zombie.png` | `#BB2222` |
| `blood_bat` | Blood Bat | `entities/hostile/blood_bat.png` | `#DD1111` |
| `wraith` | Wraith | `entities/hostile/wraith.png` | `#990000` |
| `golem` | Golem | `entities/hostile/golem.png` | `#993333` |
| `demon` | Demon | `entities/hostile/demon.png` | `#CC1100` |
| `red_dragon_hatchling` | Red Dragon Hatchling | `entities/hostile/red_dragon_hatchling.png` | `#FF0000` |

### Region 2 — Volcanic Waste

| Tag | Display Name | Sprite Path | Placeholder Color |
|-----|-------------|-------------|-------------------|
| `lava_elemental` | Lava Elemental | `entities/hostile/lava_elemental.png` | `#FF6600` |
| `fire_salamander` | Fire Salamander | `entities/hostile/fire_salamander.png` | `#EE5500` |
| `magma_golem` | Magma Golem | `entities/hostile/magma_golem.png` | `#CC4400` |
| `ash_wraith` | Ash Wraith | `entities/hostile/ash_wraith.png` | `#DD5522` |
| `ember_wolf` | Ember Wolf | `entities/hostile/ember_wolf.png` | `#FF7733` |

### Region 3 — Meadow

| Tag | Display Name | Sprite Path | Placeholder Color |
|-----|-------------|-------------|-------------------|
| `wild_boar` | Wild Boar | `entities/hostile/wild_boar.png` | `#AAAA00` |
| `giant_bee` | Giant Bee | `entities/hostile/giant_bee.png` | `#CCCC00` |
| `thorn_sprite` | Thorn Sprite | `entities/hostile/thorn_sprite.png` | `#88AA00` |
| `wind_serpent` | Wind Serpent | `entities/hostile/wind_serpent.png` | `#BBBB22` |
| `meadow_troll` | Meadow Troll | `entities/hostile/meadow_troll.png` | `#99AA11` |

### Region 4 — Forest

| Tag | Display Name | Sprite Path | Placeholder Color |
|-----|-------------|-------------|-------------------|
| `dire_wolf` | Dire Wolf | `entities/hostile/dire_wolf.png` | `#228B22` |
| `tree_ent` | Tree Ent | `entities/hostile/tree_ent.png` | `#006600` |
| `forest_spider` | Forest Spider | `entities/hostile/forest_spider.png` | `#337733` |
| `goblin` | Goblin | `entities/hostile/goblin.png` | `#118811` |
| `moss_golem` | Moss Golem | `entities/hostile/moss_golem.png` | `#2E8B2E` |

### Region 5 — Ocean

| Tag | Display Name | Sprite Path | Placeholder Color |
|-----|-------------|-------------|-------------------|
| `sea_serpent` | Sea Serpent | `entities/hostile/sea_serpent.png` | `#0066CC` |
| `crab_warrior` | Crab Warrior | `entities/hostile/crab_warrior.png` | `#004488` |
| `drowned_zombie` | Drowned Zombie | `entities/hostile/drowned_zombie.png` | `#225588` |
| `siren` | Siren | `entities/hostile/siren.png` | `#3377AA` |
| `kraken_spawn` | Kraken Spawn | `entities/hostile/kraken_spawn.png` | `#003366` |

### Region 6 — Castle

| Tag | Display Name | Sprite Path | Placeholder Color |
|-----|-------------|-------------|-------------------|
| `knight_ghost` | Knight Ghost | `entities/hostile/knight_ghost.png` | `#FF69B4` |
| `gargoyle` | Gargoyle | `entities/hostile/gargoyle.png` | `#CC5599` |
| `royal_guard` | Royal Guard | `entities/hostile/royal_guard.png` | `#DD6699` |
| `witch` | Witch | `entities/hostile/witch.png` | `#BB4488` |
| `shadow_assassin` | Shadow Assassin | `entities/hostile/shadow_assassin.png` | `#AA3377` |

### Region 7 — Ice Cave

| Tag | Display Name | Sprite Path | Placeholder Color |
|-----|-------------|-------------|-------------------|
| `ice_golem` | Ice Golem | `entities/hostile/ice_golem.png` | `#88CCEE` |
| `frost_wolf` | Frost Wolf | `entities/hostile/frost_wolf.png` | `#AADDFF` |
| `snow_wraith` | Snow Wraith | `entities/hostile/snow_wraith.png` | `#99CCDD` |
| `crystal_spider` | Crystal Spider | `entities/hostile/crystal_spider.png` | `#77BBDD` |
| `yeti` | Yeti | `entities/hostile/yeti.png` | `#BBDDEE` |

### Region 8 — The Void

| Tag | Display Name | Sprite Path | Placeholder Color |
|-----|-------------|-------------|-------------------|
| `void_walker` | Void Walker | `entities/hostile/void_walker.png` | `#440066` |
| `shadow_beast` | Shadow Beast | `entities/hostile/shadow_beast.png` | `#330055` |
| `chaos_elemental` | Chaos Elemental | `entities/hostile/chaos_elemental.png` | `#550088` |
| `nightmare` | Nightmare | `entities/hostile/nightmare.png` | `#220044` |
| `void_dragon` | Void Dragon | `entities/hostile/void_dragon.png` | `#2A0044` |

---

## Items

All item sprites live in `items/{category}/` (also mirrored under `sprites/items/{category}/`).

### Weapons

Folder: `items/weapon/`

| Tag | Display Name | Sprite Path |
|-----|-------------|-------------|
| `club` | Club | `items/weapon/club.png` |
| `wooden_sword` | Wooden Sword | `items/weapon/wooden_sword.png` |
| `carpenters_axe` | Carpenter's Axe | `items/weapon/carpenters_axe.png` |
| `bow` | Bow | `items/weapon/bow.png` |
| `staff` | Staff | `items/weapon/staff.png` |
| `rock` | Rock | `items/weapon/rock.png` |
| `walking_stick` | Walking Stick | `items/weapon/walking_stick.png` |
| `iron_sword` | Iron Sword | `items/weapon/iron_sword.png` |
| `battleaxe` | Battleaxe | `items/weapon/battleaxe.png` |
| `mace` | Mace | `items/weapon/mace.png` |
| `metal_bow` | Metal Bow | `items/weapon/metal_bow.png` |
| `daggers` | Daggers | `items/weapon/daggers.png` |
| `crossbow` | Crossbow | `items/weapon/crossbow.png` |
| `throwing_axe` | Throwing Axe | `items/weapon/throwing_axe.png` |
| `throwing_knife` | Throwing Knife | `items/weapon/throwing_knife.png` |
| `gold_sword` | Gold Sword | `items/weapon/gold_sword.png` |
| `golden_heavy_battleaxe` | Golden Heavy Battleaxe | `items/weapon/golden_heavy_battleaxe.png` |
| `golden_mace` | Golden Mace | `items/weapon/golden_mace.png` |
| `golden_bow` | Golden Bow | `items/weapon/golden_bow.png` |
| `ice_staff` | Ice Staff | `items/weapon/ice_staff.png` |
| `heavy_crossbow` | Heavy Crossbow | `items/weapon/heavy_crossbow.png` |
| `lightning_rod` | Lightning Rod | `items/weapon/lightning_rod.png` |
| `katana` | Katana | `items/weapon/katana.png` |
| `magic_wand` | Magic Wand | `items/weapon/magic_wand.png` |
| `bomb` | Bomb | `items/weapon/bomb.png` |
| `soulbound_dagger` | Soulbound Dagger | `items/weapon/soulbound_dagger.png` |
| `spell_casters_tome` | Spell Caster's Tome | `items/weapon/spell_casters_tome.png` |
| `spectral_bow` | Spectral Bow | `items/weapon/spectral_bow.png` |
| `spectral_sword` | Spectral Sword | `items/weapon/spectral_sword.png` |
| `spectral_axe` | Spectral Axe | `items/weapon/spectral_axe.png` |
| `spectral_mace` | Spectral Mace | `items/weapon/spectral_mace.png` |
| `vampiric_dagger` | Vampiric Dagger | `items/weapon/vampiric_dagger.png` |
| `thunder_hammer` | Thunder Hammer | `items/weapon/thunder_hammer.png` |
| `musket` | Musket | `items/weapon/musket.png` |
| `summoning_rod` | Summoning Rod | `items/weapon/summoning_rod.png` |
| `arcane_staff` | Arcane Staff | `items/weapon/arcane_staff.png` |
| `void_blade` | Void Blade | `items/weapon/void_blade.png` |
| `necromancers_blade` | Necromancer's Blade | `items/weapon/necromancers_blade.png` |
| `ethereal_dragonslayer_blade` | Ethereal Dragonslayer Blade | `items/weapon/ethereal_dragonslayer_blade.png` |
| `electrified_katana` | Electrified Katana | `items/weapon/electrified_katana.png` |
| `cannon` | Cannon | `items/weapon/cannon.png` |
| `infinity_staff` | Infinity Staff | `items/weapon/infinity_staff.png` |
| `phoenix_bow` | Phoenix Bow | `items/weapon/phoenix_bow.png` |
| `time_warp_blade` | Time Warp Blade | `items/weapon/time_warp_blade.png` |
| `staff_of_1000_souls` | Staff of 1000 Souls | `items/weapon/staff_of_1000_souls.png` |

### Armor

Folder: `items/armor/`

| Tag | Display Name | Sprite Path |
|-----|-------------|-------------|
| `leather_tunic` | Leather Tunic | `items/armor/leather_tunic.png` |
| `iron_chestplate` | Iron Chestplate | `items/armor/iron_chestplate.png` |
| `iron_boots` | Iron Boots | `items/armor/iron_boots.png` |
| `sentinel_gauntlets` | Sentinel Gauntlets | `items/armor/sentinel_gauntlets.png` |
| `iron_leggings` | Iron Leggings | `items/armor/iron_leggings.png` |
| `chainmail_shirt` | Chainmail Shirt | `items/armor/chainmail_shirt.png` |
| `chainmail_pants` | Chainmail Pants | `items/armor/chainmail_pants.png` |
| `iron_boots_u` | Iron Boots | `items/armor/iron_boots_u.png` |
| `titan_gauntlets` | Titan Gauntlets | `items/armor/titan_gauntlets.png` |
| `celestial_robes` | Celestial Robes | `items/armor/celestial_robes.png` |
| `archmage_robes` | Archmage Robes | `items/armor/archmage_robes.png` |
| `obsidian_helmet` | Obsidian Helmet | `items/armor/obsidian_helmet.png` |
| `obsidian_chestplate` | Obsidian Chestplate | `items/armor/obsidian_chestplate.png` |
| `obsidian_leggings` | Obsidian Leggings | `items/armor/obsidian_leggings.png` |
| `obsidian_gauntlets` | Obsidian Gauntlets | `items/armor/obsidian_gauntlets.png` |
| `obsidian_boots` | Obsidian Boots | `items/armor/obsidian_boots.png` |
| `void_helmet` | Void Helmet | `items/armor/void_helmet.png` |
| `void_chestplate` | Void Chestplate | `items/armor/void_chestplate.png` |
| `void_leggings` | Void Leggings | `items/armor/void_leggings.png` |
| `void_gauntlets` | Void Gauntlets | `items/armor/void_gauntlets.png` |
| `void_boots` | Void Boots | `items/armor/void_boots.png` |
| `gown_of_forgotten_verses` | Gown of Forgotten Verses | `items/armor/gown_of_forgotten_verses.png` |

### Shields

Folder: `items/shield/`

| Tag | Display Name | Sprite Path |
|-----|-------------|-------------|
| `wooden_shield` | Wooden Shield | `items/shield/wooden_shield.png` |
| `steel_shield` | Steel Shield | `items/shield/steel_shield.png` |
| `golden_shield` | Golden Shield | `items/shield/golden_shield.png` |
| `void_shield` | Void Shield | `items/shield/void_shield.png` |

### Clothing

Folder: `items/clothing/`

| Tag | Display Name | Sprite Path |
|-----|-------------|-------------|
| `pants` | Pants | `items/clothing/pants.png` |
| `hat` | Hat | `items/clothing/hat.png` |
| `cap` | Cap | `items/clothing/cap.png` |
| `worn_shoes` | Worn Shoes | `items/clothing/worn_shoes.png` |
| `work_shirt` | Work Shirt | `items/clothing/work_shirt.png` |
| `boots` | Boots | `items/clothing/boots.png` |
| `collared_shirt` | Collared Shirt | `items/clothing/collared_shirt.png` |
| `swimwear` | Swimwear | `items/clothing/swimwear.png` |
| `wizard_hat` | Wizard Hat | `items/clothing/wizard_hat.png` |
| `cape` | Cape | `items/clothing/cape.png` |
| `fancy_boots` | Fancy Boots | `items/clothing/fancy_boots.png` |
| `three_piece_suit` | Three-Piece-Suit | `items/clothing/three_piece_suit.png` |
| `witchs_hat` | Witch's Hat | `items/clothing/witchs_hat.png` |
| `crown` | Crown | `items/clothing/crown.png` |
| `skeleton_crown` | Skeleton Crown | `items/clothing/skeleton_crown.png` |
| `platinum_crown` | Platinum Crown | `items/clothing/platinum_crown.png` |
| `chameleon_cloak` | Chameleon Cloak | `items/clothing/chameleon_cloak.png` |
| **Dyed Shirts** | `red_shirt`, `green_shirt`, `blue_shirt`, `purple_shirt`, `orange_shirt`, `black_shirt` | `items/clothing/{color}_shirt.png` |
| **Dyed Dresses** | `white_dress`, `red_dress`, `green_dress`, `blue_dress`, `purple_dress`, `orange_dress`, `black_dress` | `items/clothing/{color}_dress.png` |
| **Dyed Hats** | `white_hat`, `red_hat`, `green_hat`, `blue_hat`, `purple_hat`, `orange_hat`, `black_hat` | `items/clothing/{color}_hat.png` |
| **Dyed Pants** | `white_pants`, `red_pants`, `green_pants`, `blue_pants`, `purple_pants`, `orange_pants`, `black_pants` | `items/clothing/{color}_pants.png` |
| **Dyed Robes** | `white_robe`, `red_robe`, `green_robe`, `blue_robe`, `purple_robe`, `orange_robe`, `black_robe` | `items/clothing/{color}_robe.png` |
| **Dyed Shoes** | `white_shoes`, `red_shoes`, `green_shoes`, `blue_shoes`, `purple_shoes`, `orange_shoes`, `black_shoes` | `items/clothing/{color}_shoes.png` |
| **Gold Clothing** | `gold_shirt`, `gold_pants`, `gold_shoes`, `gold_hat`, `gold_dress`, `gold_robe` | `items/clothing/gold_{type}.png` |

### Accessories

Folder: `items/accessory/`

| Tag | Display Name | Sprite Path |
|-----|-------------|-------------|
| `bracelet` | Bracelet | `items/accessory/bracelet.png` |
| `silver_necklace` | Silver Necklace | `items/accessory/silver_necklace.png` |
| `silver_bracelet` | Silver Bracelet | `items/accessory/silver_bracelet.png` |
| `gold_necklace` | Gold Necklace | `items/accessory/gold_necklace.png` |
| `gold_bracelet` | Gold Bracelet | `items/accessory/gold_bracelet.png` |
| `orb` | Orb | `items/accessory/orb.png` |
| `backpack` | Backpack | `items/accessory/backpack.png` |
| `lucky_coin` | Lucky Coin | `items/accessory/lucky_coin.png` |
| `undying_stone` | Undying Stone | `items/accessory/undying_stone.png` |
| `eye_of_the_cosmos` | Eye of the Cosmos | `items/accessory/eye_of_the_cosmos.png` |
| `heart_of_eternity` | Heart of Eternity | `items/accessory/heart_of_eternity.png` |
| **Silver Gem Necklaces** | `silver_ruby_necklace`, `silver_emerald_necklace`, `silver_sapphire_necklace`, `silver_diamond_necklace` | `items/accessory/silver_{gem}_necklace.png` |
| **Silver Gem Bracelets** | `silver_ruby_bracelet`, `silver_emerald_bracelet`, `silver_sapphire_bracelet`, `silver_diamond_bracelet` | `items/accessory/silver_{gem}_bracelet.png` |
| **Gold Gem Necklaces** | `gold_ruby_necklace`, `gold_emerald_necklace`, `gold_sapphire_necklace`, `gold_diamond_necklace` | `items/accessory/gold_{gem}_necklace.png` |
| **Gold Gem Bracelets** | `gold_ruby_bracelet`, `gold_emerald_bracelet`, `gold_sapphire_bracelet`, `gold_diamond_bracelet` | `items/accessory/gold_{gem}_bracelet.png` |

### Potions

Folder: `items/potion/`

| Tag | Display Name | Sprite Path |
|-----|-------------|-------------|
| `blank_potion` | Blank Potion | `items/potion/blank_potion.png` |
| `health_potion` | Health Potion | `items/potion/health_potion.png` |
| `mana_potion` | Mana Potion | `items/potion/mana_potion.png` |
| `stamina_potion` | Stamina Potion | `items/potion/stamina_potion.png` |
| `poison` | Poison | `items/potion/poison.png` |
| `full_health_potion` | Full Health Potion | `items/potion/full_health_potion.png` |
| `full_mana_potion` | Full Mana Potion | `items/potion/full_mana_potion.png` |
| `full_stamina_potion` | Full Stamina Potion | `items/potion/full_stamina_potion.png` |
| `xp_potion` | XP Potion | `items/potion/xp_potion.png` |
| `purple_potion` | Purple Potion | `items/potion/purple_potion.png` |
| `lucky_potion` | Lucky Potion | `items/potion/lucky_potion.png` |
| `void_potion` | Void Potion | `items/potion/void_potion.png` |
| `elixir_of_immortality` | Elixir of Immortality | `items/potion/elixir_of_immortality.png` |

### Scrolls

Folder: `items/scroll/`

| Tag | Display Name | Sprite Path |
|-----|-------------|-------------|
| `blank_scroll` | Blank Scroll | `items/scroll/blank_scroll.png` |
| `scroll_of_fireball` | Scroll of Fireball | `items/scroll/scroll_of_fireball.png` |
| `scroll_of_ice_crystal` | Scroll of Ice Crystal | `items/scroll/scroll_of_ice_crystal.png` |
| `scroll_of_poison` | Scroll of Poison | `items/scroll/scroll_of_poison.png` |
| `scroll_of_fire_rune` | Scroll of Fire Rune | `items/scroll/scroll_of_fire_rune.png` |
| `scroll_of_ice_rune` | Scroll of Ice Rune | `items/scroll/scroll_of_ice_rune.png` |
| `scroll_of_poison_rune` | Scroll of Poison Rune | `items/scroll/scroll_of_poison_rune.png` |
| `void_scroll` | Void Scroll | `items/scroll/void_scroll.png` |
| `void_rune_scroll` | Void Rune Scroll | `items/scroll/void_rune_scroll.png` |
| `undead_scroll` | Undead Scroll | `items/scroll/undead_scroll.png` |

### Food

Folder: `items/food/`

| Tag | Display Name | Sprite Path |
|-----|-------------|-------------|
| `bread` | Bread | `items/food/bread.png` |
| `cheese_wheel` | Cheese Wheel | `items/food/cheese_wheel.png` |
| `apple` | Apple | `items/food/apple.png` |
| `berries` | Berries | `items/food/berries.png` |
| `water_bottle` | Water Bottle | `items/food/water_bottle.png` |
| `chicken_egg` | Chicken Egg | `items/food/chicken_egg.png` |
| `pumpkin` | Pumpkin | `items/food/pumpkin.png` |
| `melon` | Melon | `items/food/melon.png` |
| `chicken` | Chicken | `items/food/chicken.png` |
| `fish` | Fish | `items/food/fish.png` |
| `cooked_salmon` | Cooked Salmon | `items/food/cooked_salmon.png` |
| `bottle_of_honey` | Bottle of Honey | `items/food/bottle_of_honey.png` |
| `magic_apple` | Magic Apple | `items/food/magic_apple.png` |
| `berries_of_the_old_gods` | Berries of the Old Gods | `items/food/berries_of_the_old_gods.png` |

### Materials

Folder: `items/material/`

| Tag | Display Name | Sprite Path |
|-----|-------------|-------------|
| `yarn` | Yarn | `items/material/yarn.png` |
| `coal` | Coal | `items/material/coal.png` |
| `cobblestone` | Cobblestone | `items/material/cobblestone.png` |
| `planks` | Planks | `items/material/planks.png` |
| `ice` | Ice | `items/material/ice.png` |
| `feather` | Feather | `items/material/feather.png` |
| `iron_ore` | Iron Ore | `items/material/iron_ore.png` |
| `gunpowder` | Gunpowder | `items/material/gunpowder.png` |
| `ink` | Ink | `items/material/ink.png` |
| `cactus` | Cactus | `items/material/cactus.png` |
| `bonemeal` | Bonemeal | `items/material/bonemeal.png` |
| `rhododendron` | Rhododendron | `items/material/rhododendron.png` |
| `rose` | Rose | `items/material/rose.png` |
| `violet` | Violet | `items/material/violet.png` |
| `lava_stone` | Lava Stone | `items/material/lava_stone.png` |
| `leather` | Leather | `items/material/leather.png` |
| `cherry_sapling` | Cherry Sapling | `items/material/cherry_sapling.png` |
| `willow_sapling` | Willow Sapling | `items/material/willow_sapling.png` |
| `pine_sapling` | Pine Sapling | `items/material/pine_sapling.png` |
| `oak_sapling` | Oak Sapling | `items/material/oak_sapling.png` |
| `palm_sapling` | Palm Sapling | `items/material/palm_sapling.png` |
| `ruby` | Ruby | `items/material/ruby.png` |
| `emerald` | Emerald | `items/material/emerald.png` |
| `sapphire` | Sapphire | `items/material/sapphire.png` |
| `diamond` | Diamond | `items/material/diamond.png` |
| `obsidian_ore` | Obsidian Ore | `items/material/obsidian_ore.png` |
| `magic_gemstone` | Magic Gemstone | `items/material/magic_gemstone.png` |
| `void_stone` | Void Stone | `items/material/void_stone.png` |
| `gold_bar` | Gold Bar | `items/material/gold_bar.png` |

### Tools

Folder: `items/tool/`

| Tag | Display Name | Sprite Path |
|-----|-------------|-------------|
| `torch` | Torch | `items/tool/torch.png` |
| `wooden_shovel` | Wooden Shovel | `items/tool/wooden_shovel.png` |
| `wooden_pickaxe` | Wooden Pickaxe | `items/tool/wooden_pickaxe.png` |
| `fishing_rod` | Fishing Rod | `items/tool/fishing_rod.png` |
| `iron_pickaxe` | Iron Pickaxe | `items/tool/iron_pickaxe.png` |
| `iron_shovel` | Iron Shovel | `items/tool/iron_shovel.png` |
| `crucible` | Crucible | `items/tool/crucible.png` |
| `magic_fishing_rod` | Magic Fishing Rod | `items/tool/magic_fishing_rod.png` |
| `tripwire_trap` | Tripwire Trap | `items/tool/tripwire_trap.png` |
| `diamond_engraved_pickaxe` | Diamond Engraved Pickaxe | `items/tool/diamond_engraved_pickaxe.png` |
| `magic_shovel` | Magic Shovel | `items/tool/magic_shovel.png` |
| `legendary_forge` | Legendary Forge | `items/tool/legendary_forge.png` |

### Keys

Folder: `items/key/`

| Tag | Display Name | Sprite Path |
|-----|-------------|-------------|
| `bronze_key` | Bronze Key | `items/key/bronze_key.png` |
| `silver_key` | Silver Key | `items/key/silver_key.png` |
| `golden_key` | Golden Key | `items/key/golden_key.png` |
| `skeleton_key` | Skeleton Key | `items/key/skeleton_key.png` |
| `void_key` | Void Key | `items/key/void_key.png` |
| `opal_key` | Opal Key | `items/key/opal_key.png` |

### Ammo

Folder: `items/ammo/`

| Tag | Display Name | Sprite Path |
|-----|-------------|-------------|
| `arrow` | Arrow | `items/ammo/arrow.png` |
| `bolt` | Bolt | `items/ammo/bolt.png` |
| `heavy_bolt` | Heavy Bolt | `items/ammo/heavy_bolt.png` |
| `fire_arrow` | Fire Arrow | `items/ammo/fire_arrow.png` |
| `ice_arrow` | Ice Arrow | `items/ammo/ice_arrow.png` |
| `poison_arrow` | Poison Arrow | `items/ammo/poison_arrow.png` |
| `cannon_ball` | Cannon Ball | `items/ammo/cannon_ball.png` |
| `explosive_arrow` | Explosive Arrow | `items/ammo/explosive_arrow.png` |
| `explosive_bolt` | Explosive Bolt | `items/ammo/explosive_bolt.png` |

### Dyes

Folder: `items/dye/`

| Tag | Display Name | Sprite Path |
|-----|-------------|-------------|
| `white_dye` | White Dye | `items/dye/white_dye.png` |
| `black_dye` | Black Dye | `items/dye/black_dye.png` |
| `red_dye` | Red Dye | `items/dye/red_dye.png` |
| `green_dye` | Green Dye | `items/dye/green_dye.png` |
| `purple_dye` | Purple Dye | `items/dye/purple_dye.png` |
| `orange_dye` | Orange Dye | `items/dye/orange_dye.png` |
| `blue_dye` | Blue Dye | `items/dye/blue_dye.png` |

### Misc

Folder: `items/misc/`

| Tag | Display Name | Sprite Path |
|-----|-------------|-------------|
| `nametag` | Nametag | `items/misc/nametag.png` |
| `candle` | Candle | `items/misc/candle.png` |
| `marbles` | Marbles | `items/misc/marbles.png` |
| `journal` | Journal | `items/misc/journal.png` |
| `jack_o_lantern` | Jack-O-Lantern | `items/misc/jack_o_lantern.png` |
| `frog_egg` | Frog Egg | `items/misc/frog_egg.png` |
| `skull` | Skull | `items/misc/skull.png` |
| `music_disc` | Music Disc | `items/misc/music_disc.png` |
| `frog` | Frog | `items/misc/frog.png` |
| `saddle` | Saddle | `items/misc/saddle.png` |
| `ancient_pottery` | Ancient Pottery | `items/misc/ancient_pottery.png` |
| `bunny` | Bunny | `items/misc/bunny.png` |
| `treasure_map` | Treasure Map | `items/misc/treasure_map.png` |
| `mysterious_candle` | Mysterious Candle | `items/misc/mysterious_candle.png` |
| `ruby_skull` | Ruby Skull | `items/misc/ruby_skull.png` |
| `mirror_to_other_realms` | Mirror to Other Realms | `items/misc/mirror_to_other_realms.png` |
| `gramophone` | Gramophone | `items/misc/gramophone.png` |
| `red_dragon_egg` | Red Dragon Egg | `items/misc/red_dragon_egg.png` |
| `green_dragon_egg` | Green Dragon Egg | `items/misc/green_dragon_egg.png` |
| `blue_dragon_egg` | Blue Dragon Egg | `items/misc/blue_dragon_egg.png` |
| `black_dragon_egg` | Black Dragon Egg | `items/misc/black_dragon_egg.png` |
| `white_dragon_egg` | White Dragon Egg | `items/misc/white_dragon_egg.png` |

---

## Backgrounds

Folder: `backgrounds/`
Loaded by `RoomCanvasView` as `backgrounds/{biome_name}.png` or `.gif`.

| Biome Tag | Display Name | Region | Folder |
|-----------|-------------|--------|--------|
| `hub` | Desert Marketplace | 0 | `backgrounds/hub/` |
| `red_dungeon` | Red Dungeon | 1 | `backgrounds/red_dungeon/` |
| `volcanic` | Volcanic Waste | 2 | `backgrounds/volcanic/` |
| `meadow` | Meadow | 3 | `backgrounds/meadow/` |
| `forest` | Forest | 4 | `backgrounds/forest/` |
| `ocean` | Ocean | 5 | `backgrounds/ocean/` |
| `castle` | Castle | 6 | `backgrounds/castle/` |
| `ice_cave` | Ice Cave | 7 | `backgrounds/ice_cave/` |
| `void` | The Void | 8 | `backgrounds/void/` |

---

## Sounds

### Music

Folder: `sounds/music/`
Loaded by `SoundManager.playMusic(musicName)`.

> Placeholder directory — no audio files present yet.

### Sound Effects

Folder: `sounds/sfx/`
Loaded by `SoundManager.playSfx(sfxName)`.

> Placeholder directory — no audio files present yet.

### Ambient

Folder: `sounds/ambient/`

> Placeholder directory — no audio files present yet.

---

## Icons

| Category | Folder |
|----------|--------|
| Menu icons | `icons/menu/` |
| Room icons | `icons/rooms/` |
| UI icons | `icons/ui/` |

> Placeholder directories — no icon files present yet.

---

## Fonts

Folder: `fonts/`

> Placeholder directory — no font files present yet.

---

## Sprites (Additional)

Mirror/alternate sprite directories under `sprites/`:

| Category | Folder |
|----------|--------|
| Backgrounds | `sprites/backgrounds/` |
| Creatures | `sprites/creatures/` |
| Effects | `sprites/effects/` |
| Objects | `sprites/objects/` |
| UI | `sprites/ui/` |
| Items (all categories) | `sprites/items/{accessory,ammo,armor,clothing,dye,food,key,material,misc,potion,scroll,shield,tool,weapon}/` |

---

## Android Resources

Located in `app/src/main/res/`.

### Drawables (`res/drawable/`)

| Resource ID | File |
|------------|------|
| `R.drawable.bottom_bar_background` | `bottom_bar_background.xml` |
| `R.drawable.bottom_bar_button` | `bottom_bar_button.xml` |
| `R.drawable.input_background` | `input_background.xml` |
| `R.drawable.inventory_slot_background` | `inventory_slot_background.xml` |
| `R.drawable.inventory_slot_selected` | `inventory_slot_selected.xml` |
| `R.drawable.panel_background` | `panel_background.xml` |

### Layouts (`res/layout/`)

| Resource ID | File | Description |
|------------|------|-------------|
| `R.layout.activity_game` | `activity_game.xml` | Main game activity |
| `R.layout.activity_main` | `activity_main.xml` | Title/start screen |
| `R.layout.fragment_character` | `fragment_character.xml` | Character stats screen |
| `R.layout.fragment_combat` | `fragment_combat.xml` | Combat encounter screen |
| `R.layout.fragment_hub` | `fragment_hub.xml` | Hub world screen |
| `R.layout.fragment_inventory` | `fragment_inventory.xml` | Inventory management |
| `R.layout.fragment_map` | `fragment_map.xml` | World map view |
| `R.layout.fragment_note` | `fragment_note.xml` | Note/lore display |
| `R.layout.fragment_room` | `fragment_room.xml` | Room exploration |
| `R.layout.fragment_shop` | `fragment_shop.xml` | Shop interface |
| `R.layout.fragment_transfer` | `fragment_transfer.xml` | Item transfer screen |
| `R.layout.item_action` | `item_action.xml` | Action list item |
| `R.layout.item_inventory_slot` | `item_inventory_slot.xml` | Inventory slot item |
| `R.layout.item_note` | `item_note.xml` | Note list item |
| `R.layout.item_shop` | `item_shop.xml` | Shop list item |
| `R.layout.stat_row` | `stat_row.xml` | Character stat row |

### Values (`res/values/`)

| File | Contents |
|------|----------|
| `colors.xml` | Game color palette |
| `strings.xml` | UI string resources |
| `themes.xml` | App theme definitions |

### Launcher Icons (`res/mipmap-*/`)

| Asset | Densities |
|-------|-----------|
| `ic_launcher.webp` | hdpi, mdpi, xhdpi, xxhdpi, xxxhdpi |
| `ic_launcher_background.webp` | hdpi, mdpi, xhdpi, xxhdpi, xxxhdpi |
| `ic_launcher_foreground.webp` | hdpi, mdpi, xhdpi, xxhdpi, xxxhdpi |
| `ic_launcher_round.webp` | hdpi, mdpi, xhdpi, xxhdpi, xxxhdpi |
| `ic_launcher.xml` | anydpi-v26 (adaptive icon) |
| `ic_launcher_round.xml` | anydpi-v26 (adaptive icon) |

### XML Config (`res/xml/`)

| File | Purpose |
|------|---------|
| `backup_rules.xml` | Android backup configuration |
| `data_extraction_rules.xml` | Data extraction rules |

---

## Asset Loading Reference

| Manager Class | Loads From | Method |
|--------------|-----------|--------|
| `GameAssetManager` | `assets/*` | `loadSprite(path)`, `loadSpriteById(id, category)` |
| `SoundManager` | `assets/sounds/music/`, `assets/sounds/sfx/` | `playMusic(name)`, `playSfx(name)` |
| `ItemRegistry` | `assets/data/items.json` | `getItem(itemId)` |
| `CreatureRegistry` | `assets/data/creatures.json` | `getCreature(creatureId)` |

> **Note:** Most sprite directories currently contain only `.gitkeep` placeholder files.
> The app uses `PlaceholderRenderer` to draw procedural shapes with `placeholderColor`
> and `placeholderShape` fields when actual sprite files are missing.
