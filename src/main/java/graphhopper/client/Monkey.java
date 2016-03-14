package graphhopper.client;

import graphhopper.client.demo.Main;

import java.io.*;
import java.util.*;
import java.util.stream.Collector;
import java.util.stream.Collectors;

/**
 * Created by aelie on 30/11/15.
 */
public class Monkey extends Thread {

    public static final int SINUSOIDE = 0;
    public static final int CONSTANT = 1;
    public static final int EXTINCTION = 2;

    public boolean newTick = true;
    Map<String, List<String>> containersByHost;
    Map<String, List<String>> pausedContainersByHost;
    Map<Platform, String> containerByPlatform;
    Map<String, Platform> platformByContainer;
    double ratio = 0;
    int type;

    boolean verbose = false;

    MonkeyDisplay monkeyDisplay;

    public Monkey(String hostListFile, int type) {
        this.type = type;
        containersByHost = new HashMap<>();
        pausedContainersByHost = new HashMap<>();
        containerByPlatform = new HashMap<>();
        platformByContainer = new HashMap<>();
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
        matchContainers();
        unpauseAll();
        monkeyDisplay = new MonkeyDisplay(this);
    }

    public void matchContainers() {
        for (String host : containersByHost.keySet()) {
            Process p;
            try {
                if (host.equals("localhost") || host.equals("127.0.0.1")) {
                    p = Runtime.getRuntime().exec("docker ps -a");
                } else {
                    p = Runtime.getRuntime().exec("ssh -t -t " + host + /*" sudo*/ " docker ps -a");
                }
                p.waitFor();
                BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
                String line = br.readLine();
                while ((line = br.readLine()) != null) {
                    String container = line.substring(0, 12);
                    int port = Integer.parseInt(line.split("->")[0].substring(line.split("->")[0].length() - 4));
                    Platform platform = Main.allPlatforms.stream().filter(pl -> pl.getAddress().equals(host.split("@")[1]) && pl.port == port).findAny().orElse(null);
                    if(platform != null) {
                        containerByPlatform.put(platform, container);
                        platformByContainer.put(container, platform);
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    public List<String> getContainers(String ipAddress) {
        List<String> result = new ArrayList<>();
        Process p;
        try {
            if (ipAddress.equals("localhost") || ipAddress.equals("127.0.0.1")) {
                p = Runtime.getRuntime().exec("docker ps -aq");
            } else {
                p = Runtime.getRuntime().exec("ssh -t -t " + ipAddress + /*" sudo*/ " docker ps -a -q");
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
        int pausedContainersNumber = (int) (Main.allPlatforms.size() * ratio);
        List<String> hosts = new ArrayList<>(containersByHost.keySet());
        List<String> containers = new ArrayList<>(/*containersByHost.values().stream()
                .flatMap(Collection::stream)
                .collect(Collectors.toList())*/platformByContainer.keySet());
        Collections.shuffle(containers);
        //unpause
        int count = 0;
        if (verbose) System.out.println("Unpaused " + pausedContainersByHost.values());
        long unpauseStart = System.currentTimeMillis();
        if(ratio > 0) {
            for (String host : pausedContainersByHost.keySet()) {
                String pausedContainersAsString = pausedContainersByHost.get(host).stream()
                        .collect(Collectors.joining(" "));
                System.out.println(pausedContainersAsString);
                count = (int) pausedContainersByHost.get(host).stream().count();
                //for (String pausedContainer : pausedContainersByHost.get(host)) {
                try {
                    //p = Runtime.getRuntime().exec("ssh -t -t obarais@" + host + " sudo iptables -A INPUT -p tcp -m tcp --dport " + port + " -j ACCEPT");
                    if (host.equals("localhost") || host.equals("127.0.0.1")) {
                        p = Runtime.getRuntime().exec("docker unpause " + /*pausedContainer*/pausedContainersAsString);
                    } else {
                        p = Runtime.getRuntime().exec("ssh -t -t " + host + /*" sudo*/" docker unpause " + /*pausedContainer*/pausedContainersAsString);
                    }
                    p.waitFor();
                    monkeyDisplay.switchAllPlatforms(true);
                    //count++;
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                //}
                pausedContainersByHost.get(host).clear();
            }
        }
        System.out.println("Unpaused " + count + " containers");
        System.out.println("Unpause : " + (System.currentTimeMillis() - unpauseStart));
        //pause
        long pauseStart = System.currentTimeMillis();
        for (int i = 0; i < pausedContainersNumber; i++) {
            Collections.shuffle(hosts);
            for (String host : hosts) {
                if (containersByHost.get(host).contains(containers.get(i))) {
                    monkeyDisplay.switchPlatform(containers.get(i), false);
                    try {
                        //p = Runtime.getRuntime().exec("ssh -t -t obarais@" + host + " sudo iptables -A INPUT -p tcp -m tcp --dport " + port + " -j DROP");
                        if (host.equals("localhost") || host.equals("127.0.0.1")) {
                            p = Runtime.getRuntime().exec("docker pause " + containers.get(i));
                        } else {
                            p = Runtime.getRuntime().exec("ssh -t -t " + host + /*" sudo*/" docker pause " + containers.get(i));
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
        if (verbose) System.out.println("Paused " + pausedContainersByHost.values());
        System.out.println("Paused " + pausedContainersNumber + " containers");
        System.out.println("Pause : " + (System.currentTimeMillis() - pauseStart));
        Info.getInstance().setMonkeyPausedPlatforms(Main.tick, pausedContainersNumber);
        /*for (String host : hosts) {
            System.out.println("Total paused containers on " + host + ": " + getPausedContainerNumber(host));
        }*/
    }

    public static int getPausedContainerNumber(String host) {
        Process p;
        String line = "";
        try {
            if (host.equals("localhost") || host.equals("127.0.0.1")) {
                p = Runtime.getRuntime().exec("docker ps -a |grep Paused| awk '{print $1;}'|wc -l");
            } else {
                p = Runtime.getRuntime().exec("ssh -t -t " + host + /*" sudo*/ " docker ps -a |grep Paused| awk '{print $1;}'|wc -l");
            }
            p.waitFor();
            BufferedReader br = new BufferedReader(new InputStreamReader(p.getInputStream()));
            line = br.readLine();
            return Integer.parseInt(line);
        } catch (IOException e) {
            e.printStackTrace();
        } catch (InterruptedException e) {
            e.printStackTrace();
        } catch (NumberFormatException nfe) {
            System.err.println(line);
            return -1;
        }
        return -1;
    }

    public int getTotalPausedContainerNumber() {
        int result = 0;
        for (String host : containersByHost.keySet()) {
            result += getPausedContainerNumber(host);
        }
        return result;
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
                if (host.equals("localhost") || host.equals("127.0.0.1")) {
                    p = Runtime.getRuntime().exec("docker ps -a |grep Paused| awk '{print $1;}'|while read i; do docker unpause $i; done");
                } else {
                    p = Runtime.getRuntime().exec("ssh -t -t " + host + /*" sudo*/ " docker ps -a |grep Paused| awk '{print $1;}'|while read i; do "/*+"sudo "*/+"docker unpause $i; done");
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

    public void run() {
        while (true) {
            monkeyRun();
            startWait();
        }
    }

    public void monkeyRun() {
        if (Main.tick > 5) {
            switch (type) {
                case SINUSOIDE:
                    ratio = (0.5 * (1 + Math.sin((double) (Main.tick - 5) / 10d)));
                    break;
                case CONSTANT:
                    ratio = 0.3;
                    break;
                case EXTINCTION:
                    ratio = 1. / Main.allPlatforms.size();
                    break;
                default:
                    ratio = 1;
            }
            twelveLittleMonkeys(ratio);
        }

        newTick = false;
    }

    public void specialMonkeyRun() {
        if (Main.tick >= 100 && Main.tick < 200) {
            ratio = 0.2;
        } else if(Main.tick >= 200 && Main.tick < 300) {
            ratio = 0.4;
        } else if(Main.tick >= 300 && Main.tick < 400) {
            ratio = 0.6;
        } else if(Main.tick >= 400 && Main.tick < 500) {
            ratio = 0.8;
        } else if(Main.tick >= 500 && Main.tick < 600) {
            ratio = 0.95;
        } else {
            ratio = 0.05;
        }
        twelveLittleMonkeys(ratio);
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

    public double getRatio() {
        return ratio;
    }
}
