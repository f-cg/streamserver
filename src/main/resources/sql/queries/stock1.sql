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
        PATTERN (A B+ C)
        DEFINE
            A AS A.price < 13,
	    B AS B.price >12 AND B.price <25,
            C AS C.price > 24
    ) MR
----
查询股价从12到19的时期
