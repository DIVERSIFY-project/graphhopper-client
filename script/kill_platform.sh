#!/usr/bin/env bash

if [ -z "$1" ]
  then
    echo "No hosts file supplied"
    exit 1
fi

for host in $(cat $1)
do
    echo $host
    if [[ $host ==  10.0.0.* ]]
    then
        echo "Cloud computer"
        ssh -t -o "ProxyCommand ssh fox@10.0.0.1 nc %h 22" $host sudo docker ps -q -a | xargs sudo docker unpause
        ssh -t -o "ProxyCommand ssh fox@10.0.0.1 nc %h 22" $host sudo docker ps -q -a | xargs sudo docker rm -f
    else
        ssh -Y $host sudo docker ps -q -a | xargs sudo docker unpause
        ssh -Y $host sudo docker ps -q -a | xargs sudo docker rm -f
    fi
done