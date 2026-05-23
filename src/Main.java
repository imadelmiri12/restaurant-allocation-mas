import javax.swing.SwingUtilities;
import com.formdev.flatlaf.FlatLightLaf;

public class Main {
    public static void main(String[] args) {
        try {
            FlatLightLaf.setup();
        } catch (Exception e) {
            e.printStackTrace();
        }

        SwingUtilities.invokeLater(() -> {
            SimulationUI ui = new SimulationUI();
            ui.setVisible(true);
        });
    }
}