package graphhopper.client;

import org.jfree.chart.ChartPanel;
import org.jfree.chart.JFreeChart;
import org.jfree.chart.axis.CategoryAxis;
import org.jfree.chart.axis.NumberAxis;
import org.jfree.chart.labels.BoxAndWhiskerToolTipGenerator;
import org.jfree.chart.plot.CategoryPlot;
import org.jfree.chart.renderer.category.BoxAndWhiskerRenderer;
import org.jfree.data.statistics.DefaultBoxAndWhiskerCategoryDataset;
import org.jfree.ui.ApplicationFrame;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by aelie on 17/03/16.
 */
public class ResultsDisplay extends ApplicationFrame {

    DefaultBoxAndWhiskerCategoryDataset boxplotDatasetDCR = new DefaultBoxAndWhiskerCategoryDataset();
    DefaultBoxAndWhiskerCategoryDataset boxplotDatasetRR = new DefaultBoxAndWhiskerCategoryDataset();
    DefaultBoxAndWhiskerCategoryDataset boxplotDatasetTS = new DefaultBoxAndWhiskerCategoryDataset();
    //Smart -> 10% -> List<values>
    Map<String, Map<Double, List<Double>>> deadClientsRatioDataset;
    Map<String, Map<Double, List<Double>>> requestRetriesDataset;
    Map<String, Map<Double, List<Double>>> totalServicesDataset;

    public static final String[] experiences = {"Current", "External CSV 1", "External CSV 2"};
    public static final String title = "Comparative results display";

    public ResultsDisplay() {
        super(title);
        getContentPane().setLayout(new GridLayout(2, 1));
        initDatasets();

        CategoryAxis xAxisDCR = new CategoryAxis("Type");
        NumberAxis yAxisDCR = new NumberAxis("Value");
        yAxisDCR.setAutoRangeIncludesZero(false);
        BoxAndWhiskerRenderer rendererDCR = new BoxAndWhiskerRenderer();
        rendererDCR.setFillBox(false);
        rendererDCR.setBaseToolTipGenerator(new BoxAndWhiskerToolTipGenerator());
        CategoryPlot plotDCR = new CategoryPlot(boxplotDatasetDCR, xAxisDCR, yAxisDCR, rendererDCR);
        JFreeChart chartDCR = new JFreeChart("Dead clients ratio", new Font("SansSerif", Font.BOLD, 14), plotDCR, true);
        ChartPanel chartPanelDCR = new ChartPanel(chartDCR);
        chartPanelDCR.setPreferredSize(new java.awt.Dimension(800, 400));

        CategoryAxis xAxisRR = new CategoryAxis("Type");
        NumberAxis yAxisRR = new NumberAxis("Value");
        yAxisRR.setAutoRangeIncludesZero(false);
        BoxAndWhiskerRenderer rendererRR = new BoxAndWhiskerRenderer();
        rendererRR.setFillBox(false);
        rendererRR.setBaseToolTipGenerator(new BoxAndWhiskerToolTipGenerator());
        CategoryPlot plotRR = new CategoryPlot(boxplotDatasetRR, xAxisRR, yAxisRR, rendererRR);
        JFreeChart chartRR = new JFreeChart("Request retries", new Font("SansSerif", Font.BOLD, 14), plotRR, true);
        ChartPanel chartPanelRR = new ChartPanel(chartRR);
        chartPanelRR.setPreferredSize(new java.awt.Dimension(800, 400));

        CategoryAxis xAxisTS = new CategoryAxis("Type");
        NumberAxis yAxisTS = new NumberAxis("Value");
        yAxisTS.setAutoRangeIncludesZero(false);
        BoxAndWhiskerRenderer rendererTS = new BoxAndWhiskerRenderer();
        rendererTS.setFillBox(false);
        rendererTS.setBaseToolTipGenerator(new BoxAndWhiskerToolTipGenerator());
        CategoryPlot plotTS = new CategoryPlot(boxplotDatasetTS, xAxisTS, yAxisTS, rendererTS);
        JFreeChart chartTS = new JFreeChart("Total services", new Font("SansSerif", Font.BOLD, 14), plotTS, true);
        ChartPanel chartPanelTS = new ChartPanel(chartTS);
        chartPanelTS.setPreferredSize(new java.awt.Dimension(800, 400));

        //setContentPane(chartPanel);
        getContentPane().add(chartPanelDCR);
        getContentPane().add(chartPanelRR);
        //getContentPane().add(chartPanelTS);
        pack();
        setVisible(true);
    }

    public void initDatasets() {
        deadClientsRatioDataset = new HashMap<>();
        requestRetriesDataset = new HashMap<>();
        totalServicesDataset = new HashMap<>();
        for (String experience : experiences) {
            deadClientsRatioDataset.put(experience, new HashMap<>());
            requestRetriesDataset.put(experience, new HashMap<>());
            totalServicesDataset.put(experience, new HashMap<>());
        }
    }

    public void addDeadClientsRatioData(String experience, int tick, double deadServersRatio, double value) {
        if (!deadClientsRatioDataset.get(experience).containsKey(deadServersRatio)) {
            deadClientsRatioDataset.get(experience).put(deadServersRatio, new ArrayList<>());
        }
        deadClientsRatioDataset.get(experience).get(deadServersRatio).add(value);
    }

    public void addRequestRetriesData(String experience, int tick, double deadServersRatio, double value) {
        if (!requestRetriesDataset.get(experience).containsKey(deadServersRatio)) {
            requestRetriesDataset.get(experience).put(deadServersRatio, new ArrayList<>());
        }
        requestRetriesDataset.get(experience).get(deadServersRatio).add(value);
    }

    public void addTotalServicesData(String experience, int tick, double deadServersRatio, double value) {
        if (!totalServicesDataset.get(experience).containsKey(deadServersRatio)) {
            totalServicesDataset.get(experience).put(deadServersRatio, new ArrayList<>());
        }
        totalServicesDataset.get(experience).get(deadServersRatio).add(value);
    }

    public void update() {
        for (String experience : experiences) {
            for (Double deadServersRatio : deadClientsRatioDataset.get(experience).keySet()) {
                boxplotDatasetDCR.add(deadClientsRatioDataset.get(experience).get(deadServersRatio), experience, deadServersRatio);
                boxplotDatasetRR.add(requestRetriesDataset.get(experience).get(deadServersRatio), experience, deadServersRatio);
                boxplotDatasetTS.add(totalServicesDataset.get(experience).get(deadServersRatio), experience, deadServersRatio);
            }
        }
    }
}
