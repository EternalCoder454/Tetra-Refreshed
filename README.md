# Tetra Refreshed

Tools and weapons you build from parts at a workbench, then improve, repair and reforge instead of
replacing.

![Minecraft](https://img.shields.io/badge/minecraft-26.1.2-brightgreen.svg)
![Loader](https://img.shields.io/badge/loader-NeoForge-orange.svg)
![License](https://img.shields.io/badge/license-see%20below-blue.svg)
[![](http://cf.way2muchnoise.eu/tetra.svg)](https://minecraft.curseforge.com/projects/tetra)

## ⚒️ About

A fork of [Tetra](https://github.com/mickelus/tetra) by Mikael Eriksson Vikner, ported to Minecraft
26.1.2 on NeoForge and Java 25. Tetra is Mikael's work. This fork is a port and nothing more, and
carries no claim of authorship over the mod. Port by EternalHell.

| | |
|---|---|
| Base | PR 931, "Port to 1.21.1 neoforge" by evelant, on top of upstream 1.20 |
| Target | Minecraft 26.1.2, NeoForge 26.1.2.95, Java 25 |
| Branch | `26.1.2` |
| Dependency | [Mutil Refreshed](https://github.com/EternalCoder454/Mutil-Refreshed), published to mavenLocal |

## 📦 Installing

Drop `tetra-*.jar` into `mods/` alongside **Mutil Refreshed**, which is required.

Two addons ship **inside this jar** and load as their own mods, so you do not install them
separately and should not put their jars in `mods/` as well:

| Bundled | By |
|---|---|
| Secrets of Forging: Revelations | AceTheEldritchKing, updated by GamerK_2 |
| Art of Forging | AceTheEldritchKing and MindFaer |

**Java 25** is required, which is what NeoForge 26.1 runs on anyway.

## 📝 Credit and permission

**Tetra is Mikael Eriksson Vikner's.** Everything this repository adds is the port.

**Secrets of Forging: Revelations** and **Art of Forging** are by
[AceTheEldritchKing](https://github.com/AceTheEldritchKing), updated by GamerK_2, and are bundled
here with his permission, given on Discord on 2026-08-18 and recorded verbatim in
[dev-permission.md](dev-permission.md). They stay his work and his projects. He
asked for them to be included as separate projects rather than absorbed, so they ship as jarJar
bundles and each loads as its own mod with its own id, the way Create bundles Flywheel.

## 🏷️ License and use

The upstream terms below apply to this fork unchanged, including the restrictions on the Perk
system and on redistribution. They are reproduced word for word. Read them before using anything
here.

The points below outline what you can, cannot and must do when dealing with the
contents of this repository.

### You CAN
 * Fork and modify the code.
 * Submit Pull Requests to this repository.
 * Copy portions of this code for use in other projects.
 * Write your own code that uses this code as a dependency. (addons, mod
   integration or datapacks)

### You CANNOT
 * Change, use or remove content or assets related to the Perk system.
 * Redistribute this in its entirety as source or compiled code.
 * Create or distribute code which contains 50% or more Functionally Equivalent
Statements* from this repository.

### You MUST
 * Not be a dick. Treat others, and the work of others, with kindness,
   humility and respect. If you can't do that then you can't use anything
   from this repository.
 * Check in on discord before overwriting existing content.

### Notes, License & Use
*A Functionally Equivalent Statement is a code fragment which, regardless of
whitespace and object names, achieves the same result within the context
of a Minecraft mod or addon.

Since this project uses Mojang mappings and most assets are heavily based on
Minecraft assets it's probably a very bad idea to use anything from this
repository outside the context of Minecraft modding.

Essentially, look at this and learn from it. Create addon mods that expand
upon it, hook up your own mods to integrate with it or tweak how it works
for your modpack.
If you see something that can be improved, please create an issue, discuss
it on discord or submit a pull request.
A lot of effort has gone into making this, do not take significant parts of
this and pass it off as your own.

©2018-2024 Mikael Eriksson Vikner

## 🤝 Contributing upstream

Source and issue tracker for tetra are upstream. Mikael's own words follow.

If you're interested in contributing to the project there is lots of work that
does not require any artistic skill or experience with development, I'd be
super happy if I could get more help with the following:
 * Help to validate or improve the "How to reproduce steps" in bug issues.
 * Look at incoming issues and compare them to existing issues to see if
   they are duplicates.
 * Help to clarify feature requests so that it is clear how/when they would
   be considered done.
 * Check out advancements and descriptions for new features and let us know
   (preferably on discord) if that helped you understand how it works
 * Compare balancing between different types of content and mechanics to

If you're a developer or if you're good with pixel art and would like to
contribute then look for the [help appreciated](https://github.com/mickelus/tetra/labels/help%20appreciated) label on issues, or hop onto
discord and wave if there are none!

By contributing code or assets this repository you grant the project full rights
over your contributions. If you do not agree with that, do not contribute.

## 💻 For developers

| File | Covers |
|---|---|
| [DEV.md](DEV.md) | building, running, and the data formats |
| [PORT-STATUS.md](PORT-STATUS.md) | the port handover, what changed in behaviour on the way to 26.1.2, and what is knowingly still missing |
| [CURRENTPLANS.md](CURRENTPLANS.md) | where the project is going after the port |
| [PLAYTESTING.md](PLAYTESTING.md) | a checklist for testing a build |
