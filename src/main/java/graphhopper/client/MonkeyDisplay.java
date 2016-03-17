package graphhopper.client;

import graphhopper.client.demo.Main;

import javax.swing.*;
import javax.swing.border.BevelBorder;
import java.awt.*;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by aelie on 14/03/16.
 */
public class MonkeyDisplay extends JFrame{

    Monkey monkey;
    JPanel jP_Matrix;
    Map<String, JPanel> panelByContainer;

    public MonkeyDisplay(Monkey monkey) {
        super();
        this.monkey = monkey;
        panelByContainer = new LinkedHashMap<>();
        init();
        setTitle("Monkey display");
        setLocation(200, 200);
        setSize(400, 400);
        setVisible(true);
    }

    public void init() {
        jP_Matrix = new JPanel(new GridLayout((int)Math.ceil(Math.sqrt(Main.allPlatforms.size())), (int)Math.ceil(Math.sqrt(Main.allPlatforms.size()))));
        this.add(jP_Matrix);
        List<String> containers = new ArrayList<>(monkey.platformByContainer.keySet());
        containers.sort((c1, c2) -> Integer.parseInt(monkey.platformByContainer.get(c1).getId().substring(1, monkey.platformByContainer.get(c1).getId().length()))
                - Integer.parseInt(monkey.platformByContainer.get(c2).getId().substring(1, monkey.platformByContainer.get(c2).getId().length())));
        for (String container : containers) {
            JPanel tmp = new JPanel(new BorderLayout());
            tmp.setBackground(Color.green);
            tmp.setBorder(new BevelBorder(0));
            tmp.add(new JLabel(monkey.platformByContainer.get(container).getId(), SwingConstants.CENTER), BorderLayout.CENTER);
            panelByContainer.put(container, tmp);
            jP_Matrix.add(tmp);
        }
    }

    public void switchPlatform(String container, boolean isRunning) {
        panelByContainer.get(container).setBackground(isRunning ? Color.green : Color.red);
    }

    public void switchAllPlatforms(boolean isRunning) {
        for(String container : panelByContainer.keySet()) {
            switchPlatform(container, isRunning);
        }
    }
}
