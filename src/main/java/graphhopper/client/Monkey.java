package graphhopper.client;

import graphhopper.client.demo.Main;

import java.io.*;
import java.util.*;
import java.util.concurrent.RunnableFuture;
import java.util.stream.Collectors;

/**
 * Created by aelie on 30/11/15.
 */
public class Monkey extends Thread {

    public boolean newTick = true;
    Map<String, List<String>> containersByHost;
    Map<String, List<String>> pausedContainersByHost;
    public double ratio = 0;

    boolean verbose = true;

    public static void main(String[] args) {
        //Monkey monkey = new Monkey("script" + File.separator + "host_ip_list_wide");
        //System.out.println(monkey.containersByHost);
        //monkey.twelveLittleMonkeys(0.1);
        //weibullDistribution();
    }

    public Monkey(String hostListFile) {
        containersByHost = new HashMap<>();
        pausedContainersByHost = new HashMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(hostListFile));
            String line;
            while ((line = br.readLine()) != null) {
                containersByHost.put(line.split(":")[0], new ArrayList<>());
                pausedContainersByHost.put(line.split(":")[0], new ArrayList<>());
            }
            for (String host : containersByHost.keySet()) {
                containersByHost.get(host).addAll(getContainers(host));
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        unpauseAll();
    }

    public List<String> getContainers(String ipAddress) {
        List<String> result = new ArrayList<>();
        Process p;
        try {
            if(ipAddress.equals("localhost") || ipAddress.equals("127.0.0.1")) {
                p = Runtime.getRuntime().exec("docker ps -aq");
            } else {
                p = Runtime.getRuntime().exec("ssh -t -t " + ipAddress + " sudo docker ps -a -q");
            }
            p.waitFor();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            String line = "";
            while ((line = br.readLine()) != null) {
                result.add(line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
        return result;
    }

    public void processPrintout(Process p) throws IOException {
        BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
        String line = "";
        while ((line = br.readLine()) != null) {
            System.out.println(line);
        }
    }

    public void twelveLittleMonkeys(double ratio) {
        Process p;
        /*Map<String, List<String>> pausedContainersByHost = new HashMap<>();
        for (String host : containersByHost.keySet()) {
            pausedContainersByHost.put(host, new ArrayList<>());
            try {
                p = Runtime.getRuntime().exec("ssh -t -t obarais@" + host + " sudo docker ps -a |grep Paused| awk '{print $1;}'");
                p.waitFor();
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = "";
                while ((line = br.readLine()) != null) {
                    pausedContainersByHost.get(host).add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println(pausedContainersByHost);
        //unpause
        for (String host : pausedContainersByHost.keySet()) {
            try {
                //p = Runtime.getRuntime().exec("ssh -t -t obarais@" + host + " sudo docker ps -a |grep Paused| awk '{print $1;}'|xargs sudo docker unpause");
                //p = Runtime.getRuntime().exec("ssh -t -t obarais@" + host + " sudo docker ps -a -q|while read i; do sudo docker unpause $i; done");
                p = Runtime.getRuntime().exec("ssh -t -t obarais@" + host + " sudo docker ps -a |grep Paused| awk '{print $1;}'|while read i; do sudo docker unpause $i; done");
                p.waitFor();
                //processPrintout(p);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }*/
        //unpauseAll();
        /*for(String host : containersByHost.keySet()) {
            pausedContainersByHost.put(host, new ArrayList<>());
            try {
                p = Runtime.getRuntime().exec("ssh -t -t obarais@" + host + " sudo docker ps -a |grep Paused| awk '{print $1;}'|xargs sudo docker unpause");
                p.waitFor();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }*/
        //unpause
        int count = 0;
        if(verbose) System.out.println("Unpaused " + pausedContainersByHost.values());
        for(String host : pausedContainersByHost.keySet()) {
            for(String pausedContainer : pausedContainersByHost.get(host)) {
                try {
                    //p = Runtime.getRuntime().exec("ssh -t -t obarais@" + host + " sudo iptables -A INPUT -p tcp -m tcp --dport " + port + " -j ACCEPT");
                    if(host.equals("localhost") || host.equals("127.0.0.1")) {
                        p = Runtime.getRuntime().exec("docker unpause " + pausedContainer);
                    } else {
                        p = Runtime.getRuntime().exec("ssh -t -t " + host + " sudo docker unpause " + pausedContainer);
                    }
                    p.waitFor();
                    count++;
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            pausedContainersByHost.get(host).clear();
        }
        System.out.println("Unpaused " + count + " containers");
        //pause
        int pausedContainersNumber = Math.max((int) (Main.allPlatforms.size() * ratio), 1);
        List<String> hosts = new ArrayList<>(containersByHost.keySet());
        List<String> containers = new ArrayList<>(containersByHost.values().stream().flatMap(Collection::stream).collect(Collectors.toList()));
        Collections.shuffle(containers);
        for (int i = 0; i < pausedContainersNumber; i++) {
            Collections.shuffle(hosts);
            for (String host : hosts) {
                if (containersByHost.get(host).contains(containers.get(i))) {
                    try {
                        //p = Runtime.getRuntime().exec("ssh -t -t obarais@" + host + " sudo iptables -A INPUT -p tcp -m tcp --dport " + port + " -j DROP");
                        if(host.equals("localhost") || host.equals("127.0.0.1")) {
                            p = Runtime.getRuntime().exec("docker pause " + containers.get(i));
                        } else {
                            p = Runtime.getRuntime().exec("ssh -t -t " + host + " sudo docker pause " + containers.get(i));
                        }
                        p.waitFor();
                        pausedContainersByHost.get(host).add(containers.get(i));
                        //processPrintout(p);
                        //System.out.println(containers.get(i) + "@" + host + " paused");
                    } catch (IOException e) {
                        e.printStackTrace();
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
        if(verbose) System.out.println("Paused " + pausedContainersByHost.values());
        System.out.println("Paused " + pausedContainersNumber + " containers");
    }

    public void unpauseAll() {
        System.out.println("Unpausing containers...");
        Process p;
        /*Map<String, List<String>> pausedContainersByHost = new HashMap<>();
        for (String host : containersByHost.keySet()) {
            pausedContainersByHost.put(host, new ArrayList<>());
            try {
                p = Runtime.getRuntime().exec("ssh -t -t obarais@" + host + " sudo docker ps -a |grep Paused| awk '{print $1;}'");
                p.waitFor();
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = "";
                while ((line = br.readLine()) != null) {
                    pausedContainersByHost.get(host).add(line);
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
        System.out.println(pausedContainersByHost.size() +" paused containers");*/
        //unpause
        //for (String host : pausedContainersByHost.keySet()) {
        for (String host : containersByHost.keySet()) {
            try {
                //p = Runtime.getRuntime().exec("ssh -t -t obarais@" + host + " sudo docker ps -a |grep Paused| awk '{print $1;}'|xargs sudo docker unpause");
                //p = Runtime.getRuntime().exec("ssh -t -t obarais@" + host + " sudo docker ps -a -q|while read i; do sudo docker unpause $i; done");
                //p = Runtime.getRuntime().exec("ssh -t -t obarais@" + host + " sudo iptables -A INPUT -p tcp -m tcp --dport " + port + " -j ACCEPT");
                if(host.equals("localhost") || host.equals("127.0.0.1")) {
                    p = Runtime.getRuntime().exec("docker ps -a |grep Paused| awk '{print $1;}'|while read i; do docker unpause $i; done");
                } else {
                    p = Runtime.getRuntime().exec("ssh -t -t " + host + " sudo docker ps -a |grep Paused| awk '{print $1;}'|while read i; do sudo docker unpause $i; done");
                }
                p.waitFor();
                //processPrintout(p);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    /*public static void weibullDistribution() {
        WeibullDistribution wd = new WeibullDistribution(1.5, 20);
        System.out.println(Arrays.toString(wd.sample(10)));
        System.out.println(Arrays.toString(wd.sample(10)));
        System.out.println(Arrays.toString(wd.sample(10)));
        System.out.println(Arrays.toString(wd.sample(10)));
        System.out.println(Arrays.toString(wd.sample(10)));
    }*/

    public void run() {
        while (true) {
            monkeyRun();
            startWait();
        }
    }

    public void monkeyRun() {
        if(Main.tick > 5) {
            ratio = (0.5 * (1 + Math.sin((double) (Main.tick - 5) / 10d)));
            System.out.println("ratio "+ratio);
            twelveLittleMonkeys(ratio);
            //twelveLittleMonkeys(0.1);
        }

        newTick = false;
    }

    synchronized void startWait() {
        try {
            while (!newTick) wait();
        } catch (InterruptedException exc) {
            System.out.println("wait() interrupted");
        }
    }

    synchronized public void notice() {
        newTick = true;
        notify();
    }

}
