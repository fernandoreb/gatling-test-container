package com.redhat.stresstest;

import static com.redhat.stresstest.SystemPropertiesUtil.*;

public final class PerfTestConfig {
    public static final String BASE_URL = getAsStringOrElse("BASE_URL", "http://keycloak-http.rhsso.svc.cluster.local:8080");
    public static final double REQUEST_PER_SECOND = getAsDoubleOrElse("REQUEST_PER_SECOND", 10f);
    public static final long DURATION_MIN = getAsIntOrElse("DURATION_MIN", 1);
    public static final int P95_RESPONSE_TIME_MS = getAsIntOrElse("P95_RESPONSE_TIME_MS", 1000);
    public static final String CLIENT = getAsStringOrElse("CLIENT", "stress-test");
    public static final String SECRET = getAsStringOrElse("SECRET", "S0E91ilGyttHbJRNPXCsnxZQL8k8AnhT");
    public static final String REALM = getAsStringOrElse("REALM", "master");

    public static final String USER = getAsStringOrElse("USER", "teste");
    public static final String PASSWORD = getAsStringOrElse("PASSWORD", "teste1234");
}
