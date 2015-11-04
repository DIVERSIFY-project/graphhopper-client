#!/usr/bin/env bash

if [ -z "$1" ]
  then
    echo "No hosts file supplied"
    exit 1
fi
if [ -z "$2" ]
  then
    echo "No deployment size supplied"
    exit 1
fi

rm -f host_ip_list

for host in $(cat $1)
do
    echo $host
    if [[ $host ==  10.0.0.* ]]
    then
        echo "Cloud computer"
        ssh -t -o "ProxyCommand ssh fox@10.0.0.1 nc %h 22" diversify@$host sudo ./ghdemo_script $2
        for i in $(seq 1 $2)
        do
            echo $host:153$i >> host_ip_list
        done
    else
        for i in $(seq 1 $2)
        do
            ssh -Y diversify@$host sudo docker run -d -p 153$i:8080 -m 2G aelie/diversify-light-3
            echo $host:153$i >> host_ip_list
        done
    fi
done

