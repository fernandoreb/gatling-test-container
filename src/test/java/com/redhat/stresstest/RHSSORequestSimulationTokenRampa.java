package com.redhat.stresstest;

import io.gatling.javaapi.core.ScenarioBuilder;
import io.gatling.javaapi.core.Simulation;
import io.gatling.javaapi.http.HttpProtocolBuilder;

import java.time.Duration;

import static com.redhat.stresstest.PerfTestConfig.*;
import static io.gatling.javaapi.core.CoreDsl.*;
import static io.gatling.javaapi.http.HttpDsl.http;
import static io.gatling.javaapi.http.HttpDsl.status;

public class RHSSORequestSimulationTokenRampa extends Simulation {

        HttpProtocolBuilder httpProtocol = http.baseUrl(BASE_URL)
                        .check(status().is(200));

        ScenarioBuilder scn = scenario("Token Rampa endpoint calls")
                        .exec(http("token rampa endpoint").post("/auth/realms/" + REALM + "/protocol/openid-connect/token")
                                        .header("Content-type", "application/x-www-form-urlencoded")
                                        .formParam("grant_type", "client_credentials").asFormUrlEncoded()
                                        .formParam("client_id", CLIENT).asFormUrlEncoded()
                                        .formParam("client_secret", SECRET).asFormUrlEncoded());
        {
                setUp(scn.injectOpen(
                                 //constantUsersPerSec(REQUEST_PER_SECOND).during(Duration.ofMinutes(DURATION_MIN))
                                 rampUsersPerSec(REQUEST_PER_SECOND/2).to(REQUEST_PER_SECOND).during(Duration.ofMinutes(DURATION_MIN)).randomized()
                                 ))
                                .protocols(httpProtocol)
                                .assertions(global().responseTime().percentile3().lt(P95_RESPONSE_TIME_MS),
                                                global().successfulRequests().percent().gt(95.0));
        }

}
