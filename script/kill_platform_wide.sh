#!/usr/bin/env bash

if [ -z "$1" ]
then
    echo "No hosts file supplied"
    exit 1
fi

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
        echo $host "pinged"
        HOSTS=("${HOSTS[@]}" "$line")
    fi
done
#for line in $(cat $1)
for line in ${HOSTS[@]}
do
    host=$(echo $line | cut --fields=1 --delimiter=";")
    #./single_kill.sh $host &
    ssh -t -t obarais@$host ./docker_kill_all.sh &
done
#wait