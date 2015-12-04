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

rm -f host_error_wide
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
    ping -q -c1 $host > /dev/null
    if [ $? -ne 0 ]
    then
        echo $host "unreachable"
        echo $host "unreachable" >> host_error_wide
    else
        ssh -t -t obarais@$host pgrep docker > /dev/null
        if [ $? -eq 0 ]
        then
            echo $host "pinged & docker on"
            HOSTS=("${HOSTS[@]}" "$line")
            DOCKERS=$(( DOCKERS + amount ))
        else
            echo $host "docker off"
            echo $host "docker off" >> host_error_wide
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
        echo $host:153$i >> host_ip_list_wide
    done
    ssh -t -t obarais@$host ./docker_init.sh $amount &
done
wait
echo $DOCKERS "dockers on" ${#HOSTS[@]} "platforms"