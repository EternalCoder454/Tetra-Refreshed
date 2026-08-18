# Permission

The record of AceTheEldritchKing granting permission for **Secrets of Forging: Revelations** and
**Art of Forging** to be ported and included in Tetra Refreshed.

Discord, **2026-08-18**. Transcribed verbatim, including typos. Nothing is paraphrased, and nothing
has been left out of the exchange.

---

**EternalHell**, 10:26 AM

> Hi @Ace [Eldritch Eden] 1.21

> I wanted to ask, is it fine to merge your Secrets of Forging: Revelations into a forked Tetra? The
> only rename is Tetra Refreshed. But I dont know if you want to keep Secrets of Forging naming. On
> the github if you do accept I'll put credits in the README

> Same for the Art of Forging addon

**Ace [Eldritch Eden] 1.21**, 10:56 AM, replying to the above

> I'm a little confused, what do you mean of a fork of Tetra?

**EternalHell**, 11:04 AM

> Sorry for the confusion, let me try to explain.

> I made my own fork of the Tetra mod which ports to 26.1.2 neoforge, renamed to Tetra Refreshed.
> What I'm asking is if I can merge your Secrets of Forging: Revelations addon (and Art of Forging)
> so they work with/inside that forked version.

> If you're fine with it, I'll keep your addon names as-is unless you'd rather I rename them too.
> Either way I'll credit you in the GitHub README. *(edited)*

**Ace [Eldritch Eden] 1.21**, 11:30 AM, replying to the above

> Sure, I don't think i have an issue with it. If you could have them be separate projects but
> included in the mod that would be cool (like how Flywheel is in Create now as a jarjar file iirc)

> Also add me as a contributor to the project as well along with the README credit

---

## What was agreed

Permission was given for both mods, with two conditions attached to it.

1. **Separate projects included in the mod**, rather than flattened into Tetra's source tree. Ace
   named how Create bundles Flywheel as a jarjar file as the shape he had in mind.
2. **Credited as a contributor** on the project, as well as in the README.

## How each condition is met

**Condition 1.** Secrets of Forging stays its own repository and its own mod id. It publishes to
mavenLocal and Tetra embeds it with jarJar, so NeoForge loads it from inside Tetra's jar and lists
it as its own entry:

```
secrets_of_forging_revelations (jar(mods/tetra-26.1.2-6.13.0.jar > secrets_of_forging_revelations-26.1.2-1.3.5.jar))
```

Nothing of either mod lives in Tetra's source tree. Everything they add to Tetra's screens is
reached from their own side, through data under `assets/tetra` and through registration points
Tetra offers.

Art of Forging is ported and builds, and is installed alongside rather than bundled while it has
had no play testing at all. Bundling an untested addon into Tetra would put Tetra's own loading at
risk. It gets the same treatment once it has been run.

**Condition 2.** Ace is credited in the README of each repository, named in `mod_authors` and
`mod_credits` so the in game mod list shows him, and is to be added as a repository collaborator on
GitHub. MindFaer is credited the same way, being the second author named in Art of Forging's own
`mod_authors`.

## What this permission does not cover

It covers porting these mods to 26.1.2 and including them in Tetra Refreshed. It is not a transfer
of ownership and not a licence change. **Both mods remain Ace's work and his projects.**

Secrets of Forging states its own terms in its README rather than a licence file. Art of Forging has
no licence file, though its `gradle.properties` declares MIT.

Tetra itself is a separate question, put to Mikael Eriksson Vikner, and is not covered here.
