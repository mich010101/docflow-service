# docflow-service

Сервис документооборота (Spring Boot + PostgreSQL + Liquibase) с пакетными переходами статусов, историей, реестром утверждений, поиском, фоновыми worker'ами и утилитой генерации документов.

## Модули

- `docflow-service` — основной Spring Boot сервис
- `docflow-generator` — CLI-утилита генерации документов через API сервиса

## Статусы

- `DRAFT`
- `SUBMITTED`
- `APPROVED`

Переходы:
- `DRAFT -> SUBMITTED`
- `SUBMITTED -> APPROVED`

## Что реализовано по ТЗ

- API создания документа (номер генерируется автоматически, статус `DRAFT`)
- API получения одного документа вместе с историей
- API пакетного получения документов по списку `id` (с пагинацией и сортировкой)
- API пакетной отправки на согласование (`submit`) с частичными результатами
- API пакетного утверждения (`approve`) с частичными результатами
- откат `approve`, если не удалось создать запись в реестре
- API поиска документов по фильтрам (`status`, `author`, период дат создания) с пагинацией и сортировкой
- API проверки конкурентного утверждения одного документа
- `SUBMIT-worker` и `APPROVE-worker` (фоновые задачи)
- утилита генерации N документов через API
- единый формат ошибок: `code + message`
- Liquibase миграции и Docker Compose для PostgreSQL
- интеграционные тесты по минимальному набору сценариев

## Выбранная трактовка периода в поиске

Период (`createdFrom`, `createdTo`) применяется к **дате создания документа** (`created_at`).

## Запуск PostgreSQL (Docker)

```bash
docker compose up -d
```

Если ранее использовался старый volume (до PostgreSQL 18+), пересоздать:

```bash
docker compose down -v
docker compose up -d
```

## Запуск сервиса

Важно: datasource-конфиг лежит в `docflow-service/src/main/resources/application-local.yaml`, поэтому запускайте с профилем `local`.

```bash
./mvnw -pl docflow-service spring-boot:run -Dspring-boot.run.profiles=local
```

или

```bash
SPRING_PROFILES_ACTIVE=local ./mvnw -pl docflow-service spring-boot:run
```

## Ключевые API

Локальный base URL (профиль `local`):

```text
http://localhost:8080/docflow-service
```

### 1. Создать документ
`POST /docflow-service/api/v1/documents`

Пример тела:

```json
{
  "title": "Договор поставки",
  "author": "alice",
  "content": "Черновик..."
}
```

### 2. Получить один документ с историей
`GET /docflow-service/api/v1/documents/{id}`

### 2b. Пакетное получение по списку id (пагинация + сортировка)
`POST /docflow-service/api/v1/documents/_batch-get`

```json
{
  "ids": ["uuid-1", "uuid-2"],
  "page": 0,
  "size": 20,
  "sortBy": "createdAt",
  "sortDir": "DESC"
}
```

### 3. Отправить на согласование (batch)
`POST /docflow-service/api/v1/documents/submit`

```json
{
  "ids": ["uuid-1", "uuid-2"]
}
```

Ответ содержит результат по каждому `id`: `SUCCESS | CONFLICT | NOT_FOUND | ERROR`.

### 4. Утвердить (batch)
`POST /docflow-service/api/v1/documents/approve`

```json
{
  "ids": ["uuid-1", "uuid-2"]
}
```

Ответ содержит результат по каждому `id`: `SUCCESS | CONFLICT | NOT_FOUND | REGISTRY_ERROR | ERROR`.

### 5. Поиск документов
`GET /docflow-service/api/v1/documents/search`

Параметры:
- `status`
- `author`
- `createdFrom` (ISO-8601, UTC)
- `createdTo` (ISO-8601, UTC)
- `page`, `size`, `sortBy`, `sortDir`

Пример:

```text
/docflow-service/api/v1/documents/search?status=APPROVED&author=alice&createdFrom=2026-02-01T00:00:00Z&createdTo=2026-02-28T23:59:59Z&page=0&size=50&sortBy=createdAt&sortDir=DESC
```

### 6. Проверка конкурентного утверждения
`POST /docflow-service/api/v1/documents/{id}/concurrency-approve-check`

```json
{
  "threads": 16,
  "attempts": 100
}
```

Ожидаемо: ровно один `SUCCESS`, остальные `CONFLICT` (или ошибки при некорректном исходном статусе).

## Утилита генерации документов

Отдельная утилита (CLI-класс), которая читает файл параметров и создаёт `N` документов через API.

Пример файла параметров: `docflow-generator/generator.properties.example`

```properties
n=100
baseUrl=http://localhost:8080/docflow-service
author=generator
```

Запуск:

```bash
./mvnw -pl docflow-generator -DskipTests compile exec:java \
  -Dexec.mainClass=com.app.docflow.tools.DocumentGeneratorCli \
  -Dexec.args=docflow-generator/generator.properties.example
```

## Фоновые процессы

В сервисе работают два scheduler-worker'а:
- `SUBMIT-worker`: берёт `DRAFT` и отправляет в batch submit
- `APPROVE-worker`: берёт `SUBMITTED` и отправляет в batch approve

Параметры в `application-local.yaml`:
- `docflow.batchSize`
- `docflow.workers.enabled`
- `docflow.workers.submitDelayMs`
- `docflow.workers.approveDelayMs`

## Что смотреть в логах

- генератор: `N`, прогресс (`x/N`), суммарное время создания
- воркеры: размер пачки, число успешных, время пачки (`elapsedMs`), сколько осталось в целевом статусе
- ключевые операции сервиса (create/submitBatch/approveBatch/concurrency-check, worker cycles) логируют время выполнения через AOP (`@LogExecutionTime`)

## Тесты

Запуск (используются Testcontainers + PostgreSQL):

```bash
./mvnw -pl docflow-service test
```

Покрытые минимальные сценарии:
- happy-path по одному документу
- пакетный submit
- пакетный approve с частичными результатами
- rollback approve при ошибке записи в реестр

## Архитектура (коротко)

Подход: `DDD-lite + Ports & Adapters`.

- `domain` — модель документа, статусы, история, порты репозиториев
- `application` — use cases (create/query/submit/approve/concurrency-check)
- `infrastructure` — JPA/JDBC adapters, sequence generators
- `api` — REST controllers + DTO + mapper + unified errors

## Optional (описание)

### 1. Что менять для уверенной обработки 5000+ id в одном запросе

- использовать chunking внутри application слоя (например по 500/1000) с потоковой обработкой и ограниченным параллелизмом
- загружать документы пачками (bulk select) вместо `findById` по одному
- выполнять batch update через SQL/JPA bulk для части сценариев, где не нужна сложная доменная логика
- выносить результаты в стрим/файл при очень больших ответах (или async job + polling API)
- добавить rate limiting / request size limits и id de-duplication на входе

### 2. Как вынести реестр утверждений в отдельную систему

Вариант A: отдельная БД
- сервис сохраняет документ и outbox-event в одной локальной транзакции
- отдельный publisher пишет событие в реестр-сервис/БД
- подтверждение через inbox/idempotency key в реестре

Вариант B: отдельный HTTP-сервис
- синхронный вызов реестра только если допускается высокая связность и SLA
- лучше: transactional outbox + асинхронная доставка (Kafka/RabbitMQ)
- обязательны idempotency-key и уникальный ключ по `documentId` в реестре
