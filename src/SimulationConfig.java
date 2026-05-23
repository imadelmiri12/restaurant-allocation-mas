public class SimulationConfig {
    public int    nPersonnes;
    public int    mRestaurants;
    public int    baseCapacity;
    public double saturationProbability;
    public int    pollSize;

    public SimulationConfig(int nPersonnes, int mRestaurants, int baseCapacity,
                            double saturationProbability, int pollSize) {
        this.nPersonnes            = nPersonnes;
        this.mRestaurants          = mRestaurants;
        this.baseCapacity          = baseCapacity;
        this.saturationProbability = saturationProbability;
        this.pollSize              = pollSize;
    }
}