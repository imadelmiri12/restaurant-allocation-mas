import java.util.Map;
import java.util.LinkedHashMap;

/**
 * Carries the final statistics of one simulation run.
 * Populated by SimulationLauncher and passed to SimulationUI.showSummary().
 */
public class SimulationResult {

    public final int totalPersons;
    public final int confirmed;
    public final int refused;
    public final int maxAttempts;

    /** restaurantName → number of confirmed persons */
    public final Map<String, Integer> distribution;

    public SimulationResult(int totalPersons,
                            int confirmed,
                            int refused,
                            int maxAttempts,
                            Map<String, Integer> distribution) {
        this.totalPersons = totalPersons;
        this.confirmed    = confirmed;
        this.refused      = refused;
        this.maxAttempts  = maxAttempts;
        this.distribution = new LinkedHashMap<>(distribution);
    }
}
