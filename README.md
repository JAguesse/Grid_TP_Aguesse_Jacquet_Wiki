# Grid_TP_Aguesse_Jacquet_Wiki
Justin Aguesse, Pierre Jacquet

Cette architecture expose un service REST de création, lecture et modification d'article. Le service est écrit en Scala et est exposé par défaut sur le port 9000.
Les articles sont stockés dans MongoDB, tout changement de leur état (i.e création/modification) entraîne la publication d'un évènement Kafka contenant la commande Mongoshell a passé pour obtenir le même contexte.

## Endpoint

Endpoint du service :
```bash
Lecture en liste     : GET  /v1/articles
Lecture spécifique   : GET  /v1/articles{id}
Création article     : POST /v1/articles
Modification article : PUT  /v1/articles/{id}
```

Exemple de body :
```bash
{
    "title":"Perruche",
    "body":"Perruche est un nom vernaculaire donné à plusieurs lignées d'oiseaux appartenant à l'Ordre des psittaciformes (en langue vernaculaire, perroquets)"
}
```