import jade.core.Profile;
import jade.core.ProfileImpl;
import jade.core.Runtime;
import jade.wrapper.AgentContainer;
import jade.wrapper.AgentController;
import jade.wrapper.StaleProxyException;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

public class SimulationLauncher {

    // ══════════════════════════════════════════════════════════════════════════
    //  Shared statistics — written by AgentPersonne, read here at the end.
    //
    //  In your AgentPersonne.java, call these static methods when an outcome
    //  is determined:
    //
    //    SimulationLauncher.recordConfirmed("Restaurant2");
    //    SimulationLauncher.recordRefusal();
    //    SimulationLauncher.recordAttempts(3);   // total tries this person made
    //    SimulationLauncher.recordDone();         // called once per person when finished
    //
    // ══════════════════════════════════════════════════════════════════════════

    // Distribution: restaurantName → confirmed count
    private static final Map<String, AtomicInteger> distribution  = new ConcurrentHashMap<>();
    private static final AtomicInteger totalRefusals  = new AtomicInteger(0);
    private static final AtomicInteger maxAttempts    = new AtomicInteger(0);
    private static final AtomicInteger confirmedCount = new AtomicInteger(0);
    private static final AtomicInteger doneCount      = new AtomicInteger(0);

    // Set by launch() so the agents know when everyone is done
    private static int    totalPersons = 0;
    private static SimulationUI sharedUI = null;
    private static List<String> restaurantNamesList = new ArrayList<>();

    // ── Called by AgentPersonne ────────────────────────────────────────────────

    public static void recordConfirmed(String restaurantName) {
        distribution.computeIfAbsent(restaurantName, k -> new AtomicInteger(0))
                    .incrementAndGet();
        confirmedCount.incrementAndGet();
    }

    public static void recordRefusal() {
        totalRefusals.incrementAndGet();
    }

    public static void recordAttempts(int attempts) {
        // Thread-safe max update
        int current;
        do {
            current = maxAttempts.get();
        } while (attempts > current && !maxAttempts.compareAndSet(current, attempts));
    }

    /**
     * Each AgentPersonne calls this exactly once when it has finished
     * (confirmed or permanently refused). When all N persons are done,
     * the summary is printed automatically.
     */
    public static void recordDone() {
        int done = doneCount.incrementAndGet();
        if (done >= totalPersons && sharedUI != null) {
            // Build result
            Map<String, Integer> dist = new LinkedHashMap<>();
            for (String name : restaurantNamesList) {
                AtomicInteger ai = distribution.get(name);
                dist.put(name, ai != null ? ai.get() : 0);
            }

            SimulationResult result = new SimulationResult(
                totalPersons,
                confirmedCount.get(),
                totalRefusals.get(),
                maxAttempts.get(),
                dist
            );

            sharedUI.showSummary(result);
            sharedUI.setStatus("Completed", SimulationUI.SimStatus.COMPLETED);
            sharedUI.enableStartButton();
        }
    }

    // ── Reset between runs ─────────────────────────────────────────────────────
    private static void reset(int n, SimulationUI ui, List<String> restaurantNames) {
        distribution.clear();
        totalRefusals.set(0);
        maxAttempts.set(0);
        confirmedCount.set(0);
        doneCount.set(0);
        totalPersons       = n;
        sharedUI           = ui;
        restaurantNamesList = new ArrayList<>(restaurantNames);
    }

    // ══════════════════════════════════════════════════════════════════════════
    //  LAUNCH
    // ══════════════════════════════════════════════════════════════════════════

    public static void launch(SimulationConfig config, SimulationUI ui) {
        new Thread(() -> {
            try {
                ui.setStatus("Running…", SimulationUI.SimStatus.RUNNING);

                Runtime rt = Runtime.instance();
                Profile profile = new ProfileImpl();
                profile.setParameter(Profile.GUI, "true");
                profile.setParameter(Profile.CONTAINER_NAME, "SMA-Resto");

                AgentContainer container = rt.createMainContainer(profile);

                if (container == null) {
                    ui.log("Erreur: impossible de créer le Main Container JADE.",
                           SimulationUI.LogType.ERROR);
                    ui.setStatus("Error", SimulationUI.SimStatus.ERROR);
                    ui.enableStartButton();
                    return;
                }

                // ── Parameters ────────────────────────────────────────────────
                ui.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", SimulationUI.LogType.HIGHLIGHT);
                ui.log("Simulation Parameters",                      SimulationUI.LogType.HIGHLIGHT);
                ui.log("━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━━", SimulationUI.LogType.HIGHLIGHT);
                ui.log("N (Personnes)          : " + config.nPersonnes);
                ui.log("M (Restaurants)        : " + config.mRestaurants);
                ui.log("Capacité de base       : " + config.baseCapacity);
                ui.log("Proba. saturation      : " + config.saturationProbability);
                ui.log("P (Taille délibération): " + config.pollSize);
                ui.log("────────────────────────────────────────");

                // ── Restaurant agents ─────────────────────────────────────────
                List<String> restaurantNames = new ArrayList<>();
                Random random = new Random();
                int totalCapacity = 0;

                ui.log("Generating Restaurants…", SimulationUI.LogType.HIGHLIGHT);

                for (int i = 0; i < config.mRestaurants; i++) {
                    int capacity;
                    do {
                        capacity = random.nextInt(config.baseCapacity) + config.baseCapacity / 2;
                    } while (capacity >= config.nPersonnes);

                    totalCapacity += capacity;
                    String name = "Restaurant" + i;
                    restaurantNames.add(name);

                    ui.log("  " + name + "  →  capacity = " + capacity);

                    AgentController rest = container.createNewAgent(
                            name,
                            "AgentRestaurant",
                            new Object[]{ capacity, config.saturationProbability }
                    );
                    rest.start();
                }

                ui.log("Total capacity Σ(Ci) = " + totalCapacity);

                if (totalCapacity <= 2 * config.nPersonnes) {
                    ui.log("⚠  Constraint Σ(Ci) > 2N NOT satisfied!", SimulationUI.LogType.ERROR);
                } else {
                    ui.log("✓  Constraint Σ(Ci) > 2N satisfied.",     SimulationUI.LogType.SUCCESS);
                }

                // ── Reset stats BEFORE starting persons ───────────────────────
                reset(config.nPersonnes, ui, restaurantNames);

                // ── Person agents ─────────────────────────────────────────────
                String[] restaurantNameArray = restaurantNames.toArray(new String[0]);
                String[] personNames = new String[config.nPersonnes];
                for (int i = 0; i < config.nPersonnes; i++) {
                    personNames[i] = "Personne" + i;
                }

                ui.log("────────────────────────────────────────");
                ui.log("Creating Person Agents…", SimulationUI.LogType.HIGHLIGHT);

                for (int i = 0; i < config.nPersonnes; i++) {
                    String name = personNames[i];
                    ui.log("  " + name + " registered");

                    AgentController person = container.createNewAgent(
                            name,
                            "AgentPersonne",
                            new Object[]{ restaurantNameArray, personNames, config.pollSize }
                    );
                    person.start();
                }

                // ── Sniffer ───────────────────────────────────────────────────
                StringBuilder sniffed = new StringBuilder();
                for (int i = 0; i < config.nPersonnes; i++) {
                    if (sniffed.length() > 0) sniffed.append(";");
                    sniffed.append("Personne").append(i);
                }
                for (int i = 0; i < config.mRestaurants; i++) {
                    if (sniffed.length() > 0) sniffed.append(";");
                    sniffed.append("Restaurant").append(i);
                }

                AgentController sniffer = container.createNewAgent(
                        "Sniffer",
                        "jade.tools.sniffer.Sniffer",
                        new Object[]{ sniffed.toString() }
                );
                sniffer.start();

                ui.log("────────────────────────────────────────");
                ui.log("Sniffer started.");
                ui.log("Agents running — waiting for results…", SimulationUI.LogType.HIGHLIGHT);

                // Summary will be printed automatically by recordDone()
                // once all N persons call it.

            } catch (StaleProxyException e) {
                ui.log("Erreur JADE: " + e.getMessage(), SimulationUI.LogType.ERROR);
                ui.setStatus("Error", SimulationUI.SimStatus.ERROR);
                ui.enableStartButton();
                e.printStackTrace();

            } catch (Exception e) {
                ui.log("Erreur générale: " + e.getMessage(), SimulationUI.LogType.ERROR);
                ui.setStatus("Error", SimulationUI.SimStatus.ERROR);
                ui.enableStartButton();
                e.printStackTrace();
            }
        }).start();
    }
}