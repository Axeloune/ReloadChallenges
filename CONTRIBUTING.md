# Contributing

Merci de vouloir contribuer à ReloadChallenges.

## Pré-requis

- JDK 25 ou plus récent
- Maven 3.9+
- Un serveur Paper compatible `26.2`

## Build local

```bash
mvn clean package
```

Le fichier généré se trouve dans `target/reload-challenges-1.0.0-SNAPSHOT.jar`.

## Règles de contribution

- Garder le code séparé par responsabilité : game, UI, map, player, listeners.
- Ne pas utiliser de NMS si une API Bukkit/Paper moderne suffit.
- Vérifier que le plugin compile avant d'ouvrir une pull request.
- Tester les changements de gameplay sur un serveur Paper quand ils touchent aux événements Minecraft.
- Garder les textes visibles en français avec accents.

## Pull requests

Une PR devrait contenir :

- le problème résolu ;
- la solution apportée ;
- les tests effectués ;
- les limites connues, si besoin.
