Teste de Carga em Containers com Gatling

Esta aplicação utiliza o framework gatling para a realização de testes de carga. Os testes aqui definidos foram utilizados aplicar uma carga no endpoint de token do RH-SSO/Keycloak. Mas podem ser utilizados para testar outras APIs.

Maiores informações:  
https://gatling.io/

O Gatling basea-se na implementação de classes de teste utilizando o framework para disparar as cargas.

No caso desta aplicação, criamos uma aplicação que expõe 2 endpoints:

1 - Endpoint que dispara o teste (que por baixo chama o teste unitário)
2 - Endpoint para coleta do relatório gerado pelo Gatling.

Como estamos falando de teste unitário (mvn gatling:test) o container precisa de acesso a internet para baixar os pacotes e depois executar os testes. Como forma de melhorar a performance, montamos um volume persistente para set utilizado pelo maven. Assim ficando mais rápida as execuções.

Exemplo de caso de teste escrito para testar o endpoint token com grant_type password
~~~
public class RHSSORequestSimulationTokenUser extends Simulation {

        HttpProtocolBuilder httpProtocol = http.baseUrl(BASE_URL)
                        .check(status().is(200));

        ScenarioBuilder scn = scenario("Token endpoint calls")
                        .exec(http("token endpoint").post("/auth/realms/" + REALM + "/protocol/openid-connect/token")
                                        .header("Content-type", "application/x-www-form-urlencoded")
                                        .formParam("grant_type", "password").asFormUrlEncoded()
                                        .formParam("client_id", CLIENT).asFormUrlEncoded()
                                        .formParam("client_secret", SECRET).asFormUrlEncoded()
                                        .formParam("username", USER).asFormUrlEncoded()
                                        .formParam("password", PASSWORD).asFormUrlEncoded());
        {
                setUp(scn.injectOpen(constantUsersPerSec(REQUEST_PER_SECOND)
                                .during(Duration.ofMinutes(DURATION_MIN))))
                                .protocols(httpProtocol)
                                .assertions(global().responseTime().percentile3().lt(P95_RESPONSE_TIME_MS),
                                                global().successfulRequests().percent().gt(95.0));
        }

}
~~~

Da forma como foi implementado, o teste irá executar todas as classes de teste contidas na pasta test. Pode-se renomear a extensão .java para evitar que o teste seja executado. Uma melhoria futura seria indicar no endpoint de início, qual caso de teste executar e suas parametrizações.

Se as classes forem alteradas é necessário gerar uma nova imagem:

Montagem da imagem - se necessário - substituir vrf e atentar ao registry
~~~
mvn clean package
podman build . -t quay.io/rh_ee_fguimara/rh-sso-gatling-test:x.y.z
podman push quay.io/rh_ee_fguimara/rh-sso-gatling-test:x.y.z
~~~

Montagem do ambiente no cluster

Criar uma namespace

Para facilitar usaremos em uma ENV
~~~
NAMESPACE=rhsso-teste
~~~

A policy abaixo é necessária para que a aplicações possa escrever no file system
~~~
oc adm policy add-scc-to-user anyuid -z default -n ${NAMESPACE}
~~~

Pode-se criar uma rota porta 8080 no rh-sso para testar um fluxo diretamente em um service (sem passar pelo ingress)
~~~
apiVersion: v1
kind: Service
metadata:
  name: keycloak-http
spec:
  ports:
    - name: keycloak
      protocol: TCP
      port: 8080
      targetPort: 8080
  selector:
    app: keycloak
    component: keycloak
~~~

Parametrizações

O arquivo *app-manifest.yaml* contém uma sessão de environments que deve ser atualizada de acordo as configurações do rhsso.
~~~
          env:
            - name: BASE_URL // URL do RHSSO. Pode-se utilizar a rota https externa
              value: "http://keycloak-http.rhsso.svc.cluster.local:8080"
            - name: REQUEST_PER_SECOND // deve-se manter o f no final
              value: "10f"
            - name: DURATION_MIN //duração do teste
              value: "1"
            - name: P95_RESPONSE_TIME_MS // Percepção 95 para interromper o teste
              value: "1000"
            - name: CLIENT //client do realm a ser utilizado no teste
              value: "stress-test"
            - name: SECRET //secret do client
              value: "cEs9QvpTR3qbFYM8T71vHvdPr3SA7dmb"
            - name: REALM // nome do realm
              value: "teste"
            - name: USER // usuário 
              value: "teste"
            - name: PASSWORD // e senha
              value: "teste1234"
~~~
Lembrando de desmarcar a opção de troca de senha na criação do usuário no rhsso


Criando a aplicação
~~~
oc apply -f app-manifest.yaml -n ${NAMESPACE}
~~~

Executar teste a partir do terminal da aplicação criada
~~~
curl --location --request POST 'http://localhost:8080/api/stresstest' --header 'Content-Type: application/json' --data-raw '{ "name": "teste1" }'
~~~

O Gatling gera um report com o resultado do teste. Pode-se export a rota da aplicação e obter pelo path: 
~~~
/api/stresstest/download?report=<nome informado na execução - campo name>
~~~

Exemplo solicitando relatorio (via browser)
~~~
https://report-rhsso-teste.apps.cluster-zvgbt.zvgbt.sandbox636.opentlc.com/api/stresstest/download?report=teste1
~~~

A execução também pode ser realizada a patir da rota externa.

Exemplo:
~~~
curl --location --request POST 'https://report-rhsso-teste.apps.cluster-zvgbt.zvgbt.sandbox636.opentlc.com/api/stresstest' --header 'Content-Type: application/json' --data-raw '{ "name": "teste1" }'
~~~

Para validar os acessos, pode-se testar keycloak diretamente com os parãmetros a serem utilizados no teste de carga.

Exemplo - grant_type = password
~~~
curl --location --request POST 'http://keycloak-http.rhsso.svc.cluster.local:8080/auth/realms/teste/protocol/openid-connect/token' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'grant_type=password' \
--data-urlencode 'client_id=stress-test' \
--data-urlencode 'client_secret=cEs9QvpTR3qbFYM8T71vHvdPr3SA7dmb' \
--data-urlencode 'username=teste' \
--data-urlencode 'password=teste1234'
~~~

Exemplo - grant_type = client_credentials
~~~
curl --location --request POST 'http://keycloak-http.rhsso.svc.cluster.local:8080/auth/realms/teste/protocol/openid-connect/token' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'grant_type=client_credentials' \
--data-urlencode 'client_id=stress-test' \
--data-urlencode 'client_secret=cEs9QvpTR3qbFYM8T71vHvdPr3SA7dmb'
~~~