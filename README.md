# ReloadChallenges

Plugin Paper 26.2 proposant plusieurs mini-jeux jouables sur une même carte avec hôte, lobby spectateur, GUIs, scoreboards, boss bars, titles, action bars, stats, FFA/équipes et reset par nouvelle instance de monde.

## Build

Paper 26.x demande un JDK 25 ou plus récent.

```bash
mvn clean package
```

Le jar sort dans `target/reload-challenges-1.0.0-SNAPSHOT.jar`.

## Commandes

- `/host` ou `/h` : aide hôte
- `/h config` : GUI de configuration
- `/h start` : lancer la partie
- `/h stop` : arrêter la partie
- `/h game <random|find|where_block|craft|speedrun|where_biome|mob_hunt|bingo>` : choisir le mini-jeu
- `/h mode <ffa|teams>` : choisir le mode
- `/h reload` : recharger la configuration

Le premier joueur connecté devient automatiquement hôte.

## Contribution

Les règles de contribution sont dans [CONTRIBUTING.md](CONTRIBUTING.md).

Les changements notables sont suivis dans [CHANGELOG.md](CHANGELOG.md).

## Licence

Ce projet est distribué sous licence MIT. Voir [LICENSE](LICENSE).

## Crédits

Plugin développé en grande partie à l'aide de Codex, sur des idées personnelles et des inspirations de mini-jeux existants.
