1- command to install kafka in docker- https://www.geeksforgeeks.org/big-data/how-to-setup-kafka-on-docker/
docker compose -f kafka-installation-docker-compose.yml down
docker compose -f kafka-installation-docker-compose.yml up -d
docker compose -f kafka-installation-docker-compose.yml logs kafka --tail=50
docker compose -f kafka-installation-docker-compose.yml exec kafka kafka-topics.sh --list --zookeeper zookeeper:2181

docker network create kafka-network
5faa34e9eee29e5c64975f2bd513a3f0c96dd12380bee084e1afddb45c595d12

2- Run a Zookeeper container using Docker and The Zookeeper is a prerequisite for running Kafka.
docker run -d --name zookeeper --network kafka-network -e ZOOKEEPER_CLIENT_PORT=2181 confluentinc/cp-zookeeper:latest
5b3b86fe0593dccbb0ee2dff88324fd1fe17e91959111565c6ccb10331f67364

3- Run a Kafka container and link it to the Zookeeper container.
docker run -d --name kafka --network kafka-network -e KAFKA_ZOOKEEPER_CONNECT=zookeeper:2181 -e KAFKA_ADVERTISED_LISTENERS=PLAINTEXT://localhost:9092 -e KAFKA_BROKER_ID=1 -e KAFKA_OFFSETS_TOPIC_REPLICATION_FACTOR=1 confluentinc/cp-kafka:latest
652ebadae6042568ea9279324b47d92a653160313f85e218316ab767e7ceeeb5


