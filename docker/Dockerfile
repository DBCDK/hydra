FROM docker.dbc.dk/payara-micro:latest

ADD config.d/* config.d
ADD *.war wars/

ENV LOGBACK_FILE file:///data/logback-include-stdout.xml

LABEL RAWREPO_URL="Full connection string for the rawrepo database. Format is 'connection-name username:pass@dbserver/dbname'. (Required)" \
      HOLDINGS_ITEMS_URL="Full connection string for the holdings items database. Format is 'connection-name username:pass@dbserver/dbname'. (Required)" \
      OPENAGENCY_URL="Url to openagency. (Required)" \
      OPENAGENCY_CACHE_AGE="Amount of hours the caching should last. Default is '8'. '0' will disable the cache. (Optional)" \
      OPENAGENCY_CONNECT_TIMEOUT="Default is '1000'. (Optional)" \
      OPENAGENCY_REQUEST_TIMEOUT="Default is '3000'. (Optional)" \
      INSTANCE_NAME="Name of the server or brønd which the docker is using"