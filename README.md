# banking-distributed-systems-lab

Laboratoire pédagogique minimal pour apprendre Kafka et les systèmes distribués dans un contexte bancaire simplifié.

Le laboratoire permettra progressivement d'expérimenter les scénarios suivants :

- ouverture de compte ;
- dépôt d'argent ;
- retrait ;
- virement ;
- notifications ;
- audit ;
- traitement asynchrone ;
- événements métier.

Ces fonctionnalités constituent uniquement la future roadmap pédagogique et ne sont pas encore implémentées.

## Prérequis

- Java 21
- Maven
- Docker avec Docker Compose

## Démarrer Kafka

```bash
docker compose up -d
```

Kafka est alors accessible sur `localhost:9092`.

Pour l'arrêter :

```bash
docker compose down
```

## Démarrer Spring Boot

```bash
mvn spring-boot:run
```
