ZombieMod
==========

28 Dead Resident Shaun's of Evil Later.

You want zombies?  You got zombies!  Undead configured - your way!


Edit and add new zombie types to the /zombiemod/genea/ folder :)


### Configuration

    debugMode: false
Enable extra debugging messages in server logs.

    blocknaturalspawns: true
All non Zombie spawns of spawntype NATURAL will be cancelled.

    dayspawner: true
Enables DarkLust's DaySpawner Code.

    chunklimit: 4
Zombie Limit per loaded chunk for DaySpawner
 
    zombiespawnratio: 50
Ratio for zombie Spawning.

    spawnmultiplier: 0
Multiply number of zombies spawned.

    givezombieplayeritems: true
Zombies spawned on player death will carry players items, false causes items to drop normally.

    allowedbreaks: [STONE, DIRT, GRASS, GRAVEL, GLASS, THIN_GLASS, MYCEL, SAND, CACTUS, CLAY, LEAVES, LOG, SOUL_SAND, WOOD, TORCH, SPONGE]
List of material types that melee zombies can break.  Used in DarkLust's BlockBreaker


### Commands

	/zombiemod reload
Reloads current configuration.

	/zombiemod stats
Lists all current ZombieMod zombies and PlayerZombies to the console

### Permissions

    zombiemod.reload
Allows user to reload config.

    zombiemod.stats
Allows user to show stats.

### Changelog

1.0:  First release.


To Do
=====
Think up more zombie types and abilities.


Known Bugs/Conflicts
====================
Player Zombies are lost on server restart.  Needs Persistance.

Skins code flaky - working with SpoutDevs on this.


