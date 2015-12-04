package graphhopper.client;

import graphhopper.client.demo.Main;
import org.apache.commons.math3.distribution.WeibullDistribution;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Created by aelie on 30/11/15.
 */
public class Monkey extends Thread {

    public boolean newTick = true;
    Map<String, List<String>> containersByHost;

    public static void main(String[] args) {
        //Monkey monkey = new Monkey("script" + File.separator + "host_ip_list_wide");
        //System.out.println(monkey.containersByHost);
        //monkey.twelveLittleMonkeys(0.1);
        weibullDistribution();
    }

    public Monkey(String hostListFile) {
        containersByHost = new HashMap<>();
        try {
            BufferedReader br = new BufferedReader(new FileReader(hostListFile));
            String line;
            while ((line = br.readLine()) != null) {
                containersByHost.put(line.split(":")[0], new ArrayList<>());
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
            p = Runtime.getRuntime().exec("ssh -t -t obarais@" + ipAddress + " sudo docker ps -a -q");
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
        unpauseAll();
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
                        p = Runtime.getRuntime().exec("ssh -t -t obarais@" + host + " sudo docker pause " + containers.get(i));
                        p.waitFor();
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
                p = Runtime.getRuntime().exec("ssh -t -t obarais@" + host + " sudo docker ps -a |grep Paused| awk '{print $1;}'|while read i; do sudo docker unpause $i; done");
                p.waitFor();
                //processPrintout(p);
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public static void weibullDistribution() {
        WeibullDistribution wd = new WeibullDistribution(1.5, 20);
        System.out.println(Arrays.toString(wd.sample(10)));
        System.out.println(Arrays.toString(wd.sample(10)));
        System.out.println(Arrays.toString(wd.sample(10)));
        System.out.println(Arrays.toString(wd.sample(10)));
        System.out.println(Arrays.toString(wd.sample(10)));
    }

    public void run() {
        while (true) {
            monkeyRun();
            startWait();
        }
    }

    public void monkeyRun() {
        if(Main.tick > 5) {
            twelveLittleMonkeys(0.1 + 0.8 * Math.sin((double) (Main.tick - 5) / 20d));
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
