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

for i in $(seq 1 $2)
    do
    echo $i
    for host in $(cat $1)
        do
        echo $host
        #token=$(ssh -Y ubuntu@$host sudo docker run -d -p 153$i:8080 songhui/smhp-final-14)
        #ssh -Y diversify@$host sudo docker run -d -p 153$i:8080 songhui/smhp-final-14 &
        #ssh -Y diversify@$host sudo docker run -d -p 153$i:8080 songhui/smhp-hopper-jetty &
        ssh -Y diversify@$host sudo docker run -d -p 153$i:8080 barais/undertowsmartgh &
        echo $host:153$i >> host_ip_list
        #echo $token >> token_list
    done
    sleep 1s
done