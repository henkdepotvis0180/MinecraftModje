# MinecraftModje — Henk Frontier

Een technologie- en automatiseringspack in wording voor **Minecraft Java 1.21.1 + NeoForge**.
De eerste implementatie is eigen wereldgeneratie als datapack. Dit datapack gebruikt de
Minecraft-registries en heeft zelf geen modloader of Java-mod nodig. Het volledige
modpack en de machines zijn nog in ontwikkeling. De NPC-mod **Henk Helpers 0.3.0**
is nu gebouwd voor **NeoForge 21.1.172**: echte voorraad, materiaal ophalen,
houtkap, hutbouw en lokale Ollama-chat. Zie [installatie en tests](helpers/README.md).

## Eerste wereld: Henk Frontier

- Brede terrassen voor een nederzetting en latere productielijnen.
- Langgerekte bergkammen voor hoogteverschillen en transportroutes.
- Lage gebieden kunnen onder water liggen; waterniveau blijft 63.
- Eigen ondergrondse holtes tussen y=-48 en y=48.
- Vanilla biomen bepalen vegetatie, ertsen en mogelijke structuren.
- De Nether en End gebruiken hun standaardgenerator.

Dit is een **experimentele terreininstelling**, nog geen visueel geteste wereld.
Het terrein en de biomen worden onafhankelijk bemonsterd: rivierbiomen zijn dus
nog geen gegarandeerd doorlopend rivierennetwerk. Er is geen gegarandeerd vlakke
of beboste spawn. Het ondergrondse gesteente is voorlopig steen; aquifers en
extra grote ertsaders staan uit, normale biome-ertsfeatures blijven beschikbaar.

## Bouwen en controleren

Python 3, zonder externe packages:

```sh
python3 -m unittest discover -s tests -v
python3 tools/build_worldgen.py
```

Uitvoer:

- `dist/henk-frontier-worldgen-0.1.0.zip` — datapack.
- `dist/henk-frontier-names-0.1.0.zip` — optioneel resourcepack voor de naam in het menu.

## Installeren op Minecraft 1.21.1 met NeoForge

1. Maak een 1.21.1 NeoForge-profiel in je launcher; Java 21 is vereist.
2. Zet het optionele namen-resourcepack in `resourcepacks` en activeer het.
3. Kies **Nieuwe wereld → Meer → Datapacks**, voeg de worldgen-zip toe en activeer hem.
4. Ga terug en selecteer wereldtype **Henk Frontier**. Zonder het resourcepack
   kan de naam als `generator.henk.frontier` verschijnen.
5. Maak een nieuwe creatieve testwereld. Een waarschuwing over experimentele
   wereldinstellingen kan verschijnen.

Voor een dedicated server: plaats het datapack vóór de eerste wereldcreatie in
`<level-name>/datapacks/` en stel `level-type=henk:frontier` in `server.properties` in.
Gebruik een nieuwe wereldmap. Test deze serverroute nog afzonderlijk.

Dit datapack verandert niet automatisch het standaardwereldtype en is niet bedoeld
om in een bestaande save te wisselen. Bewaar het pack bij de wereld zolang je die
gebruikt. Zie [handmatige acceptatietest](docs/acceptance.md) voor runtimecontroles.

## Ontwerprichting

Zie [het spelontwerp](docs/design.md). Machines en NPC-besturing zijn volgende
ontwikkelstappen. De huidige zip is een datapack, geen complete launcher-modpackexport.

## AI-hulpjes met lokale Ollama

In [`helpers/`](helpers/README.md) staat het eerste **broncodeprototype** van een
NeoForge-mod voor houtkap, een vast hutbouwplan en taken via een lokaal Ollama-model.
Dit is nog geen gecompileerde of in Minecraft geteste mod. De bestaande worldgen
blijft apart bruikbaar en is niet gewijzigd. Zie de helper-README voor bouwstappen,
commando's en de huidige beperkingen.
