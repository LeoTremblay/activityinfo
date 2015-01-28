package cucumber.runtime.junit;

import cucumber.api.Profile;
import cucumber.runtime.CucumberException;
import cucumber.runtime.Runtime;
import cucumber.runtime.model.*;
import gherkin.formatter.model.Feature;
import gherkin.formatter.model.Scenario;
import gherkin.formatter.model.Step;
import org.junit.runner.Description;
import org.junit.runner.notification.RunNotifier;
import org.junit.runners.ParentRunner;
import org.junit.runners.model.InitializationError;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ParametrizedFeatureRunner extends ParentRunner<ParentRunner> {
    private final List<ParentRunner> children = new ArrayList<ParentRunner>();

    private final CucumberFeature cucumberFeature;
    private final JUnitReporter jUnitReporter;
    private List<ParametrizedRuntime> parameters;
    private Map<Profile, Runtime> profiles;
    private Description description;

    public ParametrizedFeatureRunner(CucumberFeature cucumberFeature, JUnitReporter jUnitReporter,
                                     List<ParametrizedRuntime> parameters) throws InitializationError {
        super(null);
        this.cucumberFeature = cucumberFeature;
        this.jUnitReporter = jUnitReporter;
        this.parameters = parameters;
        buildFeatureElementRunners();
    }

    @Override
    public String getName() {
        Feature feature = cucumberFeature.getGherkinFeature();
        return feature.getKeyword() + ": " + feature.getName();
    }

    @Override
    public Description getDescription() {
        if (description == null) {
            description = Description.createSuiteDescription(getName(), cucumberFeature.getGherkinFeature());
            for (ParentRunner child : getChildren()) {
                description.addChild(describeChild(child));
            }
        }
        return description;
    }

    @Override
    protected List<ParentRunner> getChildren() {
        return children;
    }

    @Override
    protected Description describeChild(ParentRunner child) {
        return child.getDescription();
    }

    @Override
    protected void runChild(ParentRunner child, RunNotifier notifier) {
        child.run(notifier);
    }

    @Override
    public void run(RunNotifier notifier) {
        jUnitReporter.uri(cucumberFeature.getPath());
        jUnitReporter.feature(cucumberFeature.getGherkinFeature());
        super.run(notifier);
        jUnitReporter.eof();
    }

    private void buildFeatureElementRunners() {


        for (CucumberTagStatement cucumberTagStatement : cucumberFeature.getFeatureElements()) {
            for(ParametrizedRuntime parameter : parameters) {
                try {
                    ParentRunner featureElementRunner;
                    if (cucumberTagStatement instanceof CucumberScenario) {
                        featureElementRunner = new ExecutionUnitRunner(parameter.getRuntime(),
                                fork(parameter, (CucumberScenario) cucumberTagStatement), jUnitReporter);
                    } else {
                        throw new UnsupportedOperationException("todo: ScenarioOutline");
//                        featureElementRunner = new ParametrizedScenarioOutlineRunner(parameter,
//                                (CucumberScenarioOutline) cucumberTagStatement, jUnitReporter);
                    }
                    children.add(featureElementRunner);
                } catch (InitializationError e) {
                    throw new CucumberException("Failed to create scenario runner", e);
                }
            }
        }
    }

    private CucumberScenario fork(ParametrizedRuntime parameter, CucumberScenario scenario) {
        CucumberScenario copy = new CucumberScenario(cucumberFeature, 
                clone(scenario.getCucumberBackground()), 
                fork(parameter, (Scenario)scenario.getGherkinModel()));

        for (Step step : scenario.getSteps()) {
            copy.step(clone(step));
        }
        return copy;
    }

    private Scenario fork(ParametrizedRuntime parameter, Scenario scenario) {
        return new Scenario(
                scenario.getComments(), 
                scenario.getTags(), 
                scenario.getKeyword(), 
                parameter.decorateName(scenario.getName()),
                scenario.getDescription(), 
                scenario.getLine(),
                scenario.getId());
    }


    private Step clone(Step step) {
        return new Step(
                step.getComments(),
                step.getKeyword(),
                step.getName(),
                step.getLine(),
                step.getRows(),
                step.getDocString()
        );
    }
    
    private CucumberBackground clone(CucumberBackground background) {
        // TODO:
        return background;
    }
}
