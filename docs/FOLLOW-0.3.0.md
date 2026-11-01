# Volgen op verzoek: 0.3.0

Onderzoeksvraag: hoe volgt een werker zijn eigenaar met een stabiele tussenafstand?

Vanilla-referentie: Minecraft 1.21.1 `FollowOwnerGoal` uit de lokale bronnen.
Het relevante patroon is twee afstandsdrempels plus periodieke padherberekening.
De vanilla-teleportfunctie wordt niet gebruikt.

Externe referentie: [neoforge-companion-mod](https://github.com/anon923882/neoforge-companion-mod),
commit `26dccd668d0e516405018775353f484cd80cd263`, MIT-licentie.
`gradle.properties` vermeldt Minecraft 1.21.1 en NeoForge 21.1.209.
De goalregistratie gebruikt hetzelfde patroon maar een verouderde constructorvorm.
Geen code of assets overgenomen; geen derdepartijcodecredits vereist.
De referentieclone staat buiten de repository in `../_reference/`.

Onze implementatie gebruikt de bestaande servertaakbesturing, gesynchroniseerde
volgmodus en afstandspreset. Geen extra dependencies, mixins of chunkloaders.
Volgen vervangt werken; nieuwe werkzaamheden zetten volgen uit. De keuze blijft
in NBT bewaard. Offline/dode/spectator-eigenaar, andere wereld en afstand boven
64 blokken laten de NPC wachten. Een geblokkeerde route wordt gemeld en opnieuw
geprobeerd. De NPC loopt en teleporteert niet.

Verificatie: volledige build met kern- en HTTP-tests; alle negen Minecraft-
servertests geslaagd. Drie nieuwe tests controleren opt-in, echte verplaatsing,
4/2-hysterese, blijven staan, presets, NBT, eigendom, bereik, offline eigenaar
en hervatten na het openen van een doorgang. Log: `helpers/build/game-tests-follow.log`.

Nog handmatig: de knoppen en layout bekijken in de client. Rechtsklik, kies
Volg mij, sluit het venster en loop meer dan vier blokken weg. Wacht tot hij
binnen twee blokken stopt, probeer Blijf hier en wissel de afstandspreset.
