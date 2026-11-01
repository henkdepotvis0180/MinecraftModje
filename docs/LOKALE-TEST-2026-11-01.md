# Lokale build en test

Dit beschrijft de eerste 0.1.0-build. Zie [de 0.2.0-resultaten](HELPERS-0.2.0.md)
voor de nieuwe materiaal- en voorraadfuncties met echte Minecraft-servertests.

Uitgevoerd op Windows, 1 november 2026.

## Geslaagd

- Volledige `gradle build liveChatTest` met JDK 21.0.11, Gradle 8.14.3,
  ModDevGradle 2.0.107 en NeoForge 21.1.172 voor Minecraft Java 1.21.1.
- `coreTest`: begrensde taken en hutblauwdruk met 96 unieke blokken.
- `chatTest`: HTTP-request, geheugen, taakvalidatie, fouten en timeout.
- `liveChatTest`: echte lokale Ollama `qwen2.5:3b`, gesprek zonder taak,
  opdracht voor precies 16 boomstammen en stoppen.
- Vier worldgen-tests met `py -m unittest discover -s tests -v`.
- `gradle runClient`: mod ontdekt en geladen, clientresources en configuratie
  geladen, OpenGL en geluid gestart zonder modlaadfout in het clientlog.

## Hersteld

- Windows PowerShell retourneerde de checksum als bytes; expliciete UTF-8-decodering.
- Gradle-download zonder de trage PowerShell-voortgangsweergave.
- NeoForge `BlockSnapshot.restore()` in plaats van de niet-bestaande oude overload.
- Expliciete Gson-testafhankelijkheid voor de Gradle-tests.
- Geldige JSON-voorbeelden in de modelinstructie: het echte model gaf aanvankelijk
  op een hakopdracht alleen antwoordtekst zonder uitvoerbare taak.

## Artifact

`helpers/build/libs/henk-helpers-0.1.0.jar`

## Openstaande speeltest

Computer Use weigert het Java-venster met `Computer Use was not approved to use java`.
Er is daarom geen NPC gespawned of bestuurd door de agent. Voer in een aparte
creatieve testwereld met cheats deze controles uit:

1. `/helper spawn`: NPC zichtbaar, geen crash.
2. Houd een bijl vast, `/helper give`, daarna `/helper chop 16` bij vrije bomen.
   Controleer padvinden, 16 gehakte logs, slijtage en `/helper collect`.
3. Geef 96 eikenplanken; `/helper build hut` met voldoende vrije ruimte vier
   blokken voor de speler. Controleer materiaalverbruik en een voltooide hut.
4. Controleer bezette bouwplaats, `/helper stop`, opslaan/herladen en voorraad.
5. `/helper ask hoi, wat kun je?` en `/helper ask hak 16 boomstammen voor me`.
   Test `/helper stop` en `/helper forget` terwijl een aanvraag onderweg is.
6. Test twee spelers en een dedicated server voor eigendom en client/servergedrag.

Bestaande wereldgeneratie is niet aangepast. Er zijn geen commits gepusht en
geen bestanden naar een bestaande Minecraft-installatie of speelwereld gekopieerd.
