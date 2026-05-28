# Warning Event Contract v1

## 1. Scope
- Direction: `regulation-service -> warning-service`
- API: `POST /api/warning/internal/events/upsert`
- Goal: Upsert warning records idempotently by `dedupKey`

## 2. Required Fields
- `eventType`: warning event type
- `bizType`: business type (`RECTIFICATION`)
- `bizId`: business id (`rectificationId`)
- `dedupKey`: idempotency key
- `level`: warning level (`L1` or `L2`)
- `occurTime`: event occurrence time

## 3. Scope and Payload Requirements
- Scope fields:
  - `regionId`: region scope id
  - `ownerRegulatorId`: enterprise owner enforcer id (metadata only, not assignment)
- Payload fields (required for rectification SLA events):
  - `enterpriseId`
  - `regionId`
  - `deadline`
  - `overdueMinutes`
  - `status`

## 4. Idempotency Rule
- Same `dedupKey` means same warning stream.
- On repeated upsert:
  - Do not create a new warning record.
  - Update: `lastOccurTime`, `triggerCount`, `level` (keep higher), `payloadJson`.
  - Append process log: `EVENT_UPSERT`.

## 5. Level Escalation Rule
- `regulation-service` only reports overdue facts (`SLA_OVERDUE_*`) and does not send `SLA_ESCALATE_*`.
- Level escalation (`L1 -> L2`) is executed by `warning-service` scheduler.
- Upsert keeps level merge safety:
  - Existing `L2` + incoming `L1` => keep `L2`
  - Existing `L1` + incoming `L2` => final `L2` (compatibility only)

## 6. Example: First Upsert
```json
{
  "eventType": "SLA_OVERDUE_SUBMIT",
  "bizType": "RECTIFICATION",
  "bizId": 1024,
  "regionId": 330100,
  "ownerRegulatorId": 18,
  "dedupKey": "RECTIFICATION:1024:SLA_OVERDUE_SUBMIT",
  "level": "L1",
  "title": "整改提交超时",
  "content": "企业整改提交已超时",
  "sourceService": "regulation-service",
  "occurTime": "2026-03-06T10:00:00",
  "payload": {
    "rectificationId": 1024,
    "enterpriseId": 88,
    "regionId": 330100,
    "deadline": "2026-03-05T10:00:00",
    "overdueMinutes": 1440,
    "status": "ONGOING"
  }
}
```

## 7. Example: Repeated Upsert (same dedupKey, count increment)
```json
{
  "eventType": "SLA_OVERDUE_SUBMIT",
  "bizType": "RECTIFICATION",
  "bizId": 1024,
  "regionId": 330100,
  "ownerRegulatorId": 18,
  "dedupKey": "RECTIFICATION:1024:SLA_OVERDUE_SUBMIT",
  "level": "L1",
  "title": "整改提交超时",
  "content": "企业整改提交持续超时，重复上报同一预警流",
  "sourceService": "regulation-service",
  "occurTime": "2026-03-07T10:00:00",
  "payload": {
    "rectificationId": 1024,
    "enterpriseId": 88,
    "regionId": 330100,
    "deadline": "2026-03-05T10:00:00",
    "overdueMinutes": 2880,
    "status": "ONGOING"
  }
}
```
