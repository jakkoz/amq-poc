# amq-check

Przykladowy projekt z dwoma serwisami:
- `service-one-spring` (Spring Boot + `JmsTemplate` + embedded ActiveMQ broker 5.18.3)
- `service-two-camel` (Camel, odbior z `one.request`, opoznienie, odpowiedz na `one.response`)
- `service-three-flooder` (Spring Boot CLI, zasypuje `one.response` wiadomosciami z losowym `JMSCorrelationID`)

## Wymagania
- Java 17
- Maven 3.9+

## Uruchomienie
W osobnych terminalach:

```bash
mvn -pl service-one-spring spring-boot:run
```

```bash
mvn -pl service-two-camel spring-boot:run
```

Opcjonalnie uruchom flooder (jednorazowo wysyla 7000 wiadomosci na `one.response`):

```bash
mvn -pl service-three-flooder spring-boot:run
```

## Test
Wywolaj endpoint pierwszego serwisu:

```bash
curl "http://localhost:8081/api/send?message=test-123"
```

Oczekiwany format odpowiedzi:

```text
processed by camel: test-123
```

## Szczegoly implementacji
- Kolejki: `one.request`, `one.response`
- Request jest wysylany i obslugiwany synchronicznie przez dedykowany watek (`request-sender-thread`)
- Response jest odbierany przez `JmsTemplate.receiveSelected(...)` z selektorem `JMSCorrelationID = '...'`
- `service-one-spring` uzywa `activemq-jms-pool` z `maxConnections=1`
- Route Camel wysyla odpowiedz jako `InOnly` i usuwa `JMSReplyTo`, aby nie uruchamiac request-reply po stronie Camel
- `service-three-flooder` wysyla domyslnie `7000` wiadomosci (`flooder.messages`) z losowym `JMSCorrelationID` na `one.response`
- Broker uruchamia sie w `service-one-spring` jako embedded broker na `tcp://localhost:61616`
