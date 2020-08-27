Ticker
----
SELECT *
FROM Ticker
    MATCH_RECOGNIZE (
        PARTITION BY symbol
        ORDER BY rowtime
        MEASURES
            A.rowtime AS start_tstamp,
            LAST(B.rowtime) AS bottom_tstamp,
            LAST(C.rowtime) AS end_tstamp
        ONE ROW PER MATCH
        AFTER MATCH SKIP TO LAST C 
        PATTERN (A B* C)
        DEFINE
            A AS A.price = 12,
	-- 注意B的条件必须不能和C有交集，否则事件会进入B而非C
	    B AS NOT B.price = 17,
            C AS C.price = 17
    ) MR
----
查询股价从12到17的时期
