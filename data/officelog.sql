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

## 查询一 过去十秒内打开菜单的次数
SELECT CAST(TUMBLE_START(CREATEDATE, INTERVAL '10' SECOND) AS STRING) window_start,
COUNT(*) open_count
FROM officelog
WHERE OPERATION='openMenu'
GROUP BY TUMBLE(CREATEDATE, INTERVAL '10' SECOND)

-- Kafka

CREATE TABLE officelogKafka(
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
