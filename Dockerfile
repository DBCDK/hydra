FROM docker.dbc.dk/payara5-micro:latest

LABEL RAWREPO_URL="Full connection string for the rawrepo database. Format is 'username:pass@dbserver:port/dbname'. (Required)"
LABEL HOLDINGS_ITEMS_URL="Full connection string for the holdings items database. Format is 'username:pass@dbserver:port/dbname'. (Required)"
LABEL OPENAGENCY_URL="Url to openagency. (Required)"
LABEL OPENAGENCY_CACHE_AGE="Amount of hours the caching should last. Default is '8'. '0' will disable the cache. (Optional)"
LABEL OPENAGENCY_CONNECT_TIMEOUT="Default is '1000'. (Optional)"
LABEL OPENAGENCY_REQUEST_TIMEOUT="Default is '3000'. (Optional)"
LABEL INSTANCE_NAME="Name of the server or brønd which the docker is using"

COPY hydra-api/target/hydra-api-1.0-SNAPSHOT.war app.json deployments/