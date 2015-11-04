#!/usr/bin/env bash

if [ -z "$1" ]
then
    echo "No hosts file supplied"
    exit 1
fi
if [ -z "$2" ]
then
    echo "No deployment size supplied, using sizes specified in hosts file" + $1
fi
while IFS=';' read -ra ADDR;
do
    host = ${ADDR[0]}
    if [ -z "$2" ]
    then
        amount = ${ADDR[1]}
    else
        amount = $2
    fi
    echo $host
    if [[ $host ==  10.0.0.* ]]
    then
        echo "Cloud computer"
        ssh -t -o "ProxyCommand ssh fox@10.0.0.1 nc %h 22" diversify@$host sudo ./ghdemo_script $amount
        for i in $(seq 1 $amount)
        do
            echo $host:153$i >> host_ip_list
        done
    else
        for i in $(seq 1 $amount)
        do
            #ssh -Y diversify@$host sudo docker run -d -p 153$i:8080 aelie/diversify-light
            #echo $host:153$i >> host_ip_list
            ssh obarais@$host sudo docker pull aelie/diversify-light-3
        done
    fi
done <<< "$1"
