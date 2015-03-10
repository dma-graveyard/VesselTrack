
if [ -z "${SLAVE}" ]; then
	SLAVEP=""
else
	SLAVEP="--slave=${SLAVE}"
fi

if [ -z "${SERVER_PORT}" ]; then
	SERVER_PORTP=""
else
	SERVER_PORTP="--server.port=${SERVER_PORT}"
fi

if [ -z "${DB_URL}" ]; then
	DB_URLP=""
else
	DB_URLP="--spring.datasource.url=${DB_URL}"
fi

if [ -z "${AISBUS_FILTER}" ]; then
	AISBUS_FILTER=""
else
	AISBUS_FILTERP="--aisbusFilter=\"${AISBUS_FILTER}\""
fi

LATEST=`ls /archive/target/vessel-track*SNAPSHOT.war`

java -jar $LATEST $SLAVEP $SERVER_PORTP $DB_URLP $AISBUS_FILTERP
