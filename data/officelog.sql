CREATE TABLE officelog(
LOGID STRING,
USERID STRING,
CREATEDATE TIMESTAMP(3),
OPERATION STRING,
MENUID STRING,
STATUS STRING,
IP STRING,
CREATEORGID STRING,
UPDATETIME TIMESTAMP(3),
CREATEUSERID STRING,
WATERMARK FOR CREATEDATE AS CREATEDATE - INTERVAL '3' SECOND
) WITH (
'connector.type' = 'filesystem',
'connector.path' = '/tmp/officelog.csv',
'format.type' = 'csv'
)

-- Kafka

CREATE TABLE officelog(
LOGID STRING,
USERID STRING,
CREATEDATE TIMESTAMP(3),
OPERATION STRING,
MENUID STRING,
STATUS STRING,
IP STRING,
CREATEORGID STRING,
UPDATETIME TIMESTAMP(3),
CREATEUSERID STRING,
WATERMARK FOR CREATEDATE AS CREATEDATE - INTERVAL '3' SECOND
) WITH (
'connector.type' = 'kafka',
'connector.version' = 'universal',
'connector.topic' = 'officelog',
'connector.properties.zookeeper.connect' = 'localhost:2181',
'connector.properties.bootstrap.servers' = 'localhost:9092',
'format.type' = 'csv'
)

## 查询一 过去十分钟内打开菜单的次数
SELECT CAST(TUMBLE_START(CREATEDATE, INTERVAL '10' MINUTE) AS STRING) window_start,
COUNT(*) open_count
FROM officelog
WHERE OPERATION='openMenu'
GROUP BY TUMBLE(CREATEDATE, INTERVAL '10' MINUTE)

## 查询二 过去十分钟每个用户操作的次数
SELECT CAST(TUMBLE_START(CREATEDATE, INTERVAL '10' MINUTE) AS STRING) window_start,
USERID,
COUNT(*) operation_count
FROM officelog
GROUP BY TUMBLE(CREATEDATE, INTERVAL '10' MINUTE), USERID

## 查询三 过去十分钟同一个用户在两个IP地址登录的次数

select
`start_timestamp`,
USERID, `event`
from officelog
MATCH_RECOGNIZE (
    PARTITION BY USERID
    ORDER BY `CREATEDATE`
    MEASURES
        e2.`OPERATION` as `event`,
        e1.`CREATEDATE` as `start_timestamp`,
        LAST(e2.`CREATEDATE`) as `end_timestamp`
    ONE ROW PER MATCH
    AFTER MATCH SKIP TO NEXT ROW
    PATTERN (e1 e2+?) WITHIN INTERVAL '10' MINUTE
    DEFINE
        e1 as e1.OPERATION = 'login',
        e2 as e2.OPERATION = 'login' and e2.IP <> e1.IP
)
