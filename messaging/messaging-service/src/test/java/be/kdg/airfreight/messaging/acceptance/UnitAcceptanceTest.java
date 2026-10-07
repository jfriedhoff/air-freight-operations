package be.kdg.airfreight.messaging.acceptance;

import org.junit.platform.suite.api.*;

import static io.cucumber.junit.platform.engine.Constants.*;

@Suite
@IncludeEngines("cucumber")
@SelectPackages("features.messaging")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "be.kdg.airfreight.messaging.acceptance.unit")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@unit and not @to-confirm and not @wip")
@ConfigurationParameter(key = OBJECT_FACTORY_PROPERTY_NAME, value = "io.cucumber.picocontainer.PicoFactory")
class UnitAcceptanceTest {
}
