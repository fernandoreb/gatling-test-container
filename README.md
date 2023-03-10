Para rodar

~~~
NAMESPACE=rhsso-teste
~~~

~~~
oc adm policy add-scc-to-user anyuid -z default -n ${NAMESPACE}
~~~

~~~
oc apply -f app-manifest.yaml -n ${NAMESPACE}
~~~

Montagem da imagem - se necessário - substituir vrf
~~~
mvn clean package
podman build . -t quay.io/rh_ee_fguimara/rh-sso-gatling-test:x.y.z
podman push quay.io/rh_ee_fguimara/rh-sso-gatling-test:x.y.z
~~~

Criando porta 8080 no rh-sso
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

Executar teste a partir do terminal
~~~
curl --location --request POST 'http://localhost:8080/api/stresstest' --header 'Content-Type: application/json' --data-raw '{ "name": "teste1" }'
~~~

Relatorio
~~~
https://report-rhsso-teste.apps.cluster-zvgbt.zvgbt.sandbox636.opentlc.com/api/stresstest/download?report=teste1
~~~

Testar keycloak
~~~
curl --location --request POST 'http://keycloak-http.rhsso.svc.cluster.local:8080/auth/realms/teste/protocol/openid-connect/token' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'grant_type=password' \
--data-urlencode 'client_id=stress-test' \
--data-urlencode 'client_secret=cEs9QvpTR3qbFYM8T71vHvdPr3SA7dmb' \
--data-urlencode 'username=teste' \
--data-urlencode 'password=teste1234'
~~~

~~~
curl --location --request POST 'http://keycloak-http.rhsso.svc.cluster.local:8080/auth/realms/teste/protocol/openid-connect/token' \
--header 'Content-Type: application/x-www-form-urlencoded' \
--data-urlencode 'grant_type=client_credentials' \
--data-urlencode 'client_id=stress-test' \
--data-urlencode 'client_secret=cEs9QvpTR3qbFYM8T71vHvdPr3SA7dmb'
~~~