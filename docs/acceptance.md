# Acceptatie van wereldgeneratie

## Offline gecontroleerd

`python3 -m unittest discover -s tests -v` controleert JSON-integriteit, lokale
registryverwijzingen, wereldpreset/dimensies, packversies en reproduceerbare ZIPs
met `pack.mcmeta` aan de root. Dit vervangt geen Minecraft registry-decoding.

## Nog in Minecraft 1.21.1 + NeoForge te testen

1. Activeer beide packs volgens de README; selecteer Henk Frontier.
2. Maak creatieve werelden met seeds `180`, `2026` en `8675309`.
3. Controleer `latest.log` op registry-/codec-/featurefouten. Geen errors toegestaan.
4. Controleer via `/datapack list enabled` dat het datapack geladen is.
5. Bekijk per seed spawn en terrein binnen circa 1.000 blokken: terrassen,
   bergkammen, bomen, water, onbedoelde zwevende massa's en spawnveiligheid.
6. Zoek een bruikbaar startgebied met hout, steen en bereikbaar ijzer. Noteer de
   coördinaten en beoordeel of een fabriek van 64×64 haalbaar is met beperkt egaliseren.
7. Controleer ondergronds ertsen en holtes; controleer bedrock onderaan.
8. Test Nether en End, sla op, sluit en open de wereld opnieuw met het pack aanwezig.
9. Herhaal wereldcreatie op een dedicated NeoForge-server volgens de README.

Deze runtime- en visuele controles zijn nog niet uitgevoerd. De cloudproxy was
onbereikbaar, waardoor serverbestanden en een NeoForge-runtime niet konden worden
opgehaald. Geen claims over spawnkwaliteit, terreinvorm of modcompatibiliteit vóór
het uitvoeren van deze controles.
