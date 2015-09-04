#!/usr/bin/env bash

for host in $(cat $1)
    do
    echo $host
    for i in $(seq 0 $2)
        do
        echo $i
        ssh -Y ubuntu@$host sudo docker run -d -p 153$i:8080 songhui/smhp-final-14
    done
done