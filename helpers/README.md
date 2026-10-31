# Henk Helpers — prototype voor NeoForge 1.21.1

**Status:** de Java-kern (AI-taakvalidatie en bouwplan) is gecompileerd en getest.
De volledige NeoForge-mod is **nog niet gecompileerd of in Minecraft getest**.
De cloudproxy blokkeert downloads van Gradle/NeoForge. Er is daarom nog geen
geverifieerde speelbare JAR. Dit is een broncodeprototype, geen datapack.

## Bouwen op Windows

1. Installeer een **JDK 21** (niet alleen een losse Java-runtime) en zorg dat
   `JAVA_HOME` naar die JDK wijst.
2. Open PowerShell in deze map en voer `./build-windows.ps1` uit.
   Als je lokale beleid scripts blokkeert, kun je de Gradle-stappen hieronder
   handmatig uitvoeren; verander geen algemeen systeembeleid voor dit project.
3. Het script haalt Gradle 8.14.3 op via HTTPS, controleert de officiële SHA-256,
   en voert `build` uit. Eerste build heeft internet nodig.
4. Alleen na een succesvolle build: zet `build/libs/henk-helpers-0.1.0.jar` in
   de map `mods` van een Minecraft **1.21.1 / NeoForge 21.1.172** testprofiel.
   De mod moet op zowel client als server staan.

Met een bestaande Gradle 8.14.3-installatie kan ook `gradle build` in `helpers/`.
`gradle runClient` start de ontwikkelclient. De versies zijn vastgezet maar konden
hier nog niet tegen de externe repositories worden gevalideerd.

## Ollama lokaal

Installeer Ollama via de officiële site `https://ollama.com` en haal een model op:

```text
ollama pull qwen2.5:3b
```

Laat Ollama draaien; start zo nodig `ollama serve` wanneer de desktop-app de
service niet al heeft gestart. De mod gebruikt uitsluitend
`http://127.0.0.1:11434/api/generate`. Geen API-sleutel nodig.
Het model kan na de eerste start worden aangepast in
`config/henkhelpers-common.toml` met `model="..."` en daarna een herstart.
`enableLocalAi=false` schakelt modelrequests uit.

In singleplayer is dit jouw computer. Bij een dedicated server moet Ollama op
**de servercomputer** draaien, niet op de pc van de speler. Ollama is in deze
cloudsessie niet geïnstalleerd of getest. Modeldownloads vragen schijfruimte;
snelheid hangt van RAM/GPU af. De eerste aanvraag kan door modelinitialisatie traag zijn.

## Eerste gebruik (na geslaagde build, nog te verifiëren)

Test eerst in een aparte creatieve wereld, met cheats voor het spawnen:

```text
/helper spawn
```

Je krijgt een eigen werker met voorlopig het uiterlijk van een villager.
Alle opdrachten kiezen je dichtstbijzijnde eigen hulpje binnen 64 blokken.
Alleen het spawnen vereist operatorrechten. Gewone villager-handel is niet de
bedoelde bediening; gebruik de opdrachten hieronder.

### Hout hakken

1. Houd een bijl vast en typ `/helper give` (neemt de bijl uit je inventaris).
2. Zet het hulpje bij bomen; typ `/helper chop 16`.
3. Of via het model: `/helper ask hak 16 boomstammen voor mij`.
4. Met `/helper status` vraag je voortgang op en met `/helper stop` stop je.
5. Na afloop haal je met `/helper collect` zijn voorwerpen op.

Het hulpje zoekt logs in een werkgebied van 16 blokken rond zijn startpunt,
van twee blokken eronder tot vijf erboven. Het moet daadwerkelijk naar de boom
kunnen lopen en binnen bereik komen. De bijl slijt; volle opslag laat de rest als
items vallen. Het plant nog geen saplings, klimt niet en kapt nog geen volledige
hoge boom. Ook geplaatste houtblokken kunnen worden geselecteerd: gebruik een
vrij boomgebied, weg van je gebouwen.

### Bouwen

1. Geef in totaal 96 eikenplanken met `/helper give` (meermaals indien nodig).
2. Kijk naar een vrije bouwplaats en typ `/helper build hut`.
3. Of: `/helper ask bouw een hut`.

De bouwplaats begint vier blokken in je kijkrichting vanaf je blokpositie, op
voethoogte. Het is een vaste 5×5×5 schil met vloer, dak en een deuropening;
geen inrichting, deuritem of verlichting. De vloer ligt op voethoogte, dus de
hut staat één blok verhoogd. De plaats moet vooraf vrij en bereikbaar zijn.
Bestaande blokken worden niet weggegraven; bestaande eikenplanken tellen als al
gebouwd. Geen vrijvormige gebouwen uit chat en nog geen blauwdruk-editor.

## Gedrag en beperkingen

- Modeluitvoer accepteert alleen `chop` (1–64), `build`, `stop` en `status`.
  Geen servercommando's of willekeurige code uit AI-antwoorden.
- Eén modelrequest tegelijk; timeout 90 seconden. Minecraft blijft asynchroon draaien.
- `/helper stop` maakt een laat AI-antwoord ongeldig. Een taakwijziging doet hetzelfde.
- Bij te grote afstand, offline eigenaar of 30 seconden zonder voortgang stopt de taak.
- Eigenaar en spullen worden opgeslagen via entity-NBT; taken hervatten niet na herladen.
- Geen conversatiegeheugen, kistenlogistiek, automatisch volgen, districtsbeheer of
  teamselectie. Dit is de eerste taakgestuurde werker.
- Spawn-/bouwrechten en break/place-events worden gecontroleerd. Integratie met
  specifieke claimmods is niet getest; gebruik voorlopig een eigen testwereld.
- Houtkap en bouwen hebben een eenvoudige werksnelheid; balans volgt na speeltests.

## Validatie

Offline uit de repositoryroot: `python3 tools/test_helpers_core.py`.
Dit test echte Java-code, maar alleen de twee dependency-vrije kernklassen.
`gradle build` voert ook deze kerntest uit via `coreTest`.

Nog vereist: volledige compilatie, clientstart, NPC-rendering, padvinden, bijlslijtage,
exacte materiaaloverdracht, blokkering van bezette bouwplaatsen, opslag/herladen,
Ollama-foutafhandeling, late antwoorden na stop, serverstart en twee spelers die
elkaars hulpje niet kunnen bedienen. Test handmatig eerst zonder Ollama; daarna
met `ollama serve` en het ingestelde model. Geen van deze runtimechecks is al geslaagd verklaard.
