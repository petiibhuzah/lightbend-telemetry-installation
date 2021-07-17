#!/bin/bash

set -euo pipefail
IFS=$'\n\t'

NUM_CUSTOMERS=$1
PORT=$2

for i in $(seq 1 $NUM_CUSTOMERS)
do
    CUSTOMER_NAME=$(uuidgen)
    CUSTOMER_JSON='{"username": "'$CUSTOMER_NAME'", "fullname": "FULL NAME FOR '$CUSTOMER_NAME'", "email": "'$CUSTOMER_NAME'@example.com", "phone": "613-555-1212", "address": "'$i' Fake St." }'
    echo "---"
    curl -H "Content-type: application/json" -X POST -d $CUSTOMER_JSON http://localhost:$PORT/customers
    echo "---"
done