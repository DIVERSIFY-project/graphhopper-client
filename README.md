Installation :
- sur la machine des clients, le programme graphhopper-client (https://github.com/DIVERSIFY-project/graphhopper-client.git) et un IDE genre IDEA pour plus de facilité
- sur les machines des serveurs, docker et smartgh-cloudml (https://github.com/DIVERSIFY-project/smartgh-cloudml.git)
- sur la machine du dashboard, node.js, socket.io et GHDemoDashBoard (https://github.com/DIVERSIFY-project/GHDemoDashBoard.git)

Pour démarrer la démo :
- tuer tous les dockers sur les serveurs hôtes (obligatoirement Linux) : 
    sudo docker ps -a -q | xargs sudo docker rm -f
- créer un fichier hosts contenant les adresses IP de tes machines serveurs, une par ligne
- lancer le script init_platform.sh sous graphhopper-client/script (qui lance les dockers des serveurs et crée un fichier script/host_ip_list recensant les adresses de ces dockers) avec comme paramètres :
- le fichier hosts
- le nombre de serveurs à lancer par machine de serveurs
    e.g. : ./init_platform.sh hosts 25

- créer les faux clients en exécutant le main de la classe DummyClientGenerator de graphhopper-client avec comme paramètres :
- le fichier script/host_ip_list
- le fichier services.json (qui contient la description des services)
- le nombre minimum de serveurs auquel peut se connecter un client
- le nombre maximum de serveurs auquel peut se connecter un client
- le nombre de clients à créer
- le répertoire dans lequel les créer
    e.g. : java -cp <chemin_du_jar> graphhopper.client.demo.DummyClientGenerator script/host_ip_list services.json 2 8 75 dummies

- attendre autour de 5-10 minutes que les serveurs soient initialisés
- lancer les clients avec le main de la classe Main de graphhopper-client avec comme paramètres :
- le répertoire contenant les fichiers client
- le port WebSocket
    e.g. : java -cp <chemin_du_jar> graphhopper.client.demo.Main dummies/ 8099

- lancer un monkey par machine de serveurs : dans smartgh-cloudml/monkey, exécuter : 
    sudo python docker_monkey_nometa.py -i tick_websocket -a <adresse_IP_de_la_machine_clients> confzoo/weibull-age.yaml
- lancer le serveur de données pour le dashboard sur la machine dashboard : 
    node GHDemoDashBoard/node-server/plugin_node_sample_server.js
- démarrer la page GHDemoDashBoard/index.html dans un navigateur sur la machine dashboard et charger GHDemoDashBoard/savedBoards/board.json