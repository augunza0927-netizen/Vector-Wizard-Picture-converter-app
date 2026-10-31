package com.vectorconvert;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;
import org.junit.platform.suite.api.SuiteDisplayName;


@Suite
@SuiteDisplayName("Complete All In-] One Project Test Suite")
@SelectClasses({
        ToolingVerificationTest.class,
        ZipExtractionServiceTest.class,
        VectorizationServiceTest.class,
        NavigationAndPreviewTest.class,
        ConfigurationAndInstantPreviewTest.class,
        BatchProcessingManagerTest.class,
        AdditionalFeaturesTest.class,
        ExportAllIntegrationTest.class
})
public class AllInOneTestSuite {
}
