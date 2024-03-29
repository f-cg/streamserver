INT,VARCHAR,VARCHAR,CLOB,VARCHAR,VARCHAR,VARCHAR,VARCHAR

CREATE TABLE BIZLOG (
BIZID INT,
DATAID STRING,
OPERATIONNAME STRING,
OPERATIONDESC STRING,
OPERATIONUSERID STRING,
OPERATIONTIME TIMESTAMP(3),
OPERATIONUSERNAME STRING,
DUTY STRING,
WATERMARK FOR OPERATIONTIME AS OPERATIONTIME
) WITH (
'connector.type' = 'filesystem',
'connector.path' = '/tmp/bizlog.csv',
'format.type' = 'csv'
)

CREATE TABLE BIZLOG (
BIZID INT,
DATAID STRING,
OPERATIONNAME STRING,
OPERATIONDESC STRING,
OPERATIONUSERID STRING,
OPERATIONTIME TIMESTAMP(3),
OPERATIONUSERNAME STRING,
DUTY STRING,
WATERMARK FOR OPERATIONTIME AS OPERATIONTIME
) WITH (
'connector.type' = 'kafka',
'connector.version' = 'universal',
'connector.topic' = 'BIZLOG',
'connector.properties.zookeeper.connect' = 'localhost:2181',
'connector.properties.bootstrap.servers' = 'localhost:9092',
'format.type' = 'csv'
)

## 查询一 最近x分钟某种操作的频次
SELECT CAST(TUMBLE_START(OPERATIONTIME, INTERVAL '10' MINUTE) AS STRING) window_start,
COUNT(*) operation_count
FROM BIZLOG
WHERE OPERATIONNAME='公文起草'
GROUP BY TUMBLE(OPERATIONTIME, INTERVAL '10' MINUTE)


## 查询二 过去十分钟每个用户操作的次数
SELECT CAST(TUMBLE_START(CREATEDATE, INTERVAL '10' MINUTE) AS STRING) window_start,
USERID,
COUNT(*) operation_count
FROM officelog
GROUP BY TUMBLE(CREATEDATE, INTERVAL '10' MINUTE), USERID
