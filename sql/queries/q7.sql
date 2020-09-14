-- 普通flink语句查询
操作日志流
----
SELECT CAST(TUMBLE_START(OPERATIONTIME, INTERVAL '10' MINUTE) AS STRING) window_start,
COUNT(*) 操作次数
FROM 操作日志流
WHERE OPERATIONNAME='新增'
GROUP BY TUMBLE(OPERATIONTIME, INTERVAL '10' MINUTE)
----
最近10分钟新增次数
