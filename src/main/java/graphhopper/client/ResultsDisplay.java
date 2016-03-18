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

    SimpleBoxAndWhiskerCategoryDataset boxplotDatasetDCR = new SimpleBoxAndWhiskerCategoryDataset();
    SimpleBoxAndWhiskerCategoryDataset boxplotDatasetRR = new SimpleBoxAndWhiskerCategoryDataset();
    SimpleBoxAndWhiskerCategoryDataset boxplotDatasetTS = new SimpleBoxAndWhiskerCategoryDataset();
    //Smart -> 10% -> List<values>
    Map<String, Map<Double, List<Double>>> deadClientsRatioDataset;
    Map<String, Map<Double, List<Double>>> requestRetriesDataset;
    Map<String, Map<Double, List<Double>>> totalServicesDataset;

    public static final String[] experiences = {"Equitable/Popular", "Random Baseline", "Initial"};
    public static final String title = "Comparative results display";

    public ResultsDisplay() {
        super(title);
        getContentPane().setLayout(new GridLayout(2, 1));
        initDatasets();

        CategoryAxis xAxisDCR = new CategoryAxis("# Server Failures");
        NumberAxis yAxisDCR = new NumberAxis("% Unsatisfied Requests");
        yAxisDCR.setAutoRangeIncludesZero(false);
        BoxAndWhiskerRenderer rendererDCR = new BoxAndWhiskerRenderer();
        rendererDCR.setFillBox(false);
        rendererDCR.setBaseToolTipGenerator(new BoxAndWhiskerToolTipGenerator());
        rendererDCR.setMeanVisible(false);
        rendererDCR.setBaseOutlineStroke(new BasicStroke(2.5f));
        rendererDCR.setSeriesStroke(0, new BasicStroke(2.5f));
        rendererDCR.setSeriesStroke(1, new BasicStroke(2.5f));
        rendererDCR.setSeriesStroke(2, new BasicStroke(2.5f));
        CategoryPlot plotDCR = new CategoryPlot(boxplotDatasetDCR, xAxisDCR, yAxisDCR, rendererDCR);
        JFreeChart chartDCR = new JFreeChart("Request Failure Ratio", new Font("SansSerif", Font.BOLD, 14), plotDCR, true);
        ChartPanel chartPanelDCR = new ChartPanel(chartDCR);
        chartPanelDCR.setPreferredSize(new java.awt.Dimension(1000, 400));

        CategoryAxis xAxisRR = new CategoryAxis("# Server Failures");
        NumberAxis yAxisRR = new NumberAxis("# Request Retries");
        yAxisRR.setAutoRangeIncludesZero(false);
        BoxAndWhiskerRenderer rendererRR = new BoxAndWhiskerRenderer();
        rendererRR.setFillBox(false);
        rendererRR.setBaseToolTipGenerator(new BoxAndWhiskerToolTipGenerator());
        rendererRR.setMeanVisible(false);
        rendererRR.setBaseOutlineStroke(new BasicStroke(2.5f));
        rendererRR.setSeriesStroke(0, new BasicStroke(2.5f));
        rendererRR.setSeriesStroke(1, new BasicStroke(2.5f));
        rendererRR.setSeriesStroke(2, new BasicStroke(2.5f));
        CategoryPlot plotRR = new CategoryPlot(boxplotDatasetRR, xAxisRR, yAxisRR, rendererRR);
        JFreeChart chartRR = new JFreeChart("Request Retries", new Font("SansSerif", Font.BOLD, 14), plotRR, true);
        ChartPanel chartPanelRR = new ChartPanel(chartRR);
        chartPanelRR.setPreferredSize(new java.awt.Dimension(1000, 400));

        CategoryAxis xAxisTS = new CategoryAxis("# Server Failures");
        NumberAxis yAxisTS = new NumberAxis("Value");
        yAxisTS.setAutoRangeIncludesZero(false);
        BoxAndWhiskerRenderer rendererTS = new BoxAndWhiskerRenderer();
        rendererTS.setFillBox(false);
        rendererTS.setBaseToolTipGenerator(new BoxAndWhiskerToolTipGenerator());
        rendererTS.setMeanVisible(false);
        rendererTS.setBaseOutlineStroke(new BasicStroke(2.5f));
        rendererTS.setSeriesStroke(0, new BasicStroke(2.5f));
        rendererTS.setSeriesStroke(1, new BasicStroke(2.5f));
        rendererTS.setSeriesStroke(2, new BasicStroke(2.5f));
        CategoryPlot plotTS = new CategoryPlot(boxplotDatasetTS, xAxisTS, yAxisTS, rendererTS);
        JFreeChart chartTS = new JFreeChart("Total Request Types", new Font("SansSerif", Font.BOLD, 14), plotTS, true);
        ChartPanel chartPanelTS = new ChartPanel(chartTS);
        chartPanelTS.setPreferredSize(new java.awt.Dimension(1000, 400));

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

    public void addDeadClientsRatioData(String experience, double deadServersRatio, double value) {
        if (!deadClientsRatioDataset.get(experience).containsKey(deadServersRatio)) {
            deadClientsRatioDataset.get(experience).put(deadServersRatio, new ArrayList<>());
        }
        deadClientsRatioDataset.get(experience).get(deadServersRatio).add(value);
    }

    public void addRequestRetriesData(String experience, double deadServersRatio, double value) {
        if (!requestRetriesDataset.get(experience).containsKey(deadServersRatio)) {
            requestRetriesDataset.get(experience).put(deadServersRatio, new ArrayList<>());
        }
        requestRetriesDataset.get(experience).get(deadServersRatio).add(value);
    }

    public void addTotalServicesData(String experience, double deadServersRatio, double value) {
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
