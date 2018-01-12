#!/usr/bin/env bash

# Function to get local local scope for name
add_jdbc_resource_from_url "jdbc/rawrepo" ${RAWREPO_URL##* }
add_jdbc_resource_from_url "jdbc/holdingsitems" ${HOLDINGS_ITEMS_URL##* }