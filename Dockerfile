FROM docker.dbc.dk/payara5-micro:latest

LABEL RAWREPO_URL="Full connection string for the rawrepo database. Format is 'username:pass@dbserver:port/dbname'. (Required)"
LABEL HOLDINGS_ITEMS_URL="Full connection string for the holdings items database. Format is 'username:pass@dbserver:port/dbname'. (Required)"
LABEL VIPCORE_ENDPOINT="Url to vipcore. (Required)"
LABEL RAWREPO_RECORD_SERVICE_URL="Url to rawrepo record service. (Required)"
LABEL INSTANCE_NAME="Name of the server or brønd which the docker is using"

COPY hydra-api/target/hydra-api-1.0-SNAPSHOT.war app.json deployments/