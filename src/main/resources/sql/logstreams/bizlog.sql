BIZLOG
----
select BIZID, DATAID, OPERATIONNAME, OPERATIONDESC, OPERATIONUSERID, OPERATIONTIME, OPERATIONUSERNAME, DUTY
from OA.ZT_BIZOBJECTLOG, UIM.APP_USER
where OPERATIONUSERID=LOGINNAME
order by OPERATIONTIME
