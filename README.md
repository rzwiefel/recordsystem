## Record System

### Running CLI
- `clojure -X:help`

- `clojure -X:cli :input '["data.psv"]' :output '[:email :desc :last-name :asc]'`

- `clojure -X:cli :input '["/Users/ryanzwiefelhofer/Downloads/data.ssv" "/Users/ryanzwiefelhofer/Downloads/data.psv" "/Users/ryanzwiefelhofer/Downloads/data.csv"]' :output '[:email :desc :last-name :asc :first-name :desc]'`


bonus option:
`:disable-date-format true`

### Running REST API
`clojure -X:rest`

Endpoints:
- `POST /records` (Content-Type: text/<csv/psv/ssv>)
- `GET /records/:attribute`
  - Optional Query Param: `dir=<asc/desc>`
- CURLs listed at bottom of page

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
- REST API sets Content-type to corresponding type when POSTing eg `Content-Type: text/csv` (csv/psv/ssv). Note that psv/ssv are not official types but for brevity of assignment we are including the types here.


### curls for REST
```shell
curl --location --request GET 'http://localhost:1337/records/name'
```

```shell
curl --location --request POST 'http://localhost:1337/records' \
--header 'Content-Type: text/psv' \
--data-raw 'Zwiefelhofer | ryan | ryan.zwie@gmail.com | blue | 11/23/1992
Badahdah | chris | a.badahdah@gmail.com | red | 04/15/1992
Hickey | rich | rich.hickey@cognitect.com | green | 07/27/1967
Zzzwiefelhofer | ryan |ryan.zwie@gmail.com |blue | 11/23/1992
John | doe | jd@jd.com | |
lam | steve | lamste@ls.com | orange |
 | phil | | blue | 1/1/1960
 | travis | | blue | 1/1/1960
 '
```

