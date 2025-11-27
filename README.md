# NoDropBlocks

Plugin Spigot simple : supprime tous les drops des blocs listés dans `config.yml`.

## Fonctionnement

- Les blocs présents dans `disabled-blocks` **ne dropperont plus rien** :
  - quand un joueur les casse ;
  - quand ils sont détruits par une explosion (TNT, creeper, etc.).

## Installation

1. Compile le projet en `.jar` (Maven / IDE).
2. Dépose le `.jar` dans le dossier `plugins` de ton serveur.
3. Démarre le serveur pour générer la config.
4. Modifie `plugins/NoDropBlocks/config.yml` pour ajouter/retirer des blocs.
