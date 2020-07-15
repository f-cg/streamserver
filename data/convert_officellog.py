import csv
import datetime
import time

with open('./officelograw.csv', 'rt') as fr, open('./officelog.csv', 'wt') as fw:
    cr = csv.DictReader(fr)
    fields = ['LOGID', 'USERID', 'CREATEDATE', 'OPERATION', 'MENUID',
              'STATUS', 'IP', 'CREATEORGID', 'UPDATETIME', 'CREATEUSERID']
    cw = csv.DictWriter(fw, fields)
    print(fields)
    #  cw.writeheader()
    fmt = '%Y/%m/%d %H:%M'
    newfmt = '%Y-%m-%d %H:%M:%S'
    for row in cr:
        createdate = time.strptime(row['CREATEDATE'], fmt)
        updatetime = time.strptime(row['UPDATETIME'], fmt)
        row['CREATEDATE'] = time.strftime(newfmt, createdate)
        row['UPDATETIME'] = time.strftime(newfmt, updatetime)
        cw.writerow(row)
