# Henk Helpers 0.2.0: echte voorraad en materiaal ophalen

Een opdracht werd eerder afgewezen als de benodigde bijl of planken ontbraken.
Materiaal achteraf geven hervatte die opdracht niet. Er was bovendien geen echt
voorraadvenster of zichtbare terugkoppeling over de actuele materiaalstand.

De werker onthoudt nu een wachtende opdracht. Hij rust een bijl uit de rugzak uit,
loopt naar geschikte gevallen items of haalt ontbrekende bijlen/planken uit een
expliciet gekoppelde kist of vat, en werkt verder. Er is geen onbeperkte toegang
tot willekeurige kisten. De selectieknop koppelt het dichtstbijzijnde voorraadblok
en vermeldt de coördinaten. Een dubbele kist wordt per gekoppeld blokdeel behandeld.

Het voorraadmenu gebruikt de echte equipment- en rugzakslots met de standaard
Minecraft-overdracht en synchronisatie. Het linker slot accepteert alleen bijlen.
De werker wacht tijdens dit menu. Het gesprekspaneel toont gesynchroniseerde
werkstatus, plankvoorraad en daadwerkelijk uitgerust gereedschap.

De volledige huttest vond en hielp een echte bouwfout herstellen: de werker liep
op de plek waar hij een blok moest plaatsen. Hij kiest nu bereikbare werkposities
rondom het bouwwerk en loopt het laatste stukje nauwkeurig door om zichzelf
niet in te sluiten. De test laat hem echt 96 planken uit twee kiststacks ophalen,
96 hutblokken plaatsen en met nul resterende planken klaar zijn.

## Uitgevoerde controles

- `gradle build`: volledige compilatie, kernvalidatie en HTTP-integratietests.
- `gradle liveChatTest`: echte lokale Ollama `qwen2.5:3b`, gesprek/hak 16/stop.
- `gradle -PgameTests runGameTestServer`: zes tests in Minecraft/NeoForge;
  echte overdracht en NBT; shift-klik en volle rugzak; kist ophalen/hakken;
  lopend gronditems ophalen/hakken; volledige hutbouw; stoppen en eigendom.
- `py -m unittest discover -s tests -v`: vier tests geslaagd, worldgen ongewijzigd.
- GameTest-klassen en structuur staan apart onder `src/gameTest` en zitten niet
  in de gebruiks-JAR. Runlog: `helpers/build/game-tests-0.2.0.log`.

## Nog handmatig controleren

De nieuwe vensterlayout, zichtbare gereedschapsweergave en animatie zijn niet
visueel goedgekeurd. Computer Use kreeg geen toegang tot het Java-venster.
Verschillende terreinen, dubbele kisten, claimmods en uitgebreide multiplayer
blijven aanvullende speeltests. Geen automatisch crafting, hout tot planken
omzetten, onbeperkte logistiek of taakherstel na wereldherladen.

Sluit Minecraft, vervang de oude JAR door
`helpers/build/libs/henk-helpers-0.2.0.jar` en laat geen dubbele modversie staan.
Rechtsklik, kies Rugzak openen of Koppel kist/vat en geef een opdracht.

Gedragsinspiratie: [MineColonies](https://minecolonies.com/wiki/systems/request/)
en [NPC Logistics](https://github.com/DA-57EVE/NPC-Logistics/).
Geen broncode uit deze projecten overgenomen.
