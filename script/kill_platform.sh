#!/usr/bin/env bash

for host in $(cat $1)
    do
    echo $host
    ssh diversify@$host sudo docker ps -q -a | xargs ssh diversify@$host sudo docker rm -f &
done
wait