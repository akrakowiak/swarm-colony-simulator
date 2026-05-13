# Swarm Colony Simulation

This project is a Java-based simulation engine designed to model and optimize the formation of insect swarms. It utilizes a modular architecture to calculate collective features, apply behavioral rules, and find the most efficient combination of entities to complete specific environmental tasks.

## ## Core Architecture

The simulation is built on several key components that define how insects interact and aggregate their capabilities:

* **FeatureSet**: A utility class that manages a collection of integer-based attributes such as `strength`, `health`, `memory`, and `perception`. It supports additive merging and requirement checking.


* **Entity & Insect**: The `Insect` class represents the base unit of the simulation, carrying a specific `FeatureSet` and supporting the `Cloneable` interface for swarm generation.


* **Swarm**: A composite entity that aggregates the features of its members. It manages a list of `AggregationRule` objects that can dynamically modify the swarm's features based on its current state.


* **Aggregation Rules**: Logic modules like `IntelligenceBoostRule` and `BoostByBalanceRule` that simulate emergent behaviors—for instance, increasing perception when memory thresholds are met.



## ## Advanced Features

### ### Optimization Engine

The `SwarmFactory` uses a power-set approach (bitmasking) to evaluate all possible combinations ($2^n$) of available insects. It filters these candidates using a `TaskEvaluator` and selects the "best" swarm based on a surplus-scoring heuristic relative to the task requirements.

### ### Adaptive Behavior

The `AdaptiveSwarm` implementation integrates the **Observer Pattern**. It can listen for environmental events, such as an "Obstacle," and respond by injecting new rules and reapplying its logic to adapt its features in real-time.

## ## Design Patterns Used

| Pattern | Implementation |
| --- | --- |
| **Builder** | <br>`SwarmBuilder` provides a fluid interface for constructing swarms and applying rules.

 |
| **Strategy** | <br>`RuleSelectionStrategy` allows for different logic in choosing which rules to apply to a task.

 |
| **Observer** | <br>`EnvironmentObserver` enables `AdaptiveSwarm` to react to external triggers.

 |
| **Composite** | <br>`Swarm` acts as an `Entity` while containing a collection of other `Entity` objects.

 |

## ## Quick Start

The main entry point is located in `ColonySim.java`.

1. **Define Insects**: Create a list of `Insect` objects with varying `FeatureSet` values.


2. **Define a Task**: Specify the minimum requirements for a mission.


3. **Build the Swarm**: Use the `SwarmFactory` to automatically find the optimal group for that task.



```java
Task task = new Task("Navigate Maze", new FeatureSet("strength", "70", "memory", "100"));
SwarmFactory factory = new SwarmFactory(new HeuristicRuleStrategy(), new DefaultTaskEvaluator());
Swarm bestSwarm = factory.buildOptimalSwarm(allInsects, task);
[cite_start]

```