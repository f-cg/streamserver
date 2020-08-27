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
            A AS A.price = 19,
	    B AS NOT B.price = 25 AND NOT B.price = 18,
            C AS C.price = 18
    ) MR
----
查找从19到18之间没有经过25的
