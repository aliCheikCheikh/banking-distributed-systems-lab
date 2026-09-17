# Banking Distributed Systems Lab

Laboratoire Java 21 / Spring Boot consacré à Kafka et aux systèmes distribués dans un contexte bancaire simplifié.

## Pourquoi ce projet

Mon projet précédent, [Auto Stock Management](https://autostockapp.com), est un monolithe modulaire : un seul processus, une seule base, une transaction qui couvre l'ensemble d'une opération. Dans ce cadre, la cohérence est presque gratuite.

Ce laboratoire existe pour travailler ce qui se passe quand ce cadre disparaît. Deux processus distincts, un réseau entre les deux, un broker qui garantit la livraison *au moins une fois* — donc des doublons — et plus aucune transaction commune. Les questions changent de nature : que fait le consumer quand il reçoit deux fois le même événement ? Que devient l'événement si l'effet métier échoue après les tentatives de retry ? Comment rejouer un traitement sans corrompre l'état déjà écrit ?

Le domaine bancaire est volontairement minuscule — ouvrir un compte, envoyer une notification de bienvenue. L'intérêt n'est pas le métier mais la mécanique autour : un domaine trivial et une infrastructure comprise en profondeur valent mieux que l'inverse.

C'est enfin une préparation directe à mon M2 Architectures Logicielles (Nantes Université), où les systèmes distribués, le middleware et les architectures réparties occupent une large part du programme.

La section [NEXT LEARNING STEP](#next-learning-step) en fin de README liste ce qui n'est **pas** implémenté. Cette liste fait partie du projet : elle délimite ce qui a réellement été traité ici de ce qui reste à apprendre.

## Architecture actuelle

```text
Client
  |
  | HTTP POST /accounts
  v
Account Service (port 8080)
  |
  | BankAccountOpened
  v
Kafka: account-events (3 partitions)
  |
  v
Notification Service (consumer group notification-service)
  |
  | transaction locale PostgreSQL
  v
Notification PostgreSQL
  |- processed_event
  `- notification
```

Account et Notification sont deux applications Spring Boot et deux processus JVM indépendants. Le flux `BankAccountOpened` passe uniquement par Kafka.

## Modules Maven

| Module | Responsabilité | Dépendances internes autorisées |
|---|---|---|
| `integration-events` | Contrats publiés sur Kafka, indépendants des domaines internes | aucune |
| `account-domain` | Modèle et invariants du compte | aucune |
| `account-application` | Cas d’usage d’ouverture et ports Account | `account-domain`, `integration-events` |
| `account-infrastructure` | REST et producer Kafka Account | `account-application`, `integration-events` |
| `account-bootstrap` | Assemblage et exécutable Account Service | `account-infrastructure` |
| `notification-application` | Traitement idempotent et ports Notification | `integration-events` |
| `notification-infrastructure` | Consumer Kafka, JDBC et transaction Spring | `notification-application`, `integration-events` |
| `notification-bootstrap` | Configuration Kafka/PostgreSQL/Flyway et exécutable Notification Service | `notification-infrastructure` |

Il n’existe pas encore de module `notification-domain` : le comportement actuel ne justifie pas un modèle métier supplémentaire.

## Prérequis

- Java 21
- Maven 3.9+
- Docker avec Docker Compose

## Construire et tester

```bash
mvn clean verify
```

Les tests Kafka et PostgreSQL utilisent Testcontainers. Aucun broker ni PostgreSQL installé manuellement n’est requis pour la suite de tests.

## Lancer toute l’architecture avec Docker

```bash
cp .env.example .env
docker compose up --build
```

Cette commande démarre Kafka, PostgreSQL Notification, Account Service et Notification Service.

Pour arrêter les conteneurs sans supprimer les données :

```bash
docker compose down
```

Pour supprimer également le volume PostgreSQL du laboratoire :

```bash
docker compose down --volumes
```

## Workflow IntelliJ / processus locaux

Démarrer uniquement Kafka et PostgreSQL :

```bash
cp .env.example .env
docker compose up -d kafka notification-postgres
mvn clean package
```

Puis lancer les deux applications dans deux terminaux :

```bash
java -jar account-bootstrap/target/account-bootstrap-0.0.1-SNAPSHOT.jar
```

```bash
set -a
source .env
set +a
java -jar notification-bootstrap/target/notification-bootstrap-0.0.1-SNAPSHOT.jar
```

Les classes principales sont :

- `com.example.banklab.account.AccountServiceApplication`
- `com.example.banklab.notification.NotificationServiceApplication`

## Ouvrir un compte

```bash
curl -i -X POST http://localhost:8080/accounts \
  -H 'Content-Type: application/json' \
  -d '{"customerId":"11111111-1111-1111-1111-111111111111","currency":"EUR"}'
```

La réponse HTTP est `201 Created`. Account Service publie ensuite `BankAccountOpened` dans `account-events` avec `accountId` comme clé Kafka.

## Observer Notification

Le consumer journalise la réception sans payload sensible :

```text
BankAccountOpened received eventId=... accountId=...
```

Vérifier l’état PostgreSQL :

```bash
docker compose exec notification-postgres psql \
  -U notification_user -d notification_db \
  -c 'SELECT event_id, account_id, type, status FROM notification;'
```

```bash
docker compose exec notification-postgres psql \
  -U notification_user -d notification_db \
  -c 'SELECT event_id, processed_at FROM processed_event;'
```

## Idempotence Notification

Le claim et l’effet métier partagent la même transaction PostgreSQL :

```text
BEGIN
  INSERT processed_event ... ON CONFLICT (event_id) DO NOTHING
  0 ligne insérée -> duplicate, aucun effet
  1 ligne insérée -> créer la notification WELCOME
COMMIT
```

Si la création de la notification échoue, le rollback annule également le claim. Un replay ultérieur peut donc reprendre l’événement.

### Outil pédagogique de replay concurrent

Le runner de replay est conservé dans `notification-bootstrap` pour les ateliers. Il exige à la fois le profil `dev-race` et `DEV_RACE_ENABLED=true`; il ne peut donc pas s’activer avec la configuration normale.

```bash
SPRING_PROFILES_ACTIVE=dev-race \
DEV_RACE_ENABLED=true \
DEV_RACE_EVENT_ID=6e840c4e-823c-4e52-9674-6f16cf3b637b \
DEV_RACE_ACCOUNT_ID=97981ddf-23fe-4449-9a8a-3debaa01cb1f \
DEV_RACE_CUSTOMER_ID=17711250-4fb6-480d-9fef-fae856ccc71d \
DEV_RACE_CURRENCY=EUR \
DEV_RACE_OCCURRED_AT=2026-08-15T10:15:30Z \
java -jar notification-bootstrap/target/notification-bootstrap-0.0.1-SNAPSHOT.jar
```

Il republie exactement le même événement vers les partitions 0 et 1. Arrêter cette instance pédagogique après l’envoi.

## Vérifier Kafka comme buffer durable

1. Démarrer Kafka, PostgreSQL et les deux services.
2. Arrêter uniquement Notification Service.
3. Créer un compte via Account Service.
4. Redémarrer Notification Service.
5. Vérifier que l’événement en attente est consommé et que les deux tables sont alimentées.

Le consumer group reprend depuis ses offsets sans couplage de disponibilité entre les deux services.

## Configuration par environnement

| Variable | Défaut | Service |
|---|---|---|
| `KAFKA_BOOTSTRAP_SERVERS` | `localhost:9092` | Account, Notification |
| `ACCOUNT_HTTP_PORT` | `8080` | Account |
| `ACCOUNT_HOST_PORT` | `8080` | Docker Compose uniquement |
| `ACCOUNT_EVENTS_TOPIC` | `account-events` | Account, Notification, Docker Compose |
| `NOTIFICATION_DB_URL` | `jdbc:postgresql://localhost:5432/notification_db` | Notification |
| `NOTIFICATION_DB_USERNAME` | `notification_user` | Notification |
| `NOTIFICATION_DB_PASSWORD` | obligatoire | Notification |
| `NOTIFICATION_DB_HOST_PORT` | `5432` | Docker Compose uniquement |
| `NOTIFICATION_KAFKA_GROUP_ID` | `notification-service` | Notification |
| `NOTIFICATION_KAFKA_CONCURRENCY` | `3` | Notification |

`.env` est ignoré par Git. `.env.example` ne contient qu’une valeur locale illustrative : aucun secret de production n’est stocké dans le dépôt.

## Concepts expérimentés

- livraison at-least-once ;
- consumer groups et offsets ;
- partitions et concurrence ;
- retry, backoff et Dead Letter Topic ;
- idempotent consumer ;
- transaction locale PostgreSQL ;
- gestion de doublons concurrents ;
- claim atomique avec PostgreSQL.

## NEXT LEARNING STEP

Ces sujets sont volontairement non implémentés :

- persistance Account ;
- atomicité persistance Account / publication Kafka avec Outbox Pattern ;
- Inbox complète ;
- Saga ;
- Kafka Transactions / Exactly Once ;
- observabilité distribuée ;
- orchestration de conteneurs et Kubernetes.
