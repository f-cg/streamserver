#/bin/sh

cat $(find src/ -type f -not -path "src/main/resources/public/third-party/*") | wc -l
