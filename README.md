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
podman build . -t quay.io/rh_ee_fguimara/rh-sso-gatling-test:1.0.5
podman push quay.io/rh_ee_fguimara/rh-sso-gatling-test:1.0.5
~~~

Criando porta 8080 no rh-sso
~~~
apiVersion: v1
kind: Service
metadata:
  name: keycloak-http
  namespace: rhsso
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

~~~
https://rh-sso-gatling-app-test.apps.cluster-5ckz7.5ckz7.sandbox118.opentlc.com/api/stresstest/download?report=teste1
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