import javax.swing.*;
import java.awt.*;
import java.util.*;

/* RADIO    = OFF,
 * OFF      = (on -> TOP),
 * TOP      = (scan -> SCANNING | reset -> TOP | off -> OFF),
 * SCANNING = (lock -> LOCKED | end -> BOTTOM | reset -> TOP | off -> OFF),
 * LOCKED   = (scan -> SCANNING | reset -> TOP | off -> OFF),
 * BOTTOM   = (scan -> BOTTOM | reset -> TOP | off -> OFF).          */
public class Radio extends JPanel {
    static final String[][] LTS = {      // {state, action, next state}
        {"OFF", "on", "TOP"},
        {"TOP", "scan", "SCANNING"}, {"TOP", "reset", "TOP"}, {"TOP", "off", "OFF"},
        {"SCANNING", "lock", "LOCKED"}, {"SCANNING", "end", "BOTTOM"},
        {"SCANNING", "reset", "TOP"}, {"SCANNING", "off", "OFF"},
        {"LOCKED", "scan", "SCANNING"}, {"LOCKED", "reset", "TOP"}, {"LOCKED", "off", "OFF"},
        {"BOTTOM", "scan", "BOTTOM"}, {"BOTTOM", "reset", "TOP"}, {"BOTTOM", "off", "OFF"}};
    static final java.util.List<Integer> STATIONS = java.util.List.of(1061, 1021, 999, 973, 935, 895);

    String state = "OFF";
    int freq = 1080;                                  // tenths of MHz
    final StringBuilder trace = new StringBuilder();
    final JButton power = new JButton("On"), scan = new JButton("Scan"), reset = new JButton("Reset");
    final javax.swing.Timer scanner = new javax.swing.Timer(40, e -> tick());

    boolean perform(String action) {
        for (String[] t : LTS)
            if (t[0].equals(state) && t[1].equals(action)) {
                if (action.equals("on") || action.equals("reset")) freq = 1080;
                state = t[2];
                trace.append(trace.length() == 0 ? "" : " -> ").append(action);
                if (state.equals("SCANNING")) scanner.start(); else scanner.stop();
                update();
                return true;
            }
        return false;
    }

    boolean allowed(String action) {
        return Arrays.stream(LTS).anyMatch(t -> t[0].equals(state) && t[1].equals(action));
    }

    void tick() {                                     // radio performs lock / end itself
        if (--freq <= 880) { freq = 880; perform("end"); }
        else if (STATIONS.contains(freq)) perform("lock");
        else repaint();
    }

    void update() {
        power.setText(state.equals("OFF") ? "On" : "Off");
        scan.setEnabled(allowed("scan"));
        reset.setEnabled(allowed("reset"));
        repaint();
    }

    @Override protected void paintComponent(Graphics g0) {
        super.paintComponent(g0);
        Graphics2D g = (Graphics2D) g0;
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        boolean on = !state.equals("OFF");
        g.setColor(new Color(45, 48, 54));                       // body
        g.fillRoundRect(10, 10, 580, 190, 30, 30);
        g.setColor(on ? new Color(172, 212, 120) : new Color(38, 46, 32));   // LCD
        g.fillRoundRect(30, 30, 230, 100, 12, 12);
        g.setColor(new Color(240, 232, 210));                    // dial
        g.fillRoundRect(280, 30, 290, 100, 12, 12);
        g.setColor(Color.DARK_GRAY);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 12));
        for (int f = 880; f <= 1080; f += 10) {
            int x = 295 + (f - 880) * 260 / 200;
            g.drawLine(x, 55, x, f % 40 == 0 ? 72 : 63);
            if (f % 40 == 0) g.drawString("" + f / 10, x - 8, 50);
        }
        g.setColor(new Color(30, 120, 80));                      // station markers
        for (int s : STATIONS) g.fillOval(295 + (s - 880) * 260 / 200 - 4, 105, 8, 8);
        g.setColor(on ? Color.RED : Color.GRAY);                 // needle
        g.fillRect(295 + (freq - 880) * 260 / 200 - 1, 38, 3, 84);
        if (on) {
            g.setColor(new Color(28, 44, 18));
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 40));
            g.drawString(String.format("%5.1f", freq / 10.0), 40, 85);
            g.setFont(new Font(Font.MONOSPACED, Font.BOLD, 14));
            g.drawString("MHz   " + state, 45, 115);
        }
        g.setColor(Color.LIGHT_GRAY);
        g.setFont(new Font(Font.SANS_SERIF, Font.PLAIN, 13));
        String t = trace.length() > 60 ? "..." + trace.substring(trace.length() - 60) : trace.toString();
        g.drawString("State: " + state + "    Trace: " + t, 30, 165);
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> {
            Radio r = new Radio();
            r.setPreferredSize(new Dimension(600, 210));
            r.power.addActionListener(e -> r.perform(r.state.equals("OFF") ? "on" : "off"));
            r.scan.addActionListener(e -> r.perform("scan"));
            r.reset.addActionListener(e -> r.perform("reset"));
            JPanel buttons = new JPanel();
            buttons.add(r.power); buttons.add(r.scan); buttons.add(r.reset);
            JFrame f = new JFrame("FM Radio - FSP RADIO");
            f.add(r, BorderLayout.CENTER);
            f.add(buttons, BorderLayout.SOUTH);
            r.update();
            f.pack();
            f.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
            f.setVisible(true);
        });
    }
}
