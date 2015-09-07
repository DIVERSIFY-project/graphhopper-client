#!/usr/bin/env bash

for host in $(cat $1)
    do
    echo $host
    ssh ubuntu@$host sudo docker ps -q -a | xargs ssh ubuntu@$host sudo docker pause &
done
wait