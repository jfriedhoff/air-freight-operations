package be.kdg.airfreight.flightoperations.acceptance;

import org.junit.platform.suite.api.*;

import static io.cucumber.junit.platform.engine.Constants.*;

@Suite
@IncludeEngines("cucumber")
@SelectPackages("features.flightoperations")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "be.kdg.airfreight.flightoperations.acceptance.integration")
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "@integration and not @to-confirm and not @wip")
@ConfigurationParameter(key = OBJECT_FACTORY_PROPERTY_NAME, value = "io.cucumber.spring.SpringFactory")
class IntegrationAcceptanceTest {
}