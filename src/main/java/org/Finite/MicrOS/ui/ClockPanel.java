package org.Finite.MicrOS.ui;

import javax.swing.*;
import java.awt.*;
import java.text.SimpleDateFormat;
import java.util.Date;

public class ClockPanel extends JPanel {
    private final JLabel timeLabel;
    private final SimpleDateFormat timeFormat;
    private final Timer timer;

    public ClockPanel() {
        setOpaque(false);
        timeFormat = new SimpleDateFormat("HH:mm"); // don't need to show secconds
        timeLabel = new JLabel();
        timeLabel.setForeground(Color.WHITE);
        timeLabel.setFont(new Font("Segoe UI", Font.PLAIN, 12));
        add(timeLabel);

        timer = new Timer(60000, e -> updateTime()); // update every minute
        timer.start();
        updateTime();
    }

    private void updateTime() {
        timeLabel.setText(timeFormat.format(new Date()));
    }

    @Override
    public void removeNotify() {
        super.removeNotify();
        timer.stop();
    }
}
