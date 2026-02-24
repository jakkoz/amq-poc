# amq-check

Przykladowy projekt z trzema serwisami:
- `service-one-spring` (Spring Boot + `JmsTemplate` + embedded ActiveMQ broker, request-reply przez `TemporaryQueue`)
- `service-two-camel` (Camel, odbior z `one.request`, opoznienie, odpowiedz na `JMSReplyTo`)
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
- Request queue: `one.request`
- Reply queue: `TemporaryQueue` tworzona per request (ustawiana w `JMSReplyTo`)
- Request jest wysylany i obslugiwany synchronicznie przez dedykowany watek (`request-sender-thread`)
- Response jest odbierany synchronicznie z `TemporaryQueue` (bez selectorow na shared reply queue)
- `service-one-spring` uzywa `activemq-jms-pool` z `maxConnections=1`
- Route Camel odsyla odpowiedz natywnie na `JMSReplyTo`
- `service-three-flooder` wysyla domyslnie `7000` wiadomosci (`flooder.messages`) z losowym `JMSCorrelationID` na `one.response`
- Broker uruchamia sie w `service-one-spring` jako embedded broker na `tcp://localhost:61616`
