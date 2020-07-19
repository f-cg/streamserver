function delete_log(that){
    let log_logid = that.parentNode.getElementsByClass('logitem-log')[0].getAttribute('href');
    let delete_log_logid='log'+log_logid;
    console.log(delete_log_logid);
}
