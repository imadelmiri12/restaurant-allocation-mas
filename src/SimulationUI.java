import com.formdev.flatlaf.FlatDarkLaf;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.text.*;
import java.awt.*;
import java.awt.event.*;
import java.awt.geom.RoundRectangle2D;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;

/**
 * SimulationUI — dark luxury theme, FlatLaf-based.
 *
 * New in this version:
 *   • setStatus(String)  — badge changes color per state:
 *       Ready      → gray/blue
 *       Running    → blue  (pulsing)
 *       Completed  → green
 *       Error      → red
 *   • showSummary(SimulationResult) — prints a formatted final report
 *
 * Dependency: FlatLaf >= 3.x  →  https://www.formdev.com/flatlaf/
 */
public class SimulationUI extends JFrame {

    // ── Palette ────────────────────────────────────────────────────────────────
    private static final Color BG          = new Color(0x0D, 0x0F, 0x14);
    private static final Color SURFACE     = new Color(0x13, 0x16, 0x1E);
    private static final Color SURFACE2    = new Color(0x1A, 0x1E, 0x2A);
    private static final Color BORDER_COL  = new Color(0xFF, 0xFF, 0xFF, 18);
    private static final Color ACCENT      = new Color(0xF0, 0xC0, 0x60);
    private static final Color MUTED       = new Color(0x7A, 0x78, 0x90);
    private static final Color TEXT_MAIN   = new Color(0xE8, 0xE6, 0xF0);
    private static final Color LOG_INFO    = new Color(0x8A, 0x8A, 0xA0);
    private static final Color LOG_SUCCESS = new Color(0x5F, 0xBA, 0x8A);
    private static final Color LOG_ERROR   = new Color(0xE0, 0x5A, 0x6A);
    private static final Color LOG_HL      = new Color(0xF0, 0xC0, 0x60);

    // ── Status badge colors ────────────────────────────────────────────────────
    //   dot color          border tint
    private static final Color ST_READY_DOT   = new Color(0x6A, 0x8A, 0xC0);   // slate-blue
    private static final Color ST_READY_TEXT  = new Color(0x6A, 0x8A, 0xC0);
    private static final Color ST_RUN_DOT     = new Color(0x4A, 0x9A, 0xF0);   // bright blue
    private static final Color ST_RUN_TEXT    = new Color(0x4A, 0x9A, 0xF0);
    private static final Color ST_DONE_DOT    = new Color(0x5F, 0xBA, 0x8A);   // green
    private static final Color ST_DONE_TEXT   = new Color(0x5F, 0xBA, 0x8A);
    private static final Color ST_ERROR_DOT   = new Color(0xE0, 0x5A, 0x6A);   // red
    private static final Color ST_ERROR_TEXT  = new Color(0xE0, 0x5A, 0x6A);

    // ── Fonts ──────────────────────────────────────────────────────────────────
    private static final Font FONT_BODY    = new Font("Segoe UI",  Font.PLAIN,  13);
    private static final Font FONT_LABEL   = new Font("Segoe UI",  Font.BOLD,   11);
    private static final Font FONT_MONO    = new Font("Consolas",  Font.PLAIN,  13);
    private static final Font FONT_TITLE   = new Font("Georgia",   Font.ITALIC, 26);
    private static final Font FONT_EYEBROW = new Font("Consolas",  Font.PLAIN,  10);

    private static final DateTimeFormatter TS = DateTimeFormatter.ofPattern("HH:mm:ss");

    // ── Widgets ────────────────────────────────────────────────────────────────
    private JTextField nField, mField, baseCapacityField, saturationField, pField;
    private JTextPane  logPane;
    private StyledDocument logDoc;
    private JButton    startButton;
    private JLabel     statusDot;
    private JLabel     statusText;
    private JPanel     badgeWrap;          // kept so we can repaint border
    private JLabel     ruleP;
    private boolean    pulseState  = true;
    private SimStatus  currentStatus = SimStatus.READY;

    // ── Status enum ────────────────────────────────────────────────────────────
    public enum SimStatus { READY, RUNNING, COMPLETED, ERROR }

    // ══════════════════════════════════════════════════════════════════════════

    public SimulationUI() {
        setTitle("SMA / JADE — Allocation de Restaurants");
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setSize(980, 660);
        setLocationRelativeTo(null);
        setBackground(BG);

        try { setShape(new RoundRectangle2D.Double(0, 0, 980, 660, 20, 20)); }
        catch (Exception ignored) {}

        initUI();
        startPulse();
        log("System ready. Configure parameters and press Start.", LogType.SUCCESS);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  UI BUILD
    // ══════════════════════════════════════════════════════════════════════════

    private void initUI() {
        JPanel root = new JPanel(new BorderLayout(0, 0));
        root.setBackground(BG);
        root.setBorder(new EmptyBorder(24, 24, 22, 24));
        setContentPane(root);

        root.add(buildHeader(), BorderLayout.NORTH);
        root.add(buildCenter(), BorderLayout.CENTER);
        root.add(buildBottom(), BorderLayout.SOUTH);
    }

    // ── Header ─────────────────────────────────────────────────────────────────
    private JPanel buildHeader() {
        JPanel left = new JPanel();
        left.setLayout(new BoxLayout(left, BoxLayout.Y_AXIS));
        left.setOpaque(false);

        JLabel eyebrow = new JLabel("SMA / JADE — MULTI-AGENT SYSTEM");
        eyebrow.setFont(FONT_EYEBROW);
        eyebrow.setForeground(ACCENT);

        JLabel title = new JLabel("Restaurant Allocation Simulation");
        title.setFont(FONT_TITLE);
        title.setForeground(TEXT_MAIN);

        JLabel subtitle = new JLabel("Configure agents, launch the simulation, and observe emergent behaviour");
        subtitle.setFont(FONT_BODY);
        subtitle.setForeground(MUTED);

        left.add(eyebrow);
        left.add(Box.createVerticalStrut(5));
        left.add(title);
        left.add(Box.createVerticalStrut(4));
        left.add(subtitle);

        // Status badge
        statusDot = new JLabel("●");
        statusDot.setFont(new Font("Segoe UI", Font.PLAIN, 10));
        statusDot.setForeground(ST_READY_DOT);

        statusText = new JLabel("Ready");
        statusText.setFont(FONT_MONO);
        statusText.setForeground(ST_READY_TEXT);

        badgeWrap = new RoundPanel(SURFACE, 100);
        badgeWrap.setLayout(new FlowLayout(FlowLayout.CENTER, 6, 7));
        badgeWrap.setBorder(BorderFactory.createLineBorder(new Color(ST_READY_DOT.getRed(),
                ST_READY_DOT.getGreen(), ST_READY_DOT.getBlue(), 60)));
        badgeWrap.add(statusDot);
        badgeWrap.add(statusText);

        JPanel badgeHolder = new JPanel(new FlowLayout(FlowLayout.RIGHT, 0, 8));
        badgeHolder.setOpaque(false);
        badgeHolder.add(badgeWrap);

        JPanel row = new JPanel(new BorderLayout());
        row.setOpaque(false);
        row.add(left,        BorderLayout.WEST);
        row.add(badgeHolder, BorderLayout.EAST);

        JSeparator sep = new JSeparator();
        sep.setForeground(BORDER_COL);

        JPanel wrap = new JPanel(new BorderLayout(0, 14));
        wrap.setOpaque(false);
        wrap.setBorder(new EmptyBorder(0, 0, 20, 0));
        wrap.add(row, BorderLayout.CENTER);
        wrap.add(sep, BorderLayout.SOUTH);

        return wrap;
    }

    private JPanel buildCenter() {
        JPanel p = new JPanel(new GridLayout(1, 2, 18, 0));
        p.setOpaque(false);
        p.setBorder(new EmptyBorder(0, 0, 18, 0));
        p.add(buildParamsPanel());
        p.add(buildLogsPanel());
        return p;
    }

    private JPanel buildParamsPanel() {
        JPanel panel = new RoundPanel(SURFACE, 16);
        panel.setLayout(new BorderLayout(0, 14));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COL),
            new EmptyBorder(20, 20, 20, 20)
        ));

        panel.add(eyebrowLabel("Parameters"), BorderLayout.NORTH);

        nField            = styledField("5");
        mField            = styledField("3");
        baseCapacityField = styledField("8");
        saturationField   = styledField("0.3");
        pField            = styledField("2");

        JPanel form = new JPanel(new GridBagLayout());
        form.setOpaque(false);
        GridBagConstraints g = new GridBagConstraints();
        g.fill = GridBagConstraints.HORIZONTAL;

        addRow(form, g, 0, "Nombre de personnes (N)",   nField);
        addRow(form, g, 1, "Nombre de restaurants (M)", mField);
        addRow(form, g, 2, "Capacité de base",           baseCapacityField);
        addRow(form, g, 3, "Probabilité de saturation",  saturationField);
        addRow(form, g, 4, "Taille de délibération (P)", pField);

        DocumentListener dl = new DocumentListener() {
            public void insertUpdate(DocumentEvent e)  { updateRuleColors(); }
            public void removeUpdate(DocumentEvent e)  { updateRuleColors(); }
            public void changedUpdate(DocumentEvent e) { updateRuleColors(); }
        };
        nField.getDocument().addDocumentListener(dl);
        pField.getDocument().addDocumentListener(dl);

        JPanel rulesBox = new RoundPanel(SURFACE2, 10);
        rulesBox.setLayout(new BoxLayout(rulesBox, BoxLayout.Y_AXIS));
        rulesBox.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COL),
            new EmptyBorder(12, 14, 12, 14)
        ));

        JLabel rulesTitle = new JLabel("VALIDATION RULES");
        rulesTitle.setFont(FONT_EYEBROW);
        rulesTitle.setForeground(MUTED);
        rulesTitle.setBorder(new EmptyBorder(0, 0, 8, 0));
        rulesTitle.setAlignmentX(Component.LEFT_ALIGNMENT);

        ruleP = ruleLabel("P  <  N");

        rulesBox.add(rulesTitle);
        rulesBox.add(ruleLabel("N  >  0"));
        rulesBox.add(ruleLabel("M  >  0"));
        rulesBox.add(ruleLabel("0  ≤  saturation  ≤  1"));
        rulesBox.add(ruleP);

        JPanel center = new JPanel(new BorderLayout(0, 14));
        center.setOpaque(false);
        center.add(form,     BorderLayout.NORTH);
        center.add(rulesBox, BorderLayout.SOUTH);

        panel.add(center, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildLogsPanel() {
        JPanel panel = new RoundPanel(SURFACE, 16);
        panel.setLayout(new BorderLayout(0, 14));
        panel.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COL),
            new EmptyBorder(20, 20, 20, 20)
        ));

        panel.add(eyebrowLabel("Logs / Output"), BorderLayout.NORTH);

        logPane = new JTextPane();
        logPane.setEditable(false);
        logPane.setBackground(new Color(0x0A, 0x0C, 0x10));
        logPane.setFont(FONT_MONO);
        logPane.setBorder(new EmptyBorder(12, 12, 12, 12));
        logDoc = logPane.getStyledDocument();

        JScrollPane scroll = new JScrollPane(logPane);
        scroll.setBorder(BorderFactory.createLineBorder(BORDER_COL));
        scroll.getViewport().setBackground(new Color(0x0A, 0x0C, 0x10));
        scroll.getVerticalScrollBar().setUnitIncrement(12);

        panel.add(scroll, BorderLayout.CENTER);
        return panel;
    }

    private JPanel buildBottom() {
        JPanel p = new JPanel(new BorderLayout());
        p.setOpaque(false);

        JPanel leftGroup = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 0));
        leftGroup.setOpaque(false);
        leftGroup.add(ghostButton("Small Test", e -> loadSmallTest()));
        leftGroup.add(ghostButton("Clear Logs", e -> {
            logPane.setText("");
            log("Logs cleared.", LogType.INFO);
        }));

        startButton = new JButton("▶  Start Simulation");
        startButton.setFont(new Font("Segoe UI", Font.BOLD, 13));
        startButton.setBackground(ACCENT);
        startButton.setForeground(BG);
        startButton.setFocusPainted(false);
        startButton.setBorderPainted(false);
        startButton.setOpaque(true);
        startButton.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        startButton.setPreferredSize(new Dimension(185, 40));
        startButton.addActionListener(e -> startSimulation());

        startButton.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                if (startButton.isEnabled())
                    startButton.setBackground(new Color(0xF5, 0xCC, 0x72));
            }
            public void mouseExited(MouseEvent e) {
                startButton.setBackground(ACCENT);
            }
        });

        p.add(leftGroup,   BorderLayout.WEST);
        p.add(startButton, BorderLayout.EAST);
        return p;
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  COMPONENT HELPERS
    // ══════════════════════════════════════════════════════════════════════════

    private JLabel eyebrowLabel(String text) {
        JLabel lbl = new JLabel(text.toUpperCase());
        lbl.setFont(FONT_EYEBROW);
        lbl.setForeground(ACCENT);
        lbl.setBorder(new EmptyBorder(0, 0, 6, 0));
        return lbl;
    }

    private JTextField styledField(String defaultVal) {
        JTextField f = new JTextField(defaultVal, 10);
        f.setBackground(SURFACE2);
        f.setForeground(TEXT_MAIN);
        f.setFont(FONT_MONO);
        f.setCaretColor(ACCENT);
        f.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COL),
            new EmptyBorder(7, 11, 7, 11)
        ));
        f.addFocusListener(new FocusAdapter() {
            public void focusGained(FocusEvent e) {
                f.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(new Color(0xF0, 0xC0, 0x60, 110)),
                    new EmptyBorder(7, 11, 7, 11)
                ));
            }
            public void focusLost(FocusEvent e) {
                f.setBorder(BorderFactory.createCompoundBorder(
                    BorderFactory.createLineBorder(BORDER_COL),
                    new EmptyBorder(7, 11, 7, 11)
                ));
            }
        });
        return f;
    }

    private void addRow(JPanel panel, GridBagConstraints g, int row,
                        String labelText, JTextField field) {
        g.gridy   = row;
        g.gridx   = 0;
        g.weightx = 0;
        g.fill    = GridBagConstraints.NONE;
        g.insets  = new Insets(5, 0, 5, 14);

        JLabel lbl = new JLabel(labelText);
        lbl.setFont(FONT_LABEL);
        lbl.setForeground(MUTED);
        panel.add(lbl, g);

        g.gridx   = 1;
        g.weightx = 1;
        g.fill    = GridBagConstraints.HORIZONTAL;
        g.insets  = new Insets(5, 0, 5, 0);
        panel.add(field, g);
    }

    private JLabel ruleLabel(String text) {
        JLabel lbl = new JLabel("—  " + text);
        lbl.setFont(FONT_MONO);
        lbl.setForeground(MUTED);
        lbl.setBorder(new EmptyBorder(2, 0, 2, 0));
        lbl.setAlignmentX(Component.LEFT_ALIGNMENT);
        return lbl;
    }

    private JButton ghostButton(String text, ActionListener al) {
        JButton b = new JButton(text);
        b.setFont(FONT_BODY);
        b.setForeground(MUTED);
        b.setBackground(SURFACE);
        b.setFocusPainted(false);
        b.setOpaque(true);
        b.setBorderPainted(true);
        b.setBorder(BorderFactory.createCompoundBorder(
            BorderFactory.createLineBorder(BORDER_COL),
            new EmptyBorder(8, 16, 8, 16)
        ));
        b.setCursor(Cursor.getPredefinedCursor(Cursor.HAND_CURSOR));
        b.addActionListener(al);
        b.addMouseListener(new MouseAdapter() {
            public void mouseEntered(MouseEvent e) {
                b.setBackground(SURFACE2);
                b.setForeground(TEXT_MAIN);
            }
            public void mouseExited(MouseEvent e) {
                b.setBackground(SURFACE);
                b.setForeground(MUTED);
            }
        });
        return b;
    }

    private void updateRuleColors() {
        try {
            int n = Integer.parseInt(nField.getText().trim());
            int p = Integer.parseInt(pField.getText().trim());
            ruleP.setForeground(p >= n ? LOG_ERROR : MUTED);
        } catch (NumberFormatException ignored) {
            ruleP.setForeground(MUTED);
        }
    }

    // ── Pulsing dot ────────────────────────────────────────────────────────────
    private void startPulse() {
        new Timer(800, e -> {
            pulseState = !pulseState;
            if (currentStatus == SimStatus.RUNNING) {
                statusDot.setForeground(pulseState ? ST_RUN_DOT : new Color(0x1A, 0x2A, 0x4A));
            }
            // other states: dot stays solid (no pulse)
        }).start();
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  STATUS BADGE  — public API
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Single-arg convenience used by SimulationLauncher.
     * Derives SimStatus from the string keyword.
     */
    public void setStatus(String text) {
        SimStatus s;
        String lower = text.toLowerCase();
        if      (lower.contains("run"))       s = SimStatus.RUNNING;
        else if (lower.contains("complet"))   s = SimStatus.COMPLETED;
        else if (lower.contains("error") || lower.contains("erreur")) s = SimStatus.ERROR;
        else                                  s = SimStatus.READY;
        setStatus(text, s);
    }

    /**
     * Full two-arg form: pass explicit SimStatus for precise control.
     * Also accepts (String, boolean) via the overload below for back-compat.
     */
    public void setStatus(String text, SimStatus status) {
        SwingUtilities.invokeLater(() -> {
            currentStatus = status;
            statusText.setText(text);

            Color dot, textCol;
            switch (status) {
                case RUNNING   -> { dot = ST_RUN_DOT;  textCol = ST_RUN_TEXT;  }
                case COMPLETED -> { dot = ST_DONE_DOT; textCol = ST_DONE_TEXT; }
                case ERROR     -> { dot = ST_ERROR_DOT; textCol = ST_ERROR_TEXT; }
                default        -> { dot = ST_READY_DOT; textCol = ST_READY_TEXT; }
            }

            statusDot.setForeground(dot);
            statusText.setForeground(textCol);
            badgeWrap.setBorder(BorderFactory.createLineBorder(
                new Color(dot.getRed(), dot.getGreen(), dot.getBlue(), 70)
            ));
            badgeWrap.repaint();
        });
    }

    /** Back-compat overload used in older SimulationLauncher calls. */
    public void setStatus(String text, boolean isRunning) {
        setStatus(text, isRunning ? SimStatus.RUNNING : SimStatus.READY);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LOGGING
    // ══════════════════════════════════════════════════════════════════════════

    public enum LogType { INFO, SUCCESS, ERROR, HIGHLIGHT }

    public void log(String message, LogType type) {
        SwingUtilities.invokeLater(() -> {
            try {
                Color col = switch (type) {
                    case SUCCESS   -> LOG_SUCCESS;
                    case ERROR     -> LOG_ERROR;
                    case HIGHLIGHT -> LOG_HL;
                    default        -> LOG_INFO;
                };

                SimpleAttributeSet tsAttr = new SimpleAttributeSet();
                StyleConstants.setForeground(tsAttr, MUTED);
                StyleConstants.setFontFamily(tsAttr, "Consolas");
                StyleConstants.setFontSize(tsAttr, 12);

                SimpleAttributeSet msgAttr = new SimpleAttributeSet();
                StyleConstants.setForeground(msgAttr, col);
                StyleConstants.setFontFamily(msgAttr, "Consolas");
                StyleConstants.setFontSize(msgAttr, 12);

                String ts = "[" + LocalTime.now().format(TS) + "]  ";
                logDoc.insertString(logDoc.getLength(), ts,             tsAttr);
                logDoc.insertString(logDoc.getLength(), message + "\n", msgAttr);
                logPane.setCaretPosition(logDoc.getLength());
            } catch (BadLocationException ignored) {}
        });
    }

    public void log(String message) { log(message, LogType.INFO); }

    // ══════════════════════════════════════════════════════════════════════════
    //  FINAL SUMMARY
    // ══════════════════════════════════════════════════════════════════════════

    /**
     * Prints a clean final summary block to the log area.
     * Call this from SimulationLauncher once all agents have finished.
     */
    public void showSummary(SimulationResult result) {
        SwingUtilities.invokeLater(() -> {
            try {
                // ── header bar ────────────────────────────────────────────────
                logLine("", LOG_INFO);
                logLine("╔══════════════════════════════════════════╗", LOG_HL);
                logLine("║        SIMULATION FINAL SUMMARY          ║", LOG_HL);
                logLine("╚══════════════════════════════════════════╝", LOG_HL);

                // ── key metrics ───────────────────────────────────────────────
                boolean allConfirmed = result.confirmed == result.totalPersons;
                Color confirmedColor = allConfirmed ? LOG_SUCCESS : LOG_ERROR;

                logLine(String.format("  Confirmed persons  :  %d / %d",
                        result.confirmed, result.totalPersons), confirmedColor);
                logLine(String.format("  Total refusals     :  %d", result.refused),
                        result.refused == 0 ? LOG_SUCCESS : LOG_INFO);
                logLine(String.format("  Max attempts       :  %d", result.maxAttempts), LOG_INFO);

                // ── distribution ──────────────────────────────────────────────
                logLine("", LOG_INFO);
                logLine("  Final distribution :", LOG_HL);

                int maxCount = result.distribution.values().stream()
                        .mapToInt(Integer::intValue).max().orElse(1);

                for (Map.Entry<String, Integer> entry : result.distribution.entrySet()) {
                    String name  = entry.getKey();
                    int    count = entry.getValue();

                    // mini bar  ████░░░░  (max 20 chars wide)
                    int filled = maxCount > 0 ? (int) Math.round(20.0 * count / maxCount) : 0;
                    String bar = "█".repeat(filled) + "░".repeat(20 - filled);

                    Color barColor = count == 0 ? MUTED : LOG_SUCCESS;
                    logLine(String.format("  %-14s  %s  %d", name, bar, count), barColor);
                }

                logLine("", LOG_INFO);
                logLine("══════════════════════════════════════════════", LOG_HL);

            } catch (Exception ignored) {}
        });
    }

    // Internal helper — appends one styled line without a timestamp
    private void logLine(String text, Color color) throws BadLocationException {
        SimpleAttributeSet attr = new SimpleAttributeSet();
        StyleConstants.setForeground(attr, color);
        StyleConstants.setFontFamily(attr, "Consolas");
        StyleConstants.setFontSize(attr, 12);
        logDoc.insertString(logDoc.getLength(), text + "\n", attr);
        logPane.setCaretPosition(logDoc.getLength());
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  SIMULATION CONTROL
    // ══════════════════════════════════════════════════════════════════════════

    private void startSimulation() {
        try {
            int    n    = Integer.parseInt(nField.getText().trim());
            int    m    = Integer.parseInt(mField.getText().trim());
            int    base = Integer.parseInt(baseCapacityField.getText().trim());
            double sat  = Double.parseDouble(saturationField.getText().trim());
            int    p    = Integer.parseInt(pField.getText().trim());

            if (n <= 0)             { showError("N doit être > 0.");                               return; }
            if (m <= 0)             { showError("M doit être > 0.");                               return; }
            if (base <= 0)          { showError("Capacité de base doit être > 0.");                return; }
            if (sat < 0 || sat > 1) { showError("Saturation doit être entre 0 et 1.");             return; }
            if (p < 0 || p >= n)    { showError("P doit être ≥ 0 et strictement inférieure à N."); return; }

            startButton.setEnabled(false);
            setStatus("Running…", SimStatus.RUNNING);

            log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", LogType.HIGHLIGHT);
            log(String.format("Launch  N=%d  M=%d  base=%d  sat=%.2f  P=%d", n, m, base, sat, p),
                LogType.HIGHLIGHT);
            log("Parameters validated ✓", LogType.SUCCESS);

            SimulationConfig config = new SimulationConfig(n, m, base, sat, p);
            SimulationLauncher.launch(config, this);

        } catch (NumberFormatException ex) {
            showError("Veuillez entrer des valeurs numériques valides.");
        }
    }

    private void loadSmallTest() {
        nField.setText("5");
        mField.setText("3");
        baseCapacityField.setText("8");
        saturationField.setText("0.3");
        pField.setText("2");
        updateRuleColors();
        log("Small test preset loaded.", LogType.HIGHLIGHT);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Erreur de validation",
            JOptionPane.ERROR_MESSAGE);
    }

    public void enableStartButton() {
        SwingUtilities.invokeLater(() -> {
            startButton.setEnabled(true);
            setStatus("Ready", SimStatus.READY);
        });
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  INNER: Rounded panel
    // ══════════════════════════════════════════════════════════════════════════

    private static class RoundPanel extends JPanel {
        private final Color bg;
        private final int   radius;

        RoundPanel(Color bg, int radius) {
            this.bg     = bg;
            this.radius = radius;
            setOpaque(false);
        }

        @Override
        protected void paintComponent(Graphics g) {
            Graphics2D g2 = (Graphics2D) g.create();
            g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,
                                RenderingHints.VALUE_ANTIALIAS_ON);
            g2.setColor(bg);
            g2.fillRoundRect(0, 0, getWidth(), getHeight(), radius, radius);
            g2.dispose();
            super.paintComponent(g);
        }
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  ENTRY POINT
    // ══════════════════════════════════════════════════════════════════════════

    public static void main(String[] args) {
        try {
            FlatDarkLaf.setup();
            UIManager.put("Panel.background",             BG);
            UIManager.put("TextField.background",         SURFACE2);
            UIManager.put("TextField.foreground",         TEXT_MAIN);
            UIManager.put("TextField.caretForeground",    ACCENT);
            UIManager.put("ScrollPane.background",        SURFACE);
            UIManager.put("OptionPane.background",        SURFACE);
            UIManager.put("OptionPane.messageForeground", TEXT_MAIN);
        } catch (Exception e) {
            System.err.println("FlatLaf not found — falling back to default LAF.");
        }

        SwingUtilities.invokeLater(() -> new SimulationUI().setVisible(true));
    }
}