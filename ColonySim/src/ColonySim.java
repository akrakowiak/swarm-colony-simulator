import java.util.*;

class FeatureSet {
    private final Map<String, Integer> features = new HashMap<>();

    public FeatureSet(String... keysAndValues) {
        for (int i = 0; i < keysAndValues.length; i += 2) {
            features.put(keysAndValues[i], Integer.parseInt(keysAndValues[i + 1]));
        }
    }

    public void add(FeatureSet other) {
        for (Map.Entry<String, Integer> entry : other.features.entrySet()) {
            features.merge(entry.getKey(), entry.getValue(), Integer::sum);
        }
    }

    public boolean meets(FeatureSet required) {
        for (Map.Entry<String, Integer> entry : required.features.entrySet()) {
            if (this.features.getOrDefault(entry.getKey(), 0) < entry.getValue()) return false;
        }
        return true;
    }

    public int get(String key) {
        return features.getOrDefault(key, 0);
    }

    public void increase(String key, int amount) {
        features.put(key, get(key) + amount);
    }

    public Map<String, Integer> getAll() {
        return Collections.unmodifiableMap(features);
    }

    @Override
    public String toString() {
        return features.toString();
    }
}

interface Entity {
    FeatureSet getFeatures();
    String getName();
}

class Insect implements Entity, Cloneable {
    private final String name;
    private final FeatureSet features;

    public Insect(String name, FeatureSet features) {
        this.name = name;
        this.features = features;
    }

    public FeatureSet getFeatures() {
        FeatureSet copy = new FeatureSet();
        copy.add(features);
        return copy;
    }

    public String getName() {
        return name;
    }

    public Insect clone() {
        return new Insect(name, getFeatures());
    }
}

interface AggregationRule {
    void apply(Swarm swarm);
}

class IntelligenceBoostRule implements AggregationRule {
    public void apply(Swarm swarm) {
        if (swarm.getFeatures().get("memory") > 100) {
            swarm.getFeatures().increase("perception", 20);
        }
    }
}

class BoostByBalanceRule implements AggregationRule {
    public void apply(Swarm swarm) {
        FeatureSet f = swarm.getFeatures();
        if (Math.abs(f.get("strength") - f.get("memory")) <= 20 && f.get("health") > 50) {
            f.increase("perception", 15);
            f.increase("memory", 10);
        }
    }
}

interface EnvironmentObserver {
    void onEnvironmentChange(String event);
}

class Swarm implements Entity {
    protected final List<Entity> members = new ArrayList<>();
    protected final List<AggregationRule> rules = new ArrayList<>();
    protected FeatureSet features = new FeatureSet();

    public void addMember(Entity e) {
        members.add(e);
        features.add(e.getFeatures());
    }

    public void applyRules() {
        boolean changed;
        do {
            FeatureSet snapshot = new FeatureSet();
            snapshot.add(this.features);

            for (AggregationRule rule : rules) {
                rule.apply(this);
            }

            changed = !snapshot.getAll().equals(this.features.getAll());
        } while (changed);
    }

    public void addRule(AggregationRule rule) {
        rules.add(rule);
    }

    public FeatureSet getFeatures() {
        return features;
    }

    public String getName() {
        return "Swarm(" + members.size() + ")";
    }

    public void printMembers() {
        for (Entity e : members) {
            System.out.println("  - " + e.getName() + ": " + e.getFeatures());
        }
    }
}

class AdaptiveSwarm extends Swarm implements EnvironmentObserver {
    public void onEnvironmentChange(String event) {
        if (event.equals("Obstacle")) {
            this.addRule(new BoostByBalanceRule());
            this.applyRules();
        }
    }
}

class SwarmBuilder {
    private final Swarm swarm = new Swarm();

    public SwarmBuilder addEntity(Entity e) {
        swarm.addMember(e);
        return this;
    }

    public SwarmBuilder withRule(AggregationRule rule) {
        swarm.addRule(rule);
        return this;
    }

    public Swarm build() {
        swarm.applyRules();
        return swarm;
    }
}

class Task {
    private final String name;
    private final FeatureSet requirements;

    public Task(String name, FeatureSet requirements) {
        this.name = name;
        this.requirements = requirements;
    }

    public FeatureSet getRequirements() {
        return requirements;
    }

    public String getName() {
        return name;
    }
}

interface TaskEvaluator {
    boolean isSufficient(Swarm swarm, Task task);
}

class DefaultTaskEvaluator implements TaskEvaluator {
    public boolean isSufficient(Swarm swarm, Task task) {
        return swarm.getFeatures().meets(task.getRequirements());
    }
}

interface RuleSelectionStrategy {
    List<AggregationRule> selectRules(List<Entity> members, Task task);
}

class HeuristicRuleStrategy implements RuleSelectionStrategy {
    public List<AggregationRule> selectRules(List<Entity> members, Task task) {
        List<AggregationRule> rules = new ArrayList<>();
        if (task.getName().contains("Analyze") || task.getName().contains("Plan")) {
            rules.add(new IntelligenceBoostRule());
        }
        if (task.getName().contains("Navigate") || task.getRequirements().get("perception") > 30) {
            rules.add(new BoostByBalanceRule());
        }
        return rules;
    }
}

class SwarmFactory {
    private final RuleSelectionStrategy strategy;
    private final TaskEvaluator evaluator;

    public SwarmFactory(RuleSelectionStrategy strategy, TaskEvaluator evaluator) {
        this.strategy = strategy;
        this.evaluator = evaluator;
    }

    public Swarm buildOptimalSwarm(List<Insect> insects, Task task) {
        List<Swarm> candidates = new ArrayList<>();
        int n = insects.size();

        for (int i = 1; i < (1 << n); i++) {
            List<Entity> group = new ArrayList<>();
            for (int j = 0; j < n; j++) {
                if ((i & (1 << j)) != 0) {
                    group.add(insects.get(j).clone());
                }
            }

            List<AggregationRule> rules = strategy.selectRules(group, task);
            SwarmBuilder builder = new SwarmBuilder();
            group.forEach(builder::addEntity);
            rules.forEach(builder::withRule);
            Swarm swarm = builder.build();

            if (evaluator.isSufficient(swarm, task)) {
                candidates.add(swarm);
            }
        }

        return candidates.stream()
                .max(Comparator.comparingInt(s -> scoreSwarm(s, task)))
                .orElse(null);
    }

    private int scoreSwarm(Swarm swarm, Task task) {
        int score = 0;
        for (Map.Entry<String, Integer> entry : task.getRequirements().getAll().entrySet()) {
            int surplus = swarm.getFeatures().get(entry.getKey()) - entry.getValue();
            score += Math.max(0, surplus);
        }
        return score;
    }
}

public class ColonySim {
    public static void main(String[] args) {
        List<Insect> allInsects = List.of(
                new Insect("Ant1", new FeatureSet("strength", "40", "health", "30", "memory", "60", "perception", "10")),
                new Insect("Ant2", new FeatureSet("strength", "50", "health", "20", "memory", "50", "perception", "10")),
                new Insect("Ant3", new FeatureSet("strength", "30", "health", "40", "memory", "30", "perception", "15")),
                new Insect("Ant4", new FeatureSet("strength", "20", "health", "30", "memory", "80", "perception", "20"))
        );

        Task task = new Task("Navigate Maze", new FeatureSet("strength", "70", "health", "30", "memory", "100", "perception", "35"));
        SwarmFactory factory = new SwarmFactory(new HeuristicRuleStrategy(), new DefaultTaskEvaluator());

        Swarm bestSwarm = factory.buildOptimalSwarm(allInsects, task);

        if (bestSwarm != null) {
            System.out.println("Best swarm: " + bestSwarm.getName());
            System.out.println("  Features: " + bestSwarm.getFeatures());
            bestSwarm.printMembers();
        } else {
            System.out.println("No valid swarm found for task.");
        }
    }
}
