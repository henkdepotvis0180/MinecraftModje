# Richting: Henk als fabrieksmanager

## Wereld eerst

De speler moet ruimte hebben voor een dorp, magazijn en fabrieken. De eerste
terreinversie combineert terrassen met ruige ruggen. Na echte seedtests kunnen
hoogtes, bereikbaarheid van hout en de verdeling van water worden afgesteld.
Biomen en ertsen blijven eerst vanilla om het begin toegankelijk te houden.

## Geleidelijke, uitdagende automatisering (ontwerp, nog niet geïmplementeerd)

| Stap | Machine | Kern/voorbeeld | Volgende kleine verbetering |
| --- | --- | --- | --- |
| 1 | Boomkweker | Sapling bepaalt houtsoort | Langzaam hout, water nodig |
| 2 | Steengenerator | Cobblestone als voorbeeld | Stabiele steen voor bouwmateriaal |
| 3 | Houtskooloven | Verwerkt eigen hout | Eerste brandstofproductielijn |
| 4 | IJzerextractor | Eerste gevonden ijzererts of ingot als kern | Lage opbrengst, brandstof en steenverbruik |
| 5 | Mechanische aandrijving | IJzeronderdelen | Snellere machines met onderhoud |
| 6 | Transport en opslag | Eigen productie | Minder handmatig sjouwen |
| 7 | Elektrische aandrijving | Opgebouwd machinepark | Efficiëntere productie per machine |

De kern is een blijvend machineslot: geen gratis oneindige grondstoffen door alleen
een ingot neer te leggen. Brandstof/energie, cyclustijd, opslagcapaciteit en
onderhoud bepalen de kosten. Eerst een trage maar bruikbare machine, daarna kleine
upgrades. Exacte recepten en getallen volgen na een speelbare eerste keten.

## NPC's als werkers (ontwerp, nog niet geïmplementeerd)

Begin vóór de machines met houthakker, drager en bouwer. Opdrachten zoals
“verzamel 32 eikenstammen en leg ze in deze kist” worden concrete taken.
Een taalmodel vertaalt later chat naar een beperkt takenformaat; Minecraft-code
controleert en voert de acties op de server uit. Navigatie, inventaris, bereik en
voortgang horen bij de gamecode. Het model mag geen willekeurige servercommando's
uitvoeren of items creëren. Werkers gebruiken bestaande gereedschappen en materialen.

Eerst dezelfde taken via een simpel commandomenu werkend maken; daarna chat eraan
koppelen. Provider/model, kostenlimieten en eventuele lokale inferentie zijn nog
open keuzes. Er is nu geen externe AI-service geconfigureerd of aangeroepen.
