package scripta.software.core;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.core.env.Environment;

import scripta.software.common.configuration.AppConfig;
import scripta.software.common.model.ApplicationProperties;

@ExtendWith(MockitoExtension.class)
public class ScriptaApplicationTest {

    @Mock private Environment env;

    @Mock private ApplicationProperties applicationProperties;

    @InjectMocks private ScriptaApplication sPDFApplication;

    @Mock private AppConfig appConfig;

    @BeforeEach
    public void setUp() {
        ScriptaApplication.setServerPortStatic("8080");
    }

    @Test
    public void testSetServerPortStatic() {
        ScriptaApplication.setServerPortStatic("9090");
        assertEquals("9090", ScriptaApplication.getStaticPort());
    }

    @Test
    public void testGetStaticPort() {
        assertEquals("8080", ScriptaApplication.getStaticPort());
    }

    @Test
    public void testSetServerPortStaticAuto() {
        ScriptaApplication.setServerPortStatic("auto");
        assertEquals("0", ScriptaApplication.getStaticPort());
    }

    @Test
    public void testInit() {
        when(appConfig.getBackendUrl()).thenReturn("http://localhost");
        when(appConfig.getContextPath()).thenReturn("/app");
        when(appConfig.getServerPort()).thenReturn("8080");

        sPDFApplication.init();

        assertEquals("http://localhost:8080", ScriptaApplication.getStaticBaseUrl());
        assertEquals("/app", ScriptaApplication.getStaticContextPath());
        assertEquals("8080", ScriptaApplication.getStaticPort());
    }

    // Tests for getActiveProfile removed - method is now private
}
