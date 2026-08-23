package com.resqhub.view;

import java.awt.BorderLayout;
import java.awt.Font;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;

import com.resqhub.controller.ActionResult;
import com.resqhub.controller.StatsController;

/** Live operational overview shown to staff on the Overview screen. */
public class StatsPanel extends JPanel implements Refreshable {

    private final StatsController controller = new StatsController();
    private final JTextArea summaryArea = new JTextArea(10, 40);

    public StatsPanel() {
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));

        summaryArea.setEditable(false);
        summaryArea.setFont(new Font("Monospaced", Font.PLAIN, 14));

        JPanel area = new JPanel(new BorderLayout(6, 6));
        area.setBorder(BorderFactory.createTitledBorder(
                "Live situation overview"));
        area.add(new JScrollPane(summaryArea), BorderLayout.CENTER);

        JButton refreshButton = new JButton("Refresh");
        refreshButton.addActionListener(event -> refreshData());
        area.add(refreshButton, BorderLayout.SOUTH);

        add(area, BorderLayout.CENTER);
        refreshData();
    }

    @Override
    public void refreshData() {
        ActionResult result = controller.getSummary();
        if (result.isSuccess()) {
            Object payload = result.getData();
            summaryArea.setText(payload == null
                    ? result.getMessage()
                    : String.valueOf(payload));
        } else {
            summaryArea.setText(result.getMessage());
        }
    }
}
