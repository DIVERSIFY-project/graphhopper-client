#!/usr/bin/env bash

if [ -z "$1" ]
then
    echo "No hosts file supplied"
    exit 1
fi
if [ -z "$2" ]
then
    echo "No deployment size supplied, using sizes specified in hosts file" $1
fi

rm -f host_ip_list_wide

DOCKERS=0
for line in $(cat $1)
do
    host=$(echo $line | cut --fields=1 --delimiter=";")
    if [ -z "$2" ]
    then
        amount=$(echo $line | cut --fields=2 --delimiter=";")
    else
        amount=$2
    fi
    ping -q -c1 google.com > /dev/null
    if [ $? -ne 0 ]
    then
        echo $host "unreachable"
    else
        ssh -t -t obarais@e003m01.istic.univ-rennes1.fr pgrep docker > /dev/null
        if [ $? -eq 0 ]
        then
            echo $host "pinged & docker on"
            HOSTS=("${HOSTS[@]}" "$line")
            DOCKERS=$(( DOCKERS + amount ))
        else
            echo $host "docker off"
        fi
    fi
done
for line in ${HOSTS[@]}
do
    host=$(echo $line | cut --fields=1 --delimiter=";")
    if [ -z "$2" ]
    then
        amount=$(echo $line | cut --fields=2 --delimiter=";")
    else
        amount=$2
    fi
    for i in $(seq 1 $amount)
    do
        #ssh -t obarais@$host sudo docker run -d -p 153$i:8080 aelie/diversify-light-3
        #./single_init.sh $host 153$i &
        echo $host:153$i >> host_ip_list_wide
    done
    ssh -t -t obarais@$host ./docker_init.sh $amount &
done
wait
echo $DOCKERS "dockers on" ${#HOSTS[@]} "platforms"