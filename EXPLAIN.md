# EXPLAIN.md

## Пример поискового запроса

Выбранный сценарий поиска: по `status + author + period(created_at)`.

SQL (эквивалент фильтров API):

```sql
SELECT id, number, title, author, status, created_at
FROM documents
WHERE status = 'APPROVED'
  AND author = 'alice'
  AND created_at >= TIMESTAMPTZ '2026-02-01 00:00:00+00'
  AND created_at <= TIMESTAMPTZ '2026-02-28 23:59:59+00'
ORDER BY created_at DESC
LIMIT 50 OFFSET 0;
```

## EXPLAIN (ANALYZE) фактический пример

```sql
EXPLAIN (ANALYZE, BUFFERS)
SELECT id, number, title, author, status, created_at
FROM documents
WHERE status = 'APPROVED'
  AND author = 'alice'
  AND created_at >= TIMESTAMPTZ '2026-02-01 00:00:00+00'
  AND created_at <= TIMESTAMPTZ '2026-02-28 23:59:59+00'
ORDER BY created_at DESC
LIMIT 50 OFFSET 0;
```

Фактический план (пример с реальными данными):

```text
Limit  (cost=0.41..6.19 rows=1 width=81) (actual time=0.069..0.070 rows=0.00 loops=1)
  Buffers: shared hit=3
  ->  Index Scan Backward using idx_documents_author_created_at on documents  (cost=0.41..6.19 rows=1 width=81) (actual time=0.067..0.068 rows=0.00 loops=1)
        Index Cond: (((author)::text = 'alice'::text) AND (created_at >= '2026-02-01 00:00:00+00'::timestamp with time zone) AND (created_at <= '2026-02-28 23:59:59+00'::timestamp with time zone))
        Filter: ((status)::text = 'APPROVED'::text)
        Index Searches: 1
        Buffers: shared hit=3
Planning:
  Buffers: shared hit=57
Planning Time: 0.525 ms
Execution Time: 0.091 ms
```

### Комментарий к фактическому плану

На текущем наборе данных PostgreSQL выбрал `Index Scan Backward` по индексу `idx_documents_author_created_at`.

Что это означает:
- фильтрация по `author + created_at` выполняется по индексу;
- сортировка `ORDER BY created_at DESC` выполняется без отдельного `Sort` (за счёт backward scan по индексу);
- условие по `status` в данном плане применено как дополнительный `Filter` после чтения строк по индексу.

Это нормальное поведение planner'а: выбор конкретного индекса зависит от статистики, объёма данных и селективности условий. На небольших объёмах PostgreSQL может выбирать `Seq Scan`, а на более подходящих данных — использовать один из составных индексов.

## Почему такие индексы

В миграции добавлены:
- `idx_documents_status_created_at`
- `idx_documents_author_created_at`
- `idx_documents_status_author_created_at`
- `idx_document_history_document_created_at`

Обоснование:
- `status + created_at` ускоряет worker'ы (`DRAFT`/`SUBMITTED`) и часть поисковых запросов
- `author + created_at` ускоряет поиск по автору и периоду
- `status + author + created_at` покрывает основной комбинированный фильтр поиска
- `document_history(document_id, created_at)` ускоряет чтение истории документа по времени

## Что проверить локально

1. Поднять PostgreSQL (`docker compose up -d`)
2. Запустить сервис и создать данные
3. Выполнить `EXPLAIN (ANALYZE, BUFFERS)` в psql / IDEA Database Console
4. Убедиться, что план использует нужный индекс и нет лишнего full scan на больших объёмах данных
