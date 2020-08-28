-- 复杂事件
BIZLOG
----
SELECT *
FROM BIZLOG
MATCH_RECOGNIZE (
PARTITION BY DATAID
ORDER BY OPERATIONTIME
MEASURES
A.OPERATIONTIME AS 第一次修改时间,
C.OPERATIONTIME AS 第二次修改时间
ONE ROW PER MATCH
PATTERN (A C)
DEFINE
A AS OPERATIONNAME = '修改',
C AS OPERATIONNAME = '修改' AND TIMESTAMPDIFF(HOUR, A.OPERATIONTIME, C.OPERATIONTIME) > 2
) AS T
----
修改间隔超过了2小时
