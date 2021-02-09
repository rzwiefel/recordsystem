## Record System

### Running CLI
- `clojure -X:help`

- `clojure -X:cli :input '["data.psv"]' :output '[:email :desc :last-name :asc]'`


bonus option:
`:disable-date-format true`

### Running REST API
`clojure -X:rest`

TODO- clj -X:rest too?

### Assumptions

Items labeled (Beyond scope) were handled in a way that may be different vs a real production system due to time and scope constraints of this assignment

- Files end with correct  suffix of datatype (csv, psv, ssv)
- Data in files is valid/correct (Beyond scope of assignment to perform validation/datafixing/etc)
    - This could be expanded upon using clojure.spec, etc
    - Note: The system does allow for **missing** values
- Tests would usually be in separate test package but for brevity of assignment are included in source classes
- Not dealing with excessive amounts of data (beyond scope)
- Not dealing with security risks (beyond scope)
- Failing fast instead of more complex ways of dealing with bad data
    - This could be expanded upon using queues to send data that failed validation/etc
- No persistent data store

