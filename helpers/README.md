# Henk Helpers 0.3.0

Minecraft Java **1.21.1**, **NeoForge 21.1.172**, **Java 21**.
Echte NPC-voorraad, gereedschap uitrusten, materiaal ophalen, houtkap, hutbouw en lokale AI-chat.
De wereldgeneratie is niet aangepast.

## Installeren

Sluit Minecraft en vervang de oude mod door `build/libs/henk-helpers-0.3.0.jar`.
Laat niet twee Henk Helpers-versies tegelijk in `mods` staan. Gebruik dezelfde versie
op client en server. Test eerst in een aparte wereld.

Spawn met `/helper spawn` (operatorrechten). Rechtsklik daarna op je hulpje.
Het paneel **Werk & voorraad** toont zijn echte status, bijl, plankvoorraad en kistkoppeling.
Typ een vraag of opdracht en druk op Enter. Esc sluit het gesprek.
Berichten aan het model worden niet als openbare spelerschat verstuurd.

## Volgen op verzoek

Klik **Volg mij** om meelopen aan te zetten, **Blijf hier** om het uit te zetten.
Ook `volg mij`, `volg me`, `kom mee`, `blijf hier` en `stop met volgen` in het
hulpje-venster werken direct zonder Ollama. Commando's: `/helper follow`, `/helper stay`.

**Afstand** wisselt tussen **4/2**, **6/3** en **10/4 blokken**: starten boven
het eerste getal, stoppen binnen het tweede. Standaard begint hij boven vier
blokken te lopen en stopt binnen twee. Nadat hij aankomt, blijft hij rustig
staan zolang je binnen vier blokken blijft. Afstanden zijn driedimensionaal.

Maximaal **64 blokken**, in dezelfde wereld en zolang de werker geladen blijft.
Daarbuiten of bij een offline eigenaar wacht hij; kom terug binnen bereik.
Hij loopt via padvinden en teleporteert niet. Na circa dertig seconden zonder
beweging meldt hij een geblokkeerde route en blijft die opnieuw proberen.
Hij kan niet vliegen, door muren lopen of automatisch portalen gebruiken.

Volgen vervangt de huidige werkopdracht; een nieuwe hak-/bouwopdracht zet volgen
uit. **Stop taak** stopt beide. Tijdens het bekijken van de rugzak wacht hij.
Volgmodus en afstand blijven bij opslaan/herladen bewaard; standaard staat
volgen uit. Dit vergroot de bomenzoekradius nog niet.

Drie extra Minecraft-servertests slagen voor opt-in, werkelijk lopen, de 4/2-gap,
blijven staan, afstandspresets, NBT, eigendom, bereik, offline eigenaar en hervatten
na een geopende doorgang. Samen zijn alle **negen GameTests** geslaagd.
Runlog: `helpers/build/game-tests-follow.log`. De nieuwe knoppen en layout zijn
gecompileerd maar nog niet visueel in de client gecontroleerd.

## Spullen en opdrachten

**Rugzak openen** opent echte itemslots. Het eerste slot links is de uitgeruste bijl;
de acht slots rechts zijn zijn opgeslagen rugzak. Sleep items vanuit je eigen inventaris
of gebruik shift-klik. Een bijl in de rugzak wordt automatisch uitgerust voor houtkap.
De standaard villager-renderer toont het werkelijk uitgeruste gereedschap.
Tijdens het bekijken van zijn rugzak wacht deze werker; sluit het venster om door te werken.
Voorraad en gereedschap blijven bewaard bij opslaan/herladen.

**Koppel kist/vat** koppelt het dichtstbijzijnde geladen voorraadblok binnen acht blokken
horizontaal en drie verticaal van de speler. Controleer de getoonde coördinaten als er
meerdere kisten staan. Een dubbele kist levert alleen uit het gekoppelde blokdeel.
Stop er een bijl en/of eikenplanken in.

Geef bijvoorbeeld de opdracht **hak 16 boomstammen** of **bouw een hut**.
Een opdracht blijft nu actief als materiaal ontbreekt. Hij zoekt eerst passende items
op de grond binnen zestien blokken horizontaal en vier verticaal van zichzelf, daarna
in de gekoppelde kist binnen 32 blokken. Hij loopt naar het materiaal, neemt echte items
mee en hervat het werk. Items die voor een andere speler gereserveerd zijn laat hij liggen.

Zonder benodigdheden wacht hij: vul zijn rugzak of kist, of leg een bijl/planken op de grond.
Je hoeft de opdracht daarna niet opnieuw te geven. Een volle rugzak wordt gemeld.
**Stop taak** stopt direct, ook tijdens materiaal ophalen of wachten.
**Haal spullen op** geeft de echte voorraad terug voor zover je ruimte hebt; stop eerst de taak.
**Wis gesprek** wist het korte modelgeheugen en maakt een lopend antwoord ongeldig.
De zichtbare chatregels worden daarbij niet gewist.

`/helper give` blijft beschikbaar. Met een bijl of eikenplanken in de hoofdhand verplaatst
het die echt naar de NPC; zonder geschikt voorwerp opent het nu de rugzak. De melding
vermeldt het werkelijk overgedragen aantal, ook bij een volle rugzak.

## Grenzen

- Houtkap: 1-64 logs binnen 16 blokken van het werkanker, twee blokken eronder tot vijf
  erboven. Echte drops en bijlslijtage. Geplaatste logs tellen ook: werk weg van gebouwen.
  Nog geen volledige hoge bomen vellen, herplanten of klimmen.
- Hut: 5x5x5 schil met vloer, dak en open deuropening, 96 eikenplanken. Het anker begint
  vier blokken voor de speler op voethoogte. De werker bouwt vanaf bereikbare posities
  rondom het bouwwerk. Geen deuritem, inrichting of vrijvormige bouwplannen.
- Bestaande blokken worden niet weggegraven. Een onbereikbaar doel stopt de taak na
  ongeveer dertig seconden zonder voortgang. De eigenaar moet in dezelfde wereld
  binnen 64 blokken blijven. Vensterbediening en rugzaktoegang vereisen eigendom en
  maximaal acht blokken afstand.
- De kist levert bestaande bijlen en planken; nog geen automatisch crafting of logs
  tot planken verwerken, kistenroutes, afleveren of machines.
- Taken hervatten niet na wereldherladen; voorraad en kistpositie blijven bewaard.
- Henkhulp maakt geen villagergeluiden. Claimmod-integratie is niet geverifieerd.

## Lokale AI

Laat Ollama draaien met `qwen2.5:3b` (`ollama pull qwen2.5:3b`).
De mod gebruikt `http://127.0.0.1:11434/api/chat`, zonder betaalde API of sleutel.
Op een dedicated server moet Ollama op die servercomputer draaien.
Wijzig het model in `config/henkhelpers-common.toml`; `enableLocalAi=false` schakelt AI uit.

Alleen gesprekken via het hulpje of `/helper ask` gaan naar het model.
Het krijgt echte werkstatus, bijl en plankvoorraad mee. Alleen begrensde chop/build/stop/status
zijn toegestaan. Modeltekst wordt nooit als willekeurig servercommando uitgevoerd.
De game valideert eigendom, afstand en materialen; het model kan zich vergissen.

## Bouwen en testen

Vanuit de projectroot:

```powershell
powershell -NoProfile -ExecutionPolicy Bypass -File .\helpers\build-windows.ps1
```

Het script haalt Gradle 8.14.3 op, controleert de officiële SHA-256 en behoudt TLS-controle.
Met de lokale Gradle vanuit `helpers`:

```powershell
.\.tools\gradle-8.14.3\bin\gradle.bat build
.\.tools\gradle-8.14.3\bin\gradle.bat liveChatTest
.\.tools\gradle-8.14.3\bin\gradle.bat -PgameTests runGameTestServer
```

`build` voert `coreTest` en `chatTest` uit. `liveChatTest` vereist echte lokale Ollama en het model.
GameTests gebruiken een aparte testwereld en `src/gameTest`; deze klassen en structuur zitten
niet in de mod-JAR.

Werkelijk geslaagd op 1 november 2026:

- Volledige modbuild, Java-kern- en HTTP-tests.
- Echte Ollama-chat: gesprek, hak precies 16 logs, stoppen.
- Zes tests in Minecraft/NeoForge: overdracht en save/load; shift-klik/gereedschapslot/volle
  rugzak; wachten op een bijl in een kist en daarna hakken; naar een gevallen bijl lopen en
  hakken; 96 planken uit een kist halen en een volledige hut bouwen; eigendom en stoppen.
- Vier wereldgeneratietests; de eerdere ontwikkelclient laadde de mod zonder laadfout.

De vernieuwde vensterindeling en zichtbare animatie zijn nog niet visueel gecontroleerd:
Computer Use weigert het Java-venster. De servertests controleren echte blokken, materiaal,
slijtage en taakresultaten, maar bewijzen niet elk terrein of alle multiplayer-situaties.

Gedragsinspiratie: [MineColonies](https://minecolonies.com/wiki/systems/request/) en
[NPC Logistics](https://github.com/DA-57EVE/NPC-Logistics/). Geen code uit die projecten overgenomen.
